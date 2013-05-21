package proxyServer;

import java.net.*;
import java.io.*;

import android.os.AsyncTask;



public class ProxyListener extends Thread{

    private ServerSocket serverSocket = null;
    private boolean listening = true;
    int port = 8080; // default

    public ProxyListener(){
        new ProxyListener(port);
    }

    public ProxyListener(int port) {
        super();
        this.port = port;
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
                new ProxyThread(serverSocket.accept()).start();
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
