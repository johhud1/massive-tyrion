package edu.android.meshonandroid;

import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import meshonandroid.pdu.ExitNodeReqPDU;
import meshonandroid.pdu.MeshPduInterface;

import adhoc.aodv.Node;
import android.util.Log;




public class ContactManager {

    private static final Constants.ContactSelectionStrategy CONTACT_STRATEGY =
        Constants.CONTACT_STRATEGY;

    /**
     * Class responsible for tracking mesh nodes available for DataRequests
     * Returns ID of available nodes according to the chosen Contact selection
     * strategy (CONTACT_STRATEGY)
     *
     * @param
     */
    // TODO: implement startTime endTime and contentSize setting code (should
    // probably go in proxythread?) so ContactObj's and
    // fastestFirst method can actually be used
    private static class ContactObj implements Comparable<ContactObj> {
        public int Id;
        public double speed;
        long startTime;
        long endTime;
        long contentSize;


        public ContactObj() {
            speed = 0;
        }


        public ContactObj(int ID) {
            Id = ID;
        }


        @Override
        public int compareTo(ContactObj other) {// uhh, I guess compareTo
            double speedDif = speed - other.speed;
            if (speedDif < 0) {
                return (int) Math.floor(speedDif);
            } else if (speedDif > 0) {
                return (int) Math.ceil(speedDif);
            } else {
                return 0;
            }
        }


        @Override
        public boolean equals(Object other) {
            if (other instanceof ContactObj) { return Id == ((ContactObj) other).Id; }
            return false;
        }
    }

    Node mNode;
    int myContactID;
    Deque<Integer> contactQ = new ArrayDeque<Integer>(); // queue of contacts
                                                         // that we'll pick from
                                                         // for RndRbn
    SortedSet<ContactObj> contactSSet = new TreeSet<ContactObj>(); // sorted set
                                                                   // of
                                                                   // contacts
                                                                   // that will
                                                                   // be sorted
                                                                   // by speed
                                                                   // for
                                                                   // FstFrst
                                                                   // strgy
    Date updatedLast = new Date(0);


    public ContactManager(Node myNode) {
        mNode = myNode;
        this.myContactID = myNode.getNodeAddress();
    }


    public int GetContact(int reqNumber) throws NoContactsAvailableException {

        Calendar staleThreshold = Calendar.getInstance();
        staleThreshold.roll(Calendar.MINUTE, Constants.CONTACT_STALENESSTIME);
        Date stalenessThresTime = staleThreshold.getTime();
        // if the contacts are older than 5 seconds, get new ones.
        if (updatedLast.before(stalenessThresTime)) {
            clearContacts();
            mNode.sendData(0, adhoc.aodv.Constants.BROADCAST_ADDRESS,
                           new ExitNodeReqPDU(mNode.getNodeAddress(), 0, reqNumber).toBytes());
            updatedLast = new Date();
        } else if (contactPoolSize() == 0) { // if we dont have any contacts,
                                           // get some
            mNode.sendData(0, adhoc.aodv.Constants.BROADCAST_ADDRESS,
                           new ExitNodeReqPDU(mNode.getNodeAddress(), 0, reqNumber).toBytes());
            updatedLast = new Date();
        } else {
            return pickContact();
        }
        try {
            Thread.sleep(Constants.EXITNODEREP_WAITTIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return pickContact();
    }
    
    private void clearContacts(){
        switch(CONTACT_STRATEGY){
        case RoundRobin:
            contactQ.clear();
            break;
        case FastestFirst:
            contactSSet.clear();
            break;
        }
    }
    
    private int contactPoolSize(){
        switch(CONTACT_STRATEGY){
        case RoundRobin:
            return contactQ.size();
        case FastestFirst:
            return contactSSet.size();
        }
        return -1;
    }
    public int pickContact() throws NoContactsAvailableException {
        switch (CONTACT_STRATEGY) {
        case RoundRobin:
            return pickContactRndRbn();
        case FastestFirst:
            return pickContactFastestFirst();
        default:
            Log.e(ContactManager.class.getName(),
                  "pickContact in default switch. This should not happen!");
            return -1;
        }
    }


    private int pickContactFastestFirst() {
        return contactSSet.last().Id;
    }

    public class NoContactsAvailableException extends Exception {
        /**
         * Exception thrown if we can't find any Contacts
         */
        private static final long serialVersionUID = 6172352416074056093L;


        public NoContactsAvailableException(String message) {
            super(message);
        }
    }


    synchronized int pickContactRndRbn() throws NoContactsAvailableException {
        if (contactQ.size() != 0) {
            int contactId = contactQ.pop(); // pop the head off
            contactQ.add(contactId); // add the id back at the tail
            return contactId;
        }
        throw new NoContactsAvailableException("no contacts could be found");
    }


    /*
     * @Override public void update(Observable arg0, Object arg1) { String tag =
     * "ContactManager:update"; MeshPduInterface msg = (MeshPduInterface) arg1;
     * switch (msg.getPduType()) { case Constants.PDU_EXITNODEREP: Log.d(tag,
     * "got ExitNodeRep"); addContact(msg); break; default:
     *
     * }
     *
     * }
     */

    public void addContact(MeshPduInterface msg) {
        Log.d(ContactManager.class.getName(),
              "addContact. adding contact with ExitNodeRep(?) msg: " + msg.toReadableString());
        String strategy = "null";
        switch (CONTACT_STRATEGY) {
        case RoundRobin:
            strategy = "RoundRobin";
            if (msg.getSourceID() != 1 && msg.getSourceID() != 0) {
                contactQ.push(msg.getSourceID()); // add the new contact to the
                                                  // head, since its never been
                                                  // used, should be first in
                                                  // line
            }
            break;
        case FastestFirst:
            strategy = "FastestFirst";
            ContactObj newCCandidate = new ContactObj(msg.getSourceID());
            if (msg.getSourceID() != 1 && msg.getSourceID() != 0
                && !contactSSet.contains(newCCandidate)) {
                contactSSet.add(newCCandidate);
            }
            break;
        default:
            Log.e(ContactManager.class.getName(), "addContact default switch!");
        }
        Log.d(ContactManager.class.getName(), "contact selection strategy is: " + strategy);
    }

}
