package proxyServer;

import java.net.*;
import java.io.*;

import com.example.meshonandroid.pdu.AODVObserver;

import adhoc.aodv.Node;
import android.os.AsyncTask;



public class ProxyListener extends Thread{

    private ServerSocket serverSocket = null;
    private boolean listening = true;
    int port = 8080; // default
    Node node;
    AODVObserver aodvobs;

    public ProxyListener(Node node, AODVObserver obs){
        new ProxyListener(port, node, obs);
    }

    public ProxyListener(int port, Node node, AODVObserver aodvobs) {
        super();
        this.port = port;
        this.node = node;
        this.aodvobs = aodvobs;
    }

    @Override
    public void run(){
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getLocalHost());
            System.out.println("Started on: " + port + " ServerSocket: "+serverSocket.toString());
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + port);
            System.exit(-1);
        }
        while (listening) {
            try {
                new ProxyThread(serverSocket.accept(), node, aodvobs).start();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                try {
                    serverSocket.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }
    }
}
