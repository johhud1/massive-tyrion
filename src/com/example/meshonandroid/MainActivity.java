package com.example.meshonandroid;

import java.net.BindException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;

import org.apache.http.HttpHost;

import proxyServer.ProxyListener;

import com.example.meshonandroid.pdu.AODVObserver;
import com.example.meshonandroid.pdu.ExitNodeReqPDU;

import adhoc.aodv.Node;
import adhoc.aodv.exception.InvalidNodeAddressException;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;



public class MainActivity extends Activity implements Observer{

    int lastBroadcastId = 0;
    int lastDataRRId=0;
    int myContactID;
    Node myNode;

    private TextView outputTV;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button sendRreqData = (Button) findViewById(R.id.rreqdata_req);
        TextView outField = (TextView) findViewById(R.id.recvd_message_tv);

        myContactID = getMyID();

        try {
            myNode = new Node(myContactID);
            myNode.startThread();
            AODVObserver obs = new AODVObserver(myNode, myContactID, this);
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
                if(myNode!=null){
                    ExitNodeReqPDU dr = new ExitNodeReqPDU(myContactID, getBroadcastID(), getDataRRID());
                    myNode.sendData(dr.getPacketID(), 255, dr.toBytes());
                }
            }
        });
        Properties systemProperties = System.getProperties();
        systemProperties.setProperty( "http.proxyHost", "localhost");
        systemProperties.setProperty( "http.proxyPort", "8080");
        ProxyListener pl = new ProxyListener();
        pl.start();

        WebView wv = (WebView) findViewById(R.id.wv);
        wv.loadUrl("www.google.com");

    }

    public void setTextField(String text){
        TextView outField = (TextView) findViewById(R.id.recvd_message_tv);
        outField.setText(text);
    }

    private int getBroadcastID(){
        lastBroadcastId++;
        return lastBroadcastId;
    }

    private int getDataRRID(){
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
        Log.d(tag, "got update from Observable: "+arg0.toString());
        String update = (String)arg1;
        outputTV.setText(outputTV.getText()+"\n"+update);

    }
}
