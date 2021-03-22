package com.example.morro.telecomando.UI;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import com.example.morro.telecomando.Core.MpradioBTHelper;
import com.example.morro.telecomando.R;

import org.json.JSONException;
import org.json.JSONObject;

public class SettingsFragment extends Fragment {
    private View view = null;
    private MpradioBTHelper mpradioBTHelper;

    private CheckBox chkShuffle;
    private EditText inputFreq;
    private SeekBar seekFreq;
    private EditText inputStorageGain;
    private SeekBar seekGain;
    private EditText inputTreble;
    private SeekBar seekTreble;
    private JSONObject settings;
    private Switch wifiSwitch;

    private View.OnClickListener mainClickListener;

    private class AsyncSettingsDownload extends AsyncTask<String,Integer,String> {
        @Override
        protected String doInBackground(String... strings) {
            Log.d("MPRADIO", "Getting settings...");
            String settings = mpradioBTHelper.sendMessageGetReply("config get");
            return settings;
        }

        protected void onProgressUpdate(Integer... progress) {}

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d("MPRADIO", "Configuration: "+result);
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

        Log.d("MPRADIO", "Freq: " + freq);

        double sliderValue;

        inputFreq.setText(freq);
        sliderValue = (Double.valueOf(freq) - 87) / (107-87) * (seekFreq.getMax());
        seekFreq.setProgress((int)sliderValue);

        inputStorageGain.setText(gain);
        sliderValue = (Double.valueOf(gain) - (-5)) / (5-(-5)) * seekGain.getMax();
        seekGain.setProgress((int)sliderValue);

        inputTreble.setText(treble);
        sliderValue = (Double.valueOf(gain) - (-10)) / (10-(-10)) * seekGain.getMax();
        seekTreble.setProgress((int)sliderValue);

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

        String root = Environment.getExternalStorageDirectory().toString() + "/Download";
        //root = getContext().getFilesDir().toString();

        Bundle bundle = getArguments();
        mpradioBTHelper = (MpradioBTHelper) bundle.getParcelable("BTHelper");

        makeMainClickListener();

        /* Set default values for all input fields */
        // TODO: do this in after-settings fetch
        chkShuffle = view.findViewById(R.id.shuffleCheck);

        /* Not a setting anymore - keeping for reference
        ArrayAdapter<CharSequence> fileFormatAdapter = ArrayAdapter.createFromResource(this.getContext(),
                R.array.file_formats, android.R.layout.simple_spinner_item);
        fileFormatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFileFormat.setAdapter(fileFormatAdapter); */

        inputFreq = (EditText) view.findViewById(R.id.inputFreq);
        inputFreq.setText("88.8");
        seekFreq = view.findViewById(R.id.seekStation);
        seekFreq.setOnSeekBarChangeListener(seekBarChangeListener());

        inputStorageGain = (EditText) view.findViewById(R.id.inputStorageGain);
        inputStorageGain.setText("0.9");
        seekGain = (SeekBar) view.findViewById(R.id.seekGain);
        seekGain.setOnSeekBarChangeListener(seekBarChangeListener());

        inputTreble = (EditText) view.findViewById(R.id.treble);
        inputTreble.setText("0");
        seekTreble = (SeekBar) view.findViewById(R.id.seekTreble);
        seekTreble.setOnSeekBarChangeListener(seekBarChangeListener());

        /* Set the click listener for all buttons */
        view.findViewById(R.id.btnApplySettings).setOnClickListener(mainClickListener);
        view.findViewById(R.id.btnCmdSend).setOnClickListener(mainClickListener);

        /* Set wifi switch status according to RPi's status */
        wifiSwitch = view.findViewById(R.id.wifiSwitch);
        wifiSwitch.setTextOff("Off");
        wifiSwitch.setTextOn("On");

        if ((mpradioBTHelper.sendMessageGetReply("system wifi-switch status")).contains("on")){
            wifiSwitch.setChecked(true);
        }else {
            wifiSwitch.setChecked(false);
        }

        wifiSwitch.setOnCheckedChangeListener(wifiSwitchChangeListener());

        new SettingsFragment.AsyncSettingsDownload().execute("pirateradio.config", root +"/pirateradio.config");

        /* Return the inflated view to the activity who called it */
        return view;
    }

    private double round2(double value){
        return Math.round(value * 100.0) / 100.0;
    }

    private CompoundButton.OnCheckedChangeListener wifiSwitchChangeListener() {
        return new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String wifiStatus;
                if(isChecked){
                    wifiStatus = "on";
                }else{
                    wifiStatus = "off";
                }
                mpradioBTHelper.sendMessage("system wifi-switch " + wifiStatus);
                giveFeedback("Device will reboot");
            }
        };
    }

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(!fromUser) return;

                double increment = progress * 0.1;
                double value;

                switch (seekBar.getId()){
                    case R.id.seekStation:
                        value = round2(87 + increment);                 // 87/107
                        inputFreq.setText("" + value);
                        break;
                    case R.id.seekGain:
                        value = round2(-5 + increment);                 // -5/+5
                        inputStorageGain.setText("" + value);
                        break;
                    case R.id.seekTreble:
                        value = round2(-10 + increment);                // -10/+10
                        inputTreble.setText("" + value);
                        break;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };
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
            Log.d("MPRADIO", "json error");
        }
    }

    private void giveFeedback(String message){
        Log.d("MPRADIO", "Feedback: "+message);
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }

    private void sendCommand(){
        String command = ((EditText) view.findViewById(R.id.customCommand)).getText().toString();
        mpradioBTHelper.sendMessage("system "+command);
    }


}
