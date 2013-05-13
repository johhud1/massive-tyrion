package com.example.meshonandroid.pdu;

import com.example.meshonandroid.Constants;

import adhoc.aodv.exception.BadPduFormatException;

public class DataMsg implements MeshPduInterface {

    private byte type;
    private int srcID;
    private int broadcastID;
    private int packetID;
    private byte[] data;

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
     */
    public DataMsg(int srcId, int packetID, int broadcastId, byte[] data) {
        this.srcID = srcId;
        this.packetID = packetID;
        type = Constants.PDU_DATAMSG;
        this.broadcastID = broadcastId;
        this.data = data;
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

    public String toReadableString() {
        return "type:" + type + "; srcID:" + srcID + "; broadcastID:" + broadcastID + "; packetID:"
               + packetID + "; data:"+String.valueOf(data);
    }


    @Override
    public String toString() {
        return type + ";" + srcID + ";" + broadcastID + ";" + packetID + ";" + data;
    }


    @Override
    public byte[] toBytes() {
        return this.toString().getBytes();
    }


    @Override
    public void parseBytes(byte[] rawPdu) throws BadPduFormatException {
        String[] s = new String(rawPdu).split(";", 7);
        if (s.length != 5) { throw new BadPduFormatException(
                                                             "RREQ_DATA: could not split "
                                                                 + "the expected # of arguments from rawPdu. "
                                                                 + "Expecteded 7 args but were given "
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
            data = s[4].getBytes();
        } catch (NumberFormatException e) {
            throw new BadPduFormatException(
                                            "RREQ: falied in parsing arguments to the desired types");
        }
    }

    public byte[] getBytes(){
        return data;
    }

    public void setBytes(byte[] b){
        data = b;
    }

    @Override
    public byte getPduType() {
        return type;
    }


    public int getPacketID() {
        return packetID;
    }


    public void setPacketID(int id) {
        packetID = id;
    }
}
