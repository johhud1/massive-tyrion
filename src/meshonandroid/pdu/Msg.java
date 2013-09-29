package meshonandroid.pdu;

import java.io.UnsupportedEncodingException;

import meshonandroid.Constants;


import adhoc.aodv.exception.BadPduFormatException;

public class Msg implements MeshPduInterface {
    protected int mSrcId, mBroadcastId, mPacketId;
    protected byte mType;

    public Msg(){}

    public Msg(int srcId, int packetId, int broadcastId){
        mSrcId = srcId;
        mBroadcastId = broadcastId;
        mPacketId = packetId;
    }

    @Override
    public byte[] toBytes() {
        byte[] bytes = null;
        try {
            bytes = this.toString().getBytes(Constants.encoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return bytes;
    }


    @Override
    public void parseBytes(byte[] dataToParse) throws BadPduFormatException {
        String tag = Msg.class.getName();
        String[] s = new String(dataToParse).split(";", 8);
        if (s.length != 4) { throw new BadPduFormatException(
                                                             "MSG: could not split "
                                                                 + "the expected # of arguments from rawPdu. "
                                                                 + "Expecteded 4 args but were given "
                                                                 + s.length); }
        try {
            mType = Byte.parseByte(s[0]);
            mSrcId = Integer.parseInt(s[1]);
            mBroadcastId = Integer.parseInt(s[2]);
            mPacketId = Integer.parseInt(s[3]);
        } catch (NumberFormatException e) {
            throw new BadPduFormatException(
                                            "RREQ: falied in parsing arguments to the desired types");
        }
    }


    @Override
    public byte getPduType() {
        return mType;
    }


    @Override
    public int getSourceID() {
        return mSrcId;
    }


    @Override
    public int getPacketID() {
        return mPacketId;
    }


    @Override
    public int getBroadcastID() {
        return mBroadcastId;
    }


    @Override
    public String toReadableString()  {
        return "type:" + mType + "; srcID:" + mSrcId + "; broadcastID:" + mBroadcastId + "; packetID:"
            + mPacketId;
    }

    @Override
    public String toString(){
        return mType + ";" + mSrcId + ";" + mBroadcastId + ";" + mPacketId;
    }

}
