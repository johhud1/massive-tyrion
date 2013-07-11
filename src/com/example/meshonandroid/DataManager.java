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
}
