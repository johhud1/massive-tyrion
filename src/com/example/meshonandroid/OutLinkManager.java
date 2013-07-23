package com.example.meshonandroid;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.impl.client.DefaultHttpClient;

import proxyServer.ApacheRequestFactory;
import adhoc.aodv.Node;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.util.SparseIntArray;

import com.example.meshonandroid.pdu.DataMsg;
import com.example.meshonandroid.pdu.DataRepMsg;
import com.example.meshonandroid.pdu.ExitNodeRepPDU;
import com.example.meshonandroid.pdu.MeshPduInterface;



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

    private boolean mActiveNode = false;
    private Node mNode;
    private int mContactId;
    private SparseIntArray mPortToContactID = new SparseIntArray();
    private Handler msgHandler;
    private Context mContext;

    private static int BUFSIZE = 512;
    private byte[] responseBuf;


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
            // TODO Auto-generated catch block
            e3.printStackTrace();
        }
        switch (msg.getPduType()) {
        case Constants.PDU_DATAREQMSG:
            DataMsg dmsg = (DataMsg) msg;
            int pid = dmsg.getPacketID();
            // decode the http request
            String httpRequest;
            try {
                httpRequest = new String(Base64.decode(dmsg.getDataBytes(), 0), Constants.encoding);
                Log.d(tag, "got PDU_DATAREQMSG: " + httpRequest);
                try {
                    // create httpRequest from string representation
                    HttpRequest rq = ApacheRequestFactory.create(httpRequest);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    DefaultHttpClient dhc = new DefaultHttpClient();
                    // execute requested http request
                    HttpHost targetHost = new HttpHost(rq.getFirstHeader("Host").getValue());
                    HttpResponse myresp = dhc.execute(targetHost, rq);

                    // parse out the response, putting it into out stream
                    // handle statusline and headers
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
                    // handle response content, if any
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

                    // base64 encode out (holding response data) and send it
                    // back to
                    // originator
                    byte[] outBArray = Base64.encode(out.toByteArray(), 0);
                    sendForwardedTrafficMsg(outBArray.length);
                    if (outBArray.length > MAX_PAYLOAD_SIZE) {
                        // if response is too big to fit in one packet, chop it
                        // up.
                        int offset = 0;
                        int packs = (outBArray.length / MAX_PAYLOAD_SIZE) + 1;
                        Log.d(tag, "user data to large (" + outBArray.length
                                   + ") to send over aodv in 1 msg. splitting into " + packs
                                   + " msgs");
                        for (int i = 0; i < packs; i++) {
                            byte[] temp =
                                new byte[Math.min(MAX_PAYLOAD_SIZE, outBArray.length - offset)];
                            try {
                                temp = Arrays.copyOfRange(outBArray, offset, offset + temp.length);
                                DataMsg respData =
                                    new DataRepMsg(mContactId, pid, msg.getBroadcastID(), temp,
                                                   packs);
                                byte[] msgBytes = respData.toBytes();
                                mNode.sendData(pid, dmsg.getSourceID(), msgBytes);
                                pid++;
                                offset += MAX_PAYLOAD_SIZE;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        // response fits in one packet, send 'er off
                        DataMsg respData =
                            new DataRepMsg(mContactId, pid, msg.getBroadcastID(), outBArray);
                        mNode.sendData(pid, dmsg.getSourceID(), respData.toBytes());
                    }
                } catch (HttpException e2) {
                    //CONNECT methods cause an HttpException in the ApacheRequestFactory
                    //catch that, and if that was the cause of exception, handle appropriately.
                    String[] request = httpRequest.split(" ");
                    if (isConnectHttpRequest(request)) {
                        handleConnectRequest(request, dmsg);
                    } else {
                        e2.printStackTrace();
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } catch (UnsupportedEncodingException e4) {
                // TODO Auto-generated catch block
                e4.printStackTrace();
            }
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
        int BUF_SIZE = 30000;
        String[] addrPort = getAddrPort(request);
        byte[] responseBuf = new byte[BUF_SIZE];
        try {
            Socket s = new Socket(addrPort[0], Integer.parseInt(addrPort[1]));
            BufferedInputStream bis = new BufferedInputStream(s.getInputStream());
            DataRepMsg resp = makeHttpConnectResponse(dmsg.getBroadcastID());
            mNode.sendData(dmsg.getPacketID(), dmsg.getSourceID(), resp.toBytes());
            int redd = 0;
            while ((redd = bis.read(responseBuf)) != -1) {
                Log.d(tag, "got " + redd + " more bytes from: " + addrPort[0]);
            }
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // TODO: make a tcp connection to whatever the request says it should

    }


    private DataRepMsg makeHttpConnectResponse(int broadcastId) throws UnsupportedEncodingException {
        //ProtocolVersion pv = new ProtocolVersion("HTTP", 1, 1);
        //HttpResponse resp = new DefaultHttpResponseFactory().newHttpResponse(new BasicStatusLine(pv, HttpStatus.SC_OK, "OK"), null);
        String response = new String("HTTP/1.1 200 OK Connection Established\r\n\r\n");
        DataRepMsg ret = new DataRepMsg(mNode.getNodeAddress(), 0, broadcastId, response.getBytes(Constants.encoding));
        return ret;
    }


    private String[] getAddrPort(String[] httpRequest) {
        return httpRequest[1].split(":");
    }


    // forwarded traffic we are linking out to internet
    private void sendForwardedTrafficMsg(int length) {
        Message m = new Message();
        m.arg1 = Constants.FT_MSG_CODE;
        m.arg2 = length;
        msgHandler.sendMessage(m);
    }
}
