package com.example.morro.telecomando.Core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.obex.ClientSession;

/**
 * Created by morro on 12/05/18.
 */

public class BluetoothRfcommHelper {
    private BluetoothSocket rfcommsocket;
    private BluetoothAdapter mBtadapter;
    private BluetoothDevice device;
    private boolean failed = false;
    OutputStream tmpOut;
    InputStream tmpIn;
    private final UUID RFCOMMUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    public void disconnect(){
        try {
            rfcommsocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasFailed(){
        return failed;
    }

    public void setup(int n) {
        try {
            rfcommsocket = device.createInsecureRfcommSocketToServiceRecord(RFCOMMUUID);
            if(rfcommsocket.isConnected())
                rfcommsocket.close();
            System.out.println("socket connected:"+ rfcommsocket.isConnected());
            rfcommsocket.connect();
            //tmpOut = rfcommsocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            rfcommsocket = null;
            failed = true;
            //throw new RuntimeException(e);
        }
    }

    public BluetoothRfcommHelper(String address) {
        mBtadapter = BluetoothAdapter.getDefaultAdapter();
        device = mBtadapter.getRemoteDevice(address);
    }

    public boolean put(String text){
        try {
            tmpOut = rfcommsocket.getOutputStream();
            tmpOut.write(text.getBytes());
            tmpOut.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    // works
    private static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\0");     //\0 is end of string in c
        return s.hasNext() ? s.next() : "";
    }

    /*
    // works better for debugging
    private String convertStreamToString(InputStream stream) throws IOException {
        Reader reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[1024 * 4];
        String result = "";
        int r;
        do {
            r = reader.read(buffer);
            result += new String(buffer).substring(0, r);
        }while (r > 0 && buffer[r-1] != '\0');  //\0 is end of string
        return result;
    }
    */

    public String putAndGet(String text){
        String result = "error";
        try {
            tmpOut = rfcommsocket.getOutputStream();
            tmpOut.write(text.getBytes());
            tmpOut.flush();
            tmpIn = rfcommsocket.getInputStream();
            result = convertStreamToString(tmpIn);
        } catch (IOException e) {
            e.printStackTrace();
            return result;
        }
        return result;
    }

}