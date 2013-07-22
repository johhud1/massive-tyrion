package com.example.meshonandroid.pdu;

import java.io.UnsupportedEncodingException;
import java.util.Observable;
import java.util.Observer;

import com.example.meshonandroid.Constants;
import com.example.meshonandroid.DataManager;
import com.example.meshonandroid.HandlerActivity;
import com.example.meshonandroid.MainPrefActivity;
import com.example.meshonandroid.OutLinkManager;

import adhoc.aodv.Node;
import adhoc.aodv.ObserverConst;
import adhoc.aodv.Node.MessageToObserver;
import adhoc.aodv.Node.PacketToObserver;
import adhoc.aodv.exception.BadPduFormatException;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;



public class AODVObserver extends Observable implements Observer {
    private OutLinkManager mTrafficMan;
    private DataManager mDataMan;
    private HandlerActivity mActivity;

    public AODVObserver(Node node, int mId, HandlerActivity mainActivity) {
        node.addObserver(this);
        mTrafficMan =  new OutLinkManager(true, node, mId, mainActivity.getHandler());
        mActivity = mainActivity;
        mDataMan = new DataManager(node);
        this.addObserver(mTrafficMan);
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
                setMainTextViewWithString("Recieved DataReqMsg");
                notifyObservers(dataReqMsg);
                //notifyObservers("recieved PDU_DATAMSG: "+dataMsg.toReadableString());
                break;
            case Constants.PDU_DATAREPMSG:
                DataMsg dataRepMsg = new DataRepMsg();
                dataRepMsg.parseBytes(data);
                setMainTextViewWithString("Recieved DataRepMsg. srcID:"+dataRepMsg.srcID);
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
                mTrafficMan.connectionRequested(senderID, exitMsg); //sets up neccessary state and send reply;
                break;
            case Constants.PDU_EXITNODEREP:
                System.out.println("Received: PDU Exit Node Reply msg");
                setMainTextViewWithString("Recieved ExitNodeRep");
                ExitNodeRepPDU exitRep = new ExitNodeRepPDU();
                exitRep.parseBytes(data);
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
                /*
            case Constants.PDU_CHAT_REQUEST:l

                ChatRequest chatReq = new ChatRequest();
                chatReq.parseBytes(data);
                //("Recived: Chat Req :  "+chatReq.getSequenceNumber());
                chatManager.chatRequestReceived(chatReq,senderID);
                break;
            case Constants.PDU_HELLO:

                Hello hello = new Hello();
                hello.parseBytes(data);
                System.out.println("TxtMsg - Reciver: Hello from ID: "+senderID+", Return: " + hello.replyThisMessage());
                contactManager.helloRecived(hello, senderID);
                break;
            case Constants.PDU_NO_SUCH_CHAT:
                //("Recived: No s Chat");
                NoSuchChat noSuchChat = new NoSuchChat();
                noSuchChat.parseBytes(data);
                chatManager.noSuchChatRecived(noSuchChat,senderID);
                */
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
        String tag = "AODVObserver:sendMsgWithString";
        Message m = new Message();
        m.arg1 = Constants.LOG_MSG_CODE;
        Bundle b = new Bundle();
        b.putString("msg", s);
        m.setData(b);
        mActivity.getHandler().sendMessage(m);
    }

}

