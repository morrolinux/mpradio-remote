package com.example.morro.telecomando.Core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.example.morro.telecomando.UI.Main4Activity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Set;

/**
 * Created by morro on 26/04/18.
 */

public class MpradioBTHelper implements Parcelable, BluetoothFTPHelper.MpradioBTFTPHelperListener {

    /* FTP and Rfcomm helpers throw exceptions which are handled here */
    private static BluetoothFTPHelper bluetoothFTPHelper;
    public static BluetoothRfcommHelper bluetoothRfcommHelper;

    /* Listener interface implementation for progress update etc */
    private MpradioBTHelperListener listener;

    private final String address;
    private Context context;

    private static class AsyncMsgSend extends AsyncTask<Void, Void, Void> {
        String message;
        private final WeakReference<Context> weakContext;

        public AsyncMsgSend(String message, Context context) {
            this.message = message;
            this.weakContext = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                bluetoothRfcommHelper.put(message);
            } catch (IOException e) {
                Main4Activity.restartActivity(weakContext.get());
            }
            return null;
        }
    }

    public MpradioBTHelper(String address, Context context) throws IOException {
        this.address = address;
        this.context = context;
        bluetoothFTPHelper = new BluetoothFTPHelper(address);
        bluetoothRfcommHelper = new BluetoothRfcommHelper(address);
        bluetoothRfcommHelper.connect();
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
        out.writeString(address);
    }

    protected MpradioBTHelper(Parcel in) {
        this.address = in.readString();
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
        String msg = makeJsonMessage(message);
        new AsyncMsgSend(msg, context).execute();
    }

    /* Sends a key:value message */
    public void sendKVMessage(String message, String data) {
        String msg = makeJsonMessage(message, data);
        new AsyncMsgSend(msg, context).execute();
    }

    public String sendMessageGetReply(String message) {
        try {
            return bluetoothRfcommHelper.putAndGet(makeJsonMessage(message));
        } catch (IOException e) {
            Main4Activity.restartActivity(context);
            return "sendMessageGetReply ERROR";
        }
    }

    public void sendFile(String filename, MpradioBTHelperListener listener) {
        this.listener = listener;
        File file = new File(context.getFilesDir(), filename);
        byte[] fileData = new byte[(int) file.length()];

        try {
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            dis.readFully(fileData);
            dis.close();
        } catch (IOException e) {
            String err = "Can't open " + filename + " : " + e.getMessage();
            Log.e("MPRADIO", err);
            if (listener != null)
                listener.feedbackMessage(err);
            return;
        }

        try {
            bluetoothFTPHelper.startClientSession();
            bluetoothFTPHelper.put(fileData, filename, "binary", this);
            bluetoothFTPHelper.disconnect();
        } catch (IOException e) {
            if (listener != null)
                listener.onConnectionFail();
        }
    }

    public void getFile(String fileName, String destination, MpradioBTHelperListener listener) {
        this.listener = listener;
        try {
            bluetoothFTPHelper.startClientSession();
            bluetoothFTPHelper.get(fileName, destination);
            bluetoothFTPHelper.disconnect();
        } catch (Exception e) {
            if (listener != null)
                listener.onConnectionFail();
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

    /* MpradioBTFTPHelperListener interface implementation: forward progress to my listener */
    @Override
    public void onBTFTProgressUpdate(int progress) {
        if (listener != null)
            listener.onBTProgressUpdate(progress);
    }

    public interface MpradioBTHelperListener{
        void onConnectionFail();
        void onBTProgressUpdate(int progress);
        void feedbackMessage(String message);
    }
}
