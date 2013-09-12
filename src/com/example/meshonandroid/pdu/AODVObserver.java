package com.example.meshonandroid.pdu;

import java.util.Observable;
import java.util.Observer;

import proxyServer.ConnectProxyThread;
import proxyServer.ProxyThread;

import adhoc.aodv.Node;
import adhoc.aodv.Node.MessageToObserver;
import adhoc.aodv.Node.PacketToObserver;
import adhoc.aodv.ObserverConst;
import adhoc.aodv.exception.BadPduFormatException;
import android.util.Log;
import android.util.SparseArray;

import com.example.meshonandroid.Constants;
import com.example.meshonandroid.ContactManager;
import com.example.meshonandroid.FreeIPManager;
import com.example.meshonandroid.HandlerActivity;
import com.example.meshonandroid.OutLinkManager;
import com.example.meshonandroid.Utils;



public class AODVObserver implements Observer {
    private OutLinkManager mOutLinkManager;
    private HandlerActivity mActivity;
    private ContactManager mContactManager;
    private FreeIPManager mIPManager;
    private SparseArray<ProxyThread> proxyThreadArray = new SparseArray<ProxyThread>();
    private SparseArray<ConnectProxyThread> connectProxyThreadArray = new SparseArray<ConnectProxyThread>();

    public AODVObserver(Node node, int mId, HandlerActivity mainActivity, OutLinkManager oman, ContactManager cman, FreeIPManager ipman) {
        node.addObserver(this);
        mOutLinkManager =  oman;
        //mOutLinkManager.setAODVObserver(this);
        mActivity = mainActivity;
        mContactManager = cman;
        mIPManager = ipman;
        //this.addObserver(mOutLinkManager);
    }

    @Override
    public void update(Observable o, Object arg) {
        String tag = "AODVObserver:update";
        MessageToObserver msg = (MessageToObserver)arg;
        int userPacketID, destination, type = msg.getMessageType();
        switch (type) {
        case ObserverConst.ROUTE_ESTABLISHMENT_FAILURE:
            int unreachableDestinationAddrerss  = (Integer)msg.getContainedData();
            //contactManager.routeEstablishmentFailurRecived(unreachableDestinationAddrerss);
            break;
        case ObserverConst.DATA_RECEIVED:
            parseMessage(   (Integer)((PacketToObserver)msg).getSenderNodeAddress(),
                            (byte[])msg.getContainedData()  );
            break;
        case ObserverConst.INVALID_DESTINATION_ADDRESS:
            userPacketID = (Integer)msg.getContainedData();
            //FIXME slet fra timer og Contacts
            break;
        case ObserverConst.DATA_SIZE_EXCEEDES_MAX:
            userPacketID = (Integer)msg.getContainedData();
            Log.e(tag, "DATA_SIZE_EXCEEDS_MAX");
            //FIXME slet fra timer
            break;
        case ObserverConst.ROUTE_INVALID:
            destination  = (Integer)msg.getContainedData();
            //contactManager.routeInvalidRecived(destination);
            break;
        case ObserverConst.ROUTE_CREATED:
            destination = (Integer)msg.getContainedData();
            //contactManager.routeEstablishedRecived(destination);
            break;
        default:
            break;
        }
    }
    //TODO: refactor this whole thing so I'm not using observers, and just call the methods on the appropriate objects
            //can keep a list of ProxyThreads perhaps, indexed by requestId for quick dispatch.
    //IN PROGRESS
    private void parseMessage(int senderID, byte[] data){
        String tag = "AODVObserver:parseMessage";
        //setChanged();
        String[] split = new String(data).split(";",2);
        try {
            int type = Integer.parseInt(split[0]);
            switch (type) {
            case Constants.PDU_DATAREQMSG:
                DataMsg dataReqMsg = new DataReqMsg();
                dataReqMsg.parseBytes(data);
                System.out.println("Received DataReqMsg: "+dataReqMsg.toReadableString());
                setMainTextViewWithString("Recieved DataReqMsg. srcID:"+dataReqMsg.srcID + " bId: "+dataReqMsg.getBroadcastID());
                mOutLinkManager.handleMessage(dataReqMsg);
                //notifyObservers("recieved PDU_DATAMSG: "+dataMsg.toReadableString());
                break;
            case Constants.PDU_DATAREPMSG:
                DataRepMsg dataRepMsg = new DataRepMsg();
                dataRepMsg.parseBytes(data);
                setMainTextViewWithString("Recieved DataRepMsg. srcID:"+dataRepMsg.srcID + " bId: "+dataRepMsg.getBroadcastID());
                System.out.println("Received DataRepMsg: "+dataRepMsg.toReadableString());
                //TODO: what should happen here is we pick out the proper proxyThread from a sparseArray of
                //ProxyThreads and call that threads handle message or whatever
                ProxyThread rThread = proxyThreadArray.get(dataRepMsg.broadcastID);
                if(rThread != null){
                    rThread.PushPacketOnDataRepQ(dataRepMsg);   
                } else {
                    Log.e(AODVObserver.class.getName()+":DATAREPMSG", "couldn't find the ProxyThread for requestID:"+dataRepMsg.broadcastID+". somehow that ProxyThread got remove, THIS IS BAD");
                }
                //notifyObservers(dataRepMsg);

                break;
            case Constants.PDU_DATAMSG:
                DataMsg dataMsg = new DataMsg();
                dataMsg.parseBytes(data);
                System.out.println("Received DataMsg: "+dataMsg.toReadableString());
                setMainTextViewWithString("Got DataMsg");
                //TODO: don't think anything happens in this case
                break;
            case Constants.PDU_EXITNODEREQ:
                System.out.println("Received: Exit Node Request msg");
                setMainTextViewWithString("Recieved ExitNodeReq");
                ExitNodeReqPDU exitMsg = new ExitNodeReqPDU();
                exitMsg.parseBytes(data);
                Log.d(tag, exitMsg.toReadableString());
                //notifyObservers(exitMsg);
                mOutLinkManager.connectionRequested(senderID, exitMsg); //sets up neccessary state and send reply;
                break;
            case Constants.PDU_EXITNODEREP:
                System.out.println("Received: PDU Exit Node Reply msg");
                ExitNodeRepPDU exitRep = new ExitNodeRepPDU();
                exitRep.parseBytes(data);
                setMainTextViewWithString("Recieved ExitNodeRep. SrcId: "+exitRep.getSourceID()+ " bId: "+ exitRep.getBroadcastID());
                Log.d(tag, exitRep.toReadableString());
                //we recieved an exit node reply from a live contact. add this contact to the ContactManager
                mContactManager.addContact(exitRep);
                break;
            case Constants.PDU_IPDISCOVER:
                System.out.println("Recieved: IPDiscover msg");
                setMainTextViewWithString("Recieved IPDiscover msg");
                IPDiscoverMsg ipMsg = new IPDiscoverMsg();
                ipMsg.parseBytes(data);
                mIPManager.handleMessage(ipMsg);
                break;
            case Constants.PDU_CONNECTDATAMSG:
                System.out.println("Recieved: ConnectData msg");
                setMainTextViewWithString("Recieved ConnectDataMsg");
                ConnectDataMsg cMsg = new ConnectDataMsg();
                cMsg.parseBytes(data);
                Log.d(tag, cMsg.toReadableString());
                int bId = cMsg.getBroadcastID();
                //pull the appropriate ConnectProxyThread and have call the message handling function
                ConnectProxyThread cpt = connectProxyThreadArray.get(bId);
                if(cpt != null){
                    cpt.handleMessage(cMsg);
                } else {
                    Log.e(AODVObserver.class.getName(), "ConnectProxyThread for that requestId (or broadcastId as it is sometimes known) couldn't be found. must've crashed and burned.");
                }

                break;
            case Constants.PDU_CONNECTIONCLOSEMSG:
                System.out.println("Recieved: ConnectionClosed msg");
                setMainTextViewWithString("Recieved ConnectionClosedMsg");
                ConnectionClosedMsg CCMsg = new ConnectionClosedMsg();
                CCMsg.parseBytes(data);
                Log.d(tag, CCMsg.toReadableString());
                mOutLinkManager.handleMessage(CCMsg);
                break;
            default:
                break;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            //discard the message.
        } catch (BadPduFormatException e) {
            e.printStackTrace();
            //discard the message
            // Message is in the domain of invalid messages
        }
    }

    private void setMainTextViewWithString(String s){
        Utils.addMsgToMainTextLog(mActivity.getHandler(), s);
    }

    public void removeProxyThread(int broadcastId) {
        proxyThreadArray.remove(broadcastId);

    }

    public void addProxyThread(int broadcastId, ProxyThread pt){
        proxyThreadArray.put(broadcastId, pt);
    }
    public void removeConnectProxyThread(int bId){
        connectProxyThreadArray.remove(bId);
    }
    public void addConnectProxyThread(int broadcastId, ConnectProxyThread cpt) {
        connectProxyThreadArray.put(broadcastId, cpt);
    }


}

