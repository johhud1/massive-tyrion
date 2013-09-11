package Logging;

import android.database.sqlite.SQLiteDatabase;

public class LoggingDBUtils {
    public static PerfDBHelper mDBHelper;

    public static void setRequestEndTimeAndContentSize(long Id, long endTime, int cs){
        mDBHelper.setRequest(Id, endTime, cs);
    }
    public static long addRequest(long startTime){
        return mDBHelper.addRequest(startTime);
    }
}
