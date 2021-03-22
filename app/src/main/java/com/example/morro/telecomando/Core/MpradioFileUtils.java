package com.example.morro.telecomando.Core;

import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class MpradioFileUtils {

    public void writeToFile(byte[] data, String destination) throws IOException {
        Log.d("MPRADIO", "Writing data to "+destination);
        OutputStream output = new FileOutputStream(destination);
        output.write(data);
        // flushing output
        output.flush();
        // closing streams
        output.close();
    }


    public void writeToFile(InputStream inputStream, String destination) throws IOException {
        byte[] inputBytes = readFromInputStream(inputStream);
        writeToFile(inputBytes,destination);
    }

    public byte[] readFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read = 0;
        while ((read = inputStream.read(buffer, 0, buffer.length)) != -1) {
            baos.write(buffer, 0, read);
        }
        baos.flush();
        return baos.toByteArray();
    }

}
