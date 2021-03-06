package proxyServer;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import edu.android.meshonandroid.Constants;
import edu.android.meshonandroid.ContactManager;
import edu.android.meshonandroid.Utils;
import edu.android.meshonandroid.ContactManager.NoContactsAvailableException;

import meshonandroid.logging.LoggingDBUtils;
import meshonandroid.logging.PerfDBHelper;
import meshonandroid.logging.WifiScannerThread;
import meshonandroid.pdu.AODVObserver;

import adhoc.aodv.Node;
import android.content.Context;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;




public class ProxyListener extends Thread {

    private int reqNumber;
    private ServerSocket serverSocket = null;
    private boolean listening = true;
    private ContactManager contactManager;
    private LocalBroadcastManager msgBroadcaster;
    int port = 8080; // default
    Node node;
    AODVObserver aodvobs;
    Context mContext;

    public ProxyListener(LocalBroadcastManager broadcaster, int port, Node node, ContactManager cman, AODVObserver aodvobs, Context c) {
        super();
        this.port = port;
        this.node = node;
        this.aodvobs = aodvobs;
        contactManager = cman;
        reqNumber = 0;
        this.msgBroadcaster = broadcaster;
        mContext = c;
    }


    @Override
    public void run() {
        String tag = "ProxyListener:run";
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getLocalHost());
            serverSocket.setSoTimeout(Constants.LOCALPROXY_ACCEPT_TIMEOUT);
            System.out.println("Started on: " + port + " ServerSocket: " + serverSocket.toString());
        } catch (IOException e) {
            Log.e(ProxyListener.class.getName(), "Could not listen on port: " + port);
            Utils.sendUIUpdateMsg(msgBroadcaster, Constants.STATUS_MSG_CODE, "error joining mesh");
        }
        WifiScannerThread scanThread = new WifiScannerThread(mContext);
        scanThread.start();
        while (listening) {
            try {
                Socket s = serverSocket.accept();
                int reqNumber = getReqNumber();
                int c = contactManager.GetContact(reqNumber);
                long id = LoggingDBUtils.addRequest(System.currentTimeMillis(), c);
                Log.d(tag, "contactManager.GetContact() returned contact:" + c);
                //TODO: implement code for using my own modem sometimes.
                ProxyThread pt = new ProxyThread(s, node, aodvobs, reqNumber, c, msgBroadcaster, id, mContext);
                aodvobs.addProxyThread(reqNumber, pt);
                pt.start();
            } catch (InterruptedIOException e){
                continue;
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    serverSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (NoContactsAvailableException e) {
                Utils.sendUIUpdateMsg(msgBroadcaster, Constants.LOG_MSG_CODE, "No available contacts found in mesh");
                e.printStackTrace();
            }
        }
        scanThread.stopScanning();
        Log.d(ProxyListener.class.getName(), " shutting down ProxyListener port: " + port + " serverSocket: " + serverSocket.toString());
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopListening(){
        listening = false;
    }

    private int getReqNumber() {
        int r = reqNumber;
        reqNumber++;
        return r;
    }
}
