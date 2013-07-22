package com.example.meshonandroid.pdu;

import java.io.UnsupportedEncodingException;

import adhoc.aodv.exception.BadPduFormatException;

import com.example.meshonandroid.Constants;



public class IPDiscoverMsg extends DataMsg {
    protected boolean isReq;


    public IPDiscoverMsg() {
        super();
    }


    public IPDiscoverMsg(int srcId, int packetID, int broadcastId, boolean isReq) {
        super(srcId, packetID, broadcastId, Constants.PDU_IPDISCOVER, null);
        this.isReq = isReq;
    }


    @Override
    public void parseBytes(byte[] rawPdu) throws BadPduFormatException {
        String tag = "IPDiscover:parseBytes";
        String[] s = new String(rawPdu).split(";", 9);
        if (s.length != 6) { throw new BadPduFormatException(
                                                             "IPDISCOVER_DATA: could not split "
                                                                 + "the expected # of arguments from rawPdu. "
                                                                 + "Expecteded 6 args but were given "
                                                                 + s.length); }
        try {
            type = Byte.parseByte(s[0]);
            if (type != Constants.PDU_IPDISCOVER) { throw new BadPduFormatException(
                                                                                 "IPDISCOVER_DATA: pdu type did not match. "
                                                                                     + "Was expecting: "
                                                                                     + Constants.PDU_IPDISCOVER
                                                                                     + " but parsed: "
                                                                                     + type); }
            srcID = Integer.parseInt(s[1]);
            broadcastID = Integer.parseInt(s[2]);
            packetID = Integer.parseInt(s[3]);
            numRespPackets = Integer.parseInt(s[4]);
            isReq = Boolean.parseBoolean(s[5]);
            // Log.d(tag, "parsed bytes to DataMsg: "+this.toReadableString());
        } catch (NumberFormatException e) {
            throw new BadPduFormatException(
                                            "RREQ: falied in parsing arguments to the desired types");
        }
    }


    @Override
    public String toString() {

        return type + ";" + srcID + ";" + broadcastID + ";" + packetID + ";" + numRespPackets + ";"
               + isReq;

    }


    @Override
    public String toReadableString() {
        return "type:" + type + "; srcID:" + srcID + "; broadcastID:" + broadcastID + "; packetID:"
               + packetID + "; isReq: " + isReq;
    }


    public void setIsReq(boolean b) {
        isReq = b;
    }


    public boolean isReq() {
        return isReq;
    }

}
