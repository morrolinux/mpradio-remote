package com.example.morro.telecomando.UI;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.widget.TextView;
import com.example.morro.telecomando.Core.MpradioBTHelper;
import com.example.morro.telecomando.R;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
        TextView connecting = findViewById(R.id.connecting);
        progressBar.setVisibility(View.VISIBLE);
        connecting.setVisibility(View.VISIBLE);

        /* Init MpradioBTHelper + Action Fragment with progress bar update */
        actionFragmentInit = new ActionFragmentInit(this);
        actionFragmentInit.setProgressBar(progressBar);
        actionFragmentInit.execute("mpradio");

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
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    bluetoothAdapter.cancelDiscovery();
                }
        }
    }

    protected BluetoothDevice getDevice(String deviceName) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals(deviceName)) {
                    bluetoothAdapter.cancelDiscovery(); // The device is already paired, no need to.
                    return device;
                }
            }
        }
        return null;
    }

    protected void unbondDevice(BluetoothDevice device) {
        try {
            device.getClass().getMethod("removeBond").invoke(device);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException illegalAccessException) {
            illegalAccessException.printStackTrace();
        }
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

        if (mpradioBTHelper == null) {
            Log.e("MPRADIO", "Bluetooth not connected!");
        } else if (id == R.id.nav_manage) {
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

    protected boolean permissionNotGiven() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ||
                (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED);
    }

    private void askForPermission(){

        if (permissionNotGiven()) {
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

    public static void restartActivity(Context context) {
        Intent intent = new Intent(context, Main4Activity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        ((Activity) context).finish();
        Runtime.getRuntime().exit(0);
    }

    @SuppressLint("StaticFieldLeak")  // there's only one activity (we swap Fragments): no leak can occur
    public class ActionFragmentInit extends AsyncTask<String, Integer, Void> {
        private ProgressBar progressBar;
        private final Context context;
        TextView connecting = findViewById(R.id.connecting);
        BluetoothDevice device;
        String errorMessage = "Please check if you meet the following conditions:\n\n" +
                "1) Bluetooth must be ENABLED on this device\n" +
                "2) The Raspberry Pi must be within reach\n" +
                "3) The Raspberry Pi must be paired to this device (if it's not, please pair it)\n" +
                "4) The paired Pi must have the default name: mpradio\n" +
                "5) There must be just one device called mpradio within your paired devices";

        public ActionFragmentInit(Context context) {
            this.context = context;
        }

        public void setProgressBar(ProgressBar bar) {
            this.progressBar = bar;
        }

        @Override
        protected Void doInBackground(String... strings) {
            String deviceName = strings[0];
            int discoveryTime = 60;

            /* wait for the user to enable bluetooth and give permissions */
            while (!bluetoothAdapter.isEnabled() || permissionNotGiven())
                sleep(1000);

            int i = discoveryTime;

            /* always executed on launch */
            while (mpradioBTHelper == null) {

                /* get device address (or null if device is not paired) */
                device = getDevice(deviceName);

                /* (re)start discovery and wait for pairing until paired */
                while (device == null) {
                    if (i % discoveryTime == 0) {
                        boolean discoveryStarted = bluetoothAdapter.startDiscovery();
                        Log.d("MPRADIO", "Not paired. discovery started: " + discoveryStarted);
                    }
                    i++;
                    sleep(1000);   // wait for bluetooth discovery and pairing TODO: togliere polling
                    device = getDevice(deviceName);
                }

                /* If device is paired but unreachable, un-pair it and scan again: user might have changed Pi */
                try {
                    mpradioBTHelper = new MpradioBTHelper(device.getAddress(), context);
                } catch (Exception e) {
                    Log.d("MPRADIO", "Bluetooth error: " + e.getClass() + " " + device.getAddress());
                    unbondDevice(device);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v){
            progressBar.setVisibility(View.GONE);
            connecting.setVisibility(View.GONE);

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

