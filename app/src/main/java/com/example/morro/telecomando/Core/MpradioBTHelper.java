package com.example.morro.telecomando.Core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import static java.lang.Thread.sleep;

/**
 * Created by morro on 26/04/18.
 */

public class MpradioBTHelper implements Parcelable, BluetoothFTPHelper.MpradioBTFTPHelperListener {
    public static final String ACTION_SONG_NAME = "song_name";
    public static final String ACTION_GET_LIBRARY = "library";
    public static final String ACTION_GET_CONFIG = "config get";
    public static final String ACTION_SET_CONFIG = "config set";
    public static final String ACTION_GET_WIFI_STATUS = "system wifi-switch status";
    public static final String ACTION_PAUSE = "pause";
    public static final String ACTION_PLAY = "play";
    public static final String ACTION_RESUME = "resume";
    public static final String ACTION_NEXT = "next";
    public static final String ACTION_RESTART_MPRADIO = "system systemctl restart mpradio";
    public static final String ACTION_POWEROFF = "system poweroff";
    public static final String ACTION_REBOOT = "system reboot";
    public static final String ACTION_SEEK = "SEEK";
    public static final String ACTION_SCAN = "SCAN";

    /* FTP and Rfcomm helpers throw exceptions which are handled here */
    private static BluetoothFTPHelper bluetoothFTPHelper;
    public static BluetoothRfcommHelper bluetoothRfcommHelper;

    /* Listener interface implementation for progress update etc */
    private MpradioBTHelperListener listener;

    private final String address;
    private Context context;

    private static class AsyncMsgSend extends AsyncTask<Void, Void, Void> {
        private final WeakReference<Context> weakContext;
        PutAndGetListener listener = null;
        Integer sleepTime = 0;
        String message;
        String action;
        String reply = null;

        public AsyncMsgSend(String message, Context context) {
            this.message = message;
            this.weakContext = new WeakReference<>(context);
        }

        public AsyncMsgSend(String message, String action, Context context,
                            PutAndGetListener listener, Integer sleepTime) {
            this(message, context);
            this.listener = listener;
            this.sleepTime = sleepTime;
            this.action = action;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try { sleep(sleepTime); } catch (InterruptedException e) { e.printStackTrace(); }

            try {
                if (listener != null)
                    reply = bluetoothRfcommHelper.putAndGet(message);
                else
                    bluetoothRfcommHelper.put(message);
            } catch (IOException e) {
                Main4Activity.restartActivity(weakContext.get());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            if (listener != null) {
                listener.onAsyncReply(action, reply);
            }
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

    private String strToJson(String message){
        return strToJson(message, "");
    }

    private String strToJson(String message, String data){
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
        String msg = strToJson(message);
        new AsyncMsgSend(msg, context).execute();
    }

    /* Sends a key:value message */
    public void sendKVMessage(String message, String data) {
        String msg = strToJson(message, data);
        new AsyncMsgSend(msg, context).execute();
    }

    public void getNowPlaying(PutAndGetListener listener) {
        String msg = strToJson(ACTION_SONG_NAME);
        new AsyncMsgSend(msg, ACTION_SONG_NAME, context, listener, 1000).execute();
    }

    public void getLibrary(PutAndGetListener listener) {
        String msg = strToJson(ACTION_GET_LIBRARY);
        new AsyncMsgSend(msg, ACTION_GET_LIBRARY, context, listener, 0).execute();
    }

    public void getSettings(PutAndGetListener listener) {
        String msg = strToJson(ACTION_GET_CONFIG);
        new AsyncMsgSend(msg, ACTION_GET_CONFIG, context, listener, 0).execute();
    }

    public void getWifiStatus(PutAndGetListener listener) {
        String msg = strToJson(ACTION_GET_WIFI_STATUS);
        new AsyncMsgSend(msg, ACTION_GET_WIFI_STATUS, context, listener, 0).execute();
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
        } catch (Exception e) {
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

    public static BluetoothDevice getDevice(String deviceName) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals(deviceName)) {
                    bluetoothAdapter.cancelDiscovery(); // The device is already paired, no need to.
                    return device;
                }
            }
        }
        return null;
    }

    public void closeConnection() {
        try {
            bluetoothRfcommHelper.disconnect();
            bluetoothFTPHelper.disconnect();
        } catch (IOException e) {
            Log.e("MPRADIO", "closeConnection ERROR: " + e.getMessage());
        }
    }

    /* listen for ACTION_FOUND events during scan and pair the device*/
    public static class ScanAndPairDevice extends BroadcastReceiver {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String deviceName = device.getName();
            String DeviceAddress = device.getAddress(); // MAC address

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d("MPRADIO", "found device: " + deviceName + " : " + DeviceAddress);
                if (deviceName != null && deviceName.equals("mpradio"))
                    device.createBond(); // pair the device
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals((action)))
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    bluetoothAdapter.cancelDiscovery();
                }
        }
    }

    public static void unbondDevice(BluetoothDevice device) {
        try {
            device.getClass().getMethod("removeBond").invoke(device);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException illegalAccessException) {
            illegalAccessException.printStackTrace();
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

    public interface PutAndGetListener {
        void onAsyncReply(String action, String result);
    }

}
