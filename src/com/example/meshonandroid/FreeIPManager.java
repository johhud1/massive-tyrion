package com.example.meshonandroid;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import adhoc.aodv.Node;
import android.util.Log;

import com.example.meshonandroid.pdu.IPDiscoverMsg;
import com.example.meshonandroid.pdu.MeshPduInterface;



public class FreeIPManager implements Observer {

    Date updatedLast = new Date(0);
    private boolean[] availableIPs = new boolean[Constants.MAX_NODES]; // true
    // indicates
    // that address
    // is available
    private short freeIPs = 254;
    private boolean isJoining = false;
    private Node mNode;


    public FreeIPManager(Node n) {
        mNode = n;
        for (int i = 0; i < availableIPs.length; i++) {
            availableIPs[i] = true;
        }
    }


    public void setNode(Node n) {
        mNode = n;
    }


    public int getFreeID() { // returns a random IP that we haven't heard
                             // of as being in use in the mesh
        String tag = "FreeIPManager:getFreeID";
        isJoining = true;
        Calendar fiveSecsAgo = Calendar.getInstance();
        fiveSecsAgo.roll(Calendar.SECOND, -Constants.IP_staleness_time);
        // if the contacts are older than 5 seconds, get new ones.
        if (updatedLast.before(fiveSecsAgo.getTime())) {
            try {
                Log.d(tag, "IP list is stale. broadcasting IPDiscover to gather fresher IP info");
                mNode.sendData(0, adhoc.aodv.Constants.BROADCAST_ADDRESS,
                               new IPDiscoverMsg(mNode.getNodeAddress(), 0, 0, true).toBytes());
                updatedLast = new Date();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        isJoining = false;
        if (freeIPs != 0) {
            int ip = -1;
            int i;
            for (i = 2; i < availableIPs.length; i++) {
                // i = new Random().nextInt(availableIPs.length);
                if (availableIPs[i]) {
                    ip = i;
                    break;
                }
            }
            availableIPs[i] = false;
            freeIPs--;
            return ip;
        } else {
            Log.e(tag, "no free ip's available :( RUNNN!");
            return -1;
        }
    }


    @Override
    public void update(Observable observable, Object msg) {
        String tag = "NetworkInfo:update";
        MeshPduInterface meshMsg = (MeshPduInterface) msg;
        Log.d(tag, "got msg: " + meshMsg.toReadableString());
        switch (meshMsg.getPduType()) {
        case Constants.PDU_IPDISCOVER:
            IPDiscoverMsg m = (IPDiscoverMsg) msg;
            try {
                // m.parseBytes(msg.toBytes());
                if (m.isReq() && (mNode.getNodeAddress() != 1)) {// is a
                                                                 // request,
                                                                 // and we
                                                                 // are a
                                                                 // joined
                                                                 // node.
                                                                 // Send a
                                                                 // response.
                                                                 // done.
                    Log.d(tag, "got an IPDiscover request from " + m.getSourceID()
                               + " sending IPDiscover response");
                    mNode
                        .sendData(0, adhoc.aodv.Constants.BROADCAST_ADDRESS,
                                  new IPDiscoverMsg(mNode.getNodeAddress(), 1, 1, false).toBytes());
                    return;
                }
                if (isJoining && !m.isReq()) { // we are joining, and the
                                               // message is a response. set
                                               // availableIPs appropriately
                    Log.d(tag, "got an IPDiscover response from " + m.getSourceID()
                               + ". setting that address as unavailable");
                    availableIPs[m.getSourceID()] = false;
                } else {// we are not joining, or the message is a request.
                    Log.d(tag,
                          "got an IPDiscover msg. but we are either not joining, or we are and it's not a response. Ignoring");
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
