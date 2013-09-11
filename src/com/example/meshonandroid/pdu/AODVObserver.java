package com.example.meshonandroid.pdu;

import java.util.Observable;
import java.util.Observer;

import adhoc.aodv.Node;
import adhoc.aodv.Node.MessageToObserver;
import adhoc.aodv.Node.PacketToObserver;
import adhoc.aodv.ObserverConst;
import adhoc.aodv.exception.BadPduFormatException;
import android.util.Log;

import com.example.meshonandroid.Constants;
import com.example.meshonandroid.HandlerActivity;
import com.example.meshonandroid.OutLinkManager;
import com.example.meshonandroid.Utils;



public class AODVObserver extends Observable implements Observer {
    private OutLinkManager mOutLinkManager;
    private HandlerActivity mActivity;

    public AODVObserver(Node node, int mId, HandlerActivity mainActivity, OutLinkManager oman) {
        node.addObserver(this);
        mOutLinkManager =  oman;
        mOutLinkManager.setAODVObserver(this);
        mActivity = mainActivity;
        this.addObserver(mOutLinkManager);
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
            //can keep a list of ProxyThreads perhaps, indexed by requestId for quick dispatch
    private void parseMessage(int senderID, byte[] data){
        String tag = "AODVObserver:parseMessage";
        setChanged();
        String[] split = new String(data).split(";",2);
        try {
            int type = Integer.parseInt(split[0]);
            switch (type) {
            case Constants.PDU_DATAREQMSG:
                DataMsg dataReqMsg = new DataReqMsg();
                dataReqMsg.parseBytes(data);
                System.out.println("Received DataReqMsg: "+dataReqMsg.toReadableString());
                setMainTextViewWithString("Recieved DataReqMsg. srcID:"+dataReqMsg.srcID + " bId: "+dataReqMsg.getBroadcastID());
                notifyObservers(dataReqMsg);
                //notifyObservers("recieved PDU_DATAMSG: "+dataMsg.toReadableString());
                break;
            case Constants.PDU_DATAREPMSG:
                DataMsg dataRepMsg = new DataRepMsg();
                dataRepMsg.parseBytes(data);
                setMainTextViewWithString("Recieved DataRepMsg. srcID:"+dataRepMsg.srcID + " bId: "+dataRepMsg.getBroadcastID());
                System.out.println("Received DataRepMsg: "+dataRepMsg.toReadableString());
                notifyObservers(dataRepMsg);
                //notifyObservers("recieved PDU_DATAMSG: "+dataMsg.toReadableString());
                break;
            case Constants.PDU_DATAMSG:
                DataMsg dataMsg = new DataMsg();
                dataMsg.parseBytes(data);
                System.out.println("Received DataMsg: "+dataMsg.toReadableString());
                setMainTextViewWithString("Got DataMsg");
                notifyObservers(dataMsg);
                //notifyObservers("recieved PDU_DATAMSG: "+dataMsg.toReadableString());
                break;
            case Constants.PDU_EXITNODEREQ:
                System.out.println("Received: Exit Node Request msg");
                setMainTextViewWithString("Recieved ExitNodeReq");
                ExitNodeReqPDU exitMsg = new ExitNodeReqPDU();
                exitMsg.parseBytes(data);
                Log.d(tag, exitMsg.toReadableString());
                notifyObservers(exitMsg);
                //notifyObservers("recieved PDU_EXITNODEREQ: "+exitMsg.toReadableString());
                mOutLinkManager.connectionRequested(senderID, exitMsg); //sets up neccessary state and send reply;
                break;
            case Constants.PDU_EXITNODEREP:
                System.out.println("Received: PDU Exit Node Reply msg");
                ExitNodeRepPDU exitRep = new ExitNodeRepPDU();
                exitRep.parseBytes(data);
                setMainTextViewWithString("Recieved ExitNodeRep. SrcId: "+exitRep.getSourceID()+ " bId: "+ exitRep.getBroadcastID());
                Log.d(tag, exitRep.toReadableString());
                notifyObservers(exitRep);
                break;
            case Constants.PDU_IPDISCOVER:
                System.out.println("Recieved: IPDiscover msg");
                setMainTextViewWithString("Recieved IPDiscover msg");
                IPDiscoverMsg ipMsg = new IPDiscoverMsg();
                ipMsg.parseBytes(data);
                notifyObservers(ipMsg);
                break;
            case Constants.PDU_CONNECTDATAMSG:
                System.out.println("Recieved: ConnectData msg");
                setMainTextViewWithString("Recieved ConnectDataMsg");
                ConnectDataMsg cMsg = new ConnectDataMsg();
                cMsg.parseBytes(data);
                Log.d(tag, cMsg.toReadableString());
                notifyObservers(cMsg);
                break;
            case Constants.PDU_CONNECTIONCLOSEMSG:
                System.out.println("Recieved: ConnectionClosed msg");
                setMainTextViewWithString("Recieved ConnectionClosedMsg");
                ConnectionClosedMsg CCMsg = new ConnectionClosedMsg();
                CCMsg.parseBytes(data);
                Log.d(tag, CCMsg.toReadableString());
                notifyObservers(CCMsg);
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

}

