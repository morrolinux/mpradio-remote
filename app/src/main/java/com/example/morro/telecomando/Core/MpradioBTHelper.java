package com.example.morro.telecomando.Core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import javax.obex.ClientSession;

/**
 * Created by morro on 26/04/18.
 */

public class MpradioBTHelper implements Parcelable {

    //static BluetoothOPPHelper bluetoothOPPHelper;
    private static BluetoothFTPHelper bluetoothFTPHelper;
    public static BluetoothRfcommHelper bluetoothRfcommHelper;
    private MpradioBTHelperListener listener;
    private ClientSession mSession; //TRIAL

    public MpradioBTHelper(String name,MpradioBTHelperListener listener){
        this.listener = listener;
        String address = getDeviceAddress(name);
        //bluetoothOPPHelper = new BluetoothOPPHelper(address);
        // bluetoothFTPHelper = new BluetoothFTPHelper(address);
        // mSession = bluetoothFTPHelper.setup(1); //TODO: remove useless parameters

        bluetoothRfcommHelper = new BluetoothRfcommHelper(address);
        bluetoothRfcommHelper.setup(1);
        if(bluetoothRfcommHelper.hasFailed()){
            System.out.println("BT CONNECTION FAILED!");
            listener.onConnectionFail();
        }
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

    public void sendFile(String srcFileName,String dstFileName){
        File file = new File(srcFileName);
        byte[] fileData = new byte[(int) file.length()];
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(new FileInputStream(file));
            dis.readFully(fileData);
            dis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        bluetoothFTPHelper.put(mSession,fileData,dstFileName,"binary");
    }

    public void getFile(String fileName, String destination){
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

    public interface MpradioBTHelperListener{
        void onConnectionFail();
    }
}
