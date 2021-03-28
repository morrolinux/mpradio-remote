package com.example.morro.telecomando.Core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.TextView;

import com.example.morro.telecomando.R;
import com.example.morro.telecomando.UI.Main4Activity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Set;

import static com.example.morro.telecomando.UI.ActionsFragment.ACTION_GET_LIBRARY;
import static com.example.morro.telecomando.UI.ActionsFragment.ACTION_SONG_NAME;
import static java.lang.Thread.sleep;

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
            if (listener != null)
                listener.onAsyncReply(action, reply);
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

    public void getNowPlaying(PutAndGetListener listener) {
        String msg = makeJsonMessage(ACTION_SONG_NAME);
        new AsyncMsgSend(msg, ACTION_SONG_NAME, context, listener, 1000).execute();
    }

    public void getLibrary(PutAndGetListener listener) {
        String msg = makeJsonMessage(ACTION_GET_LIBRARY);
        new AsyncMsgSend(msg, ACTION_GET_LIBRARY, context, listener, 0).execute();
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

    public interface PutAndGetListener {
        void onAsyncReply(String action, String result);
    }

}
