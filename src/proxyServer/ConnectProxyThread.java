package proxyServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeoutException;

import com.example.meshonandroid.Constants;
import com.example.meshonandroid.pdu.ConnectDataMsg;
import com.example.meshonandroid.pdu.DataMsg;
import com.example.meshonandroid.pdu.MeshPduInterface;

import adhoc.aodv.Node;
import android.util.Base64;
import android.util.Log;



public class ConnectProxyThread extends Thread implements Observer {

    // constants, should probably move these somewhere
    private int TIMEOUT = 500;
    private int BUFSIZE = 30000;

    private Socket socket;
    private boolean listening = true;
    private boolean isReq;
    private int broadcastId;
    private int Destination;
    private Node myNode;


    public ConnectProxyThread(Socket s, Node n, MeshPduInterface msg, boolean isOutLink,
                              Observable aodvObserver) {
        socket = s;
        Destination = msg.getSourceID();
        broadcastId = msg.getBroadcastID();
        myNode = n;
        isReq = !isOutLink;
        aodvObserver.addObserver(this);
    }


    @Override
    public void run() {
        String tag = "ConnectProxyThread:run";
        try {
            socket.setSoTimeout(TIMEOUT);
            InputStream in = socket.getInputStream();
            byte[] data = new byte[BUFSIZE];
            int redd = 0;

            while (true) {
                try {
                    redd = in.read(data);
                    if (redd == -1) {
                        //read returning -1 indicates socket is closed. i hope.
                        break;
                    }
                    // read data from in, put it in a ConnectDataMsg and send it
                    // off
                    if (redd > 0) {
                        sendData(data, redd);
                    }
                } catch (SocketTimeoutException e) {
                    //don't want to wait to long, so timeout and send any recieved data
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
                sendSocketCloseMsg(myNode, Destination, isReq);
            }
        } catch (IOException e) {
            sendSocketCloseMsg(myNode, Destination, isReq);
            e.printStackTrace();
        }

    }

    private void sendSocketCloseMsg(Node myNode, int broadcastId, boolean isReq){

        ConnectDataMsg cdm =
            new ConnectDataMsg(myNode.getNodeAddress(), 0, broadcastId, new byte[0], isReq,
                               true);
        try {
            myNode.sendData(0, Destination, cdm.toBytes());
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void sendData(byte[] data, int length) throws IOException {
        String tag = "ConnectProxyInfo";// "ConnectProxyThread:sendData";
        byte[] sendData = new byte[length];
        System.arraycopy(data, 0, sendData, 0, sendData.length);
        ConnectDataMsg cdm =
            new ConnectDataMsg(myNode.getNodeAddress(), 0, broadcastId, Base64.encode(sendData, 0),
                               isReq, false);
        String msg = new String(sendData, Constants.encoding);
        Log.d(tag, "got data from our connection (" + sendData.length
                   + " bytes), sending ConnectDataMsg to: " + Destination);
        myNode.sendData(0, Destination, cdm.toBytes());
    }


    @Override
    public void update(Observable arg0, Object data) {
        String tag = "ConnectProxyInfo"; // "ConnectProxyThread:update";
        MeshPduInterface msg = (MeshPduInterface) data;
        DataMsg dmsg;
        if (broadcastId == (msg.getBroadcastID())) {
            Log.d(tag, "BroadcastID matchs");
            switch (msg.getPduType()) {
            case Constants.PDU_CONNECTDATAMSG:
                ConnectDataMsg cdm = (ConnectDataMsg) msg;
                Log.d(tag, "got a connect data msg: " + cdm.toReadableString());
                if (!cdm.isReq() && cdm.isConnectionSetupMsg() && cdm.getDataBytes().length == 0) {
                    // empty connection setup message. means the connection was
                    // closed. so close out our socket.
                    try {
                        Log.d("ConnectProxy", "GOT EMPTY ConnectDataMsg; closing our socket");
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                if(cdm.isReq() && !cdm.isConnectionSetupMsg()){
                    //got CONNECT tcp data, extract from msg, decode and forward it to the target host
                    byte[] d = Base64.decode(cdm.getDataBytes(), 0);
                    try {
                        String dAsString = new String(d, Constants.encoding);
                        Log.d(tag, "writing Connect Data out to target host("+socket.getInetAddress().getHostAddress()+": "+dAsString+")");
                        socket.getOutputStream().write(d);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                break;
            default:
                Log.d(tag, "default case");
            }
            // TODO Auto-generated method stub

        }
    }
}
