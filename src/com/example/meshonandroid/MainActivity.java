package com.example.meshonandroid;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.Proxy;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;

import proxyServer.ProxyListener;

import com.example.meshonandroid.pdu.AODVObserver;
import com.example.meshonandroid.pdu.ExitNodeReqPDU;

import adhoc.aodv.Node;
import adhoc.aodv.exception.InvalidNodeAddressException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;



public class MainActivity extends Activity implements Observer {

    int lastBroadcastId = 0;
    int lastDataRRId = 0;
    int myContactID;
    Node myNode;
    Handler handler;

    private TextView outputTV;


    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //BAD - should get rid of, but makes for faster, dont' have to put all network activities in seperate threads
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button sendRreqData = (Button) findViewById(R.id.rreqdata_req);
        outputTV= (TextView) findViewById(R.id.recvd_message_tv);
        handler = new Handler() {
            @SuppressWarnings("unchecked")
            @Override
            public void handleMessage(Message msg) {
                outputTV.setText(outputTV.getText() + "\n" + msg.getData().getString("msg"));
            }
        };

        myContactID = getMyID();

        try {
            myNode = new Node(myContactID);
            myNode.startThread();
            AODVObserver obs = new AODVObserver(myNode, myContactID, this);
            ProxyListener pl = new ProxyListener(8080, myNode, obs);
            pl.start();
        } catch (BindException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidNodeAddressException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        sendRreqData.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (myNode != null) {
                    /*
                    ExitNodeReqPDU dr =
                        new ExitNodeReqPDU(myContactID, getBroadcastID(), getDataRRID());
                    myNode.sendData(dr.getPacketID(), 255, dr.toBytes());
                    */
                    URLConnection urlConn;
                    try {
                        URL url  = new URL("http://192.168.1.1");
                        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 8080));

                        InputStream response = url.openConnection(proxy).getInputStream();
                        System.out.println(response.read());

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
/*
        HttpURLConnection urlConn;
        try {
            URL url  = new URL("http://www.google.com");
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 8080));

            InputStream response = url.openConnection(proxy).getInputStream();
            System.out.println(response.read());

        } catch (IOException e) {
            e.printStackTrace();
        }
        */

    }


    public void setTextField(String text) {
        TextView outField = (TextView) findViewById(R.id.recvd_message_tv);
        outField.setText(text);
    }


    private int getBroadcastID() {
        lastBroadcastId++;
        return lastBroadcastId;
    }


    private int getDataRRID() {
        lastDataRRId++;
        return lastBroadcastId;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    public int getMyID() {
        NetworkInterface nif;
        try {
            nif = NetworkInterface.getByName("wlan0");
            int myid = ((short) nif.getInetAddresses().nextElement().getAddress()[3]);
            Log.d("getPhoneType", "myContactId is fourth ip segment:" + myid);
            return myid;
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
        }
    }


    @Override
    public void update(Observable arg0, Object arg1) {
        String tag = "MainActivity:update";
        Log.d(tag, "got update from Observable: " + arg1.toString());
        Message m = new Message();
        Bundle b = new Bundle();
        b.putString("msg", arg1.toString());
        m.setData(b);
        handler.sendMessage(m);

    }
}
