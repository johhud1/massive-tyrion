package proxyServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Observable;
import java.util.Observer;

import adhoc.aodv.Node;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import com.example.meshonandroid.Constants;
import com.example.meshonandroid.MeshMsgReceiver;
import com.example.meshonandroid.Utils;
import com.example.meshonandroid.pdu.AODVObserver;
import com.example.meshonandroid.pdu.ConnectDataMsg;
import com.example.meshonandroid.pdu.DataMsg;
import com.example.meshonandroid.pdu.MeshPduInterface;



public class ConnectProxyThread extends Thread implements MeshMsgReceiver {

    // constants, should probably move these somewhere
    private int TIMEOUT = 500;
    private int BUFSIZE = 30000;

    private Socket socket;
    private boolean listening = true;
    private boolean isRequesterSide;
    private int broadcastId;
    private int Destination;
    private Node myNode;
    private int msgCode;
    private Handler msgHandler;
    private AODVObserver mAodvObserver;

    volatile boolean keepListening = true;


    public ConnectProxyThread(Socket s, Node n, int dest, int bId, boolean isOutLink,
                              AODVObserver aodvObserver, Handler msgHandler) {
        socket = s;
        Destination = dest;
        broadcastId = bId;
        myNode = n;
        isRequesterSide = !isOutLink;
        // aodvObserver.addObserver(this);
        mAodvObserver = aodvObserver;
        if (isRequesterSide) {
            msgCode = Constants.TTM_MSG_CODE;
        } else {
            msgCode = Constants.TFM_MSG_CODE;
        }
        this.msgHandler = msgHandler;
    }


    @Override
    public void run() {
        String tag = "ConnectProxyThread:run";
        try {
            socket.setSoTimeout(TIMEOUT);
            InputStream in = socket.getInputStream();
            byte[] data = new byte[BUFSIZE];
            int redd = 0;

            while (keepListening) {
                try {
                    redd = in.read(data);
                    if (redd == -1) {
                        // read returning -1 indicates socket is closed. i hope.
                        break;
                    }
                    // read data from in, put it in a ConnectDataMsg and send it
                    // off
                    if (redd > 0) {
                        sendData(data, redd);
                    }
                } catch (SocketTimeoutException e) {
                    // don't want to wait to long, so timeout and send any
                    // recieved data
                    if (redd > 0) {
                        sendData(data, redd);
                    }
                } finally {
                    redd = 0;
                }
            }
            if (redd == -1) {
                // connection was closed. Send a CDM to the originator (with
                // isConnectionSetup = true and no Data to signal to close
                // connection on that end)
                Log.d(tag,
                      "connection appears to have been terminated. Sending emtpy ConnectDataMsg to: "
                          + Destination);
                // mAodvObserver.deleteObserver(this);
                sendSocketCloseMsg(myNode, Destination, isRequesterSide);
            }
        } catch (IOException e) {
            // mAodvObserver.deleteObserver(this);
            sendSocketCloseMsg(myNode, Destination, isRequesterSide);
            e.printStackTrace();
        }

    }


    private void sendSocketCloseMsg(Node myNode, int broadcastId, boolean isReq) {
        ConnectDataMsg cdm =
            new ConnectDataMsg(myNode.getNodeAddress(), 0, broadcastId, new byte[0], isReq, true);
        try {
            myNode.sendData(0, Destination, cdm.toBytes());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


    private void sendData(byte[] data, int length) throws IOException {
        String tag = "ConnectProxyInfo";// "ConnectProxyThread:sendData";
        byte[] sendData = new byte[length];
        System.arraycopy(data, 0, sendData, 0, sendData.length);
        Utils.sendTrafficMsg(msgHandler, length, msgCode);
        ConnectDataMsg cdm =
            new ConnectDataMsg(myNode.getNodeAddress(), 0, broadcastId, Base64.encode(sendData, 0),
                               isRequesterSide, false);
        String msg = new String(sendData, Constants.encoding);
        Log.d(tag, "got data from our connection (" + sendData.length
                   + " bytes), sending ConnectDataMsg to: " + Destination);
        myNode.sendData(0, Destination, cdm.toBytes());
    }


    /*
     * @Override public void update(Observable arg0, Object data) { String tag =
     * "ConnectProxyInfo"; MeshPduInterface msg = (MeshPduInterface) data;
     * DataMsg dmsg; if (broadcastId == (msg.getBroadcastID())) { Log.d(tag,
     * "BroadcastID matchs"); switch (msg.getPduType()) { case
     * Constants.PDU_CONNECTDATAMSG: ConnectDataMsg cdm = (ConnectDataMsg) msg;
     * Log.d(tag, "got a connect data msg: " + cdm.toReadableString()); if
     * (cdm.isConnectionSetupMsg() && cdm.getDataBytes().length == 0) { // empty
     * connection setup message. means the connection was // closed. so close
     * out our socket. set keepListening false keepListening = false; try {
     * Log.d("ConnectProxy", "GOT EMPTY ConnectDataMsg; closing our socket");
     * socket.close(); //TODO: should we remove this from aodvobserver ??
     * //mAodvObserver.deleteObserver(this); } catch (IOException e) {
     * e.printStackTrace(); } } if(!cdm.isConnectionSetupMsg()){ //got CONNECT
     * tcp data, extract from msg, decode and forward it to the target host
     * byte[] d = Base64.decode(cdm.getDataBytes(), 0); try {
     * if(isRequesterSide){ Utils.sendTrafficMsg(msgHandler, d.length,
     * Constants.TTM_MSG_CODE); } else { Utils.sendTrafficMsg(msgHandler,
     * d.length, Constants.TFM_MSG_CODE); } String dAsString = new String(d,
     * Constants.encoding); Log.d(tag,
     * "writing Connect Data out to target host("
     * +socket.getInetAddress().getHostAddress()+": "+dAsString+")");
     * socket.getOutputStream().write(d); } catch (IOException e) {
     * e.printStackTrace(); } } break; default: Log.d(tag, "default case"); }
     *
     * } }
     */

    @Override
    public void handleMessage(MeshPduInterface msg) {
        String tag = "ConnectProxyInfo";
        DataMsg dmsg;
        // TODO: remove this check, should be unnecessary with new design (sparseArray of ints (requestID's) to ConnextProxyThreads)
        if (broadcastId == (msg.getBroadcastID())) {
            Log.d(tag, "BroadcastID matchs");

            ConnectDataMsg cdm = (ConnectDataMsg) msg;
            Log.d(tag, "got a connect data msg: " + cdm.toReadableString());
            if (cdm.isConnectionSetupMsg() && cdm.getDataBytes().length == 0) {
                // empty connection setup message. means the connection was
                // closed. so close out our socket. set keepListening false
                keepListening = false;
                try {
                    Log.d("ConnectProxy", "GOT EMPTY ConnectDataMsg; closing our socket");
                    socket.close();
                    // TODO: should we remove this from aodvobserver ??
                    // mAodvObserver.deleteObserver(this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            //if (!cdm.isConnectionSetupMsg()) {
                // got CONNECT tcp data, extract from msg, decode and forward it
                // to the target host
                byte[] d = Base64.decode(cdm.getDataBytes(), 0);
                try {
                    if (isRequesterSide) {
                        Utils.sendTrafficMsg(msgHandler, d.length, Constants.TTM_MSG_CODE);
                    } else {
                        Utils.sendTrafficMsg(msgHandler, d.length, Constants.TFM_MSG_CODE);
                    }
                    String dAsString = new String(d, Constants.encoding);
                    Log.d(tag, "writing Connect Data out to target host("
                               + socket.getInetAddress().getHostAddress() + ": " + dAsString + ")");
                    socket.getOutputStream().write(d);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        //}
    }
}
