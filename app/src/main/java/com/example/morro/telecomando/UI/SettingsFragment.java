package com.example.morro.telecomando.UI;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
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

    private View.OnClickListener mainClickListener;

    private class AsyncSettingsDownload extends AsyncTask<String,Integer,String> {
        String filePath;
        @Override
        protected String doInBackground(String... strings) {
            filePath = strings[1];
            System.out.println("Downloading "+filePath);
            mpradioBTHelper.getFile(strings[0],strings[1]);
            return "";
        }

        protected void onProgressUpdate(Integer... progress) {}

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                System.out.println("Reading config file "+filePath);
                readConfigFile(filePath);
            } catch (IOException e) {
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

    private void readConfigFile(String fileName) throws IOException {
        Wini ini = new Wini(new File(fileName));
        inputFreq.setText(ini.get("PIRATERADIO","frequency"));
        inputTreble.setText(ini.get("PIRATERADIO","treble"));
        inputStorageGain.setText(ini.get("PIRATERADIO","storageGain"));
        String implementation = ini.get("PIRATERADIO","implementation");
        boolean btBoost = ini.get("PIRATERADIO","btBoost",boolean.class);
        boolean shuffle = ini.get("PLAYLIST","shuffle",boolean.class);
        String fileFormat = ini.get("PLAYLIST","fileFormat");

        int implementationIndex = 0;
        if (implementation.equals("pi_fm_rds"))
            implementationIndex = 0;
        else
            implementationIndex = 1;
        spImplementation.setSelection(implementationIndex);

        chkShuffle.setChecked(shuffle);
        chkBTBoost.setChecked(btBoost);

        int fileFormatIndex = 0;
        if (fileFormat.equals("all")){
            fileFormatIndex = 0;
        }else if(fileFormat.equals("mp3")){
            fileFormatIndex = 1;
        }else if(fileFormat.equals("flac")){
            fileFormatIndex = 2;
        }
        spFileFormat.setSelection(fileFormatIndex);

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
        //TODO: just sendMessage ini file here
        giveFeedback("Hang on...");
        try {
            fetchUISettings();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mpradioBTHelper.sendFile(root+"/pirateradio.config","pirateradio.config");
    }

    public void fetchUISettings() throws IOException {
        String fileName = "/pirateradio.config";
        Wini ini = new Wini(new File(root+fileName));
        ini.put("PIRATERADIO", "frequency", inputFreq.getText().toString());
        ini.put("PIRATERADIO", "storageGain", inputStorageGain.getText().toString());
        ini.put("PIRATERADIO", "implementation", spImplementation.getSelectedItem().toString());
        ini.put("PIRATERADIO","btBoost",chkBTBoost.isChecked());
        ini.put("PLAYLIST", "shuffle", chkShuffle.isChecked());
        ini.put("PLAYLIST", "fileFormat", spFileFormat.getSelectedItem().toString());
        ini.put("PIRATERADIO","treble", inputTreble.getText().toString());
        ini.store();
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
