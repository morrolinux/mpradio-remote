package com.example.morro.telecomando.ui;

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
import com.example.morro.telecomando.core.MpradioBTHelper;
import com.example.morro.telecomando.R;
import org.json.JSONException;
import org.json.JSONObject;
import static com.example.morro.telecomando.core.MpradioBTHelper.ACTION_GET_CONFIG;
import static com.example.morro.telecomando.core.MpradioBTHelper.ACTION_GET_WIFI_STATUS;
import static com.example.morro.telecomando.core.MpradioBTHelper.ACTION_SET_CONFIG;

public class SettingsFragment extends Fragment implements MpradioBTHelper.PutAndGetListener {
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

    @Override
    public void onAsyncReply(String action, String result) {
        if (action.equals(ACTION_GET_CONFIG)) {
            try {
                updateUIConfig(result);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            view.findViewById(R.id.settingsProgressBar).setVisibility(View.GONE);
        } else if (action.equals(ACTION_GET_WIFI_STATUS)) {
            wifiSwitch.setChecked(result.contains("on"));
            wifiSwitch.setOnCheckedChangeListener(makeWifiSwitchChangeListener());
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        mpradioBTHelper.getSettings(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
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

        /* Set default values for all input fields */
        chkShuffle = view.findViewById(R.id.shuffleCheck);

        /* Not a setting anymore - keeping for reference
        ArrayAdapter<CharSequence> fileFormatAdapter = ArrayAdapter.createFromResource(this.getContext(),
                R.array.file_formats, android.R.layout.simple_spinner_item);
        fileFormatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFileFormat.setAdapter(fileFormatAdapter); */

        inputFreq = (EditText) view.findViewById(R.id.inputFreq);
        inputFreq.setText("88.8");
        seekFreq = view.findViewById(R.id.seekStation);
        seekFreq.setOnSeekBarChangeListener(makeSeekBarChangeListener());

        inputStorageGain = (EditText) view.findViewById(R.id.inputStorageGain);
        inputStorageGain.setText("0.9");
        seekGain = (SeekBar) view.findViewById(R.id.seekGain);
        seekGain.setOnSeekBarChangeListener(makeSeekBarChangeListener());

        inputTreble = (EditText) view.findViewById(R.id.treble);
        inputTreble.setText("0");
        seekTreble = (SeekBar) view.findViewById(R.id.seekTreble);
        seekTreble.setOnSeekBarChangeListener(makeSeekBarChangeListener());

        /* Set the click listener for all buttons */
        view.findViewById(R.id.btnApplySettings).setOnClickListener(makeMainClickListener());
        view.findViewById(R.id.btnCmdSend).setOnClickListener(makeMainClickListener());

        /* Set wifi switch status according to RPi's status */
        wifiSwitch = view.findViewById(R.id.wifiSwitch);
        wifiSwitch.setTextOff("Off");
        wifiSwitch.setTextOn("On");


        mpradioBTHelper.getWifiStatus(this);
        mpradioBTHelper.getSettings(this);

        view.findViewById(R.id.settingsProgressBar).setVisibility(View.VISIBLE);

        // wifiSwitch.setOnCheckedChangeListener(wifiSwitchChangeListener());
        /* Return the inflated view to the activity who called it */
        return view;
    }

    private CompoundButton.OnCheckedChangeListener makeWifiSwitchChangeListener() {
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

    private SeekBar.OnSeekBarChangeListener makeSeekBarChangeListener() {
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

    private View.OnClickListener makeMainClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btnApplySettings:
                        applySettings();
                        break;
                    case R.id.btnCmdSend:
                        Toast.makeText(getContext(), "Hang on...", Toast.LENGTH_LONG).show();
                        sendCommand();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void sendCommand(){
        String command = ((EditText) view.findViewById(R.id.customCommand)).getText().toString();
        mpradioBTHelper.sendMessage("system "+command);
    }

    public void applySettings(){
        giveFeedback("Hang on...");
        fetchUIConfig();
        mpradioBTHelper.sendKVMessage(ACTION_SET_CONFIG, settings.toString());
    }

    private void updateUIConfig(String jsonData) throws JSONException{

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
        sliderValue = (Double.parseDouble(freq) - 87) / (107-87) * (seekFreq.getMax());
        seekFreq.setProgress((int)sliderValue);

        inputStorageGain.setText(gain);
        sliderValue = (Double.parseDouble(gain) - (-5)) / (5-(-5)) * seekGain.getMax();
        seekGain.setProgress((int)sliderValue);

        inputTreble.setText(treble);
        sliderValue = (Double.parseDouble(gain) - (-10)) / (10-(-10)) * seekGain.getMax();
        seekTreble.setProgress((int)sliderValue);

        chkShuffle.setChecked(shuffle);

    }

    public void fetchUIConfig(){
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

    private static double round2(double value){
        return Math.round(value * 100.0) / 100.0;
    }

    private static Boolean strToBool(String s){
        return s.toLowerCase().equals("true");
    }

}
