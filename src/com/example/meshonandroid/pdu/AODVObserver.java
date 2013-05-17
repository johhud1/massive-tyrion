package com.example.meshonandroid.pdu;

import java.util.Observable;
import java.util.Observer;

import com.example.meshonandroid.Constants;

import adhoc.aodv.Node;
import adhoc.aodv.ObserverConst;
import adhoc.aodv.Node.MessageToObserver;
import adhoc.aodv.Node.PacketToObserver;
import adhoc.aodv.exception.BadPduFormatException;
import android.util.Log;
import android.widget.TextView;


public class AODVObserver implements Observer {
    private TextView outField;

    public AODVObserver(Node node, TextView outputField) {
        node.addObserver(this);
        outField = outputField;
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
        String[] split = new String(data).split(";",2);
        try {
            int type = Integer.parseInt(split[0]);
            switch (type) {
            case Constants.PDU_DATAMSG:
                System.out.println("Received: Msg");
                DataMsg dataMsg = new DataMsg();
                dataMsg.parseBytes(data);
                outField.setText(outField.getText()+"\n"+dataMsg.toReadableString());
                break;
            case Constants.PDU_EXITNODEREQ:
                System.out.println("Received: Exit Node Request msg");
                ExitNodeReqPDU exitMsg = new ExitNodeReqPDU();
                exitMsg.parseBytes(data);
                Log.d(tag, exitMsg.toReadableString());
                break;
            case Constants.PDU_EXITNODEREP:
                System.out.println("Received: PDU Exit Node Reply msg");
                ExitNodeRepPDU repMsg = new ExitNodeRepPDU();
                repMsg.parseBytes(data);
                Log.d(tag, repMsg.toReadableString());
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
            //discard the message.
        } catch (BadPduFormatException e) {
            //discard the message
            // Message is in the domain of invalid messages
        }
    }

}

