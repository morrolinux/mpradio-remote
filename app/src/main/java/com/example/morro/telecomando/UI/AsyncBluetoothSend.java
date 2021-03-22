package com.example.morro.telecomando.UI;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.morro.telecomando.Core.MpradioBTHelper;

public class AsyncBluetoothSend extends AsyncTask<String,String,String> {
    MpradioBTHelper mpradioBTHelper;
    Activity activity;

    @Override
    protected String doInBackground(String... strings) {
        mpradioBTHelper.sendFile(strings[0],strings[1]);
        return null;
    }

    public AsyncBluetoothSend(MpradioBTHelper mpradioBTHelper,Activity activity){
        this.mpradioBTHelper = mpradioBTHelper;
        this.activity = activity;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        String message = "Sending update package to the Pi. \n" +
                "this will take some time...";
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        Log.d("MPRADIO", message);
    }

    @Override
    protected void onPostExecute(String file_url) {
        String message = "Update package sent to the Pi. \n" +
                "Please wait until it reboots...";
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        Log.d("MPRADIO", message);
    }

}
