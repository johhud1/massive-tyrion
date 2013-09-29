package meshonandroid.pdu;

import meshonandroid.Constants;
import adhoc.aodv.exception.BadPduFormatException;


public class ExitNodeRepPDU implements MeshPduInterface {
    private byte type;
    private int srcId;
    private int broadcastId;
    private int packetId;

    public ExitNodeRepPDU(){}

    public ExitNodeRepPDU(int srcId, int packetId, int broadcastId){
    this.srcId = srcId;
    this.packetId = packetId;
    type = Constants.PDU_EXITNODEREP;
    this.broadcastId = broadcastId;
    }
    @Override
    public byte[] toBytes() {
        return this.toString().getBytes();
    }


    @Override
    public void parseBytes(byte[] dataToParse) throws BadPduFormatException {
        String[] s = new String(dataToParse).split(";", 7);
        if (s.length != 4) { throw new BadPduFormatException(
                                                             "PDU_EXITNODEREP: could not split "
                                                                 + "the expected # of arguments from rawPdu. "
                                                                 + "Expecteded 7 args but were given "
                                                                 + s.length); }
        try {
            type = Byte.parseByte(s[0]);
            if (type != Constants.PDU_EXITNODEREP) { throw new BadPduFormatException(
                                                                                 "PDU_EXITNODEREP: pdu type did not match. "
                                                                                     + "Was expecting: "
                                                                                     + Constants.PDU_EXITNODEREP
                                                                                     + " but parsed: "
                                                                                     + type); }
            srcId = Integer.parseInt(s[1]);
            broadcastId = Integer.parseInt(s[2]);
            packetId = Integer.parseInt(s[3]);
        } catch (NumberFormatException e) {
            throw new BadPduFormatException(
                                            "PDU_EXITNODEREP: falied in parsing arguments to the desired types");
        }
    }


    @Override
    public byte getPduType() {
        return type;
    }

    public String toReadableString() {
        return "type:" + type + "; srcID:" + srcId + "; broadcastID:" + broadcastId + "; packetID:"
               + packetId;
    }
    @Override
    public String toString() {
        return type + ";" + srcId + ";" + broadcastId + ";" + packetId;
    }
   @Override
   public int getSourceID() {
       return this.srcId;
   }

   public int getPacketID(){
       return packetId;
   }

   public int getBroadcastID(){
       return broadcastId;
   }

}
