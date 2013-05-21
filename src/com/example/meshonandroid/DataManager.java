package com.example.meshonandroid;

import java.io.UnsupportedEncodingException;

import com.example.meshonandroid.pdu.DataMsg;
import adhoc.aodv.Node;
import android.util.Log;

public class DataManager {
    private Node mNode;
    private int mContactId;


    public DataManager(Node node){
        mNode = node;
    }

    public void sendDataMsg(int dest){
        String tag = "DataManager:sendDataMsg";
        String msg = "Hello";
        try {
            Log.d(tag, "sending Data Msg with data:"+new String(msg.getBytes("UTF-8"), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        DataMsg dataMsg;
        try {
            dataMsg = new DataMsg(mContactId, 0, 0, msg.getBytes(Constants.encoding));
            mNode.sendData(0, dest, dataMsg.toBytes());
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
