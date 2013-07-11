package proxyServer;

import java.net.*;
import java.io.*;
import java.util.*;

import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.ProtocolVersion;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.io.HttpResponseWriter;
import org.apache.http.io.HttpMessageWriter;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.DefaultedHttpContext;

import com.example.meshonandroid.Constants;
import com.example.meshonandroid.pdu.AODVObserver;
import com.example.meshonandroid.pdu.DataMsg;
import com.example.meshonandroid.pdu.DataReqMsg;
import com.example.meshonandroid.pdu.ExitNodeReqPDU;
import com.example.meshonandroid.pdu.MeshPduInterface;

import adhoc.aodv.Node;
import android.util.Base64;
import android.util.Log;



public class ProxyThread extends Thread implements Observer {
    private static final int MAX_PACKETS = 20 ;
    private byte[][] packetBuf;

    private Socket socket = null;
    private Node node;

    //
    private DataOutputStream out;
    private String httpRequest;
    private AODVObserver mAodvObs;
    private int broadcastId;
    private int contactID;
    private int recievedPackets = 0;
    private int expectedRespPackets = 1;


    public ProxyThread(Socket socket, Node node, AODVObserver aodvObs, int reqNumber) {
        super("ProxyThread");
        this.socket = socket;
        this.node = node;
        aodvObs.addObserver(this);
        mAodvObs = aodvObs;
        this.broadcastId = reqNumber;
        packetBuf = new byte[MAX_PACKETS][];
    }


    public ProxyThread(Socket accept, Node node2, AODVObserver aodvobs, int reqNumber2,
                       int getContact) {
        this(accept, node2, aodvobs, reqNumber2);
        contactID = getContact;
    }


    public void run() {
        try {
            out = new DataOutputStream(socket.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String inputLine, outputLine;
            int cnt = 0;
            String urlToCall = "";
            httpRequest = new String();
            // /////////////////////////////////
            // begin get request from client
            while ((inputLine = in.readLine()) != null) {
                try {
                    httpRequest += inputLine + '\n';
                    StringTokenizer tok = new StringTokenizer(inputLine);
                    tok.nextToken();
                } catch (Exception e) {
                    break;
                }
                // parse the first line of the request to find the url
                if (cnt == 0) {
                    String[] tokens = inputLine.split(" ");
                    urlToCall = tokens[1];
                    // can redirect this to output log
                    System.out.println("Request for : " + urlToCall);
                }

                cnt++;
            }
            // end get request from client

            // using contactManager, contactID should be set to valid hasData
            // mesh node. send data straight off.

            try {
                DataMsg dreq =
                    new DataReqMsg(node.getNodeAddress(), 0, broadcastId, Base64.encode(httpRequest
                        .getBytes(Constants.encoding), 0));
                String junk = dreq.toString();
                byte[] b = dreq.toBytes();
                DataReqMsg asd = new DataReqMsg();
                asd.parseBytes(b);
                node.sendData(1, contactID, dreq.toBytes());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            // BEGIN OLD BROADCAST METHOD
            // begin send request to mesh network
            // just gonna broadcast the request, anyone with a connection can
            // pick it up and respond
            // node.sendData(0, adhoc.aodv.Constants.BROADCAST_ADDRESS,
            // new ExitNodeReqPDU(node.getNodeAddress(), 0,
            // reqNumber).toBytes());
            // send client data in update (recieved notifications from
            // AODVObserver)
            // end send request to mesh network

        } catch (Exception e) {
            // can redirect this to error log
            System.err.println("Encountered exception: " + e);
        }
    }


    @Override
    public void update(Observable observable, Object data) {
        String tag = "ProxyThread:update";
        MeshPduInterface msg = (MeshPduInterface) data;
        Log.d(tag, "got msg: " + msg.toString());
        // split on two notifications here : (route request reply - in which
        // case send data to those addresses; and msg data in which case,
        // display returned data to client)
        if (checkIDNumber(msg.getBroadcastID())) {
            Log.d(tag, "BroadcastID matchs");
            switch (msg.getPduType()) {
            case Constants.PDU_DATAREPMSG:
                Log.d(tag, "got pdu_datamsg");
                DataMsg dmsg = (DataMsg) msg;

                packetBuf[recievedPackets] = dmsg.getDataBytes(); // undecoded
                                                                  // b64 bytes
                recievedPackets++;

                if (recievedPackets == dmsg.getNumRespPackets()) {
                    byte[] reconstructedResp;
                    ByteArrayOutputStream bOS = new ByteArrayOutputStream();
                    try {
                        int offset = 0;
                        for (int i = 0; i < recievedPackets; i++) {
                            bOS.write(packetBuf[i]);
                        }

                        out.write(Base64.decode(bOS.toByteArray(), 0));
                        out.flush();
                        if (socket != null) {
                            // done. close out the socket, and remove this as an
                            // aodv
                            // observer
                            mAodvObs.deleteObserver(this);
                            socket.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case Constants.PDU_EXITNODEREP:
                break;
            case Constants.PDU_EXITNODEREQ:
                Log.d(tag, "got PDU_EXITNODEREQ");
                // do nothing
                break;
            default:
                Log.d(tag, "default switch");
            }
        } else {
            // broadcast ID doesn't match our own, packet must be for different
            // request/proxythread
            Log.d(tag,
                  "BroadcastId doesn't match. Ours:" + broadcastId + " found:"
                      + msg.getBroadcastID());
        }
    }


    private boolean checkIDNumber(int i) {
        return (i == broadcastId);
    }

}
