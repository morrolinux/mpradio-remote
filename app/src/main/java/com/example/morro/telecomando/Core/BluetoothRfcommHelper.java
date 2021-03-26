package com.example.morro.telecomando.Core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

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
    private final BluetoothAdapter mBtadapter;
    private String device_address = "";
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

    public void setup() throws IOException{
        BluetoothDevice device = mBtadapter.getRemoteDevice(device_address);
        rfcommsocket = device.createInsecureRfcommSocketToServiceRecord(RFCOMMUUID);
        if(rfcommsocket.isConnected())
            rfcommsocket.close();
        Log.d("MPRADIO", "socket connected: "+ rfcommsocket.isConnected());
        rfcommsocket.connect();
    }

    public BluetoothRfcommHelper(String address) {
        mBtadapter = BluetoothAdapter.getDefaultAdapter();
        device_address = address;
    }

    public boolean put(String text) throws IOException {
        tmpOut = rfcommsocket.getOutputStream();
        tmpOut.write(text.getBytes());
        tmpOut.flush();
        return true;
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

    public String putAndGet(String text) throws IOException {
        String result = "error";
        tmpOut = rfcommsocket.getOutputStream();
        tmpOut.write(text.getBytes());
        tmpOut.flush();
        tmpIn = rfcommsocket.getInputStream();
        result = convertStreamToString(tmpIn);

        return result;
    }

}