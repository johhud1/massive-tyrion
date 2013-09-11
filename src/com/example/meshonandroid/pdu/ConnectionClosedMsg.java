package com.example.meshonandroid.pdu;

import com.example.meshonandroid.Constants;

public class ConnectionClosedMsg extends Msg {

    public ConnectionClosedMsg(){
    }
    public ConnectionClosedMsg(int srcId, int packetId, int broadcastId) {
        super(srcId, packetId, broadcastId);
        mType = Constants.PDU_CONNECTIONCLOSEMSG;
    }

}
