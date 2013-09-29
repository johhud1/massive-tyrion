package meshonandroid.pdu;

import java.io.UnsupportedEncodingException;

import meshonandroid.Constants;

import adhoc.aodv.exception.BadPduFormatException;


public class DataRepMsg extends DataMsg {

    public DataRepMsg(int srcId, int packetID, int broadcastId, byte[] data, boolean areMore){
        super(srcId, packetID, broadcastId, Constants.PDU_DATAREPMSG, data, areMore);
    }

    public DataRepMsg(int srcId, int packetID, int broadcastId, byte[] data){
        super(srcId, packetID, broadcastId, Constants.PDU_DATAREPMSG, data);
    }

    public DataRepMsg() {
        super();
    }

    @Override
    public void parseBytes(byte[] rawPdu) throws BadPduFormatException {
        String tag = "DataReqMsg:parseBytes";
        String[] s = new String(rawPdu).split(";", 7);
        if (s.length != 6) { throw new BadPduFormatException(
                                                             "DATAREPMSG: could not split "
                                                                 + "the expected # of arguments from rawPdu. "
                                                                 + "Expecteded 6 args but were given "
                                                                 + s.length); }
        try {
            type = Byte.parseByte(s[0]);
            if (type != Constants.PDU_DATAREPMSG) { throw new BadPduFormatException(
                                                                                 "DATAREPMSG: pdu type did not match. "
                                                                                     + "Was expecting: "
                                                                                     + Constants.PDU_DATAREPMSG
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
                                            "DATAREPMSG: falied in parsing arguments to the desired types");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

}
