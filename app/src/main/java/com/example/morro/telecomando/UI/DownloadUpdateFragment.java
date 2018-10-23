package com.example.morro.telecomando.UI;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.morro.telecomando.Core.MpradioBTHelper;
import com.example.morro.telecomando.R;


public class DownloadUpdateFragment extends Fragment implements View.OnClickListener{
    MpradioBTHelper mpradioBTHelper;
    String updateFolderPath;
    Context context;
    ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle bundle = getArguments();
        mpradioBTHelper = (MpradioBTHelper) bundle.getParcelable("BTHelper");
        updateFolderPath = Environment.getExternalStorageDirectory().toString()+"/Download";

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_download_updates, container, false);
        ((Button)view.findViewById(R.id.btnDownloadCore)).setOnClickListener(this);
        ((Button)view.findViewById(R.id.btnUpdateCore)).setOnClickListener(this);
        ((Button)view.findViewById(R.id.btnDownloadPiFm)).setOnClickListener(this);
        ((Button)view.findViewById(R.id.btnUpdatePiFm)).setOnClickListener(this);
        ((Button)view.findViewById(R.id.btnDownloadApp)).setOnClickListener(this);
        ((Button)view.findViewById(R.id.btnUpdateApp)).setOnClickListener(this);

        /* Progress Bar */
        progressBar = (ProgressBar) view.findViewById(R.id.downloadProgress);
        progressBar.setVisibility(View.GONE);

        return view;
    }

        @Override
        public void onClick(View v) {
            context = getContext();
            AsyncBluetoothSend asyncBluetoothSend = new AsyncBluetoothSend(mpradioBTHelper,getActivity());
            AsyncURLDownload asyncURLDownload = new AsyncURLDownload(context);
            asyncURLDownload.setProgressBar(progressBar);

            switch (v.getId()) {
                case R.id.btnDownloadCore:
                    asyncURLDownload.execute("https://github.com/morrolinux/mpradio/archive/master.zip",updateFolderPath+"/mpradio-master.zip");
                    break;
                case R.id.btnUpdateCore:
                    mpradioBTHelper.sendMessage("system systemctl stop mpradio");
                    asyncBluetoothSend.execute(updateFolderPath+"/mpradio-master.zip","mpradio-master.zip");
                    break;
                case R.id.btnDownloadPiFm:
                    asyncURLDownload.execute("https://github.com/Miegl/PiFmAdv/archive/master.zip",updateFolderPath+"/pifmadv-master.zip");
                    break;
                case R.id.btnUpdatePiFm:
                    mpradioBTHelper.sendMessage("system systemctl stop mpradio");
                    asyncBluetoothSend.execute(updateFolderPath+"/pifmadv-master.zip","pifmadv-master.zip");
                    break;
                case R.id.btnDownloadApp:
                    Toast.makeText(getActivity(), "Not implemented yet!", Toast.LENGTH_LONG).show();
                    //new AsyncURLDownload().execute("https://github.com/morrolinux/mpradio/archive/app.apk","/Download/mpradio.apk");
                    break;
                case R.id.btnUpdateApp:
                    Toast.makeText(getActivity(), "Not implemented yet!", Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }



}
