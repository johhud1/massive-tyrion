package Logging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQuery;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
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
    private static final String TIME_VAR_TYPE = " INTEGER,";
    private static final String CS_VAR_TYPE = " INTEGER,";

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table " + TABLE_REQUESTS + "("
                                                  + COLUMN_ID
                                                  + " integer primary key autoincrement,"
                                                  + COLUMN_START_TIME + TIME_VAR_TYPE
                                                  + COLUMN_END_TIME + TIME_VAR_TYPE
                                                  + COLUMN_CONTENT_SIZE + CS_VAR_TYPE
                                                  + COLUMN_RES_HOST + " text,"
                                                  + COLUMN_RES + " text"
                                                  + ");";


    //private SQLiteDatabase db;
    public PerfDBHelper(Context c) {
        // change name to null for in mem db
        super(c, "MOA_DB_" + (new Date().toLocaleString()), null, version);

    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(PerfDBHelper.class.getName(), "Upgrading database from version " + oldVersion + " to "
                                      + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REQUESTS);
        onCreate(db);
    }

    synchronized long addRequest(long start){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_START_TIME, start);
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

}
