package com.example.meshonandroid;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.util.Log;




@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MainPrefFrag extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    private String hostKey = getResources().getString(R.string.host_key);
    private String portKey = getResources().getString(R.string.port_key);
    private String isRunningKey = getResources().getString(R.string.isRunning_key);

    private EditTextPreference mHostTextPref;
    private EditTextPreference mPortTextPref;
    private CheckBoxPreference mIsRunningCB;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        mHostTextPref = (EditTextPreference) findPreference("host");
        mPortTextPref = (EditTextPreference) findPreference("port");
        mIsRunningCB = (CheckBoxPreference) findPreference("isRunning");
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

}
