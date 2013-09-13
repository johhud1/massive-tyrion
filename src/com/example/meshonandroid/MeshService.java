package com.example.meshonandroid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Enumeration;

import com.example.meshonandroid.pdu.AODVObserver;

import proxyServer.ProxyListener;
import adhoc.aodv.Node;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;



public class MeshService extends Service {

    protected static final String MESH_MSG_KEY = "meshservicemsg";
    public static final String MESH_RESULT = "meshresult";
    private static final Object MESH_START_ERROR_MSG = "error starting mesh service";

    private LocalBroadcastManager broadcaster;

    private FreeIPManager mFreeIPManager;
    AODVObserver mObs;
    private ContactManager mContactManager;
    private ProxyListener mPl;
    private OutLinkManager mOutLinkManager;
    private Node mNode;


    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        broadcaster = LocalBroadcastManager.getInstance(this);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    makeForeground();
                    mNode = initializeStartNode();
                    // TODO: find out what's going on here and make a better fix
                    Thread.sleep(3000);// gotta wait a second for the new node
                                       // to do its' thing. look don't ask
                                       // questions it just works this way
                    mNode = joinMesh(mNode);
                } catch (Exception e) {
                    e.printStackTrace();
                    Utils.sendUIUpdateMsg(broadcaster, Constants.STATUS_MSG_CODE, MESH_START_ERROR_MSG);
                }
            }
        }).start();

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        mPl.stopListening();
        mNode.stopThread();
        mNode.unBind();
        mNode.deleteObserver(mObs);
        mNode = null;
        Utils.sendUIUpdateMsg(broadcaster, Constants.STATUS_MSG_CODE, "mesh service OFF");
    }


    private Node initializeStartNode() throws Exception {
        Node n = null;

        String[] cmd = { "source " + Constants.INIT_SCRIPT_PATH + " 1" };
        RunAsRoot(cmd);
        int myContactID = getMyID();
        n = new Node(myContactID); // start myNode with 192.168.2.1
                                   // use this node to gather IP info
                                   // for final ip/ID assignment.
        n.startThread();
        startManagers(n, myContactID, false);
        // Utils.sendBroadcastMsg(broadcaster, Constants.STATUS_MSG_CODE,
        // "mesh service OFF");
        return n;
    }


    private Node joinMesh(Node node) {
        try {
            int rId = mFreeIPManager.getFreeID();
            node.stopThread();
            node.unBind();
            String cmd = "source " + Constants.INIT_SCRIPT_PATH + " " + rId;
            Log.d(MeshService.class.getName(), "got available address for last ip segment: " + rId
                                               + ". Cmd string: " + cmd);
            RunAsRoot(new String[] { cmd });
            int myContactID = getMyID();
            node = new Node(myContactID);
            node.startThread();
            startManagers(node, myContactID, true);
            mPl = new ProxyListener(broadcaster, 8080, node, mContactManager, mObs, this);
            mPl.start();
            Utils.sendUIUpdateMsg(broadcaster, Constants.STATUS_MSG_CODE, "mesh service ON. ID: "
                                                                          + rId);
        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendUIUpdateMsg(broadcaster, Constants.STATUS_MSG_CODE,
                                  MESH_START_ERROR_MSG);
        }
        return node;
    }


    private void startManagers(Node n, int myContactID, boolean activeOutLink) {
        mFreeIPManager = new FreeIPManager(n);
        mOutLinkManager = new OutLinkManager(activeOutLink, n, myContactID, broadcaster, this);
        mContactManager = new ContactManager(n);
        mObs =
            new AODVObserver(myContactID, broadcaster, mOutLinkManager, mContactManager,
                             mFreeIPManager);
        n.addObserver(mObs);
        mOutLinkManager.setAODVObserver(mObs);
    }


    private void makeForeground() {
        int NOTIFICATION_ID = 2;
        Notification notification =
            new Notification(R.drawable.ic_launcher, getText(R.string.join_mesh_notif),
                             System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, NetworkInfoActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, "Mesh On Android", "Active", pendingIntent);

        startForeground(NOTIFICATION_ID, notification);
    }


    public void RunAsRoot(String[] cmds) throws IOException {
        Process p = Runtime.getRuntime().exec("su");
        OutputStream os = p.getOutputStream();// stdin
        InputStream is = p.getInputStream();// stdout
        for (String tmpCmd : cmds) {
            os.write((tmpCmd + "\n").getBytes());
            os.flush();
        }
        try {
            Thread.sleep(1900);// wait a bit so the script has time to run
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public int getMyID() throws Exception {
        String tag = MeshService.class.getName() + ":getMyID";
        NetworkInterface nif;
        int myid = -1;

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
    }

}
