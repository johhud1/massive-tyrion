package meshonandroid;

import com.example.meshonandroid.R;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;



public class MainPrefActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    private String hostKey;
    private String portKey;
    private String isRunningKey;

    private EditTextPreference mHostTextPref;
    private EditTextPreference mPortTextPref;
    private CheckBoxPreference mIsRunningCB;

    int lastBroadcastId = 0;
    int lastDataRRId = 0;
    int myContactID;

    private PreferenceFragment mPrefFrag;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hostKey = getResources().getString(R.string.host_key);
        portKey = getResources().getString(R.string.port_key);
        //isRunningKey = getResources().getString(R.string.isRunning_key);

        addPreferencesFromResource(R.xml.preferences);

        mHostTextPref = (EditTextPreference) findPreference(hostKey);
        mPortTextPref = (EditTextPreference) findPreference(portKey);
        //mIsRunningCB = (CheckBoxPreference) findPreference(isRunningKey);

        //myContactID = getMyID();

    }

    @Override
    public void onResume(){
        super.onResume();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);
        mHostTextPref.setSummary(sp.getString(hostKey, "127.0.0.1"));
        mPortTextPref.setSummary(sp.getString(portKey, "8080"));
    }

    @Override
    public void onPause(){
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        String tag = "MainPrefFrag:onPreferenceChange";

        if(key.equals(isRunningKey)) {
            Log.d(tag, "isRunning preference changed");

        }
        else if(key.equals(hostKey)){
            Log.d(tag, "host preference changed");
            mHostTextPref.setSummary(sp.getString(hostKey, "127.0.0.1"));
        }
        else if(key.equals(portKey)){
            Log.d(tag, "port preference changed");
            mPortTextPref.setSummary(sp.getString(portKey, "8080"));
        }
        else{

            Log.d(tag, "no preference key match");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


}
