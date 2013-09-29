package meshonandroid;

import meshonandroid.pdu.MeshPduInterface;

public interface MeshMsgReceiver {

    public void handleMessage(MeshPduInterface msg);

}
