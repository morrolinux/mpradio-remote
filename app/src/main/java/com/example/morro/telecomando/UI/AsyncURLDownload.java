package com.example.morro.telecomando.UI;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

class AsyncURLDownload extends AsyncTask<String, Integer, Integer> {

    public static final Integer DOWNLOAD_OK = 0;
    public static final Integer DOWNLOAD_ERR = 1;

    ProgressBar bar;
    Context context;

    AsyncURLDownload(Context context){
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        bar.setVisibility(View.VISIBLE);
        Log.d("MPRADIO", "Starting download");
    }

    public void setProgressBar(ProgressBar bar) {
        this.bar = bar;
        bar.setMax(100);
    }


    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (this.bar != null) {
            bar.setProgress(values[0]);
        }
    }

    /**
     * Downloading file in background thread
     * */
    @Override
    protected Integer doInBackground(String... args) {
        int count;
        int bufferSize = 8192;
        try {
            URL url = new URL(args[0]);
            String destination = args[1];

            Log.d("MPRADIO", "Downloading");

            URLConnection conection = url.openConnection();
            conection.connect();
            InputStream input = new BufferedInputStream(url.openStream(), bufferSize);
            OutputStream output = new FileOutputStream(destination);

            int fileLength = conection.getContentLength();
            byte tmpBuffer[] = new byte[bufferSize];

            long total = 0;   //keep track of file downloaded/length
            while ((count = input.read(tmpBuffer)) != -1) {
                total += count;
                publishProgress((int) ((total/(float)fileLength)*100));
                output.write(tmpBuffer, 0, count);
            }

            output.flush();
            output.close();
            input.close();
        } catch (Exception e) {
            Log.e("MPRADIO: ", e.getMessage());
            return DOWNLOAD_ERR;
        }

        return DOWNLOAD_OK;
    }


    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        String resultMessage = "";
        if (result.equals(DOWNLOAD_OK))
            resultMessage = "Download successful!";
        else
            resultMessage = "Download ERROR!";

        Log.d("MPRADIO", resultMessage);
        Toast.makeText(context, resultMessage, Toast.LENGTH_LONG).show();

        bar.setVisibility(View.GONE);
        bar.setProgress(0);
    }


}
