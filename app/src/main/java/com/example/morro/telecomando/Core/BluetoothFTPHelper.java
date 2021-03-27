package com.example.morro.telecomando.Core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.ObexTransport;
import javax.obex.Operation;
import javax.obex.ResponseCodes;

import static java.lang.Integer.min;

/**
 * Created by morro on 03/05/18.
 */

public class BluetoothFTPHelper {
    private final BluetoothDevice device;
    private final UUID FTPUUID = UUID.fromString(("00001106-0000-1000-8000-00805f9b34fb"));
    private BluetoothSocket mBtSocket;
    private ClientSession clientSession = null;

    public BluetoothFTPHelper(String address) {
        BluetoothAdapter mBtadapter = BluetoothAdapter.getDefaultAdapter();
        device = mBtadapter.getRemoteDevice(address);
    }

    public void startClientSession() throws IOException {
        /* set connection header */
        UUID uuid = UUID.fromString("F9EC7BC4-953C-11D2-984E-525400DC9E09");
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        byte[] bytes = bb.array();
        HeaderSet headerset = new HeaderSet();
        headerset.setHeader(HeaderSet.TARGET, bytes);

        /* create and connect the socket */
        mBtSocket = device.createInsecureRfcommSocketToServiceRecord(FTPUUID);
        mBtSocket.connect();

        /* create session and set header */
        clientSession = new ClientSession(new BluetoothObexTransport(mBtSocket));
        headerset = clientSession.connect(headerset);

        if (headerset.getResponseCode() == ResponseCodes.OBEX_HTTP_OK) {
            Log.d("MPRADIO", "headerset.getResponseCode() : OBEX_HTTP_OK");
        } else {
            Log.d("MPRADIO", "headerset.getResponseCode() : " + headerset.getResponseCode());
            clientSession.disconnect(headerset);
        }
    }

    protected void put(byte[] bytes, String filename, String type,
                       MpradioBTFTPHelperListener listener) throws IOException {

        Operation putOperation;
        OutputStream mOutput;

        /* send file metadata to the server */
        final HeaderSet hs = new HeaderSet();
        hs.setHeader(HeaderSet.NAME, filename);
        hs.setHeader(HeaderSet.TYPE, type);
        hs.setHeader(HeaderSet.LENGTH, ((long) bytes.length));
        putOperation = clientSession.put(hs);

        /* send the actual file */
        int offset = 0;
        int bufferSize = 4096;
        mOutput = putOperation.openOutputStream();

        while (offset < bytes.length) {
            bufferSize = min(bufferSize, bytes.length - offset);
            mOutput.write(bytes, offset, bufferSize);
            offset += bufferSize;
            Log.d("MPRADIO", "OFFSET: " + offset + " LEN: "+ bytes.length);
            if (listener != null)
                listener.onBTFTProgressUpdate((int) (1.0 * offset/bytes.length * 100));
        }

        mOutput.close();
        putOperation.close();
    }


    protected void get(final String filename, final String destination) throws IOException {
        Operation getOperation;
        DataInputStream dataInputStream;

        /* get file metadata from the server */
        final HeaderSet hs = new HeaderSet();
        hs.setHeader(HeaderSet.NAME, filename);
        getOperation = clientSession.get(hs);

        /* download the actual file */
        dataInputStream = new DataInputStream(getOperation.openDataInputStream());
        MpradioFileUtils.writeToFile(dataInputStream,destination);
        dataInputStream.close();

        getOperation.close();
    }

    public void disconnect() throws IOException {
        mBtSocket.close();
    }

    public interface MpradioBTFTPHelperListener {
        void onBTFTProgressUpdate(int progress);
    }

    private static class MpradioFileUtils {

        public static void writeToFile(byte[] data, String destination) throws IOException {
            Log.d("MPRADIO", "Writing data to " + destination);
            OutputStream output = new FileOutputStream(destination);
            output.write(data);
            output.flush();
            output.close();
        }

        public static void writeToFile(InputStream inputStream, String destination) throws IOException {
            byte[] inputBytes = readFromInputStream(inputStream);
            writeToFile(inputBytes,destination);
        }

        public static byte[] readFromInputStream(InputStream inputStream) throws IOException {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read = 0;
            while ((read = inputStream.read(buffer, 0, buffer.length)) != -1) {
                stream.write(buffer, 0, read);
            }
            stream.flush();
            return stream.toByteArray();
        }

    }

    private static class BluetoothObexTransport implements ObexTransport {
        private BluetoothSocket mSocket = null;
        public BluetoothObexTransport(BluetoothSocket socket) {
            this.mSocket = socket;
        }
        @Override
        public void close() throws IOException {
            mSocket.close();
        }
        @Override
        public DataInputStream openDataInputStream() throws IOException {
            return new DataInputStream(openInputStream());
        }
        @Override
        public DataOutputStream openDataOutputStream() throws IOException {
            return new DataOutputStream(openOutputStream());
        }
        @Override
        public InputStream openInputStream() throws IOException {
            return mSocket.getInputStream();
        }
        @Override
        public OutputStream openOutputStream() throws IOException {
            return mSocket.getOutputStream();
        }
        @Override
        public void connect() throws IOException {
        }
        @Override
        public void create() throws IOException {
        }
        @Override
        public void disconnect() throws IOException {
        }
        @Override
        public void listen() throws IOException {
        }
        public boolean isConnected() throws IOException {
            return true;
        }
        @Override
        public int getMaxTransmitPacketSize() {
            return 90000;//mSocket.getMaxTransmitPacketSize();
        }
        @Override
        public int getMaxReceivePacketSize() {
            return 90000;//mSocket.getMaxReceivePacketSize();
        }
        public String getRemoteAddress() {
            if (mSocket == null) {
                return null;
            }
            return mSocket.getRemoteDevice().getAddress();
        }
        @Override
        public boolean isSrmSupported() {
            //  if (mSocket.getConnectionType() == BluetoothSocket.TYPE_L2CAP) {
            //      return true;
            //   }
            return false;
        }
    }

}
