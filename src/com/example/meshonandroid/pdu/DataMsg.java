package com.example.meshonandroid.pdu;

import java.io.UnsupportedEncodingException;

import adhoc.aodv.exception.BadPduFormatException;

import com.example.meshonandroid.Constants;

public class DataMsg implements MeshPduInterface {

    protected byte type;
    protected int srcID;
    protected int broadcastID;
    protected int packetID;
    protected byte[] data;
    protected boolean areMorePackets;

    public DataMsg(){};
    /**
     * Constructor for creating a data message request PDU
     *
     * @param srcId
     *            the originators node id
     * @param packetID
     *            packet identifier
     * @param broadcastId
     *            along with the source address this number uniquely identifies
     *            this route request PDU
     * @param areMorePackets
     *          field indicating how many packets the data had to be split into
     *
     */
    public DataMsg(int srcId, int packetID, int broadcastId, byte type, byte[] data, boolean areMorePackets) {
        this.srcID = srcId;
        this.packetID = packetID;
        this.type = type;
        this.broadcastID = broadcastId;
        this.data = data;
        this.areMorePackets = areMorePackets;
    }

    public DataMsg(int srcId, int packetID, int broadcastId, byte type, byte[] data){
        this(srcId, packetID, broadcastId, type, data, false);
    }


    /*
     * public int getBroadcastId(){ return broadcastID; }
     *
     * public int getSourceSequenceNumber(){ return srcSeqNum; }
     *
     * public void setDestSeqNum(int destinationSequenceNumber){ destSeqNum =
     * destinationSequenceNumber; }
     *
     * public int getHopCount(){ return hopCount; }
     *
     * public void incrementHopCount(){ hopCount++; }
     */

    public String toReadableString(){
        return "type:" + type + "; srcID:" + srcID + "; broadcastID:" + broadcastID + "; packetID:"
               + packetID;// + "; data:"+new String(data, Constants.encoding);
    }


    @Override
    public String toString() {
        try {
            return type + ";" + srcID + ";" + broadcastID + ";" + packetID + ";" + areMorePackets + ";" + new String(data, Constants.encoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "error encoding string";
        }
    }


    @Override
    public byte[] toBytes() throws UnsupportedEncodingException {
        byte[] bytes = this.toString().getBytes(Constants.encoding);
        //Log.d("toBytes", "toBytes returning: "+new String(bytes, Constants.encoding));
        return bytes;
    }


    @Override
    public void parseBytes(byte[] rawPdu) throws BadPduFormatException {
        String tag = "DataMsg:parseBytes";
        String[] s = new String(rawPdu).split(";", 8);
        if (s.length != 6) { throw new BadPduFormatException(
                                                             "RREQ_DATA: could not split "
                                                                 + "the expected # of arguments from rawPdu. "
                                                                 + "Expecteded 6 args but were given "
                                                                 + s.length); }
        try {
            type = Byte.parseByte(s[0]);
            if (type != Constants.PDU_DATAMSG) { throw new BadPduFormatException(
                                                                                 "RREQ_DATA: pdu type did not match. "
                                                                                     + "Was expecting: "
                                                                                     + Constants.PDU_DATAMSG
                                                                                     + " but parsed: "
                                                                                     + type); }
            srcID = Integer.parseInt(s[1]);
            broadcastID = Integer.parseInt(s[2]);
            packetID = Integer.parseInt(s[3]);
            areMorePackets = Boolean.parseBoolean(s[4]);
            data = s[5].getBytes(Constants.encoding);
            //Log.d(tag, "parsed bytes to DataMsg: "+this.toReadableString());
        } catch (NumberFormatException e) {
            throw new BadPduFormatException(
                                            "RREQ: falied in parsing arguments to the desired types");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public byte[] getDataBytes(){
        return data;
    }

    public void setDataBytes(byte[] b){
        data = b;
    }

    @Override
    public byte getPduType() {
        return type;
    }

    public int getBroadcastID(){
        return broadcastID;
    }

    public int getPacketID() {
        return packetID;
    }

    @Override
    public int getSourceID() {
        return this.srcID;
    }

    public void setPacketID(int id) {
        packetID = id;
    }

    public boolean getAreMorePackets(){
        return areMorePackets;
    }
}
