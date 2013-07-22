package com.example.meshonandroid;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

import proxyServer.ProxyListener;

import com.example.meshonandroid.pdu.AODVObserver;
import com.example.meshonandroid.pdu.ExitNodeReqPDU;
import com.example.meshonandroid.pdu.IPDiscoverMsg;
import com.example.meshonandroid.pdu.MeshPduInterface;

import adhoc.aodv.Node;
import adhoc.aodv.exception.BadPduFormatException;
import adhoc.aodv.exception.InvalidNodeAddressException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;



public class NetworkInfoActivity extends Activity implements HandlerActivity {

    static int MAX_NODES = 254;
    static String PROXY_IP = "127.0.0.1";
    static int PROXY_PORT = 8080;

    private TextView outputTV;
    private WebView mWV;
    private NetworkInfoActivity mThis = this;

    public Handler handler;

    private FreeIPManager mFreeIPManager;
    private Node myNode;
    AODVObserver mObs;

    private double trafficThroughMesh = 0;
    private double trafficFromMesh = 0;

    private class FreeIPManager implements Observer {
        Date updatedLast = new Date(0);
        private boolean[] availableIPs = new boolean[MAX_NODES]; // true
                                                                 // indicates
                                                                 // that address
                                                                 // is available
        private short freeIPs = 254;
        private boolean isJoining = false;
        private Node mNode;


        public FreeIPManager(Node n) {
            mNode = n;
            for (int i = 0; i < availableIPs.length; i++) {
                availableIPs[i] = true;
            }
        }


        public void setNode(Node n) {
            mNode = n;
        }


        private int getFreeID() { // returns a random IP that we haven't heard
                                  // of as being in use in the mesh
            String tag = "FreeIPManager:getFreeID";
            isJoining = true;
            Calendar fiveSecsAgo = Calendar.getInstance();
            fiveSecsAgo.roll(Calendar.SECOND, -Constants.IP_staleness_time);
            // if the contacts are older than 5 seconds, get new ones.
            if (updatedLast.before(fiveSecsAgo.getTime())) {
                try {
                    Log.d(tag,
                          "IP list is stale. broadcasting IPDiscover to gather fresher IP info");
                    mNode.sendData(0, adhoc.aodv.Constants.BROADCAST_ADDRESS,
                                   new IPDiscoverMsg(mNode.getNodeAddress(), 0, 0, true).toBytes());
                    updatedLast = new Date();
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            isJoining = false;
            if (freeIPs != 0) {
                int ip = -1;
                int i;
                for (i = 2; i < availableIPs.length; i++) {
                    // i = new Random().nextInt(availableIPs.length);
                    if (availableIPs[i]) {
                        ip = i;
                        break;
                    }
                }
                availableIPs[i] = false;
                freeIPs--;
                return ip;
            } else {
                Log.e(tag, "no free ip's available :( RUNNN!");
                return -1;
            }
        }


        @Override
        public void update(Observable observable, Object msg) {
            String tag = "NetworkInfo:update";
            MeshPduInterface meshMsg = (MeshPduInterface) msg;
            try {
                Log.d(tag, "got msg: " + meshMsg.toReadableString());
            } catch (UnsupportedEncodingException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            switch (meshMsg.getPduType()) {
            case Constants.PDU_IPDISCOVER:
                IPDiscoverMsg m = (IPDiscoverMsg) msg;
                try {
                    // m.parseBytes(msg.toBytes());
                    if (m.isReq() && (mNode.getNodeAddress() != 1)) {// is a
                                                                     // request,
                                                                     // and we
                                                                     // are a
                                                                     // joined
                                                                     // node.
                                                                     // Send a
                                                                     // response.
                                                                     // done.
                        Log.d(tag, "got an IPDiscover request from " + m.getSourceID()
                                   + " sending IPDiscover response");
                        mNode.sendData(0, adhoc.aodv.Constants.BROADCAST_ADDRESS,
                                       new IPDiscoverMsg(mNode.getNodeAddress(), 1, 1, false)
                                           .toBytes());
                        return;
                    }
                    if (isJoining && !m.isReq()) { // we are joining, and the
                                                   // message is a response. set
                                                   // availableIPs appropriately
                        Log.d(tag, "got an IPDiscover response from " + m.getSourceID()
                                   + ". setting that address as unavailable");
                        availableIPs[m.getSourceID()] = false;
                    } else {// we are not joining, or the message is a request.
                        Log.d(tag,
                              "got an IPDiscover msg. but we are either not joining, or we are and it's not a response. Ignoring");
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }


    @SuppressLint("NewApi")
    public void onCreate(Bundle savedInstanceState) {
        // BAD - should get rid of, but makes for faster, dont' have to put all
        // network activities in seperate threads
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        adhoc.etc.Debug.setDebugStream(System.out);

        setContentView(R.layout.netinfo_layout);
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.arg1) {
                case Constants.LOG_MSG_CODE:
                    TextView tv = (TextView) findViewById(R.id.recvd_message_tv);
                    setTextView(R.id.recvd_message_tv, tv.getText()+msg.getData().getString("msg")+"\n");
                    break;
                case Constants.STATUS_MSG_CODE:
                    setTextView(R.id.status_tv, msg.getData().getString("msg"));
                    break;
                case Constants.TF_MSG_CODE: //traffic forwarded through mesh, (on our behalf) ie total data not going through 3g, instead being forwarded through mesh
                    trafficThroughMesh += (double)msg.arg2/(double)1000;
                    setTextView(R.id.tf_forus_tv, "Data through mesh (KBytes) "+trafficThroughMesh);
                    break;
                case Constants.FT_MSG_CODE: //forwarded traffic that we have acted as the out link for. ie data we have pushed through our 3g modem for the benefit of the mesh
                    trafficFromMesh += (double)msg.arg2/(double)1000;
                    setTextView(R.id.tf_byus_tv, "Data from mesh (KBytes): "+trafficFromMesh);
                }

            }
        };

        try {
            myNode = initializeStartNode();
        } catch (Exception e) {
            setTextView(R.id.status_tv, "Error initializing Node");
            e.printStackTrace();
        }

        mWV = (WebView) findViewById(R.id.webView);
        Button sendUDPBroadcastBut = (Button) findViewById(R.id.sendUDPButton);
        CheckBox startProxy = (CheckBox) findViewById(R.id.start_service_but);

        mWV.setWebViewClient(new myWebViewClient());
        Button loadPageBut = (Button) findViewById(R.id.load_page_but);
        loadPageBut.setOnClickListener(new urlReqOnClickListener("http://www.reddit.com")); // SET
                                                                                            // IP:port
                                                                                            // for
                                                                                            // server
                                                                                            // here!!!
        CheckBox cb = (CheckBox) findViewById(R.id.start_service_but);
        cb.setEnabled(true);
        setProxy(mWV);
    }


    public void onCheckboxClicked(View view) {
        final String tag = "networkInfoActivity:onCheckboxClicked";
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch (view.getId()) {
        case R.id.start_service_but:
            if (checked) {
                // start proxy and mesh service

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Random r = new Random();
                            int rId = mFreeIPManager.getFreeID();// r.nextInt(MAX_NODES);
                            myNode.stopThread();
                            myNode.unBind();
                            String cmd = "source /data/mybin/init.sh " + rId;
                            Log.d(tag, "got available address for last ip segment: " + rId
                                       + ". Cmd string: " + cmd);
                            RunAsRoot(new String[] { cmd });//
                            int myContactID = getMyID();
                            // myNode.setAddress(myContactID);
                            myNode = new Node(myContactID);
                            myNode.startThread();
                            mFreeIPManager.setNode(myNode);
                            mObs = new AODVObserver(myNode, myContactID, mThis);
                            mObs.addObserver(mFreeIPManager);
                            ProxyListener pl = new ProxyListener(mThis.getHandler(), 8080, myNode, mObs);
                            pl.start();
                            Message m = new Message();
                            m.arg1 = Constants.STATUS_MSG_CODE;
                            Bundle b = new Bundle();
                            b.putString("msg", "mesh service ON");
                            m.setData(b);
                            mThis.getHandler().sendMessage(m);

                        } /*
                           * catch (BindException e) { e.printStackTrace(); }
                           * catch (InvalidNodeAddressException e) {
                           * e.printStackTrace(); } catch (SocketException e) {
                           * setStatusField(
                           * "failure binding to address. Try force stopping application, and retrying"
                           * ); e.printStackTrace(); } catch
                           * (UnknownHostException e) { e.printStackTrace(); }
                           * catch (IOException e) { // TODO Auto-generated
                           * catch block e.printStackTrace(); }
                           */catch (Exception e) {
                            setTextView(R.id.status_tv, "error starting mesh service");
                            e.printStackTrace();
                        }
                    }
                }).start();

            } else {
                myNode.stopThread();
                myNode.unBind();
                try {
                    myNode = initializeStartNode();
                } catch (Exception e) {
                    setTextView(R.id.status_tv, "Error stopping and reinitializing node");
                    e.printStackTrace();
                }

            }
            break;
        }
    }


    private Node initializeStartNode() throws Exception {
        Node n = null;

        int myContactID;// = getMyID();
        String[] cmd = { "source /data/mybin/init.sh 1" };// new
                                                          // Random().nextInt(MAX_NODES)
                                                          // };
        RunAsRoot(cmd);
        myContactID = getMyID();
        n = new Node(myContactID); // start myNode with 192.168.2.0.
                                   // use this node to gather IP info
                                   // for final ip/ID assignment.
        n.startThread();
        mFreeIPManager = new FreeIPManager(n); // have to have myNode
                                               // initialized for this
                                               // guy to work (NPE
                                               // danger-zone). perhaps
                                               // should refactor for
                                               // ease of life.
        mObs = new AODVObserver(n, myContactID, mThis);
        mObs.addObserver(mFreeIPManager);// add freeIpManager as observer,
                                         // need to be notified of
                                         // IPDiscover messages
        setTextView(R.id.status_tv, "mesh service OFF");
        return n;

    }


    public void RunAsRoot(String[] cmds) throws IOException {
        Process p = Runtime.getRuntime().exec("su");
        OutputStream os = p.getOutputStream();
        for (String tmpCmd : cmds) {
            os.write((tmpCmd + "\n").getBytes());
            os.flush();
        }
        try {
            Thread.sleep(1600);// wait a bit so the script has time to run
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
