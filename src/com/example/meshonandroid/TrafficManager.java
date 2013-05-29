package com.example.meshonandroid;

import java.io.UnsupportedEncodingException;
import java.util.Observable;
import java.util.Observer;

import com.example.meshonandroid.pdu.DataMsg;
import com.example.meshonandroid.pdu.ExitNodeRepPDU;
import com.example.meshonandroid.pdu.MeshPduInterface;

import adhoc.aodv.Node;
import android.util.Log;
import android.util.SparseIntArray;

public class TrafficManager implements Observer{

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
    public TrafficManager(boolean haveData, Node node, int myID){
        mContactId = myID;
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

    @Override
    public void update(Observable arg0, Object m) {
        String tag = "TrafficManager:update";
        MeshPduInterface msg = (MeshPduInterface) m;
        Log.d(tag, "got update. msg: "+m.toString());
        switch (msg.getPduType()){
            case Constants.PDU_DATAMSG:
                DataMsg dmsg = (DataMsg) msg;
            try {
                String resp = new String("request recieved");
                Log.d(tag, "got data request, sending response "+resp);
                mNode.sendData(dmsg.getPacketID()+1, dmsg.getSouceID(), resp.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            default:
                Log.d(tag, "got something not PDU_DATAMSG");
        }

    }
}
