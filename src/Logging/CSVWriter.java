package Logging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.util.Log;

public class CSVWriter {
    private String delim = ",";
    private FileWriter fw;
    private int numFields = 0;


    CSVWriter(String[] fields){
        String tag = "CSVWriter:constructor";
        String name = "MOA_csv_log"+(new Date().toLocaleString());

        numFields = fields.length;
        try {
            fw = new FileWriter(new File(name));
            for(int i=0; i<fields.length; i++){
                fw.write(fields[i]+delim);
            }
            fw.write('\n');
        } catch (IOException e) {
            Log.e(tag, "error creating csv file");
            e.printStackTrace();
        }
    }

    public void addRecord(String[] vals) throws Exception{
        if(vals.length != numFields){
            throw new Exception("number of values("+vals.length+") doesn't match number of records in this CSV ("+numFields+")");
        }
        for(int i=0; i<vals.length; i++){
            fw.write("\""+vals[i]+"\"");
        }
        fw.write('\n');
    }
    public void close() throws IOException{
        fw.close();
    }
}
