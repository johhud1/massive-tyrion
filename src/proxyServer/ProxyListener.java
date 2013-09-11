package proxyServer;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import Logging.LoggingDBUtils;
import Logging.PerfDBHelper;
import adhoc.aodv.Node;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.example.meshonandroid.Constants;
import com.example.meshonandroid.ContactManager;
import com.example.meshonandroid.ContactManager.NoContactsAvailableException;
import com.example.meshonandroid.Utils;
import com.example.meshonandroid.pdu.AODVObserver;



public class ProxyListener extends Thread {

    private int reqNumber;
    private ServerSocket serverSocket = null;
    private boolean listening = true;
    private ContactManager contactManager;
    private Handler msgHandler;
    int port = 8080; // default
    Node node;
    AODVObserver aodvobs;
    Context mContext;

    public ProxyListener(Handler msgHandler, int port, Node node, AODVObserver aodvobs, Context c) {
        super();
        this.port = port;
        this.node = node;
        this.aodvobs = aodvobs;
        contactManager = new ContactManager(node);
        aodvobs.addObserver(contactManager);
        reqNumber = 0;
        this.msgHandler = msgHandler;
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
            System.exit(-1);
        }
        while (listening) {
            try {
                Socket s = serverSocket.accept();
                long id = LoggingDBUtils.addRequest(System.currentTimeMillis());
                int reqNumber = getReqNumber();
                int c = contactManager.GetContact(reqNumber);
                Log.d(tag, "contactManager.GetContact() returned contact:" + c);
                new ProxyThread(s, node, aodvobs, reqNumber, c, msgHandler, id, mContext).start();
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
                Utils.addMsgToMainTextLog(msgHandler, "No available contacts found in mesh");
                e.printStackTrace();
            } 
        }
        Log.d(ProxyListener.class.getName(), " shutting down ProxyListener port: " + port + " serverSocket: " + serverSocket.toString());
        try {
            serverSocket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
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
