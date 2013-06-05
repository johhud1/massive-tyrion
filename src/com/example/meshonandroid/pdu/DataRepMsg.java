package com.example.meshonandroid.pdu;

import java.io.UnsupportedEncodingException;

import adhoc.aodv.exception.BadPduFormatException;

import com.example.meshonandroid.Constants;

public class DataRepMsg extends DataMsg {

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
        if (s.length != 5) { throw new BadPduFormatException(
                                                             "DATAREPMSG: could not split "
                                                                 + "the expected # of arguments from rawPdu. "
                                                                 + "Expecteded 5 args but were given "
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
            data = s[4].getBytes(Constants.encoding);
            //Log.d(tag, "parsed bytes to DataMsg: "+this.toReadableString());
        } catch (NumberFormatException e) {
            throw new BadPduFormatException(
                                            "DATAREPMSG: falied in parsing arguments to the desired types");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
