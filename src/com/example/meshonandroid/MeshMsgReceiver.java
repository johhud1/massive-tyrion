package com.example.meshonandroid;

import com.example.meshonandroid.pdu.MeshPduInterface;

public interface MeshMsgReceiver {

    public void handleMessage(MeshPduInterface msg);

}
