package com.example.meshonandroid.pdu;

import java.io.UnsupportedEncodingException;

import adhoc.aodv.exception.BadPduFormatException;

import com.example.meshonandroid.Constants;

public class DataReqMsg extends DataMsg {

    public DataReqMsg(int srcId, int packetID, int broadcastId, byte[] data){
        super(srcId, packetID, broadcastId, Constants.PDU_DATAREQMSG, data);
    }

    public DataReqMsg(){
        super();
    }

    @Override
    public void parseBytes(byte[] rawPdu) throws BadPduFormatException {
        String tag = "DataReqMsg:parseBytes";
        String[] s = new String(rawPdu).split(";", 7);
        if (s.length != 6) { throw new BadPduFormatException(
                                                             "DATAREQMSG: could not split "
                                                                 + "the expected # of arguments from rawPdu. "
                                                                 + "Expecteded 6 args but were given "
                                                                 + s.length); }
        try {
            type = Byte.parseByte(s[0]);
            if (type != Constants.PDU_DATAREQMSG) { throw new BadPduFormatException(
                                                                                 "DATAREQMSG: pdu type did not match. "
                                                                                     + "Was expecting: "
                                                                                     + Constants.PDU_DATAREQMSG
                                                                                     + " but parsed: "
                                                                                     + type); }
            srcID = Integer.parseInt(s[1]);
            broadcastID = Integer.parseInt(s[2]);
            packetID = Integer.parseInt(s[3]);
            numRespPackets = Integer.parseInt(s[4]);
            data = s[5].getBytes(Constants.encoding);
            //Log.d(tag, "parsed bytes to DataMsg: "+this.toReadableString());
        } catch (NumberFormatException e) {
            throw new BadPduFormatException(
                                            "DATAREQ: falied in parsing arguments to the desired types");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

}
