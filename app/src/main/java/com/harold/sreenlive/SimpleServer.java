package com.harold.sreenlive;

import android.util.Log;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;


public class SimpleServer extends WebSocketServer{

    private static final String LOG_TAG = "SimpleServer";

    //private List<WebSocket> mConns;

    public SimpleServer(InetSocketAddress address){
        super(address);
        //mConns = new ArrayList<WebSocket>();
    }

    public void sendMsg(String s){
        //Log.d(LOG_TAG, mConns.toString());
        Log.d(LOG_TAG, "send");
    }

    public void onMessage(WebSocket conn, ByteBuffer message){
        Log.d(LOG_TAG, "bytebuffer message is coming!");
    }

    public void onMessage(WebSocket conn, String message){
        Log.d(LOG_TAG, "string message is coming!");
    }

    public void onOpen(WebSocket conn, ClientHandshake handshake){
        //mConns.add(conn);
        //conn.send("Welcome to server!");
        Log.d(LOG_TAG, "new connection");
    }

    public void  onClose(WebSocket conn, int code, String reason, boolean remote){
        //mConns.remove(conn);
        Log.d(LOG_TAG, "connection is closed!");
    }

    public void onError(WebSocket conn, Exception ex){
        Log.d(LOG_TAG, "on error");
        ex.printStackTrace();
    }

    public void onStart(){
        Log.d(LOG_TAG, "started!");
    }
}
