package meshonandroid.pdu;

import edu.android.meshonandroid.Constants;

public class ConnectionClosedMsg extends Msg {

    public ConnectionClosedMsg(){
    }
    public ConnectionClosedMsg(int srcId, int packetId, int broadcastId) {
        super(srcId, packetId, broadcastId);
        mType = Constants.PDU_CONNECTIONCLOSEMSG;
    }

}
