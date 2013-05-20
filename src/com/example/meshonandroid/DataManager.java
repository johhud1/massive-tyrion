package com.example.meshonandroid;

import com.example.meshonandroid.pdu.DataMsg;
import adhoc.aodv.Node;

public class DataManager {
	private Node mNode;
	private int mContactId;
	
	
	public DataManager(Node node){
		mNode = node;
	}
	
	public void sendDataMsg(int dest){
		String msg = "Hello";
		DataMsg dataMsg = new DataMsg(mContactId, 0, 0, msg.getBytes());
		mNode.sendData(0, dest, dataMsg.toBytes());
		
	}
}
