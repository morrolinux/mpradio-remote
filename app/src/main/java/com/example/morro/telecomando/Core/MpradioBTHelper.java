package com.example.morro.telecomando.Core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import com.example.morro.telecomando.UI.Main4Activity;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

/**
 * Created by morro on 26/04/18.
 */

public class MpradioBTHelper implements Parcelable, BluetoothFTPHelper.MpradioBTFTPHelperListener {

    private static BluetoothFTPHelper bluetoothFTPHelper;
    public static BluetoothRfcommHelper bluetoothRfcommHelper;
    private MpradioBTHelperListener listener;
    private final String address;
    private Context context;

    public MpradioBTHelper(String address, Context context) throws IOException {
        this.address = address;
        this.context = context;
        bluetoothFTPHelper = new BluetoothFTPHelper(address);
        bluetoothRfcommHelper = new BluetoothRfcommHelper(address);
        bluetoothRfcommHelper.setup(); // TODO: possiamo rimuoverlo?
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

    public void sendMessage(String message) {
        try {
            bluetoothRfcommHelper.put(makeJsonMessage(message));
        } catch (IOException e) {
            Main4Activity.restartActivity(context);
        }
    }

    public void sendMessage(String message, String data) {
        try {
            bluetoothRfcommHelper.put(makeJsonMessage(message,data));
        } catch (IOException e) {
            Main4Activity.restartActivity(context);
        }
    }

    public String sendMessageGetReply(String message) {
        try {
            return bluetoothRfcommHelper.putAndGet(makeJsonMessage(message));
        } catch (IOException e) {
            Main4Activity.restartActivity(context);
            return "sendMessageGetReply ERROR";
        }
    }

    public void sendFile(String srcFileName, String dstFileName) {
        File file = new File(srcFileName);
        byte[] fileData = new byte[(int) file.length()];

        try {
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            dis.readFully(fileData);
            dis.close();
        } catch (IOException e) {
            String err = "Can't open " + srcFileName + " : " + e.getMessage();
            Toast.makeText(context.getApplicationContext(), err, Toast.LENGTH_LONG).show();
            Log.e("MPRADIO", err);
        }

        try {
            bluetoothFTPHelper.startClientSession();
            bluetoothFTPHelper.put(fileData, dstFileName, "binary", this);
            bluetoothFTPHelper.disconnect();
        } catch (IOException e) {
            Main4Activity.restartActivity(context);
        }
    }

    public void getFile(String fileName, String destination) {
        try {
            bluetoothFTPHelper.startClientSession();
            bluetoothFTPHelper.get(fileName, destination);
            bluetoothFTPHelper.disconnect();
        } catch (Exception e) {
            Main4Activity.restartActivity(context);
        }
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

    public void closeConnection() {
        try {
            bluetoothRfcommHelper.disconnect();
            bluetoothFTPHelper.disconnect();
        } catch (IOException e) {
            Log.e("MPRADIO", "closeConnection ERROR: " + e.getMessage());
        }
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
