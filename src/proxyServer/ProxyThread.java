package proxyServer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.Observable;
import java.util.Observer;

import Logging.LoggingDBUtils;
import adhoc.aodv.Node;
import android.content.Context;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import com.example.meshonandroid.Constants;
import com.example.meshonandroid.Utils;
import com.example.meshonandroid.pdu.AODVObserver;
import com.example.meshonandroid.pdu.ConnectDataMsg;
import com.example.meshonandroid.pdu.ConnectionClosedMsg;
import com.example.meshonandroid.pdu.DataMsg;
import com.example.meshonandroid.pdu.DataRepMsg;
import com.example.meshonandroid.pdu.DataReqMsg;
import com.example.meshonandroid.pdu.MeshPduInterface;



public class ProxyThread extends Thread implements Observer {
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
    private Handler msgHandler; // main activity handler (used for sending text
                                // msgs to main thread)
    private ConnectProxyThread mConnectProxyThread;
    private long dbId; // id number of this request in db
    private int cSize = 0;
    private Context mContext;


    public ProxyThread(Socket socket, Node node, AODVObserver aodvObs, int reqNumber,
                       Handler msgHandler, long id, Context c) {
        super("ProxyThread");
        this.socket = socket;
        this.node = node;
        aodvObs.addObserver(this);
        mAodvObs = aodvObs;
        dbId = id;
        this.msgHandler = msgHandler;
        this.broadcastId = reqNumber;
        packetBuf = new DataRepMsg[WINDOW_SIZE];
        mContext = c;
    }


    public ProxyThread(Socket accept, Node node2, AODVObserver aodvobs, int reqNumber2,
                       int getContact, Handler h, long id, Context c) {
        this(accept, node2, aodvobs, reqNumber2, h, id, c);
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

            // using contactManager, contactID should be set to valid hasData
            // mesh node. send data straight off.
            try {
                DataMsg dreq =
                    new DataReqMsg(node.getNodeAddress(), 0, broadcastId, Base64.encode(httpRequest
                        .getBytes(Constants.encoding), 0));
                node.sendData(0, destinationID, dreq.toBytes());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            // can redirect this to error log
            System.err.println("Encountered exception: " + e);
        }
    }


    @Override
    public void update(Observable observable, Object data) {
        String tag = "ProxyThread:update";
        MeshPduInterface msg = (MeshPduInterface) data;;
        // check if this msg is for this ProxyThread
        // (multiple threads, one for each http request, so multiple outstanding
        // DataResps)
        // broadcastID acts as identifier
        if (checkIDNumber(msg.getBroadcastID())) {
            try {
                Log.d(tag, "BroadcastID match; got msg: " + msg.toReadableString());
            } catch (UnsupportedEncodingException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            switch (msg.getPduType()) {
            case Constants.PDU_DATAREPMSG:
                Log.d(tag, "got pdu_datamsg");
                DataRepMsg dmsg = (DataRepMsg) msg;

                packetBuf[(dmsg.getPacketID() % WINDOW_SIZE)] = dmsg;
                // decoded
                // b64 bytes
                try {
                    if (recievedPackets == dmsg.getPacketID()) {
                        byte[] outArray = Base64.decode(dmsg.getDataBytes(), 0);
                        handleOutArray(outArray, dmsg);
                    } else {
                        // we got a packet out of order, write out anything that
                        // we can
                        // ie anything after index recievedPackets
                        Log.e(ProxyThread.class.getName(),
                              "packet recieved out of order. very untested code; check everythings working properly");
                        int index = recievedPackets % WINDOW_SIZE;
                        while (packetBuf[index] != null
                               && isMoreRecent(packetBuf[(recievedPackets + 1) % WINDOW_SIZE],
                                               packetBuf[index])) {
                            byte[] outArray = Base64.decode(packetBuf[index].getDataBytes(), 0);
                            handleOutArray(outArray, dmsg);
                            // sendTrafficForwardedMsg(outArray.length);
                            // Utils.sendTrafficMsg(msgHandler, outArray.length,
                            // Constants.TF_MSG_CODE);
                            index = recievedPackets % WINDOW_SIZE;
                        }

                    }
                } catch (Exception e) {
                    // local browser may have closed connection because stream's
                    // no longer used
                    // or something worse may have happened. In either case send
                    // a connectionKill message to the appropriate node
                    Log.e(ProxyThread.class.getName(), "exception " + e);
                    mAodvObs.deleteObserver(this);
                    node.sendData(0, destinationID, new ConnectionClosedMsg(node.getNodeAddress(),
                                                                            0, broadcastId)
                        .toBytes());
                    // we're not gonna get a 'last packet' in this case, so just
                    // set the contentsize and endtime now
                    LoggingDBUtils.setRequestEndTimeAndContentSize(dbId,
                                                                   System.currentTimeMillis(),
                                                                   cSize);
                }

                break;
            case Constants.PDU_EXITNODEREP:
                break;
            case Constants.PDU_EXITNODEREQ:
                Log.d(tag, "got PDU_EXITNODEREQ");
                // do nothing
                break;
            case Constants.PDU_DATAMSG:
                Log.d(tag, "got PDU_DATAMSG");
                break;
            case Constants.PDU_CONNECTDATAMSG:
                Log.d(tag, "got PDU_CONNECTDATAMSG");
                ConnectDataMsg cdm = (ConnectDataMsg) msg;
                if (!cdm.isReq() && cdm.isConnectionSetupMsg()) {
                    // initial response from outlink, after recieving a
                    // CONNECTION method dataReqMsg (should be
                    // "OK 200 Connection Established")
                    // here we setup ConnectionProxy thread, which will run,
                    // reading from the socket, and buffering the stream into
                    // ConnectDataMsg's
                    new ConnectProxyThread(socket, node, cdm, false, mAodvObs, msgHandler).start();
                    byte[] b = Base64.decode(cdm.getDataBytes(), 0);
                    try {
                        String debug = new String(b, Constants.encoding);
                        out.write(b);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                Log.d(tag, "default switch");
            }
        } else {
            // broadcast ID doesn't match our own, packet must be for different
            // request/proxythread
            /*
             * Log.d(tag, "BroadcastId doesn't match. Ours:" + broadcastId +
             * " found:" + msg.getBroadcastID());
             */
        }
    }


    private void handleOutArray(byte[] outArray, DataRepMsg dmsg) throws IOException {
        // sendTrafficForwardedMsg(outArray.length);
        Utils.sendTrafficMsg(msgHandler, outArray.length, Constants.TTM_MSG_CODE);
        cSize += outArray.length;
        String respMsg = new String(outArray, Constants.encoding);
        out.write(outArray);
        out.flush();
        if (socket != null && !dmsg.getAreMorePackets()) {
            // done. close out the socket, and remove this
            // as an
            // aodv
            // observer
            mAodvObs.deleteObserver(this);
            LoggingDBUtils.setRequestEndTimeAndContentSize(dbId, System.currentTimeMillis(), cSize);
            socket.close();
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
