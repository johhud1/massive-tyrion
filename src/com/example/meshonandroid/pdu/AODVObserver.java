package com.example.meshonandroid.pdu;

import java.io.UnsupportedEncodingException;
import java.util.Observable;
import java.util.Observer;

import com.example.meshonandroid.Constants;
import com.example.meshonandroid.DataManager;
import com.example.meshonandroid.MainActivity;
import com.example.meshonandroid.TrafficManager;

import adhoc.aodv.Node;
import adhoc.aodv.ObserverConst;
import adhoc.aodv.Node.MessageToObserver;
import adhoc.aodv.Node.PacketToObserver;
import adhoc.aodv.exception.BadPduFormatException;
import android.util.Log;



public class AODVObserver extends Observable implements Observer {
    private TrafficManager mTrafficMan;
    private DataManager mDataMan;
    private MainActivity mActivity;

    public AODVObserver(Node node, int mId, MainActivity mainActivity) {
        node.addObserver(this);
        mTrafficMan =  new TrafficManager(true, node, mId);
        mDataMan = new DataManager(node);
        this.addObserver(mainActivity);
        this.addObserver(mTrafficMan);
    }

    @Override
    public void update(Observable o, Object arg) {
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
            case Constants.PDU_DATAMSG:
                DataMsg dataMsg = new DataMsg();
                dataMsg.parseBytes(data);
                System.out.println("Received DataMsg: "+dataMsg.toReadableString());
                notifyObservers(dataMsg);
                //notifyObservers("recieved PDU_DATAMSG: "+dataMsg.toReadableString());
                break;
            case Constants.PDU_EXITNODEREQ:
                System.out.println("Received: Exit Node Request msg");
                ExitNodeReqPDU exitMsg = new ExitNodeReqPDU();
                exitMsg.parseBytes(data);
                Log.d(tag, exitMsg.toReadableString());
                notifyObservers(exitMsg);
                //notifyObservers("recieved PDU_EXITNODEREQ: "+exitMsg.toReadableString());
                mTrafficMan.connectionRequested(senderID); //sets up neccessary state and send reply;
                break;
            case Constants.PDU_EXITNODEREP:
                System.out.println("Received: PDU Exit Node Reply msg");
                ExitNodeRepPDU exitRep = new ExitNodeRepPDU();
                exitRep.parseBytes(data);
                Log.d(tag, exitRep.toReadableString());
                boolean  c = hasChanged();
                notifyObservers(exitRep);
                c = hasChanged();
                //notifyObservers("recieved PDU_EXITNODEREP: "+exitRep.toReadableString());
                mDataMan.sendDataMsg(senderID);

                //ExitNodeRepPDU repMsg = new ExitNodeRepPDU();
                //repMsg.parseBytes(data);
                //Log.d(tag, repMsg.toReadableString());
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
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}

