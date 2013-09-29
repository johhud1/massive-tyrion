package meshonandroid;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import meshonandroid.pdu.MeshPduInterface;

import adhoc.aodv.Node;
import android.util.Log;




public class FreeIPManager {

    private boolean[] availableIPs = new boolean[Constants.MAX_NODES]; // true
    // indicates
    // that address
    // is available
    private short freeIPs = 254;


    public FreeIPManager() {
        for (int i = 0; i < availableIPs.length; i++) {
            availableIPs[i] = true;
        }
    }


/**
 *
 * @return a IP that doesn't appear to be in use already in the mesh
 */
    public int getFreeID() {
        String tag = "FreeIPManager:getFreeID";

        if (freeIPs != 0) {
            int ip = -1;
            int i;
            for (i = 2; i < availableIPs.length; i++) {
                if (availableIPs[i]) {
                    ip = i;
                    break;
                }
            }
            availableIPs[i] = false;
            freeIPs--;
            return ip;
        } else {
            Log.e(tag, "no free ip's available :( RUNNN!");
            return -1;
        }
    }



    public void routeEstablishedRecived(int destination) {
        String tag = FreeIPManager.class.getName()+":routeEstablishedRecieved";
        Log.d(tag, "got route establishedRecieved from " + destination
                   + ". setting that address as unavailable");
        availableIPs[destination] = false;
        freeIPs--;
    }


    public void invalidDestinationAddress(int userPacketID) {
        // TODO Auto-generated method stub

    }


}
