package proxyServer;

import java.net.*;
import java.io.*;

import com.example.meshonandroid.ContactManager;
import com.example.meshonandroid.ContactManager.NoContactsAvailableException;
import com.example.meshonandroid.pdu.AODVObserver;

import adhoc.aodv.Node;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;



public class ProxyListener extends Thread{

    private int reqNumber;
    private ServerSocket serverSocket = null;
    private boolean listening = true;
    private ContactManager contactManager;
    private Handler msgHandler;
    int port = 8080; // default
    Node node;
    AODVObserver aodvobs;

    public ProxyListener(Handler msgHandler, int port, Node node, AODVObserver aodvobs) {
        super();
        this.port = port;
        this.node = node;
        this.aodvobs = aodvobs;
        contactManager =  new ContactManager(node);
        aodvobs.addObserver(contactManager);
        reqNumber = 0;
        this.msgHandler = msgHandler;
    }

    @Override
    public void run(){
        String tag = "ProxyListener:run";
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getLocalHost());
            System.out.println("Started on: " + port + " ServerSocket: "+serverSocket.toString());
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + port);
            System.exit(-1);
        }
        while (listening) {
            try {
                Socket s = serverSocket.accept();
                int c = contactManager.GetContact(getReqNumber());
                Log.d(tag, "contactManager.GetContact() returned contact:"+c);
                new ProxyThread(s, node, aodvobs, getReqNumber(), c, msgHandler).start();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    serverSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (NoContactsAvailableException e) {
                e.printStackTrace();
            }
        }
    }

    private int getReqNumber(){
        int r = reqNumber;
        reqNumber++;
        return r;
    }
}
