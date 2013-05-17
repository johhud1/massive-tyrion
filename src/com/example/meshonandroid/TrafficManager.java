package com.example.meshonandroid;

import com.example.meshonandroid.pdu.ExitNodeRepPDU;

import adhoc.aodv.Node;
import android.util.SparseIntArray;

public class TrafficManager {

    private boolean mHaveData = true;
    private Node mNode;
    private int mContactId;
    private SparseIntArray mPortToContactID = new SparseIntArray();
    /**
     * Constructor for creating a traffic manager.
     *
     * The traffic manager is responsible for responding to ExitNodeReqPDU's and for managing the mapping from local ports to Mesh network ID's (in order to forward traffic appropriately (think NAT))
     *
     * @param
     */
    public TrafficManager(boolean haveData, Node node, int mid){
        mContactId = mid;
        mNode = node;
        mHaveData = haveData;
    }

    public void connectionRequested(int reqContactId){
        if(mHaveData){
            setupForwardingConn(reqContactId);
        } else {
            //ignore
        }
    }

    private void setupForwardingConn(int reqContactId){
        ExitNodeRepPDU rep = new ExitNodeRepPDU(mContactId, 0, 0);
        mNode.sendData(0 ,reqContactId, rep.toBytes());

    }
}
