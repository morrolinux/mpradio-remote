package com.example.morro.telecomando.UI;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.example.morro.telecomando.Core.ContentPi;
import com.example.morro.telecomando.Core.Song;
import com.example.morro.telecomando.Core.MpradioBTHelper;
import com.example.morro.telecomando.R;
import java.util.ArrayList;

import static java.lang.Thread.sleep;

public class ActionsFragment extends Fragment
        implements ItemAdapter.ItemAdapterListener, MpradioBTHelper.PutAndGetListener{
    public static final String ACTION_SONG_NAME = "song_name";
    public static final String ACTION_GET_LIBRARY = "library";
    public static final String ACTION_PAUSE = "pause";
    public static final String ACTION_PLAY = "play";
    public static final String ACTION_RESUME = "resume";
    public static final String ACTION_NEXT = "next";
    public static final String ACTION_RESTART_MPRADIO = "system systemctl restart mpradio";
    public static final String ACTION_POWEROFF = "system poweroff";
    public static final String ACTION_REBOOT = "system reboot";
    public static final String ACTION_SEEK = "SEEK";
    public static final String ACTION_SCAN = "SCAN";

    private ArrayList<Song> songs;
    private ItemAdapter itemAdapter;
    private MpradioBTHelper mpradioBTHelper;
    private View.OnClickListener mainClickListener;
    private TextView txtNowPlaying;

    @Override
    public void onResume(){
        super.onResume();

        /* uncomment to test what happens when cached db library differs from Pi contents :) */
        // ContentPi.dbInsertSong("000 - Updating Library...", "This is cached data", "Pi library might differ", "A", "A", getContext());

        /* get music library from local db while we wait to fetch the updated library from the Pi */
        ContentPi.dbGetLibrary(songs, getContext());
        itemAdapter.notifyDataSetChanged();

        mpradioBTHelper.getNowPlaying(this);
        mpradioBTHelper.getLibrary(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        /* Inflate the desired layout first */
        View view = inflater.inflate(R.layout.actions_fragment, container, false);

        /* Tell the fragment there are menu items */
        setHasOptionsMenu(true);

        /* Start the Mpradio Bluetooth helper */
        Bundle bundle = getArguments();
        mpradioBTHelper = (MpradioBTHelper) bundle.getParcelable("BTHelper");

        makeMainClickListener();

        /* Set the click listener for all buttons */
        view.findViewById(R.id.btnStop).setOnClickListener(mainClickListener);
        view.findViewById(R.id.btnStart).setOnClickListener(mainClickListener);
        view.findViewById(R.id.btnRestart).setOnClickListener(mainClickListener);
        view.findViewById(R.id.btnSkip).setOnClickListener(mainClickListener);
        view.findViewById(R.id.btnReload).setOnClickListener(mainClickListener);
        view.findViewById(R.id.btnShutdown).setOnClickListener(mainClickListener);
        view.findViewById(R.id.btnReboot).setOnClickListener(mainClickListener);
        view.findViewById(R.id.btnSeekForward).setOnClickListener(mainClickListener);
        view.findViewById(R.id.btnSeekBackwards).setOnClickListener(mainClickListener);

        // RECYCLERVIEW
        RecyclerView rvLibrary = view.findViewById(R.id.rvLibrary);
        // Now playing
        txtNowPlaying = view.findViewById(R.id.lblNow_playing);
        // Initialize items
        songs = Song.buildDummyTrackList(0);
        // Create adapter passing in the sample user data
        itemAdapter = new ItemAdapter(this.getContext(), songs,this);
        // Attach the adapter to the recyclerview to populate items
        rvLibrary.setAdapter(itemAdapter);
        // Set layout manager to position the items
        rvLibrary.setLayoutManager(new LinearLayoutManager(this.getContext()));
        // add line separator between recycler view items
        rvLibrary.addItemDecoration(new DividerItemDecoration(rvLibrary.getContext(), 1));
        // add swipe and drag gestures
        SwipeAndDragHelper swipeAndDragHelper = new SwipeAndDragHelper(itemAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(swipeAndDragHelper);
        touchHelper.attachToRecyclerView(rvLibrary);
        /* Return the inflated view to the activity that called it */
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main4, menu);
        super.onCreateOptionsMenu(menu,inflater);

        /* SEARCH BUTTON Configuration */
        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getContext().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        // listening to search query text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                itemAdapter.getFilter().filter(query);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String query) {
                itemAdapter.getFilter().filter(query);
                return false;
            }
        });
    }

    /** SELECTED ITEM ACTION (from ItemAdapterListener) */
    @Override
    public void onItemSelected(Song song) {
        Log.d("MPRADIO", "SELECTED: "+ song);
        Toast.makeText(this.getContext().getApplicationContext(), "Selected: " + song, Toast.LENGTH_LONG).show();

        if(song.getItemPath().equals("/..")) {
            ContentPi.dbQuery(songs, getContext(), null, null);
            itemAdapter.notifyDataSetChanged();
            return;
        }

        mpradioBTHelper.sendKVMessage(ACTION_PLAY, song.getJson());
        mpradioBTHelper.getNowPlaying(this);
    }

    @Override
    public void onItemSwiped(Song song, int direction) {
        String selectionClause;
        String[] selArgs;

        if(song.getItemPath().equals("/..")) {
            selectionClause = null;
            selArgs = null;
        } else if (direction == ItemTouchHelper.LEFT) {
            Toast.makeText(getContext(), "ARTIST: " + song.getArtist(), Toast.LENGTH_LONG).show();
            selectionClause = ContentPi.SONG_ARTIST + " = ? ";
            selArgs = new String[]{song.getArtist()};
        } else {
            Toast.makeText(getContext(), "ALBUM: " + song.getAlbum(), Toast.LENGTH_LONG).show();
            selectionClause = ContentPi.SONG_ALBUM + " = ? ";
            selArgs = new String[]{song.getAlbum()};
        }

        ContentPi.dbQuery(songs, getContext(), selectionClause, selArgs);

        if(!song.getItemPath().equals("/.."))
            songs.add(0, new Song("..", "BACK", "TO ALL MUSIC", "", "/.."));

        itemAdapter.notifyDataSetChanged();


        // reloadRemotePlaylist(song.getTitle());
    }

    /* actions to perform when we receive an async message reply */
    @Override
    public void onAsyncReply(String action, String result) {
        // Log.d("MPRADIO", "onAsyncReply " + action + " " + result);
        if(action.equals(ACTION_SONG_NAME)) {
            txtNowPlaying.setText(result);
        } else if(action.equals(ACTION_GET_LIBRARY)) {
            ContentPi.dbCreateFromJSON(result, getContext());  // process JSON and insert in DB
            ContentPi.dbGetLibrary(songs, getContext());            // get Song ArrayList from DB
            itemAdapter.notifyDataSetChanged();                     // update the view
        }
    }


    private void skip() {
        mpradioBTHelper.sendMessage(ACTION_NEXT);
        mpradioBTHelper.getNowPlaying(this);
    }
    private void stop() {
        mpradioBTHelper.sendMessage(ACTION_PAUSE);
    }
    private void start() {
        mpradioBTHelper.sendMessage(ACTION_RESUME);
    }
    private void restart() {
        mpradioBTHelper.sendMessage(ACTION_RESTART_MPRADIO);
    }
    private void shutdown() {
        giveFeedback("Hang on...");
        mpradioBTHelper.sendMessage(ACTION_POWEROFF);
    }
    private void reboot() {
        giveFeedback("Hang on...");
        mpradioBTHelper.sendMessage(ACTION_REBOOT);
    }
    private void seekBackwards() {
        mpradioBTHelper.sendMessage(ACTION_SEEK + " -10");
    }
    private void seekForward() {
        mpradioBTHelper.sendMessage(ACTION_SEEK + " +10");
    }

    private void giveFeedback(String message){
        Log.d("MPRADIO", "Feedback: "+message);
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }

    private void reloadRemotePlaylist(String path){
        mpradioBTHelper.sendMessage(ACTION_SCAN + " " + path);
        mpradioBTHelper.getNowPlaying(this);
        mpradioBTHelper.getLibrary(this);
    }

    /** Creates the main click listener for this Fragment */
    private void makeMainClickListener() {
        mainClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btnStop:
                        stop();
                        break;
                    case R.id.btnStart:
                        start();
                        break;
                    case R.id.btnRestart:
                        restart();
                        break;
                    case R.id.btnSkip:
                        skip();
                        break;
                    case R.id.btnReload:
                        reloadRemotePlaylist("/pirateradio");
                        break;
                    case R.id.btnShutdown:
                        shutdown();
                        break;
                    case R.id.btnReboot:
                        reboot();
                        break;
                    case R.id.btnSeekForward:
                        seekForward();
                        break;
                    case R.id.btnSeekBackwards:
                        seekBackwards();
                        break;
                    default:
                        break;
                }
            }
        };
    }

}
