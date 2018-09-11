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

        SimpleServer server = new SimpleServer(new InetSocketAddress(host, 8887));
       //server.run();
        server.start();
        Log.d(LOG_TAG, "web service is started at:"+host+":"+8887);

        while(true){
            if(""!=this.data){
                server.broadcast(this.data);
                Log.d(LOG_TAG, this.data);
                this.data="";
            }

            try{
                Thread.sleep(10);
            }catch(Exception e){

            }
        }
    }

}
