package com.example.meshonandroid;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class Utils {

    // send msg containing amount of forwarded traffic we have pushed out to
    // internet to the given handler
    public static void sendForwardedTrafficMsg(Handler handler, int length) {
        Message m = new Message();
        m.arg1 = Constants.FT_MSG_CODE;
        m.arg2 = length;
        handler.sendMessage(m);
    }

    public static void addMsgToMainTextLog(Handler handler, String s){
        String tag = "Utils:addMsgToMainTextLog";
        Message m = new Message();
        m.arg1 = Constants.LOG_MSG_CODE;
        Bundle b = new Bundle();
        b.putString("msg", s);
        m.setData(b);
        handler.sendMessage(m);
    }
}
