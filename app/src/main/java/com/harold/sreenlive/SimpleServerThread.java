package com.harold.sreenlive;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class SimpleServerThread extends Thread {

    private static final String LOG_TAG = "SimpleServer";

    public String data="123";


    public SimpleServerThread(){
        super();
    }

    public void run(){
        Util util = new Util();
        String host = util.getIpAddressString();
        Log.d(LOG_TAG, "ip:"+host);

        Log.d(LOG_TAG, "33333333333333333333333333333");
        SimpleServer server = new SimpleServer(new InetSocketAddress(host, 8887));
       //server.run();
        server.start();

        while(true){
            if(""!=this.data){
                server.broadcast("Hi ");
                Log.d(LOG_TAG, "broadcase Hi");
                this.data="";
            }
        }
    }

}
