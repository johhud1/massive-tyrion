package com.example.meshonandroid;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

public class TabHostActivity extends TabActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tabhost_layout);

        Resources res = getResources(); // Resource object to get Drawables
        TabHost tabHost = getTabHost();  // The activity TabHost
        TabHost.TabSpec spec;  // Resusable TabSpec for each tab
        Intent intent;  // Reusable Intent for each tab

        // Create an Intent to launch an Activity for the tab (to be reused)
        intent = new Intent().setClass(this, MainPrefActivity.class);

        // Initialize a TabSpec for each tab and add it to the TabHost
        spec = tabHost.newTabSpec("artists").setIndicator("Settings",
                          res.getDrawable(R.drawable.ic_launcher))
                      .setContent(intent);
        tabHost.addTab(spec);

        // Do the same for the other tab
        intent = new Intent().setClass(this, NetworkInfoActivity.class);
        spec = tabHost.newTabSpec("songs").setIndicator("Network Info",
                          res.getDrawable(R.drawable.ic_launcher))
                      .setContent(intent);
        tabHost.addTab(spec);

        tabHost.setCurrentTab(2);
    }
}
