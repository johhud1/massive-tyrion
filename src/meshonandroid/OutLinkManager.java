package meshonandroid;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import meshonandroid.pdu.AODVObserver;
import meshonandroid.pdu.ConnectDataMsg;
import meshonandroid.pdu.ConnectionClosedMsg;
import meshonandroid.pdu.DataMsg;
import meshonandroid.pdu.ExitNodeRepPDU;
import meshonandroid.pdu.MeshPduInterface;
import meshonandroid.pdu.Msg;

import proxyServer.ConnectProxyThread;
import adhoc.aodv.Node;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import ch.boye.httpclientandroidlib.HttpRequest;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.ProtocolException;
import ch.boye.httpclientandroidlib.client.RedirectStrategy;
import ch.boye.httpclientandroidlib.client.methods.HttpUriRequest;
import ch.boye.httpclientandroidlib.conn.ClientConnectionManager;
import ch.boye.httpclientandroidlib.conn.routing.HttpRoute;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.impl.conn.PoolingClientConnectionManager;
import ch.boye.httpclientandroidlib.pool.ConnPoolControl;
import ch.boye.httpclientandroidlib.protocol.HttpContext;




@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class OutLinkManager implements MeshMsgReceiver {

    private boolean mActiveNode = false;
    private Node mNode;
    private LocalBroadcastManager msgBroadcaster;
    private Context mContext;
    private DefaultHttpClient dhc;
    private SparseArray<HttpFetcher> fetcherMap = new SparseArray<HttpFetcher>();

    private AODVObserver mAodvObserver;


    /**
     * Constructor for creating an outbound traffic manager.
     *
     * This class handles both standard HTTP requests (ie GET) as well as the
     * proxy TCP port forwarding CONNECT HTTP request, as well as requests to
     * use this node as an exit point from the mesh. When receiving a HTTP
     * request, the OutLinkManager checks if its a CONNECT request. if so, it
     * spawns off a ConnectProxyThread to handle the tunneling and forwarding.
     * If it's a standard HTTP request, the manager spawns off a HTTPFetcher in
     * its own thread, to download the resource
     *
     * @param
     */
    public OutLinkManager(boolean activeNode, Node node, int myID, LocalBroadcastManager broadcaster, Context c) {
        mNode = node;
        mActiveNode = activeNode;
        this.msgBroadcaster = broadcaster;
        mContext = c;
        PoolingClientConnectionManager pccm = new PoolingClientConnectionManager();
        pccm.setMaxTotal(150);
        pccm.setDefaultMaxPerRoute(50);
        dhc = new DefaultHttpClient(pccm);
        dhc.setRedirectStrategy(new RedirectStrategy() {

            @Override
            public
                boolean
                isRedirected(HttpRequest arg0, HttpResponse arg1, HttpContext arg2)
                                                                                   throws ProtocolException {
                return false;
            }


            @Override
            public
                HttpUriRequest
                getRedirect(HttpRequest arg0, HttpResponse arg1, HttpContext arg2)
                                                                                  throws ProtocolException {
                return null;
            }
        });
    }


    public void connectionRequested(int senderID, MeshPduInterface msg) {
        String tag = "OutLinkManager:connectionRequested";
        if (mActiveNode && haveData()) {
            Log.d(tag, "got connection request from:"+senderID+" , seting up forwarding, responded with exitnoderep");
            setupForwardingConn(senderID, msg);

        } else {
            Log.d(tag,
                  "got a connection request, but we're either inactive or have no data. ignoring request");
        }
    }


    private boolean haveData() {
        ConnectivityManager connectivityManager =
            (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    private void setupForwardingConn(int reqContactId, MeshPduInterface msg) {
        ExitNodeRepPDU rep = new ExitNodeRepPDU(mNode.getNodeAddress(), 0, msg.getBroadcastID());
        mNode.sendData(0, reqContactId, rep.toBytes());

    }


    /*
     * @Override public void update(Observable arg0, Object m) { String tag =
     * "OutLinkManager:update"; MeshPduInterface msg = (MeshPduInterface) m; try
     * { Log.d(tag, "got update. msg: " + msg.toReadableString()); } catch
     * (UnsupportedEncodingException e3) { e3.printStackTrace(); } switch
     * (msg.getPduType()) { case Constants.PDU_DATAREQMSG: DataMsg dmsg =
     * (DataMsg) msg; //int pid = dmsg.getPacketID(); // decode the http request
     * String httpRequest; try { httpRequest = new
     * String(Base64.decode(dmsg.getDataBytes(), 0), Constants.encoding);
     * Log.d(tag, "got PDU_DATAREQMSG: " + httpRequest); String[] request =
     * httpRequest.split(" "); if (isConnectHttpRequest(request)) {
     * handleConnectRequest(request, dmsg); } else { HttpFetcher fetcher = new
     * HttpFetcher(httpRequest, dmsg, mNode, msgHandler, dhc);
     * fetcherMap.put(dmsg.getBroadcastID(), fetcher); new
     * Thread(fetcher).start(); } } catch (UnsupportedEncodingException e4) {
     * e4.printStackTrace(); } break; case Constants.PDU_CONNECTIONCLOSEMSG:
     * ConnectionClosedMsg CCMsg = (ConnectionClosedMsg) msg;
     * Log.d(OutLinkManager.class.getName(),
     * "closing connection. broadcastId: "+CCMsg.getBroadcastID()); HttpFetcher
     * toClose = fetcherMap.get(CCMsg.getBroadcastID()); toClose.connectionOpen
     * = false; fetcherMap.remove(CCMsg.getBroadcastID()); break; case
     * Constants.PDU_CONNECTDATAMSG: break; default: }
     *
     * }
     */

    private boolean isConnectHttpRequest(String[] request) {
        if ("CONNECT".equalsIgnoreCase(request[0])) { return true; }
        return false;
    }


    private void handleConnectRequest(String[] request, DataMsg dmsg) {
        String tag = "OutLinkManager:handleConnectRequest";

        String[] addrPort = getAddrPort(request);
        try {
            Socket targetConn = new Socket(addrPort[0], Integer.parseInt(addrPort[1]));
            ConnectProxyThread cpt = new ConnectProxyThread(targetConn, mNode, dmsg.getSourceID(), dmsg.getBroadcastID(), true, mAodvObserver, msgBroadcaster);
            mAodvObserver.addConnectProxyThread(dmsg.getBroadcastID(), cpt);
            cpt.start();
            // BufferedInputStream bis = new
            // BufferedInputStream(mConnectSocket.getInputStream());
            ConnectDataMsg resp = makeHttpConnectResponse(dmsg.getBroadcastID());
            mNode.sendData(dmsg.getPacketID(), dmsg.getSourceID(), resp.toBytes());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(tag, "error creating CONNECT connection to target host:" + addrPort[0] + ":"
                       + addrPort[1]);
            e.printStackTrace();
        }

    }

    //TODO: ugh this is awful. Summon the will to refactor things later so this doesn't happen :s
    public void setAODVObserver(AODVObserver obs){
        mAodvObserver = obs;
    }

    private ConnectDataMsg
        makeHttpConnectResponse(int broadcastId) throws UnsupportedEncodingException {
        String response = new String("HTTP/1.1 200 OK Connection Established\r\n\r\n");
        ConnectDataMsg ret =
            new ConnectDataMsg(mNode.getNodeAddress(), 0, broadcastId, Base64.encode(response
                .getBytes(Constants.encoding), 0), false, true);
        return ret;
    }


    private String[] getAddrPort(String[] httpRequest) {
        return httpRequest[1].split(":");
    }


    @Override
    public void handleMessage(MeshPduInterface msg) {
        switch (msg.getPduType()) {
        case Constants.PDU_DATAREQMSG:
            DataMsg dmsg = (DataMsg) msg;
            // int pid = dmsg.getPacketID();
            // decode the http request
            String httpRequest;
            try {
                httpRequest = new String(Base64.decode(dmsg.getDataBytes(), 0), Constants.encoding);
                Log.d(OutLinkManager.class.getName(), "got PDU_DATAREQMSG: " + httpRequest);
                String[] request = httpRequest.split(" ");
                if (isConnectHttpRequest(request)) {
                    handleConnectRequest(request, dmsg);
                } else {
                    HttpFetcher fetcher =
                        new HttpFetcher(httpRequest, dmsg, mNode, msgBroadcaster, dhc);
                    fetcherMap.put(dmsg.getBroadcastID(), fetcher);
                    new Thread(fetcher).start();
                }
            } catch (UnsupportedEncodingException e4) {
                e4.printStackTrace();
            }
            break;
        case Constants.PDU_CONNECTIONCLOSEMSG:
            ConnectionClosedMsg CCMsg = (ConnectionClosedMsg) msg;
            Log.d(OutLinkManager.class.getName(),
                  "closing connection. broadcastId: " + CCMsg.getBroadcastID());
            HttpFetcher toClose = fetcherMap.get(CCMsg.getBroadcastID());
            toClose.connectionOpen = false;
            fetcherMap.remove(CCMsg.getBroadcastID());
            break;
        case Constants.PDU_CONNECTDATAMSG:
            break;
        default:
        }
    }
}
