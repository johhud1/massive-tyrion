package meshonandroid.logging;

import java.util.List;

import android.net.wifi.ScanResult;

public class LoggingDBUtils {
    public static PerfDBHelper mDBHelper;

    public static void setRequestEndTimeAndContentSize(long Id, long endTime, int cs){
        mDBHelper.setRequest(Id, endTime, cs);
    }
    public static long addRequest(long startTime, int id){
        return mDBHelper.addRequest(startTime, id);
    }

    public static void setScanResult(List<ScanResult> r){
        mDBHelper.setScanResult(r);
    }
}
