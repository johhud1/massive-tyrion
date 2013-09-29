package meshonandroid.pdu;

import meshonandroid.Constants;
import adhoc.aodv.exception.BadPduFormatException;




public class ExitNodeReqPDU implements MeshPduInterface {

    private byte type;
    private int srcID;
    private int broadcastID;
    private int packetID;


    public ExitNodeReqPDU() {}


    /**
     * Constructor for creating a data route request PDU
     *
     * @param sourceNodeAddress
     *            the originators node address
     * @param sourceSequenceNumber
     *            originators sequence number
     * @param destinationSequenceNumber
     *            should be set to the last known sequence number of the
     *            destination
     * @param broadcastId
     *            along with the source address this number uniquely identifies
     *            this route request PDU
     */
    public ExitNodeReqPDU(int srcId, int packetID, int broadcastId) {
        this.srcID = srcId;
        this.packetID = packetID;
        type = Constants.PDU_EXITNODEREQ;
        this.broadcastID = broadcastId;
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
               + packetID;
    }


    @Override
    public String toString() {
        return type + ";" + srcID + ";" + broadcastID + ";" + packetID;
    }


    @Override
    public byte[] toBytes() {
        return this.toString().getBytes();
    }


    @Override
    public void parseBytes(byte[] rawPdu) throws BadPduFormatException {
        String[] s = new String(rawPdu).split(";", 7);
        if (s.length != 4) { throw new BadPduFormatException(
                                                             "RREQ_DATA: could not split "
                                                                 + "the expected # of arguments from rawPdu. "
                                                                 + "Expecteded 7 args but were given "
                                                                 + s.length); }
        try {
            type = Byte.parseByte(s[0]);
            if (type != Constants.PDU_EXITNODEREQ) { throw new BadPduFormatException(
                                                                                 "RREQ_DATA: pdu type did not match. "
                                                                                     + "Was expecting: "
                                                                                     + Constants.PDU_EXITNODEREQ
                                                                                     + " but parsed: "
                                                                                     + type); }
            srcID = Integer.parseInt(s[1]);
            broadcastID = Integer.parseInt(s[2]);
            packetID = Integer.parseInt(s[3]);
        } catch (NumberFormatException e) {
            throw new BadPduFormatException(
                                            "RREQ: falied in parsing arguments to the desired types");
        }
    }


    @Override
    public byte getPduType() {
        return type;
    }


    public int getPacketID() {
        return packetID;
    }

    public int getBroadcastID(){
        return broadcastID;
    }

    @Override
    public int getSourceID() {
        return this.srcID;
    }

    public void setPacketID(int id) {
        packetID = id;
    }

}
