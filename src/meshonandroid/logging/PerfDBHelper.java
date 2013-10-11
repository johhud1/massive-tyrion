package meshonandroid.logging;

import java.util.Date;
import java.util.List;

import edu.android.meshonandroid.Constants;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.wifi.ScanResult;
import android.util.Log;



public class PerfDBHelper extends SQLiteOpenHelper {
    private static final int version = 1;

    private static final String TABLE_REQUESTS = "requests";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_START_TIME = " st";
    private static final String COLUMN_END_TIME = " et";
    private static final String COLUMN_CONTENT_SIZE = " cs";
    private static final String COLUMN_RES = " resource";
    private static final String COLUMN_RES_HOST = "reshost";
    private static final String COLUMN_NODE_ID = "nid";
    private static final String TIME_VAR_TYPE = " INTEGER,";
    private static final String CS_VAR_TYPE = " INTEGER,";

    private static final String TABLE_SCAN_RESULTS = "wifiscantable";
    private static final String SCAN_COLUMN_MAC = " macaddress";
    private static final String SCAN_COLUMN_RSSI = " rssi";
    private static final String SCAN_COLUMN_RSSI_TYPE = " INTEGER,";
    // Database creation sql statement
    private static final String REQUEST_DATABASE_CREATE = "create table " + TABLE_REQUESTS + "("
                                                  + COLUMN_ID
                                                  + " integer primary key autoincrement,"
                                                  + COLUMN_START_TIME + TIME_VAR_TYPE
                                                  + COLUMN_END_TIME + TIME_VAR_TYPE
                                                  + COLUMN_CONTENT_SIZE + CS_VAR_TYPE
                                                  + COLUMN_RES_HOST + " text,"
                                                  + COLUMN_RES + " text,"
                                                  + COLUMN_NODE_ID + " text"
                                                  + ");";
    private static final String SCAN_DATABASE_CREATE = "create table " + TABLE_SCAN_RESULTS + "("
                                                  + COLUMN_ID + " integer primary key autoincrement,"
                                                  + COLUMN_START_TIME + TIME_VAR_TYPE
                                                  + SCAN_COLUMN_RSSI + SCAN_COLUMN_RSSI_TYPE
                                                  + SCAN_COLUMN_MAC + " text"
                                                  + ");";

    /**
     * This DBHelper class handles storing the logging info in an sqlite3 DB. Each row stores info for a HTTP request
     * The columns are as follows:
     * COLUMN_ID - integer, primary key for entries.
     * COLUMN_START_TIME - integer, the request start time
     * COLUMN_END_TIME - integer, the request end time
     * COLUMN_CONTENT_SIZE - integer, request content size
     * COLUMN_RES_HOST - text, the host of the requested resource
     * COLUMN_RES - text, the resource url
     * COLUMN_NODE_ID - text, the node ID that is performing the request fetch
     * @param c
     */
    public PerfDBHelper(Context c) {
        // change name to null for in mem db
        super(c, "MOA_DB_" + (new Date().toLocaleString()), null, version);

    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(REQUEST_DATABASE_CREATE);
        db.execSQL(SCAN_DATABASE_CREATE);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(PerfDBHelper.class.getName(), "Upgrading database from version " + oldVersion + " to "
                                      + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REQUESTS);
        onCreate(db);
    }

    synchronized long addRequest(long start, int nodeID){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_START_TIME, start);
        cv.put(COLUMN_NODE_ID, nodeID);
        long id = db.insert(TABLE_REQUESTS, null, cv);
        Log.d(PerfDBHelper.class.getName(), "added request with dbId: "+id);
        if(id == -1){
            Log.e(PerfDBHelper.class.getName(), " error inserting value into db");
        }
        db.close();
        return id;
    }
    synchronized void setRequest(long id, long end, int cs){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_CONTENT_SIZE, cs);
        cv.put(COLUMN_END_TIME, end);
        Log.d(PerfDBHelper.class.getName(), "setting request info for dbId:" + id);
        int effected = db.update(TABLE_REQUESTS, cv, COLUMN_ID + " = " + id, null);
        if(effected != 1){
            Log.e(PerfDBHelper.class.getName(), " not 1 row effected by setRequest. effected= "+effected+ " CV= "+cv.toString()+" dbId= "+id);
        }
        db.close();
    }

    synchronized void setScanResult(List<ScanResult> results){
        SQLiteDatabase db = this.getWritableDatabase();
        for(ScanResult r : results){
            if(r.SSID.equalsIgnoreCase(Constants.MESH_WIFI_SSID)){
                Log.d(PerfDBHelper.class.getName(), " adding mesh network node wifi scan result to db: "+r.toString());
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_START_TIME, System.currentTimeMillis());
                cv.put(SCAN_COLUMN_MAC, r.BSSID);
                cv.put(SCAN_COLUMN_RSSI, r.level);
                db.insert(TABLE_SCAN_RESULTS, null, cv);
            } else {
                Log.d(PerfDBHelper.class.getName(), " ignoring ssid scan result: "+r.toString());
            }

        }
        db.close();

    }

}
