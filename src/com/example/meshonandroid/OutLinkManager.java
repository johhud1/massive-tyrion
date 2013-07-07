package com.example.meshonandroid;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Observable;
import java.util.Observer;

import org.apache.http.Header;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.impl.DefaultHttpRequestFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.DefaultClientConnection;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.DefaultedHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;

import proxyServer.ApacheRequestFactory;

import com.example.meshonandroid.pdu.DataMsg;
import com.example.meshonandroid.pdu.DataRepMsg;
import com.example.meshonandroid.pdu.ExitNodeRepPDU;
import com.example.meshonandroid.pdu.ExitNodeReqPDU;
import com.example.meshonandroid.pdu.MeshPduInterface;

import adhoc.aodv.Node;
import adhoc.aodv.pdu.AodvPDU;
import android.util.Base64;
import android.util.Log;
import android.util.SparseIntArray;



public class OutLinkManager implements Observer {

    private boolean mHaveData = true;
    private Node mNode;
    private int mContactId;
    private SparseIntArray mPortToContactID = new SparseIntArray();

    private static int BUFSIZE = 512;
    private byte[] responseBuf = new byte[BUFSIZE];


    /**
     * Constructor for creating a traffic manager.
     *
     * The traffic manager is responsible for responding to ExitNodeReqPDU's and
     * for managing the mapping from local ports to Mesh network ID's (in order
     * to forward traffic appropriately (think NAT))
     *
     * @param
     */
    public OutLinkManager(boolean haveData, Node node, int myID) {
        mContactId = myID;
        mNode = node;
        mHaveData = haveData;
    }


    public void connectionRequested(int senderID, MeshPduInterface msg) {
        if (mHaveData) {
            setupForwardingConn(senderID, msg);
        } else {
            // ignore
        }
    }


    private void setupForwardingConn(int reqContactId, MeshPduInterface msg) {
        ExitNodeRepPDU rep = new ExitNodeRepPDU(mContactId, 0, msg.getBroadcastID());
        mNode.sendData(0, reqContactId, rep.toBytes());

    }


    @Override
    public void update(Observable arg0, Object m) {
        String tag = "TrafficManager:update";
        MeshPduInterface msg = (MeshPduInterface) m;
        Log.d(tag, "got update. msg: " + m.toString());
        switch (msg.getPduType()) {
        case Constants.PDU_DATAREQMSG:
            DataMsg dmsg = (DataMsg) msg;
            try {
                // got a data request, forward the request to outlink
                // Log.d(tag, "got data request, sending response: " +
                // fakeResp);
                String httpRequest =
                    new String(Base64.decode(dmsg.getDataBytes(), 0), Constants.encoding);
                Log.d(tag, "got PDU_DATAREQMSG: " + httpRequest);
                HttpRequest rq = ApacheRequestFactory.create(httpRequest);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DefaultHttpClient dhc = new DefaultHttpClient();
                try {
                    // dhc.execute(new HttpHost("192.168.1.1"), rq);
                    HttpHost targetHost = new HttpHost(rq.getFirstHeader("Host").getValue());
                    HttpResponse myresp = dhc.execute(targetHost, rq);
                    BufferedInputStream respStream =
                        new BufferedInputStream(myresp.getEntity().getContent());
                    StatusLine respSL = myresp.getStatusLine();
                    out.write((respSL.getProtocolVersion().toString()+" "+respSL.getStatusCode()+" "+respSL.getReasonPhrase()+"\n").getBytes(Constants.encoding));
                    Header[] headers = myresp.getAllHeaders();
                    for(Header h : headers){
                        out.write((h.getName()+": "+h.getValue()+"\n").getBytes(Constants.encoding));
                    }
                    out.write(("\r\n").getBytes(Constants.encoding));
                    long contLength = myresp.getEntity().getContentLength();
                    Log.d(tag, "response Entity length: " + contLength);
                    Header hContLength = myresp.getHeaders("Content-length")[0];
                    int bufLength = new Integer(hContLength.getValue());
                    int redd = 0;
                    int offset = 0;
                    if (contLength > 0) {
                        // write the response from responseBuf to out
                        responseBuf = new byte[bufLength];
                        while ((redd = respStream.read(responseBuf, 0, bufLength)) != -1) {
                            out.write(responseBuf, 0, redd);
                            // offset += redd;
                        }
                    } else {
                        while ((redd = respStream.read(responseBuf, 0, BUFSIZE)) > 0) {
                            out.write(responseBuf, 0, redd);
                        }
                    }

                    // respStream.read(responseBuf);
                    // Log.d(tag, new String(responseBuf, Constants.encoding));
                } catch (ClientProtocolException e2) {
                    // TODO Auto-generated catch block
                    e2.printStackTrace();
                } catch (IOException e2) {
                    // TODO Auto-generated catch block
                    e2.printStackTrace();
                }
                /*
                 * DefaultHttpClientConnection hcc = new
                 * DefaultHttpClientConnection(); try { hcc.bind(new
                 * Socket(rq.getRequestLine().getUri(), 80), rq.getParams());
                 * HttpContext hc = new BasicHttpContext(); HttpRequestExecutor
                 * hre = new HttpRequestExecutor(); HttpResponse resp; resp =
                 * hre.execute(rq, hcc, hc); } catch (IOException e1) { // TODO
                 * Auto-generated catch block e1.printStackTrace(); } catch
                 * (HttpException e) { // TODO Auto-generated catch block
                 * e.printStackTrace(); }
                 */

                // base64 encode out (holding response data) and send it back to
                // originator
                int pid = dmsg.getPacketID() + 1;
                if (out.size() > adhoc.aodv.Constants.MAX_PACKAGE_SIZE) {
                    Log.e("OutLink Manager", "user data to large to send over aodv");
                } else {
                    DataMsg respData =
                        new DataRepMsg(mContactId, pid, msg.getBroadcastID(), Base64.encode(out
                            .toByteArray(), 0));
                    mNode.sendData(pid, dmsg.getSourceID(), respData.toBytes());
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            break;
        default:
            Log.d(tag, "got something not PDU_DATAREQMSG");
        }

    }
}
