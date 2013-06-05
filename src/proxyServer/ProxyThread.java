package proxyServer;

import java.net.*;
import java.io.*;
import java.util.*;

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

    private Socket socket = null;
    private static final int BUFFER_SIZE = 32768;
    private Node node;

    //
    private DataOutputStream out;
    private String httpRequest;


    public ProxyThread(Socket socket, Node node, AODVObserver aodvObs) {
        super("ProxyThread");
        this.socket = socket;
        this.node = node;
        aodvObs.addObserver(this);
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

            // begin send request to mesh network
            // just gonna broadcast the request, anyone with a connection can
            // pick it up and respond
            node.sendData(0, adhoc.aodv.Constants.BROADCAST_ADDRESS,
                          new ExitNodeReqPDU(node.getNodeAddress(), 0, 0).toBytes());
            // send client data in update (recieved notifications from
            // AODVObserver)

            // end send request to mesh network

            // /////////////////////////////////
            // begin send response to client
            /*
             * byte by[] = new byte[BUFFER_SIZE]; int index = is.read(by, 0,
             * BUFFER_SIZE); while (index != -1) { out.write(by, 0, index);
             * index = is.read(by, 0, BUFFER_SIZE); } out.flush();
             */
            // end send response to client
            // /////////////////////////////////
        } catch (Exception e) {
            // can redirect this to error log
            System.err.println("Encountered exception: " + e);
            // encountered error - just send nothing back, so
            // processing can continue
            // out.writeBytes("err");
        }
        /*
         * // close out all resources if (rd != null) { rd.close(); } if (out !=
         * null) { out.close(); } if (in != null) { in.close(); }
         */
    }


    @Override
    public void update(Observable observable, Object data) {
        String tag = "ProxyThread:update";
        MeshPduInterface msg = (MeshPduInterface) data;
        Log.d(tag, "got msg: " + msg.toString());
        // split on two notifications here : (route request reply - in which
        // case send data to those addresses; and msg data in which case,
        // display returned data to client)
        switch (msg.getPduType()) {
        case Constants.PDU_DATAREPMSG:
            Log.d(tag, "got pdu_datamsg");
            DataMsg dmsg = (DataMsg) msg;
            byte[] bytes = Base64.decode(dmsg.getDataBytes(), 0);
            try {
                String resp = new String(bytes, Constants.encoding);
                Log.d(tag, "proxy got response: " + resp);
            } catch (UnsupportedEncodingException e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
            }
            try {
                out.write(bytes, 0, bytes.length);
                out.flush();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            break;
        case Constants.PDU_EXITNODEREP:
            Log.d(tag, "got pdu_exitnoderep: sending off:" + httpRequest);
            try {
                DataMsg dreq = new DataReqMsg(node.getNodeAddress(), 1, 1, Base64.encode(httpRequest
                                                                          .getBytes(Constants.encoding), 0));
                node.sendData(1,
                              msg.getSourceID(), dreq.toBytes());
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            break;
        case Constants.PDU_EXITNODEREQ:
            Log.d(tag, "got PDU_EXITNODEREQ");
            // do nothing
            break;
        default:
            Log.d(tag, "default switch");

        }
    }
}
