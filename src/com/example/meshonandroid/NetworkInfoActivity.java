package com.example.meshonandroid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Enumeration;

import proxyServer.ProxyListener;
import Logging.LoggingDBUtils;
import Logging.PerfDBHelper;
import adhoc.aodv.Node;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.example.meshonandroid.pdu.AODVObserver;



public class NetworkInfoActivity extends Activity implements HandlerActivity {

    static String PROXY_IP = "127.0.0.1";
    static int PROXY_PORT = 8080;

    private TextView outputTV;
    private WebView mWV;

    public Handler handler;
    private BroadcastReceiver mReceiver;

    AODVObserver mObs;

    //TODO: should move this traffic data into MeshService so doesn't get wiped with the activity
    private double trafficThroughMesh = 0;
    private double trafficFromMesh = 0;

    CheckBox mCb;
    @Override
    @SuppressLint("NewApi")
    public void onCreate(Bundle savedInstanceState) {
        // BAD - should get rid of, but makes for faster, dont' have to put all
        // network activities in seperate threads
        Log.d(NetworkInfoActivity.class.getName(), "in onCreate");
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        //adhoc.etc.Debug.setDebugStream(System.out);


        setContentView(R.layout.netinfo_layout);
        mWV = (WebView) findViewById(R.id.webView);

        mWV.setWebViewClient(new myWebViewClient());
        Button loadPageBut = (Button) findViewById(R.id.load_page_but);
        loadPageBut.setOnClickListener(new urlReqOnClickListener("https://www.gmail.com")); // SET
                                                                                            // IP:port
                                                                                            // for
                                                                                            // server
                                                                                            // here!!!
        LoggingDBUtils.mDBHelper =  new PerfDBHelper(this); //since all access needs to be to the same DBHelper, stick it in this handy static class
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int code = intent.getIntExtra(Constants.MESH_MSG_CODE_KEY, -1);
                Log.d(NetworkInfoActivity.class.getName(), "recieved broadcast msg. code: "+code);
                if(code == -1){
                    Utils.LogError(this, "couldn't extract msg code from intent");
                    return;
                }
                switch (code) {
                case Constants.LOG_MSG_CODE:
                    TextView tv = (TextView) findViewById(R.id.recvd_message_tv);
                    String msg = intent.getStringExtra(Constants.MESH_MSG_KEY);
                    setTextView(R.id.recvd_message_tv, tv.getText()
                                                       + msg + "\n");
                    break;
                case Constants.STATUS_MSG_CODE:
                    msg = intent.getStringExtra(Constants.MESH_MSG_KEY);
                    setTextView(R.id.status_tv, msg);
                    mCb.setEnabled(true);
                    break;
                case Constants.TTM_MSG_CODE: // traffic forwarded through mesh,
                                            // (on our behalf) ie total data not
                                            // going through 3g, instead being
                                            // forwarded through mesh
                    int traffic = intent.getIntExtra(Constants.MESH_MSG_KEY, -1);
                    trafficThroughMesh += (double) traffic / (double) 1000;
                    setTextView(R.id.tf_forus_tv, "Data through mesh (KBytes) "
                                                  + trafficThroughMesh);
                    break;
                case Constants.TFM_MSG_CODE: // forwarded traffic that we have
                                            // acted as the out link for. ie
                                            // data we have pushed through our
                                            // 3g modem for the benefit of the
                                            // mesh
                    traffic = intent.getIntExtra(Constants.MESH_MSG_KEY, -1);
                    trafficFromMesh += (double) traffic / (double) 1000;
                    setTextView(R.id.tf_byus_tv, "Data from mesh (KBytes): " + trafficFromMesh);
                }
            }
        };
        mCb = (CheckBox) findViewById(R.id.start_service_but);
        mCb.setEnabled(true);
        /*
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.arg1) {
                case Constants.LOG_MSG_CODE:
                    TextView tv = (TextView) findViewById(R.id.recvd_message_tv);
                    setTextView(R.id.recvd_message_tv, tv.getText()
                                                       + msg.getData().getString("msg") + "\n");
                    break;
                case Constants.STATUS_MSG_CODE:
                    setTextView(R.id.status_tv, msg.getData().getString("msg"));
                    mCb.setEnabled(true);
                    break;
                case Constants.TTM_MSG_CODE: // traffic forwarded through mesh,
                                            // (on our behalf) ie total data not
                                            // going through 3g, instead being
                                            // forwarded through mesh
                    trafficThroughMesh += (double) msg.arg2 / (double) 1000;
                    setTextView(R.id.tf_forus_tv, "Data through mesh (KBytes) "
                                                  + trafficThroughMesh);
                    break;
                case Constants.TFM_MSG_CODE: // forwarded traffic that we have
                                            // acted as the out link for. ie
                                            // data we have pushed through our
                                            // 3g modem for the benefit of the
                                            // mesh
                    trafficFromMesh += (double) msg.arg2 / (double) 1000;
                    setTextView(R.id.tf_byus_tv, "Data from mesh (KBytes): " + trafficFromMesh);
                }

            }
        };*/
/*
        try {
            myNode = initializeStartNode(handler);
        } catch (Exception e) {
            setTextView(R.id.status_tv, "Error initializing Node");
            e.printStackTrace();
        }
*/

        setProxy(mWV);
    }

    //set as onClick method for start_service_but checkbox in the layout .xml file
    public void onCheckboxClicked(View view) {
        final String tag = "networkInfoActivity:onCheckboxClicked";
        mCb.setEnabled(false);//disable cb until process is completed,
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();
        Intent meshService = new Intent(this, MeshService.class);
        // Check which checkbox was clicked
        switch (view.getId()) {
        case R.id.start_service_but:
            if (checked) {
                //signalled by recieving msg in this classes msghandler

                // start proxy and mesh service

                startService(meshService);
                /*
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            int rId = mFreeIPManager.getFreeID();
                            myNode.stopThread();
                            myNode.unBind();
                            String cmd = "source "+Constants.INIT_SCRIPT_PATH + " " + rId;
                            Log.d(tag, "got available address for last ip segment: " + rId
                                       + ". Cmd string: " + cmd);
                            RunAsRoot(new String[] { cmd });//
                            int myContactID = getMyID();
                            myNode = new Node(myContactID);
                            myNode.startThread();
                            startManagers(myNode, myContactID, true);
                            mPl =
                                new ProxyListener(getHandler(), 8080, myNode, mContactManager, mObs, mThis);
                            mPl.start();
                            Utils.sendHandlerMsg(getHandler(), Constants.STATUS_MSG_CODE, "mesh service ON. ID: "+rId);

                        } catch (Exception e) {
                            Utils.sendHandlerMsg(getHandler(), Constants.STATUS_MSG_CODE, "error starting mesh service");
                        }
                    }
                }).start();*/

            } else {
                stopService(meshService);
            }
            /*
            if(myNode != null && mPl != null){
                myNode.stopThread();
                myNode.unBind();
                mPl.stopListening();
                try {
                    myNode = initializeStartNode(handler);
                } catch (Exception e) {
                    Utils.sendHandlerMsg(getHandler(), Constants.STATUS_MSG_CODE, "Error stopping and reinitializing node");
                    e.printStackTrace();
            } else {
                mCb.setEnabled(true);
            }*/
            break;
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((mReceiver), new IntentFilter(MeshService.MESH_RESULT));
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onStop();
    }
/*
    private Node initializeStartNode(Handler handler) throws Exception {
        Node n = null;

        String[] cmd = { "source "+Constants.INIT_SCRIPT_PATH+" 1" };
        RunAsRoot(cmd);
        int myContactID = getMyID();
        n = new Node(myContactID); // start myNode with 192.168.2.0.
                                   // use this node to gather IP info
                                   // for final ip/ID assignment.
        n.startThread();
        startManagers(n, myContactID, false);
        Utils.sendHandlerMsg(handler, Constants.STATUS_MSG_CODE, "mesh service OFF");
        return n;

    }*/
/*
    private void startManagers(Node n, int myContactID, boolean activeOutLink){
        mFreeIPManager = new FreeIPManager(n);
        mOutLinkManager = new OutLinkManager(activeOutLink, n, myContactID, handler, this);
        mContactManager = new ContactManager(n);
        mObs = new AODVObserver(n, myContactID, mThis, mOutLinkManager, mContactManager, mFreeIPManager);
        mOutLinkManager.setAODVObserver(mObs);
    }*/

    @Override
    public void onDestroy() {
        super.onDestroy();
        /*
        if (!meshIsOn() && (myNode != null)) {
            myNode.stopThread();
            myNode.unBind();
            myNode = null;
        }*/
    }


    private boolean meshIsOn() { // this is a questionable way to determine if
                                 // the mesh service is on
        CheckBox cb = (CheckBox) findViewById(R.id.start_service_but);
        if (cb != null) return cb.isChecked();
        return false;
    }

    private class urlReqOnClickListener implements OnClickListener {
        private String mUrl;

        public urlReqOnClickListener(String url) {
            super();
            mUrl = url;
        }


        @Override
        public void onClick(View v) {
            mWV.loadUrl(mUrl);
        }

    }


    public int getMyID() throws Exception {
        String tag = "NetworkInfoActivity:getMyID";
        NetworkInterface nif;
        int myid = -1;
        try {
            nif = NetworkInterface.getByName("wlan0");
            Enumeration<InetAddress> addresses = nif.getInetAddresses();
            if (addresses.hasMoreElements()) {
                byte b = addresses.nextElement().getAddress()[3];
                byte[] ba = new byte[4];
                ba[3] = b;
                ByteBuffer bb = ByteBuffer.wrap(ba);
                myid = bb.getInt(); // THISIS SO AWESOME JAVA DOESN'T HAVE
                                    // UNSIGNED TYPES!!! thats cool
                Log.d(tag, "myContactId is fourth ip segment:" + myid);
            } else {
                Log.e(tag, "error getInetAddresses returned no addresses");
                throw new Exception("getMyID exception: no associated IP addresses found");
            }

            return myid;
        } catch (SocketException e) {
            e.printStackTrace();
            return -1;
        }
    }


    public void setProxy(WebView wView) {
        try {
            Class jwcjb = Class.forName("android.webkit.JWebCoreJavaBridge");
            Class params[] = new Class[1];
            params[0] = Class.forName("android.net.ProxyProperties");
            Method updateProxyInstance = jwcjb.getDeclaredMethod("updateProxy", params);

            Class wv = Class.forName("android.webkit.WebView");
            Field mWebViewCoreField = wv.getDeclaredField("mWebViewCore");
            Object mWebViewCoreFieldIntance = getFieldValueSafely(mWebViewCoreField, wView);

            Class wvc = Class.forName("android.webkit.WebViewCore");
            Field mBrowserFrameField = wvc.getDeclaredField("mBrowserFrame");
            Object mBrowserFrame =
                getFieldValueSafely(mBrowserFrameField, mWebViewCoreFieldIntance);

            Class bf = Class.forName("android.webkit.BrowserFrame");
            Field sJavaBridgeField = bf.getDeclaredField("sJavaBridge");
            Object sJavaBridge = getFieldValueSafely(sJavaBridgeField, mBrowserFrame);

            Class ppclass = Class.forName("android.net.ProxyProperties");
            Class pparams[] = new Class[3];
            pparams[0] = String.class;
            pparams[1] = int.class;
            pparams[2] = String.class;
            Constructor ppcont = ppclass.getConstructor(pparams);

            updateProxyInstance.invoke(sJavaBridge, ppcont.newInstance(PROXY_IP, PROXY_PORT, null));
        } catch (Exception ex) {}
    }


    /**
     * Set Proxy for Android 4.1 and above.
     */
    public boolean setProxyICSPlus(WebView webview, String host, int port, String exclusionList) {

        Log.d("", "Setting proxy with >= 4.1 API.");

        try {

            Class wvcClass = Class.forName("android.webkit.WebViewClassic");
            Class wvParams[] = new Class[1];
            wvParams[0] = Class.forName("android.webkit.WebView");
            Method fromWebView = wvcClass.getDeclaredMethod("fromWebView", wvParams);
            Object webViewClassic = fromWebView.invoke(null, webview);

            Class wv = Class.forName("android.webkit.WebViewClassic");
            Field mWebViewCoreField = wv.getDeclaredField("mWebViewCore");
            Object mWebViewCoreFieldIntance =
                getFieldValueSafely(mWebViewCoreField, webViewClassic);

            Class wvc = Class.forName("android.webkit.WebViewCore");
            Field mBrowserFrameField = wvc.getDeclaredField("mBrowserFrame");
            Object mBrowserFrame =
                getFieldValueSafely(mBrowserFrameField, mWebViewCoreFieldIntance);

            Class bf = Class.forName("android.webkit.BrowserFrame");
            Field sJavaBridgeField = bf.getDeclaredField("sJavaBridge");
            Object sJavaBridge = getFieldValueSafely(sJavaBridgeField, mBrowserFrame);

            Class ppclass = Class.forName("android.net.ProxyProperties");
            Class pparams[] = new Class[3];
            pparams[0] = String.class;
            pparams[1] = int.class;
            pparams[2] = String.class;
            Constructor ppcont = ppclass.getConstructor(pparams);

            Class jwcjb = Class.forName("android.webkit.JWebCoreJavaBridge");
            Class params[] = new Class[1];
            params[0] = Class.forName("android.net.ProxyProperties");
            Method updateProxyInstance = jwcjb.getDeclaredMethod("updateProxy", params);

            updateProxyInstance.invoke(sJavaBridge, ppcont.newInstance(host, port, exclusionList));

        } catch (Exception ex) {
            Log.e("", "Setting proxy with >= 4.1 API failed with error: " + ex.getMessage());
            return false;
        }

        Log.d("", "Setting proxy with >= 4.1 API successful!");
        return true;
    }


    private Object
        getFieldValueSafely(Field field, Object classInstance) throws IllegalArgumentException,
                                                              IllegalAccessException {
        boolean oldAccessibleValue = field.isAccessible();
        field.setAccessible(true);
        Object result = field.get(classInstance);
        field.setAccessible(oldAccessibleValue);
        return result;
    }

    private class myWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return false;
        }
    }


    public void setTextView(int id, String msg) {
        String tag = "NetworkInfoActivity:setTextView";
        TextView tv = (TextView) findViewById(id);
        if (tv != null) {
            tv.setText(msg);
        } else {
            Log.e(tag, "could not set textview, tv is null");
        }
    }


    @Override
    public Handler getHandler() {
        return handler;
    }

}
