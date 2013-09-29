package meshonandroid;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;



public class Utils {

    // send msg containing amount of forwarded traffic we have pushed out to
    // internet to the given handler
    public static void sendTrafficMsg(Handler handler, int length, int msgCode) {
        Message m = new Message();
        m.arg1 = msgCode; // Constants.FT_MSG_CODE;
        m.arg2 = length;
        handler.sendMessage(m);
    }


    public static void sendHandlerMsg(Handler handler, int msgCode, String msg) {
        Message m = new Message();
        m.arg1 = msgCode;
        Bundle b = new Bundle();
        b.putString("msg", msg);
        m.setData(b);
        handler.sendMessage(m);
    }

/*
    public static void addMsgToMainTextLog(Handler handler, String s) {
        String tag = "Utils:addMsgToMainTextLog";
        Message m = new Message();
        m.arg1 = Constants.LOG_MSG_CODE;
        Bundle b = new Bundle();
        b.putString("msg", s);
        m.setData(b);
        handler.sendMessage(m);
    }
*/

    public static void LogError(Object Class, String msg) {
        Log.e(Class.getClass().getName(), msg);
    }


    /**
     * broadcast a message to the NetworkInfoActvity. Only accepts Strings or
     * ints
     *
     * @param broadcaster
     *            broadcaster object to send the broadcast the message with
     * @param msgCode
     *            indicates what field in the UI the message will be put/handled
     *            (consult Constants for codes)
     * @param message
     *            the message you want to send
     */
    public static void sendUIUpdateMsg(LocalBroadcastManager broadcaster, int msgCode,
                                        Object message) {
        Intent intent = new Intent(MeshService.MESH_RESULT);
        intent.putExtra(Constants.MESH_MSG_CODE_KEY, msgCode);
        if (message != null) {
            if (message instanceof String) {
                intent.putExtra(Constants.MESH_MSG_KEY, (String) message);
            } else if (message instanceof Integer) {
                intent.putExtra(Constants.MESH_MSG_KEY, ((Integer) message).intValue());
            }
        }
        broadcaster.sendBroadcast(intent);
    }
}
