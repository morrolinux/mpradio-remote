package com.example.morro.telecomando.UI;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.example.morro.telecomando.Core.MpradioBTHelper;
import com.example.morro.telecomando.R;

import java.util.Set;

import static android.os.SystemClock.sleep;

public class Main4Activity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final int MY_PERMISSIONS_REQUEST = 200;
    private static final int REQUEST_ENABLE_BT = 100;
    private Fragment actionsFragment;
    protected MpradioBTHelper mpradioBTHelper;
    private Bundle bundle;
    private static boolean mainLoaded = false;
    private DrawerLayout drawer;
    private BluetoothAdapter bluetoothAdapter;
    private final BroadcastReceiver receiver = new ScanAndPairDevice();
    ActionFragmentInit actionFragmentInit;
    SharedPreferences globalSettings;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        if(mpradioBTHelper != null)
            mpradioBTHelper.closeConnection();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Ask for permissions */
        askForPermission();

        /* Make sure bluetooth is enabled. If not, ask user for permission to enable it. */
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.d("MPRADIO", "Bluetooth not supported!");
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        /* Register for broadcasts when a device is discovered. */
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        /*
        // fetch global settings
        globalSettings = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = globalSettings.edit();
        editor.putString("device_name", globalSettings.getString("deviceName", "mpradio"));
        editor.apply();
        */

        /* Activity content */
        setContentView(R.layout.activity_main4);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /* Navigation drawer */
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        /* Navigation view inside the navigation drawer */
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        /* Progress Bar (connecting with Pi) */
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        /* Init MpradioBTHelper + Action Fragment with progress bar update */
        actionFragmentInit = new ActionFragmentInit();
        actionFragmentInit.setProgressBar(progressBar);
        actionFragmentInit.execute();

    }

    /* listen for ACTION_FOUND events during scan and pair the device*/
    class ScanAndPairDevice extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String deviceName = device.getName();
            String DeviceAddress = device.getAddress(); // MAC address

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d("MPRADIO", "found device: " + deviceName + " : " + DeviceAddress);
                if (deviceName != null && deviceName.equals("mpradio"))
                    device.createBond(); // pair the device
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals((action)))
                if (BluetoothDevice.EXTRA_BOND_STATE.equals(BluetoothDevice.BOND_BONDED)) {
                    bluetoothAdapter.cancelDiscovery();
                }
        }
    }

    protected String getDeviceAddress(String deviceName) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        String deviceAddress = null;
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals(deviceName)) {
                    deviceAddress = device.getAddress(); // MAC address
                    bluetoothAdapter.cancelDiscovery(); // The device is already paired, no need to.
                    break;
                }
            }
        }
        return deviceAddress;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode < 0)
            Log.d("MPRADIO", "User denied bluetooth permission");
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        /* Close drawer if open, otherwise forward back button signal */
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Toolbar navigation buttons handle
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_manage) {
            Fragment settingsFragment = new SettingsFragment();
            settingsFragment.setArguments(bundle);
            replaceFragment(settingsFragment);
        } else if (id == R.id.nav_fetch_updates) {
            Fragment downloadUpdateFragment = new DownloadUpdateFragment();
            downloadUpdateFragment.setArguments(bundle);
            replaceFragment(downloadUpdateFragment);
        } else if (id == R.id.nav_controls) {
            actionsFragment = new ActionsFragment();
            actionsFragment.setArguments(bundle);
            replaceFragment(actionsFragment);
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void replaceFragment(Fragment fragment){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_action,fragment);
        if(mainLoaded)
            transaction.addToBackStack(null);
        else
            mainLoaded = true;

        transaction.commit();
    }

    protected boolean checkForPermission() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                &&
                (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private void askForPermission(){

        if (!checkForPermission()) {
            // Ask for permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST);
        }
    }

    protected void loadErrorFragment(String errorMessage){
        ErrorFragment errorFragment = new ErrorFragment();
        bundle = new Bundle();
        bundle.putSerializable("title","BT CONNECTION ERROR");
        bundle.putSerializable("message",errorMessage);
        errorFragment.setArguments(bundle);
        replaceFragment(errorFragment);
    }


    public class ActionFragmentInit extends AsyncTask<Void, Integer, Void>
            implements MpradioBTHelper.MpradioBTHelperListener {
        private boolean connectionFailed = false;

        private String deviceAddress;
        private ProgressBar bar;
        String errorMessage = "Please check if you meet the following conditions:\n\n" +
                "1) Bluetooth must be ENABLED on this device\n" +
                "2) The Raspberry Pi must be within reach\n" +
                "3) The Raspberry Pi must be paired to this device (if it's not, please pair it)\n" +
                "4) The paired Pi must have the default name: mpradio\n" +
                "5) There must be just one device called mpradio within your paired devices";

        @Override
        public void onConnectionFail() {
            connectionFailed = true;
        }

        public void setProgressBar(ProgressBar bar) {
            this.bar = bar;
        }

        @Override
        protected void onPreExecute(){
            /* get device address (or null if device is not paired) */
            deviceAddress = getDeviceAddress("mpradio");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            while (!bluetoothAdapter.isEnabled() || !checkForPermission())
                sleep(500);

            if (deviceAddress == null) {
                Boolean discoveryStarted = bluetoothAdapter.startDiscovery();
                Log.d("MPRADIO", "Not paired. discovery started: " + discoveryStarted);
            }

            while (getDeviceAddress("mpradio") == null)
                sleep(500);   // wait for bluetooth discovery and pairing TODO: togliere polling

            initBtHelper(deviceAddress);
            return null;
        }

        protected void initBtHelper(String deviceAddress){ //TODO: change implementation to use address instead of name
            mpradioBTHelper = new MpradioBTHelper("mpradio", this);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (this.bar != null) {
                bar.setProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(Void v){
            if(connectionFailed)
                loadErrorFragment(errorMessage);
            else
                loadActionsFragment();

            bar.setVisibility(View.GONE);
        }

        protected void loadActionsFragment() {
            /* Instantiate ActionsFragment */
            actionsFragment = new ActionsFragment();
            /* Pass mpradioBTHelper to the fragment */
            bundle = new Bundle();
            bundle.putParcelable("BTHelper", mpradioBTHelper);
            actionsFragment.setArguments(bundle);
            /* Replace actionsFragment into fragment_action container */
            replaceFragment(actionsFragment);
        }

    }

}

