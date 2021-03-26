package com.example.morro.telecomando.Core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

import javax.obex.ClientSession;

/**
 * Created by morro on 26/04/18.
 */

public class MpradioBTHelper implements Parcelable, BluetoothFTPHelper.MpradioBTFTPHelperListener {

    private static BluetoothFTPHelper bluetoothFTPHelper;
    public static BluetoothRfcommHelper bluetoothRfcommHelper;
    private MpradioBTHelperListener listener;
    private String address;

    public MpradioBTHelper(String address, MpradioBTHelperListener listener) throws IOException {
        this.address = address;
        this.listener = listener;
        bluetoothFTPHelper = new BluetoothFTPHelper(address);
        bluetoothRfcommHelper = new BluetoothRfcommHelper(address);
        bluetoothRfcommHelper.setup();
    }

    protected MpradioBTHelper(Parcel in) {
        address = in.readString();
        Log.d("MPRADIO", "RESUME EXECUTION, PARCELABLE " + address);
    }

    /* resume app execution */
    public static final Creator<MpradioBTHelper> CREATOR = new Creator<MpradioBTHelper>() {
        @Override
        public MpradioBTHelper createFromParcel(Parcel in) {
            return new MpradioBTHelper(in);
        }
        @Override
        public MpradioBTHelper[] newArray(int size) {
            return new MpradioBTHelper[size];
        }
    };

    @Override
    public void writeToParcel(Parcel out, int flags) {
        //out.writeParcelable(this,0);
        out.writeString(address);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private String makeJsonMessage(String message){
        return makeJsonMessage(message, "");
    }

    private String makeJsonMessage(String message, String data){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("command", message);
            jsonObject.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    public void sendMessage(String message) throws IOException {
        try {
            bluetoothRfcommHelper.put(makeJsonMessage(message));
        } catch (IOException e) {
            listener.onConnectionFail();
            throw e;
        }
    }

    public void sendMessage(String message, String data) throws IOException {
        try {
            bluetoothRfcommHelper.put(makeJsonMessage(message,data));
        } catch (IOException e) {
            listener.onConnectionFail();
            throw e;
        }
    }

    public String sendMessageGetReply(String message) throws IOException {
        String r = "[{ERROR: ERROR}]";
        try {
            r = bluetoothRfcommHelper.putAndGet(makeJsonMessage(message));
        } catch (IOException e) {
            listener.onConnectionFail();
            throw e;
        }
        return r;
    }

    public void sendFile(String srcFileName, String dstFileName) throws IOException {
        File file = new File(srcFileName);
        byte[] fileData = new byte[(int) file.length()];

        try {
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            dis.readFully(fileData);
            dis.close();
        } catch (IOException e) {
            Log.e("MPRADIO", "Can't open " + srcFileName + " : " + e.getMessage());
            throw e;
        }

        bluetoothFTPHelper.startClientSession();
        bluetoothFTPHelper.put(fileData,dstFileName,"binary", this);
        bluetoothFTPHelper.disconnect();
    }

    public void getFile(String fileName, String destination) throws IOException {
        bluetoothFTPHelper.startClientSession();
        bluetoothFTPHelper.get(fileName, destination);
        bluetoothFTPHelper.disconnect();
    }

    public String getDeviceAddress(String name){
        Set<BluetoothDevice> pairedDevices;
        BluetoothAdapter mBluetoothAdapter;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                if(deviceName.equals(name))
                    return device.getAddress(); // MAC address
            }
        }
        return "";
    }

    public void closeConnection() throws IOException {
        bluetoothRfcommHelper.disconnect();
        bluetoothFTPHelper.disconnect();
    }

    public void setListener(MpradioBTHelperListener listener) {
        this.listener = listener;
    }

    @Override
    public void onBFTPConnectionFail() {
        Log.e("MPRADIO", "Bluetooth FTP connection failed");
        listener.onConnectionFail();
    }

    @Override
    public void onBTFTProgressUpdate(int progress) {
        listener.onBTProgressUpdate(progress);
    }

    public interface MpradioBTHelperListener{
        void onConnectionFail();
        void onBTProgressUpdate(int progress);
    }
}
