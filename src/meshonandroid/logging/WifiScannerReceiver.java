package meshonandroid.logging;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;
/***
 * Class that recieves wifiscan results. wifiscans should be running constantly whenever a request
 * is being handled by the mesh
 * @author Jack
 *
 */

public class WifiScannerReceiver extends BroadcastReceiver {

    private WifiManager wifiManager;

    public WifiScannerReceiver(WifiManager wifiManager){
        this.wifiManager = wifiManager;
    }
    @Override
    public void onReceive(Context arg0, Intent arg1) {
        StringBuilder sb = new StringBuilder();
        List<ScanResult>wifiList = wifiManager.getScanResults();
        LoggingDBUtils.setScanResult(wifiList);
        /*
        for(int i = 0; i < wifiList.size(); i++){
            sb.append(new Integer(i+1).toString() + ".");
            sb.append((wifiList.get(i)).toString());
            sb.append("\\n");
        }
        Log.d(WifiScannerReceiver.class.getName(), " scan result is: "+sb.toString());
        */
    }

}
