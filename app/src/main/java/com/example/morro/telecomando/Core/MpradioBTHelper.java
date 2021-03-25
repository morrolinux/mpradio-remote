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

    //static BluetoothOPPHelper bluetoothOPPHelper;
    private static BluetoothFTPHelper bluetoothFTPHelper;
    public static BluetoothRfcommHelper bluetoothRfcommHelper;
    private MpradioBTHelperListener listener;

    public MpradioBTHelper(String address, MpradioBTHelperListener listener) throws IOException {
        this.listener = listener;
        bluetoothFTPHelper = new BluetoothFTPHelper(address, this);
        bluetoothRfcommHelper = new BluetoothRfcommHelper(address);
        try {
            bluetoothRfcommHelper.setup();
        } catch (IOException e) {
            Log.e("MPRADIO", "BT CONNECTION FAILED!");
            listener.onConnectionFail();    //TODO: refactor and remove listener, just forward throws to top level?
        }
    }

    public void setListener(MpradioBTHelperListener listener) {
        this.listener = listener;
    }

    public void closeConnection(){
        bluetoothRfcommHelper.disconnect();
        bluetoothFTPHelper.disconnect();
    }

    protected MpradioBTHelper(Parcel in) {
    }

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
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        //out.writeParcelable(this,0);
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

    public void sendMessage(String message){
        bluetoothRfcommHelper.put(makeJsonMessage(message));
    }

    public void sendMessage(String message, String data){
        bluetoothRfcommHelper.put(makeJsonMessage(message,data));
    }

    public String sendMessageGetReply(String message){
        return bluetoothRfcommHelper.putAndGet(makeJsonMessage(message));
    }

    public void sendFile(String srcFileName,String dstFileName) throws IOException {
        File file = new File(srcFileName);
        byte[] fileData = new byte[(int) file.length()];
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(new FileInputStream(file));
            dis.readFully(fileData);
            dis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("MPRADIO", "btHelper::sendFile about to call put ");
        ClientSession mSession = bluetoothFTPHelper.setup();
        bluetoothFTPHelper.put(mSession,fileData,dstFileName,"binary");
    }

    public void getFile(String fileName, String destination) throws IOException {
        ClientSession mSession = bluetoothFTPHelper.setup();
        bluetoothFTPHelper.get(mSession,fileName,destination);
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

    @Override
    public void onConnectionFail() { }

    @Override
    public void onBTFTProgressUpdate(int progress) {
        listener.onBTProgressUpdate(progress);
        // Log.d("MPRADIO", "onBTFTProgressUpdate: " + progress);
    }

    public interface MpradioBTHelperListener{
        void onConnectionFail();
        void onBTProgressUpdate(int progress);
    }
}
