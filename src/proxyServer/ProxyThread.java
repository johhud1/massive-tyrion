package proxyServer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Observable;
import java.util.Observer;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import meshonandroid.Constants;
import meshonandroid.OutLinkManager;
import meshonandroid.Utils;
import meshonandroid.pdu.AODVObserver;
import meshonandroid.pdu.ConnectDataMsg;
import meshonandroid.pdu.ConnectionClosedMsg;
import meshonandroid.pdu.DataMsg;
import meshonandroid.pdu.DataRepMsg;
import meshonandroid.pdu.DataReqMsg;
import meshonandroid.pdu.MeshPduInterface;

import Logging.LoggingDBUtils;
import Logging.PerfDBHelper;
import adhoc.aodv.Node;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;




public class ProxyThread extends Thread {
    private static final int MAX_PACKETS = 200; // this number is arbitrary
    // private byte[][] packetBuf;
    private DataRepMsg[] packetBuf;
    private static final int WINDOW_SIZE = 10;

    private Socket socket = null;
    private Node node;

    //
    private DataOutputStream out;
    private BufferedReader in;
    private String httpRequest;
    private AODVObserver mAodvObs;
    private int broadcastId; // id number of this request, used to identify resp
                             // msgs from other responses to requests on this
                             // phone
    private int destinationID; // id of node in mesh that will be doing the data
                               // transfer for us
    private int recievedPackets = 0;
    private LocalBroadcastManager msgBroadcaster; // broadcaster (used for sending text
                                // msgs to main thread)
    private ConnectProxyThread mConnectProxyThread;
    private long dbId; // id number of this request in db
    private int cSize = 0;
    private boolean expectingMorePackets = true;

    private LinkedBlockingQueue<DataRepMsg> dataRepQ = new LinkedBlockingQueue<DataRepMsg>();


    public ProxyThread(Socket socket, Node node, AODVObserver aodvObs, int reqNumber,
                       LocalBroadcastManager msgBroadcaster, long id, Context c) {
        super("ProxyThread");
        this.socket = socket;
        this.node = node;
        // aodvObs.addObserver(this);
        mAodvObs = aodvObs;
        dbId = id;
        this.msgBroadcaster = msgBroadcaster;
        this.broadcastId = reqNumber;
        packetBuf = new DataRepMsg[WINDOW_SIZE];
    }


    public ProxyThread(Socket accept, Node node2, AODVObserver aodvobs, int reqNumber2,
                       int getContact, LocalBroadcastManager msgBroadcaster, long id, Context c) {
        this(accept, node2, aodvobs, reqNumber2, msgBroadcaster, id, c);
        destinationID = getContact;
    }


    public void run() {
        String tag = "ProxyThread:run";
        try {
            out = new DataOutputStream(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String inputLine, outputLine;
            int cnt = 0;
            String urlToCall = "";
            httpRequest = new String();
            // /////////////////////////////////
            // begin get request from client
            while ((inputLine = in.readLine()) != null) {
                httpRequest += inputLine + "\n";
                if (cnt == 0) {
                    Log.d(tag, "request: " + httpRequest);
                }
                if (inputLine.equals("")) {
                    httpRequest += "\n";
                    break;
                }
                cnt++;

            }
            // end get request from client

            // for simplicity, gonna check if its a CONNECT request here, spawn
            // ConnectProxyThread and add it to the
            // AODVObservers ConnectProxyList. this is a bit hacky
            //destination node shold recieve dataReq, handle it in OutLinkManager, (setup CPT over there)
            //and respond with ConnectDataMsg
            String[] request = httpRequest.split(" ");
            if (isConnectHttpRequest(request)) {
                ConnectProxyThread cpt =
                    new ConnectProxyThread(socket, node, destinationID, broadcastId, false,
                                           mAodvObs, msgBroadcaster);
                mAodvObs.addConnectProxyThread(broadcastId, cpt);
                cpt.start();
                DataMsg dreq =
                    new DataReqMsg(node.getNodeAddress(), 0, broadcastId, Base64.encode(httpRequest
                        .getBytes(Constants.encoding), 0));
                node.sendData(0, destinationID, dreq.toBytes());
                expectingMorePackets = false;
                mAodvObs.removeProxyThread(broadcastId);
            } else {
                // using contactManager, contactID should be set to valid
                // hasData
                // mesh node. send data straight off.
                try {
                    Log.d(ProxyThread.class.getName(), "sending request to nodeID: "+destinationID);
                    DataMsg dreq =
                        new DataReqMsg(node.getNodeAddress(), 0, broadcastId,
                                       Base64.encode(httpRequest.getBytes(Constants.encoding), 0));
                    node.sendData(0, destinationID, dreq.toBytes());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            // can redirect this to error log
            System.err.println("Encountered exception: " + e);
        }
        // receive packets and forward them to the local user until we don't
        // expect anymore
        /* uncomment this block for more threads -- also get rid of handlemessage and change AODVObserver DataRepMsg switch case back
        while (expectingMorePackets) {
            DataRepMsg rep = null;
            try {
                rep = dataRepQ.take();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            if (rep != null) {
                Log.d(ProxyThread.class.getName(), "got pdu_datamsg");
                try {
                    forwardMsgDataToReceiver(rep);
                } catch (IOException e) {
                    // local browser may have closed connection because stream's
                    // no longer used
                    // or something worse may have happened. In either case send
                    // a connectionKill message to the appropriate node so it
                    // stops its HTTPFetcher thread
                    e.printStackTrace();
                    // mAodvObs.deleteObserver(this);
                    node.sendData(0, destinationID, new ConnectionClosedMsg(node.getNodeAddress(),
                                                                            0, broadcastId)
                        .toBytes());
                    stopProxyThread();
                }
            }
        }*/
    }
    public void handleMessage(DataRepMsg rep){
        Log.d(ProxyThread.class.getName(), "got pdu_datamsg");
        try {
            forwardMsgDataToReceiver(rep);
        } catch (IOException e) {
            // local browser may have closed connection because stream's
            // no longer used
            // or something worse may have happened. In either case send
            // a connectionKill message to the appropriate node so it
            // stops its HTTPFetcher thread
            e.printStackTrace();
            // mAodvObs.deleteObserver(this);
            node.sendData(0, destinationID, new ConnectionClosedMsg(node.getNodeAddress(),
                                                                    0, broadcastId)
                .toBytes());
            stopProxyThread();
        }
    }

    private boolean isConnectHttpRequest(String[] request) {
        if ("CONNECT".equalsIgnoreCase(request[0])) { return true; }
        return false;
    }


    /**
     * always called when the proxy thread is stopped, so close the socket,
     * remove this ProxyThread from the list of active proxyThreads, update the
     * database with the request stats and set boolean so run finishes
     */
    private void stopProxyThread() {
        Log.d(ProxyThread.class.getName()+":stopProxyThread", "closing thread for bId: "+broadcastId);
        LoggingDBUtils.setRequestEndTimeAndContentSize(dbId, System.currentTimeMillis(), cSize);
        mAodvObs.removeProxyThread(broadcastId);
        expectingMorePackets = false;
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void PushPacketOnDataRepQ(DataRepMsg msg) {
        dataRepQ.add(msg);
    }

    private void forwardMsgDataToReceiver(DataRepMsg dmsg) throws IOException {
        byte[] outArray = Base64.decode(dmsg.getDataBytes(), 0);
        Utils.sendUIUpdateMsg(msgBroadcaster,  Constants.TTM_MSG_CODE, Integer.valueOf(outArray.length));

        cSize += outArray.length;
        out.write(outArray);
        out.flush();
        if (socket != null && !dmsg.getAreMorePackets()) {
            // done. close out the socket, and remove this
            // as an
            // aodv
            // observer
            stopProxyThread();
        }
        recievedPackets++;
    }


    private boolean isMoreRecent(DataRepMsg first, DataRepMsg other) {
        return (first.getPacketID() > other.getPacketID());
    }


    private boolean checkIDNumber(int i) {
        return (i == broadcastId);
    }

}
