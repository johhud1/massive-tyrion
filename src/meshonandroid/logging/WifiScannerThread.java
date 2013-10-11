package meshonandroid.logging;

import edu.android.meshonandroid.Constants;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WifiScannerThread extends Thread{

    private boolean running;
    private int numClients;
    WifiScannerReceiver receiver;
    private Context c;

    public WifiScannerThread(Context c){
        super("ScannerThread");
        running = true;
        numClients  = 1;
        this.c = c;
    }
    public void stopScanning(){
        if(receiver != null){
            c.unregisterReceiver(receiver);
        }
        running = false;
    }

    @Override
    public void run(){
        WifiManager mainWifi = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
        receiver = new WifiScannerReceiver(mainWifi);
        if(!mainWifi.isWifiEnabled()){
            Log.d(WifiScannerThread.class.getName(), " wifi disabled. Enabling...");
            mainWifi.setWifiEnabled(true);
        }
        c.registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        //c.sendBroadcast(new Intent(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mainWifi.startScan();
        while(running){
            if(numClients > 0){
                if(!mainWifi.startScan()){
                    Log.e(WifiScannerThread.class.getName(), " wifiScan start failed");
                }
            }
            try {
                Thread.sleep(Constants.WIFI_SCAN_SLEEP_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
