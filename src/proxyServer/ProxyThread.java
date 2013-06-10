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
    private AODVObserver mAodvObs;
    private int reqNumber;
    private int contactID;

    public ProxyThread(Socket socket, Node node, AODVObserver aodvObs, int reqNumber) {
        super("ProxyThread");
        this.socket = socket;
        this.node = node;
        aodvObs.addObserver(this);
        mAodvObs = aodvObs;
        this.reqNumber = reqNumber;
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

            //using contactManager, contactID should be set to valid hasData mesh node. send data straight off.

            try {
                DataMsg dreq =
                    new DataReqMsg(node.getNodeAddress(), 1, reqNumber, Base64.encode(httpRequest
                        .getBytes(Constants.encoding), 0));
                node.sendData(1, contactID, dreq.toBytes());
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            //BEGIN OLD BROADCAST METHOD
            // begin send request to mesh network
            // just gonna broadcast the request, anyone with a connection can
            // pick it up and respond
            //node.sendData(0, adhoc.aodv.Constants.BROADCAST_ADDRESS,
            //              new ExitNodeReqPDU(node.getNodeAddress(), 0, reqNumber).toBytes());
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
                    // done. close out the socket, and remove this as an aodv
                    // observer
                    try {
                        mAodvObs.deleteObserver(this);
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                break;
            case Constants.PDU_EXITNODEREP:
/*
                Log.d(tag, "got pdu_exitnoderep: sending off:" + httpRequest);
                try {
                    DataMsg dreq =
                        new DataReqMsg(node.getNodeAddress(), 1, reqNumber, Base64.encode(httpRequest
                            .getBytes(Constants.encoding), 0));
                    Log.d(tag, "contactID:"+contactID+" vs. dmsg.getSourceID():"+msg.getSourceID());
                    node.sendData(1, contactID, dreq.toBytes());
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }*/
                break;
            case Constants.PDU_EXITNODEREQ:
                Log.d(tag, "got PDU_EXITNODEREQ");
                // do nothing
                break;
            default:
                Log.d(tag, "default switch");
            }
        } else {
            //broadcast ID doesn't match our own, packet must be for different request/proxythread
            Log.d(tag, "BroadcastId doesn't match. Ours:"+reqNumber+ " found:"+msg.getBroadcastID());
        }
    }


    private boolean checkIDNumber(int i) {
        return (i == reqNumber);
    }

}
