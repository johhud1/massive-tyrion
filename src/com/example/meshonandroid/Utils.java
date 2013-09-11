package com.example.meshonandroid;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class Utils {

    // send msg containing amount of forwarded traffic we have pushed out to
    // internet to the given handler
    public static void sendTrafficMsg(Handler handler, int length, int msgCode) {
        Message m = new Message();
        m.arg1 = msgCode; //Constants.FT_MSG_CODE;
        m.arg2 = length;
        handler.sendMessage(m);
    }

    public static void sendHandlerMsg(Handler handler, int msgCode, String msg){
        Message m = new Message();
        m.arg1 = msgCode;
        Bundle b = new Bundle();
        b.putString("msg", msg);
        m.setData(b);
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
