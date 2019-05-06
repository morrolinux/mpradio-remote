package com.example.morro.telecomando.UI;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.JsonReader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.morro.telecomando.Core.MpradioBTHelper;
import com.example.morro.telecomando.R;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

public class SettingsFragment extends Fragment {
    private View view = null;
    private MpradioBTHelper mpradioBTHelper;

    private Spinner spImplementation;
    private CheckBox chkShuffle;
    private CheckBox chkBTBoost;
    private Spinner spFileFormat;
    private EditText inputFreq;
    private EditText inputStorageGain;
    private EditText inputTreble;
    private String root;
    private JSONObject settings;

    private View.OnClickListener mainClickListener;

    private class AsyncSettingsDownload extends AsyncTask<String,Integer,String> {
        @Override
        protected String doInBackground(String... strings) {
            System.out.println("Getting settings...");
            String settings = mpradioBTHelper.sendMessageGetReply("config get");
            return settings;
        }

        protected void onProgressUpdate(Integer... progress) {}

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            System.out.println("Configuration: "+result);
            try {
                readConfiguration(result);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // TODO: Add UI Settings update according to configuration file
            //TextView txt = (TextView) view.findViewById(R.id.lblNow_playing);
            //txt.setText(result);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        //new SettingsFragment.AsyncSettingsDownload().execute("pirateradio.config","/Download/pirateradio.config");
    }

    /** Creates the main click listener for this Fragment */
    private void makeMainClickListener() {
        mainClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btnApplySettings:
                        applySettings();
                        break;
                    case R.id.btnCmdSend:
                        sendCommand();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void readConfiguration(String jsonData) throws JSONException{

        settings = new JSONObject(jsonData);
        JSONObject pirateradio = settings.getJSONObject("PIRATERADIO");
        JSONObject playlist = settings.getJSONObject("PLAYLIST");
        JSONObject rds = settings.getJSONObject("RDS");

        String freq = pirateradio.getString("frequency");
        String gain = pirateradio.getString("storageGain");
        String treble = pirateradio.getString("treble");

        Boolean shuffle = strToBool(playlist.getString("shuffle"));

        System.out.println("Freq:" + freq);

        inputFreq.setText(freq);
        inputStorageGain.setText(gain);
        inputTreble.setText(treble);
        chkShuffle.setChecked(shuffle);

    }

    private Boolean strToBool(String s){
        return s.toLowerCase().equals("true");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /* Inflate the desired layout first */
        view = inflater.inflate(R.layout.settings_fragment, container, false);

        root = Environment.getExternalStorageDirectory().toString()+"/Download";
        //root = getContext().getFilesDir().toString();

        Bundle bundle = getArguments();
        mpradioBTHelper = (MpradioBTHelper) bundle.getParcelable("BTHelper");

        makeMainClickListener();

        /* Set default values for all input fields */
        // TODO: do this in after-settings fetch
        spImplementation = (Spinner) view.findViewById(R.id.spImplementation);
        ArrayAdapter<CharSequence> implementationsAdapter = ArrayAdapter.createFromResource(this.getContext(),
        R.array.implementations, android.R.layout.simple_spinner_item);
        implementationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spImplementation.setAdapter(implementationsAdapter);

        chkShuffle = view.findViewById(R.id.shuffleCheck);
        chkBTBoost = view.findViewById(R.id.btBoostCheck);

        spFileFormat = (Spinner) view.findViewById(R.id.spFileFormat);
        ArrayAdapter<CharSequence> fileFormatAdapter = ArrayAdapter.createFromResource(this.getContext(),
                R.array.file_formats, android.R.layout.simple_spinner_item);
        fileFormatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFileFormat.setAdapter(fileFormatAdapter);

        inputFreq = (EditText) view.findViewById(R.id.inputFreq);
        inputFreq.setText("88.8");

        inputStorageGain = (EditText) view.findViewById(R.id.inputStorageGain);
        inputStorageGain.setText("0.9");

        inputTreble = (EditText) view.findViewById(R.id.treble);
        inputTreble.setText("0");

        /* Set the click listener for all buttons */
        view.findViewById(R.id.btnApplySettings).setOnClickListener(mainClickListener);
        view.findViewById(R.id.btnCmdSend).setOnClickListener(mainClickListener);

        new SettingsFragment.AsyncSettingsDownload().execute("pirateradio.config",root+"/pirateradio.config");

        /* Return the inflated view to the activity who called it */
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    public void applySettings(){
        giveFeedback("Hang on...");
        fetchUISettings();
        mpradioBTHelper.sendMessage("config set", settings.toString());
    }

    public void fetchUISettings(){
        try{
            settings.getJSONObject("PIRATERADIO").put("frequency", inputFreq.getText().toString());
            settings.getJSONObject("PIRATERADIO").put("storageGain", inputStorageGain.getText().toString());
            settings.getJSONObject("PIRATERADIO").put("treble", inputTreble.getText().toString());
            settings.getJSONObject("PLAYLIST").put("shuffle", String.valueOf(chkShuffle.isChecked()));
        }catch (JSONException e){
            System.out.println("json error");
        }
    }

    private void giveFeedback(String message){
        System.out.println("Feedback: "+message);
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }

    private void sendCommand(){
        String command = ((EditText) view.findViewById(R.id.customCommand)).getText().toString();
        mpradioBTHelper.sendMessage("system "+command);
    }


}
