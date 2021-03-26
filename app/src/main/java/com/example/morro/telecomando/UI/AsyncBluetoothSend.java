package com.example.morro.telecomando.UI;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.morro.telecomando.Core.MpradioBTHelper;

public class AsyncBluetoothSend extends AsyncTask<String,String,String>
        implements MpradioBTHelper.MpradioBTHelperListener {
    String preMessage =" Sending update package to the Pi. this will take some time...";
    String postMessage = "Update package sent to the Pi. Please wait until it reboots...";
    String errMessage = "Error connecting to FTP service!";
    MpradioBTHelper mpradioBTHelper;
    @SuppressLint("StaticFieldLeak")
    Activity activity;
    @SuppressLint("StaticFieldLeak")
    ProgressBar progressBar;


    public AsyncBluetoothSend(MpradioBTHelper mpradioBTHelper, Activity activity, ProgressBar pb){
        this.mpradioBTHelper = mpradioBTHelper;
        this.mpradioBTHelper.setListener(this);
        this.activity = activity;
        this.progressBar = pb;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Toast.makeText(activity, preMessage, Toast.LENGTH_LONG).show();
        Log.d("MPRADIO", preMessage);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected String doInBackground(String... strings) {
        mpradioBTHelper.sendFile(strings[0],strings[1]);
        return null;
    }

    @Override
    protected void onPostExecute(String file_url) {
        Toast.makeText(activity, postMessage, Toast.LENGTH_LONG).show();
        Log.d("MPRADIO", postMessage);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onBTProgressUpdate(int progress) {
        progressBar.setProgress(progress);
    }

    @Override
    public void onConnectionFail() { }
}
