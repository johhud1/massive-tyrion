package com.example.meshonandroid;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import com.example.meshonandroid.pdu.ExitNodeReqPDU;
import com.example.meshonandroid.pdu.MeshPduInterface;

import adhoc.aodv.Node;
import adhoc.aodv.Node.MessageToObserver;
import android.util.Log;



public class ContactManager implements Observer {
    Node mNode;
    int myContactID;
    ArrayList<Integer> contactList = new ArrayList<Integer>();
    Date updatedLast = new Date(0);
    int lastUsedContact;


    public ContactManager(Node myNode) {
        lastUsedContact = 0;
        mNode = myNode;
        this.myContactID = myNode.getNodeAddress();
    }


    public int GetContact(int reqNumber) throws NoContactsAvailableException {

        Calendar fiveSecsAgo = Calendar.getInstance();
        fiveSecsAgo.roll(Calendar.SECOND, -5);
        // if the contacts are older than 5 seconds, get new ones.
        if (updatedLast.before(fiveSecsAgo.getTime())) {
            mNode.sendData(0, adhoc.aodv.Constants.BROADCAST_ADDRESS,
                           new ExitNodeReqPDU(mNode.getNodeAddress(), 0, reqNumber).toBytes());
        } else if (contactList.size() == 0) { // if we dont have any contacts,
                                              // get some
            mNode.sendData(0, adhoc.aodv.Constants.BROADCAST_ADDRESS,
                           new ExitNodeReqPDU(mNode.getNodeAddress(), 0, reqNumber).toBytes());
        }
        try {
            Thread.sleep(Constants.EXITNODEREP_WAITTIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }//TODO: even if the exitNodeReq we just broadcast doesn't return anything, we still return these old contacts. Is that desired behavior?
        updatedLast = new Date();
        if (contactList.size() != 0) {
            if (lastUsedContact == contactList.size()) {
                lastUsedContact = 1;
                return contactList.get(0);
            } else {
                lastUsedContact++;
                return contactList.get(lastUsedContact - 1);
            }
        }
        throw new NoContactsAvailableException("no contacts could be found");
    }

    public class NoContactsAvailableException extends Exception {
        /**
         *
         */
        private static final long serialVersionUID = 6172352416074056093L;


        public NoContactsAvailableException(String message) {
            super(message);
        }
    }


    @Override
    public void update(Observable arg0, Object arg1) {
        String tag = "ContactManager:update";
        MeshPduInterface msg = (MeshPduInterface) arg1;
        switch (msg.getPduType()) {
        case Constants.PDU_EXITNODEREP:
            Log.d(tag, "got ExitNodeRep");
            contactList.add(msg.getSourceID());
            break;
        default:

        }

    }

}
