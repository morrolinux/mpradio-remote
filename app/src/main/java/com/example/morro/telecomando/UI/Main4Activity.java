package com.example.morro.telecomando.UI;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
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

public class Main4Activity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final int MY_PERMISSIONS_REQUEST_READ_STORAGE = 0;
    private static final int REQUEST_ENABLE_BT = 100;
    private Fragment actionsFragment;
    protected MpradioBTHelper mpradioBTHelper;
    private Bundle bundle;
    private static boolean mainLoaded = false;
    private DrawerLayout drawer;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mpradioBTHelper != null)
            mpradioBTHelper.closeConnection();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.d("MPRADIO", "Bluetooth not supported!");
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        /* Init MpradioBTHelper + Action Fragment with progress bar update */
        ActionFragmentInit actionFragmentInit = new ActionFragmentInit();
        actionFragmentInit.setProgressBar(progressBar);
        actionFragmentInit.execute();

        /* Ask for permissions */
        askForPermission();
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


    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main4, menu);
        return true;
    } */


    /**
     * Toolbar menu clicks handling TODO: remove?
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Fragment settingsFragment = new SettingsFragment();
            settingsFragment.setArguments(bundle);
            replaceFragment(settingsFragment);
            return true;
        }

        if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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

    private void askForPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, ask for permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_STORAGE);
        }
    }


    public class ActionFragmentInit extends AsyncTask<Void, Integer, Void>
            implements MpradioBTHelper.MpradioBTHelperListener {
        private boolean connectionFailed = false;

        ProgressBar bar;

        @Override
        public void onConnectionFail() {
            connectionFailed = true;
        }

        public void setProgressBar(ProgressBar bar) {
            this.bar = bar;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            /* Start the Mpradio Bluetooth helper */
            mpradioBTHelper = new MpradioBTHelper("mpradio",this);
            return null;
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
                loadErrorFragment();
            else
                loadActionsFragment();

            bar.setVisibility(View.GONE);
        }

        protected void loadActionsFragment(){
            /* Instantiate ActionsFragment */
            actionsFragment = new ActionsFragment();
            /* Pass mpradioBTHelper to the fragment */
            bundle = new Bundle();
            bundle.putParcelable("BTHelper", mpradioBTHelper);
            actionsFragment.setArguments(bundle);
            /* Replace actionsFragment into fragment_action container */
            replaceFragment(actionsFragment);
        }

        protected void loadErrorFragment(){
            String errorMessage = "Please check if you meet the following conditions:\n\n" +
                    "1) Bluetooth must be ENABLED on this device\n" +
                    "2) The Raspberry Pi must be within reach\n" +
                    "3) The Raspberry Pi must be paired to this device (if it's not, please pair it)\n" +
                    "4) The paired Pi must have the default name: mpradio\n" +
                    "5) There must be just one device called mpradio within your paired devices";
            ErrorFragment errorFragment = new ErrorFragment();
            bundle = new Bundle();
            bundle.putSerializable("title","BT CONNECTION ERROR");
            bundle.putSerializable("message",errorMessage);
            errorFragment.setArguments(bundle);
            replaceFragment(errorFragment);
        }

    }

}

