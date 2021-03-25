package com.example.morro.telecomando.Core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.ObexTransport;
import javax.obex.Operation;
import javax.obex.ResponseCodes;

/**
 * Created by morro on 03/05/18.
 */

public class BluetoothFTPHelper {

    private BluetoothAdapter mBtadapter;
    private BluetoothDevice device;
    private BluetoothSocket mBtSocket;
    private final UUID FTPUUID = UUID.fromString(("00001106-0000-1000-8000-00805f9b34fb"));
    private String TAG = "BluetoothFTPHelper";
    MpradioFileUtils mpradioFileUtils;

    public void disconnect(){
        try {
            mBtSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BluetoothFTPHelper(String address) {
        mBtadapter = BluetoothAdapter.getDefaultAdapter();
        device = mBtadapter.getRemoteDevice(address);
        try {
            mBtSocket = device.createInsecureRfcommSocketToServiceRecord(FTPUUID);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ClientSession setup() {
        ClientSession mSession = null;
        UUID uuid = UUID.fromString("F9EC7BC4-953C-11D2-984E-525400DC9E09");
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        byte[] bytes = bb.array();
        try {
            // connect the socket
            mBtSocket.connect();
            BluetoothObexTransport mTransport = null;

            mSession = new ClientSession((ObexTransport) (mTransport = new BluetoothObexTransport(mBtSocket)));

            HeaderSet headerset = new HeaderSet();
            headerset.setHeader(HeaderSet.TARGET, bytes);

            headerset = mSession.connect(headerset);

            if (headerset.getResponseCode() == ResponseCodes.OBEX_HTTP_OK) {
                //boolean mConnected = true;
            } else {
                mSession.disconnect(headerset);
            }
        } catch (Exception e) {
            // e.printStackTrace();
            Log.d("MPRADIO", "Bluetooth FTP error: " + e.getMessage());
        }
        return mSession;
    }

    protected boolean put(ClientSession session, byte[] bytes, String filename, String type) throws IOException {
        Log.d("MPRADIO", "ftpHelper::put ");

        Operation putOperation = null;
        OutputStream mOutput = null;

        // sendMessage a file with meta data to the server
        final HeaderSet hs = new HeaderSet();
        hs.setHeader(HeaderSet.NAME, filename);
        hs.setHeader(HeaderSet.TYPE, type);
        hs.setHeader(HeaderSet.LENGTH, ((long) bytes.length));
        Log.v(TAG, filename);
        putOperation = session.put(hs);

        mOutput = putOperation.openOutputStream();
        mOutput.write(bytes);
        mOutput.close();
        putOperation.close();

        return true;
    }


    protected boolean get(final ClientSession session, final String filename, final String destination) {
        mpradioFileUtils = new MpradioFileUtils();
        boolean retry = true;
        int times = 0;
        while (retry && times < 4) {
            Operation getOperation = null;
            DataInputStream dataInputStream = null;
            try {
                final HeaderSet hs = new HeaderSet();
                hs.setHeader(HeaderSet.NAME, filename);
                Log.v(TAG, filename);
                getOperation = session.get(hs);

                dataInputStream = new DataInputStream(getOperation.openDataInputStream());
                mpradioFileUtils.writeToFile(dataInputStream,destination);
                dataInputStream.close();

                getOperation.close();
            } catch (Exception e) {
                Log.e(TAG, "getFile failed", e);
                retry = true;
                times++;
                continue;
            } finally {
                try {

                    if (dataInputStream != null)
                        dataInputStream.close();
                    if (getOperation != null)
                        getOperation.close();
                } catch (Exception e) {
                    Log.e(TAG, "getFile finally failed", e);
                    retry = true;
                    times++;
                }
            }
            retry = false;
            return true;
        }
        return false;
    }

}
