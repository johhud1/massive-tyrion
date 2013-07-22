package com.example.meshonandroid;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
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
import org.apache.http.client.params.ClientPNames;
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
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.util.SparseIntArray;



@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class OutLinkManager implements Observer {

    private static int MAX_PAYLOAD_SIZE = adhoc.aodv.Constants.MAX_PACKAGE_SIZE - (4 * 1000); // 8
                                                                                              // ints
                                                                                              // times
                                                                                              // 4
                                                                                              // bytes
                                                                                              // per
                                                                                              // int
                                                                                              // =
                                                                                              // total
                                                                                              // reserved
                                                                                              // bytes;

    private boolean mHaveData = true;
    private Node mNode;
    private int mContactId;
    private SparseIntArray mPortToContactID = new SparseIntArray();
    private Handler msgHandler;

    private static int BUFSIZE = 512;
    private byte[] responseBuf;


    /**
     * Constructor for creating a traffic manager.
     *
     * The traffic manager is responsible for responding to ExitNodeReqPDU's and
     * for managing the mapping from local ports to Mesh network ID's (in order
     * to forward traffic appropriately (think NAT))
     *
     * @param
     */
    public OutLinkManager(boolean haveData, Node node, int myID, Handler msgHandler) {
        //mContactId = myID;
        mNode = node;
        mHaveData = haveData;
        this.msgHandler = msgHandler;
    }


    public void connectionRequested(int senderID, MeshPduInterface msg) {
        if (mHaveData) {
            setupForwardingConn(senderID, msg);
        } else {
            // ignore
        }
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
            // TODO Auto-generated catch block
            e3.printStackTrace();
        }
        switch (msg.getPduType()) {
        case Constants.PDU_DATAREQMSG:
            DataMsg dmsg = (DataMsg) msg;
            try {
                //decode the http request
                String httpRequest =
                    new String(Base64.decode(dmsg.getDataBytes(), 0), Constants.encoding);
                Log.d(tag, "got PDU_DATAREQMSG: " + httpRequest);
                //create httpRequest from string representation
                HttpRequest rq = ApacheRequestFactory.create(httpRequest);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DefaultHttpClient dhc = new DefaultHttpClient();
                try {
                    //execute requested http request
                    HttpHost targetHost = new HttpHost(rq.getFirstHeader("Host").getValue());
                    HttpResponse myresp = dhc.execute(targetHost, rq);

                    //parse out the response, putting it into out stream
                    //handle statusline and headers
                    StatusLine respSL = myresp.getStatusLine();
                    out.write((respSL.getProtocolVersion().toString() + " "
                               + respSL.getStatusCode() + " " + respSL.getReasonPhrase() + "\n")
                        .getBytes(Constants.encoding));
                    Header[] headers = myresp.getAllHeaders();
                    for (Header h : headers) {
                        out.write((h.getName() + ": " + h.getValue() + "\n")
                            .getBytes(Constants.encoding));
                    }
                    out.write(("\r\n").getBytes(Constants.encoding));
                    //handle response content, if any
                    if (myresp.getEntity() != null) {
                        BufferedInputStream respStream =
                            new BufferedInputStream(myresp.getEntity().getContent());
                        long contLength = myresp.getEntity().getContentLength();
                        Log.d(tag, "response Entity length: " + contLength);
                        Header[] hs = myresp.getHeaders("Content-length");
                        int bufLength = 0;
                        if (hs.length > 0) {
                            Header hContLength = hs[0];// there may be a
                                                       // mismatch between
                                                       // contLength and
                                                       // buflength, could
                                                       // present a problem.
                                                       // idk.
                            bufLength = new Integer(hContLength.getValue());
                        }
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
                            responseBuf = new byte[BUFSIZE];
                            while ((redd = respStream.read(responseBuf, 0, BUFSIZE)) > 0) {
                                out.write(responseBuf, 0, redd);
                            }
                        }
                    }
                } catch (ClientProtocolException e2) {
                    e2.printStackTrace();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }

                // base64 encode out (holding response data) and send it back to
                // originator
                int pid = dmsg.getPacketID();
                byte[] outBArray = Base64.encode(out.toByteArray(), 0);
                sendForwardedTrafficMsg(outBArray.length);
                if (outBArray.length > MAX_PAYLOAD_SIZE) {
                    //if response is too big to fit in one packet, chop it up.
                    int offset = 0;
                    int packs = (outBArray.length / MAX_PAYLOAD_SIZE) + 1;
                    Log.d(tag, "user data to large (" + outBArray.length
                               + ") to send over aodv in 1 msg. splitting into " + packs + " msgs");
                    for (int i = 0; i < packs; i++) {
                        byte[] temp =
                            new byte[Math.min(MAX_PAYLOAD_SIZE, outBArray.length - offset)];
                        try {
                            temp = Arrays.copyOfRange(outBArray, offset, offset + temp.length);
                            DataMsg respData =
                                new DataRepMsg(mContactId, pid, msg.getBroadcastID(), temp, packs);
                            byte[] msgBytes = respData.toBytes();
                            mNode.sendData(pid, dmsg.getSourceID(), msgBytes);
                            pid++;
                            offset += MAX_PAYLOAD_SIZE;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    //response fits in one packet, send 'er off
                    DataMsg respData =
                        new DataRepMsg(mContactId, pid, msg.getBroadcastID(), outBArray);
                    mNode.sendData(pid, dmsg.getSourceID(), respData.toBytes());
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (HttpException e1) {
                e1.printStackTrace();
            }
            break;
        default:
            Log.d(tag, "got something not PDU_DATAREQMSG");
        }

    }

    //forwarded traffic we are linking out to internet
    private void sendForwardedTrafficMsg(int length) {
        Message m = new Message();
        m.arg1 = Constants.FT_MSG_CODE;
        m.arg2 = length;
        msgHandler.sendMessage(m);
    }
}
