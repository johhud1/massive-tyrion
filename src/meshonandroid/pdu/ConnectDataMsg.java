package meshonandroid.pdu;

import java.io.UnsupportedEncodingException;

import meshonandroid.Constants;

import adhoc.aodv.exception.BadPduFormatException;


public class ConnectDataMsg extends DataMsg {
    protected boolean isReq;
    protected boolean isConnectionSetupMsg;

    public ConnectDataMsg(int srcId, int packetID, int broadcastId, byte[] data, boolean isReq, boolean isConn){
        this(srcId, packetID, broadcastId, data);
        this.isReq = isReq;
        this.isConnectionSetupMsg = isConn;
    }
    public ConnectDataMsg(int srcId, int packetID, int broadcastId, byte[] data){
        super(srcId, packetID, broadcastId, Constants.PDU_CONNECTDATAMSG, data);
    }

    public ConnectDataMsg(){
        super();
    }

    @Override
    public void parseBytes(byte[] rawPdu) throws BadPduFormatException {
        String tag = "ConnectDataMsg:parseBytes";
        String[] s = new String(rawPdu).split(";", 8);
        if (s.length != 7) { throw new BadPduFormatException(
                                                             "CONNECTDATAMSG: could not split "
                                                                 + "the expected # of arguments from rawPdu. "
                                                                 + "Expecteded 7 args but were given "
                                                                 + s.length); }
        try {
            type = Byte.parseByte(s[0]);
            if (type != Constants.PDU_CONNECTDATAMSG) { throw new BadPduFormatException(
                                                                                 "CONNECTDATAMSG: pdu type did not match. "
                                                                                     + "Was expecting: "
                                                                                     + Constants.PDU_CONNECTDATAMSG
                                                                                     + " but parsed: "
                                                                                     + type); }
            srcID = Integer.parseInt(s[1]);
            broadcastID = Integer.parseInt(s[2]);
            packetID = Integer.parseInt(s[3]);
            isReq = Boolean.parseBoolean(s[4]);
            isConnectionSetupMsg = Boolean.parseBoolean(s[5]);
            data = s[6].getBytes(Constants.encoding);
            //Log.d(tag, "parsed bytes to DataMsg: "+this.toReadableString());
        } catch (NumberFormatException e) {
            throw new BadPduFormatException(
                                            "CONNECTDATAMSG: falied in parsing arguments to the desired types");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        try {
            return type + ";" + srcID + ";" + broadcastID + ";" + packetID + ";" + isReq + ";" + isConnectionSetupMsg + ";" + new String(data, Constants.encoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "error encoding string";
        }
    }

    public String toReadableString(){
        return "type:" + type + "; srcID:" + srcID + "; broadcastID:" + broadcastID + "; packetID:"
               + packetID + ";" +isReq + ";" + isConnectionSetupMsg;// + "; data:"+new String(data, Constants.encoding);
    }

    public boolean isReq(){
        return isReq;
    }
    public boolean isConnectionSetupMsg(){
        return isConnectionSetupMsg;
    }
}
