package com.example.meshonandroid;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;

import proxyServer.ConnectProxyThread;
import adhoc.aodv.Node;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.util.SparseIntArray;
import ch.boye.httpclientandroidlib.HttpRequest;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.ProtocolException;
import ch.boye.httpclientandroidlib.client.RedirectStrategy;
import ch.boye.httpclientandroidlib.client.methods.HttpUriRequest;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.impl.conn.PoolingClientConnectionManager;
import ch.boye.httpclientandroidlib.protocol.HttpContext;

import com.example.meshonandroid.pdu.ConnectDataMsg;
import com.example.meshonandroid.pdu.DataMsg;
import com.example.meshonandroid.pdu.ExitNodeRepPDU;
import com.example.meshonandroid.pdu.MeshPduInterface;



@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class OutLinkManager implements Observer {


    private boolean mActiveNode = false;
    private Node mNode;
    private int mContactId;
    private SparseIntArray mPortToContactID = new SparseIntArray();
    private Handler msgHandler;
    private Context mContext;
    private DefaultHttpClient dhc;

    private static int BUFSIZE = 512;
    private byte[] responseBuf;

    private Observable mAodvObserver;


    /**
     * Constructor for creating a traffic manager.
     *
     * The traffic manager is responsible for responding to ExitNodeReqPDU's and
     * for managing the mapping from local requests to Mesh network ID's (in
     * order to forward traffic appropriately (think NAT))
     *
     * @param
     */
    public OutLinkManager(boolean activeNode, Node node, int myID, Handler msgHandler, Context c) {
        mNode = node;
        mActiveNode = activeNode;
        this.msgHandler = msgHandler;
        mContext = c;
        dhc = new DefaultHttpClient(new PoolingClientConnectionManager());
        dhc.setRedirectStrategy(new RedirectStrategy() {

            @Override
            public boolean
                isRedirected(HttpRequest arg0, HttpResponse arg1, HttpContext arg2)
                                                                                   throws ProtocolException {
                return false;
            }


            @Override
            public HttpUriRequest
                getRedirect(HttpRequest arg0, HttpResponse arg1, HttpContext arg2) throws ProtocolException {
                return null;
            }
        });
    }


    public void connectionRequested(int senderID, MeshPduInterface msg) {
        String tag = "OutLinkManager:connectionRequested";
        if (mActiveNode && haveData()) {
            Log.d(tag, "got connection request, seting up forwarding, responded with exitnoderep");
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


    @Override
    public void update(Observable arg0, Object m) {
        String tag = "OutLinkManager:update";
        MeshPduInterface msg = (MeshPduInterface) m;
        try {
            Log.d(tag, "got update. msg: " + msg.toReadableString());
        } catch (UnsupportedEncodingException e3) {
            e3.printStackTrace();
        }
        switch (msg.getPduType()) {
        case Constants.PDU_DATAREQMSG:
            DataMsg dmsg = (DataMsg) msg;
            //int pid = dmsg.getPacketID();
            // decode the http request
            String httpRequest;
            try {
                httpRequest = new String(Base64.decode(dmsg.getDataBytes(), 0), Constants.encoding);
                Log.d(tag, "got PDU_DATAREQMSG: " + httpRequest);
                // CONNECT methods cause an HttpException in the
                // ApacheRequestFactory
                // catch that, and if that was the cause of exception, handle
                // appropriately.
                String[] request = httpRequest.split(" ");
                if (isConnectHttpRequest(request)) {
                    handleConnectRequest(request, dmsg);
                } else {
                        new Thread(new HttpFetcher(httpRequest, dmsg, mNode, msgHandler, dhc)).start();
                }
            } catch (UnsupportedEncodingException e4) {
                e4.printStackTrace();
            }
            break;
        case Constants.PDU_CONNECTDATAMSG:
            break;
        default:
            Log.d(tag, "got something not PDU_DATAREQMSG");
        }

    }


    private boolean isConnectHttpRequest(String[] request) {
        if ("CONNECT".equalsIgnoreCase(request[0])) { return true; }
        return false;
    }


    private void handleConnectRequest(String[] request, DataMsg dmsg) {
        String tag = "OutLinkManager:handleConnectRequest";

        String[] addrPort = getAddrPort(request);
        byte[] responseBuf = new byte[BUFSIZE];
        try {
            Socket targetConn = new Socket(addrPort[0], Integer.parseInt(addrPort[1]));
            new ConnectProxyThread(targetConn, mNode, dmsg, true, mAodvObserver, msgHandler).start();
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

    // THIS IS THE WORST THING I'VE EVER DONE. and there's more in AODVObservers
    // constructor. this madness should really be contained.
    public void setAODVObserver(Observable aodvObserver) {
        this.mAodvObserver = aodvObserver;

    }
}
