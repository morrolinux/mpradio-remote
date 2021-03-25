package com.example.morro.telecomando.Core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;

import static java.lang.Integer.min;

/**
 * Created by morro on 03/05/18.
 */

public class BluetoothFTPHelper {

    private BluetoothAdapter mBtadapter;
    private BluetoothDevice device;
    private BluetoothSocket mBtSocket;
    private final UUID FTPUUID = UUID.fromString(("00001106-0000-1000-8000-00805f9b34fb"));
    private String TAG = "BluetoothFTPHelper";
    private MpradioBTFTPHelperListener listener = null;
    MpradioFileUtils mpradioFileUtils;

    public void disconnect(){
        try {
            mBtSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BluetoothFTPHelper(String address, MpradioBTFTPHelperListener listener) {
        mBtadapter = BluetoothAdapter.getDefaultAdapter();
        device = mBtadapter.getRemoteDevice(address);
        this.listener = listener;
    }

    public ClientSession setup() {
        ClientSession mSession = null;
        UUID uuid = UUID.fromString("F9EC7BC4-953C-11D2-984E-525400DC9E09");
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        byte[] bytes = bb.array();
        try {
            /* create and connect the socket */
            mBtSocket = device.createInsecureRfcommSocketToServiceRecord(FTPUUID);
            mBtSocket.connect();

            mSession = new ClientSession(new BluetoothObexTransport(mBtSocket));

            HeaderSet headerset = new HeaderSet();
            headerset.setHeader(HeaderSet.TARGET, bytes);
            headerset = mSession.connect(headerset);

            if (headerset.getResponseCode() == ResponseCodes.OBEX_HTTP_OK) {
                Log.d("MPRADIO", "headerset.getResponseCode() : OBEX_HTTP_OK");
            } else {
                Log.d("MPRADIO", "headerset.getResponseCode() : " + headerset.getResponseCode());
                mSession.disconnect(headerset);
            }
        } catch (Exception e) {
            Log.d("MPRADIO", "Bluetooth FTP error: " + e.getMessage());
        }
        return mSession;
    }

    protected void put(ClientSession session, byte[] bytes, String filename, String type) throws IOException {

        Operation putOperation = null;
        OutputStream mOutput = null;

        /* send file metadata to the server */
        final HeaderSet hs = new HeaderSet();
        hs.setHeader(HeaderSet.NAME, filename);
        hs.setHeader(HeaderSet.TYPE, type);
        hs.setHeader(HeaderSet.LENGTH, ((long) bytes.length));
        putOperation = session.put(hs);

        /* send the actual file */
        int offset = 0;
        int bufferSize = 4096;
        mOutput = putOperation.openOutputStream();

        while (offset < bytes.length) {
            bufferSize = min(bufferSize, bytes.length - offset);
            mOutput.write(bytes, offset, bufferSize);
            offset += bufferSize;
            listener.onBTFTProgressUpdate((int) (1.0 * offset/bytes.length * 100));
            Log.d("MPRADIO", "OFFSET: " + offset + " LEN: "+ bytes.length);
        }

        // mOutput.write(bytes);
        mOutput.close();
        putOperation.close();
    }


    protected void get(final ClientSession session, final String filename, final String destination) throws IOException {
        mpradioFileUtils = new MpradioFileUtils();

        Operation getOperation;
        DataInputStream dataInputStream;

        /* get file metadata from the server */
        final HeaderSet hs = new HeaderSet();
        hs.setHeader(HeaderSet.NAME, filename);
        getOperation = session.get(hs);

        /* download the actual file */
        dataInputStream = new DataInputStream(getOperation.openDataInputStream());
        mpradioFileUtils.writeToFile(dataInputStream,destination);
        dataInputStream.close();

        getOperation.close();
    }

    public interface MpradioBTFTPHelperListener {
        void onConnectionFail();
        void onBTFTProgressUpdate(int progress);
    }

}
