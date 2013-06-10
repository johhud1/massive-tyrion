package com.example.meshonandroid;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.BindException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.Proxy;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;

import proxyServer.ProxyListener;

import com.example.meshonandroid.pdu.AODVObserver;
import com.example.meshonandroid.pdu.ExitNodeReqPDU;

import adhoc.aodv.Node;
import adhoc.aodv.exception.InvalidNodeAddressException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.support.v4.widget.SearchViewCompat.OnCloseListenerCompat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;



public class MainActivity extends Activity {

    static String PROXY_IP = "localhost";
    static int PROXY_PORT = 8080;

    int lastBroadcastId = 0;
    int lastDataRRId = 0;
    int myContactID;
    Node myNode;
    public Handler handler;

    private TextView outputTV;
    private WebView mWV;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // BAD - should get rid of, but makes for faster, dont' have to put all
        // network activities in seperate threads
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EditText urlField = (EditText) findViewById(R.id.url_et);
        Button getUrlButton = (Button) findViewById(R.id.req_url);
        Button get1921Button = (Button) findViewById(R.id.req_192_1);
        Button get192136Button = (Button) findViewById(R.id.req_192_136);

        getUrlButton.setOnClickListener(new urlReqOnClickListener(urlField.getText().toString()));
        get192136Button.setOnClickListener(new urlReqOnClickListener("http://192.168.1.132"));
        get1921Button.setOnClickListener(new urlReqOnClickListener("http://192.168.1.1"));

        outputTV = (TextView) findViewById(R.id.recvd_message_tv);
        mWV = (WebView) findViewById(R.id.wv);
        setProxy(mWV);
        mWV.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                /*
                String[] spliturl = url.split(".");
                if(spliturl[spliturl.length].equals(".html")){
                    return false;
                } else {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                }*/
                return false;
                //return super.shouldOverrideUrlLoading(view, url);
            }
        });
        handler = new Handler() {
            @SuppressWarnings("unchecked")
            @Override
            public void handleMessage(Message msg) {
                setTextField(msg.getData().getString("msg"));
            }
        };

        myContactID = getMyID();

        try {
            myNode = new Node(myContactID);
            myNode.startThread();
            AODVObserver obs = new AODVObserver(myNode, myContactID, this);
            ProxyListener pl = new ProxyListener(8080, myNode, obs);
            pl.start();
        } catch (BindException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidNodeAddressException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        /*
         * HttpURLConnection urlConn; try { URL url = new
         * URL("http://www.google.com"); Proxy proxy = new
         * Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 8080));
         *
         * InputStream response = url.openConnection(proxy).getInputStream();
         * System.out.println(response.read());
         *
         * } catch (IOException e) { e.printStackTrace(); }
         */

    }


    public void setWebView(String htmlString) {
        WebView wv = (WebView) findViewById(R.id.wv);
        wv.loadData(htmlString, "text/html", Constants.encoding);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(myNode != null){
            myNode.stopThread();
        }
    }

    public void setTextField(String text) {
        TextView outField = (TextView) findViewById(R.id.recvd_message_tv);
        Date now = new Date();
        if(outField!=null){
            outField.setText(outputTV.getText() + "\n" +now.toLocaleString()+ ": "+text);
        }
    }


    private int getBroadcastID() {
        lastBroadcastId++;
        return lastBroadcastId;
    }


    private int getDataRRID() {
        lastDataRRId++;
        return lastBroadcastId;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    public int getMyID() {
        NetworkInterface nif;
        try {
            nif = NetworkInterface.getByName("wlan0");
            int myid = ((short) nif.getInetAddresses().nextElement().getAddress()[3]);
            Log.d("getPhoneType", "myContactId is fourth ip segment:" + myid);
            return myid;
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
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
            if (myNode != null) {
                /*
                 * ExitNodeReqPDU dr = new ExitNodeReqPDU(myContactID,
                 * getBroadcastID(), getDataRRID());
                 * myNode.sendData(dr.getPacketID(), 255, dr.toBytes());
                 */

                mWV.loadUrl(mUrl);

                /*
                URLConnection urlConn;
                try {
                    URL url = new URL(mUrl);
                    Proxy proxy =
                        new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 8080));

                    InputStream response = url.openConnection(proxy).getInputStream();
                    byte[] responseBuf = new byte[512];
                    ByteArrayOutputStream in = new ByteArrayOutputStream();
                    int offset = 0;
                    while (response.read(responseBuf, offset, 512) > 0) {
                        in.write(responseBuf);
                    }

                    String htmlString = in.toString(Constants.encoding);
                    setWebView(htmlString);
                    response.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }*/
            }
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
    public boolean setProxyICSPlus(WebView webview, String host, int port,
                                          String exclusionList) {

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
    /*
     * @Override public void update(Observable arg0, Object arg1) { String tag =
     * "MainActivity:update"; Log.d(tag, "got update from Observable: " +
     * arg1.toString()); Message m = new Message(); Bundle b = new Bundle();
     * b.putString("msg", arg1.toString()); m.setData(b);
     * handler.sendMessage(m);
     *
     * }
     */
}
