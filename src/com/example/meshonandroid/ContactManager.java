package com.example.meshonandroid;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import adhoc.aodv.Node;
import android.util.Log;

import com.example.meshonandroid.pdu.ExitNodeReqPDU;
import com.example.meshonandroid.pdu.MeshPduInterface;



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

        Calendar staleThreshold = Calendar.getInstance();
        staleThreshold.roll(Calendar.MINUTE, Constants.CONTACT_STALENESSTIME);
        Date stalenessThresTime = staleThreshold.getTime();
        // if the contacts are older than 5 seconds, get new ones.
        if (updatedLast.before(stalenessThresTime)) {
            mNode.sendData(0, adhoc.aodv.Constants.BROADCAST_ADDRESS,
                           new ExitNodeReqPDU(mNode.getNodeAddress(), 0, reqNumber).toBytes());
            updatedLast = new Date();
        } else if (contactList.size() == 0) { // if we dont have any contacts,
                                              // get some
            mNode.sendData(0, adhoc.aodv.Constants.BROADCAST_ADDRESS,
                           new ExitNodeReqPDU(mNode.getNodeAddress(), 0, reqNumber).toBytes());
            updatedLast = new Date();
        } else {
            return pickContactRndRbn();
        }
        try {
            Thread.sleep(Constants.EXITNODEREP_WAITTIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }// TODO: even if the exitNodeReq we just broadcast doesn't return
         // anything, we still return these old contacts. Is that desired
         // behavior?
        return pickContactRndRbn();
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

    private int pickContactRndRbn() throws NoContactsAvailableException{
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

    @Override
    public void update(Observable arg0, Object arg1) {
        String tag = "ContactManager:update";
        MeshPduInterface msg = (MeshPduInterface) arg1;
        switch (msg.getPduType()) {
        case Constants.PDU_EXITNODEREP:
            Log.d(tag, "got ExitNodeRep");
            if (msg.getSourceID() != 1 || msg.getSourceID() != 0) {
                contactList.add(msg.getSourceID());
            }
            break;
        default:

        }

    }

}
