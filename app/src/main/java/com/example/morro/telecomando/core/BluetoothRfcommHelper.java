package com.example.morro.telecomando.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by morro on 12/05/18.
 */

public class BluetoothRfcommHelper {
    private final BluetoothDevice device;
    private final UUID RFCOMMUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private BluetoothSocket rfcommsocket;

    public void disconnect(){
        try {
            rfcommsocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connect() throws IOException{
        rfcommsocket = device.createInsecureRfcommSocketToServiceRecord(RFCOMMUUID);
        if(rfcommsocket.isConnected())
            rfcommsocket.close();
        rfcommsocket.connect();
    }

    public BluetoothRfcommHelper(String address) {
        BluetoothAdapter mBtadapter = BluetoothAdapter.getDefaultAdapter();
        device = mBtadapter.getRemoteDevice(address);
    }

    public boolean put(String text) throws IOException {
        OutputStream tmpOut = rfcommsocket.getOutputStream();
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
        OutputStream tmpOut = rfcommsocket.getOutputStream();
        tmpOut.write(text.getBytes());
        tmpOut.flush();
        InputStream tmpIn = rfcommsocket.getInputStream();
        result = convertStreamToString(tmpIn);

        return result;
    }

}