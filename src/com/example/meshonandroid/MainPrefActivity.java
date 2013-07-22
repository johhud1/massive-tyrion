package com.example.meshonandroid;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.BindException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import proxyServer.ProxyListener;
import adhoc.aodv.Node;
import adhoc.aodv.exception.InvalidNodeAddressException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.meshonandroid.pdu.AODVObserver;



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
    Node myNode;


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
    public void onDestroy() {
        super.onDestroy();
        if(myNode != null){
            myNode.stopThread();
        }
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
