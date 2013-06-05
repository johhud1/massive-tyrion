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

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
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
import com.example.meshonandroid.pdu.MeshPduInterface;

import adhoc.aodv.Node;
import android.util.Base64;
import android.util.Log;
import android.util.SparseIntArray;



public class TrafficManager implements Observer {

    private boolean mHaveData = true;
    private Node mNode;
    private int mContactId;
    private SparseIntArray mPortToContactID = new SparseIntArray();
    private String fakeResp = "HTTP/1.1 200 OK\n" + "Date: Mon, 23 May 2005 22:38:34 GMT\n"
                          + "Server: Apache/1.3.3.7 (Unix) (Red-Hat/Linux)\n"
                          + "Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n"
                          + "Etag: \"3f80f-1b6-3e1cb03b\"\n"
                          + "Content-Type: text/html; charset=UTF-8\n" + "Content-Length: 21\n"
                          + "Connection: close\n" + "request recieved\r\n\r\n";
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
    public TrafficManager(boolean haveData, Node node, int myID) {
        mContactId = myID;
        mNode = node;
        mHaveData = haveData;
    }


    public void connectionRequested(int reqContactId) {
        if (mHaveData) {
            setupForwardingConn(reqContactId);
        } else {
            // ignore
        }
    }


    private void setupForwardingConn(int reqContactId) {
        ExitNodeRepPDU rep = new ExitNodeRepPDU(mContactId, 0, 0);
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
                //Log.d(tag, "got data request, sending response: " + fakeResp);
                String httpRequest = new String(Base64.decode(dmsg.getDataBytes(), 0), Constants.encoding);
                Log.d(tag, "got data msg. Data (request?): "+httpRequest);
                HttpRequest rq = ApacheRequestFactory.create(httpRequest);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DefaultHttpClient dhc = new DefaultHttpClient();
                try {
                    //dhc.execute(new HttpHost("192.168.1.1"), rq);
                    HttpHost targetHost = new HttpHost(rq.getFirstHeader("Host").getValue());
                    HttpResponse myresp = dhc.execute(targetHost, rq);
                    BufferedInputStream respStream = new BufferedInputStream(myresp.getEntity().getContent());
                    long contLength = myresp.getEntity().getContentLength();
                    Log.d(tag, "response Entity length: "+contLength);
                    int bufLength = (int) contLength;
                    int offset = 0;
                    if(contLength > 0){
                        responseBuf = new byte[bufLength];
                        respStream.read(responseBuf);
                        out.write(responseBuf);
                    } else {
                        while(respStream.read(responseBuf, offset, BUFSIZE)>0){
                            out.write(responseBuf);
                        }
                    }

                    //respStream.read(responseBuf);
                    //Log.d(tag, new String(responseBuf, Constants.encoding));
                } catch (ClientProtocolException e2) {
                    // TODO Auto-generated catch block
                    e2.printStackTrace();
                } catch (IOException e2) {
                    // TODO Auto-generated catch block
                    e2.printStackTrace();
                }
                /*
                DefaultHttpClientConnection hcc = new DefaultHttpClientConnection();
                try {
                    hcc.bind(new Socket(rq.getRequestLine().getUri(), 80), rq.getParams());
                    HttpContext hc = new BasicHttpContext();
                    HttpRequestExecutor hre = new HttpRequestExecutor();
                    HttpResponse resp;
                    resp = hre.execute(rq, hcc, hc);
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (HttpException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }*/
                int pid = dmsg.getPacketID() + 1;
                DataMsg respData =
                    new DataRepMsg(mContactId, pid, 0, Base64.encode(out.toByteArray(), 0));
                mNode.sendData(pid, dmsg.getSourceID(), respData.toBytes());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            break;
        default:
            Log.d(tag, "got something not PDU_DATAREQMSG");
        }

    }
}
