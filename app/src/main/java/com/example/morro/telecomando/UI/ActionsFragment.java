package com.example.morro.telecomando.UI;

import android.app.SearchManager;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static java.lang.Thread.sleep;

public class ActionsFragment extends Fragment implements ItemAdapter.ItemAdapterListener {
    private View view = null;
    ArrayList<Song> songs;
    ItemAdapter itemAdapter;
    private MpradioBTHelper mpradioBTHelper;
    private View.OnClickListener mainClickListener;
    RecyclerView rvLibrary;
    SearchView searchView;

    private class AsyncUIUpdate extends AsyncTask<String,Integer,String> {
        String action;

        /**
         * TODO: onPreExecute carica la libreria dal database sqlite
         * TODO: doInBackground fa il fetch dei dati più aggiornati dal Pi e li inserisce nel db (bloccante)
         * TODO: onProgressUpdate potrei avvisare l'utente che i dati potrebbero essere incompleti
         * TODO: onPostExecute aggiorna l'itemAdapter con gli ultimi dati su sqlite
         */
/*
        @Override
        protected void onPreExecute(){
            boolean succ = false;
            while (!succ){
                succ = contentPi.getTrackList(songs);
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d("MPRADIO", "SONGS:" + songs);
        }
*/
        @Override
        protected String doInBackground(String... strings) {
            //return mpradioBTHelper.fetch(strings[0]);
            action = strings[0];
            String result = mpradioBTHelper.sendMessageGetReply(strings[0]);
            return result;
        }

        protected void onProgressUpdate(Integer... progress) {}

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(action.equals("song_name")) {
                // result = (result.substring(0,result.indexOf("\n")-1)).substring(result.indexOf("=")+2);
                ((TextView) view.findViewById(R.id.lblNow_playing)).setText(result);
            }else if(action.equals("library")){
                createTrackList(result, songs);
                itemAdapter.notifyDataSetChanged();
            }
        }
    }


    public void createTrackList(String content, ArrayList<Song> songs) {
        songs.clear();                      //CLEAR instead of adding duplicates
        try {
                Log.d("MPRADIO", "received library:"+ content);
                JSONArray jsonarray = new JSONArray(content);
                JSONObject jsonobject;
                for (int i = 0; i < jsonarray.length(); i++) {
                    jsonobject = jsonarray.getJSONObject(i);

                    String title = jsonobject.getString("title");
                    String artist = jsonobject.getString("artist");
                    String album = jsonobject.getString("album");
                    String year = jsonobject.getString("year");
                    String path = jsonobject.getString("path");

                    insertSong(title, artist, album, path, year, getContext());

                    songs.add(new Song(title, artist, album, year, path));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
    }

    /** Creates the main click listener for this Fragment */
    private void makeMainClickListener() {
        mainClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStop:
                    stop();
                    //new AsyncURLDownload().execute("https://github.com/morrolinux/mpradio/archive/master.zip","/Download/mpradio-master.zip");
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

    @Override
    public void onResume(){
        super.onResume();
        new AsyncUIUpdate().execute("song_name");
        new AsyncUIUpdate().execute("library");
    }


    // TODO: MOVE IT WHERE IT'S SUPPOSED TO BE..
    private static void insertSong(String title, String artist, String album, String path, String year, Context context) {

        ContentResolver resolver = context.getContentResolver();
        ContentProviderClient client = resolver.acquireContentProviderClient(ContentPi.CONTENT_URI);
        ContentPi contentPi = (ContentPi) client.getLocalContentProvider();

        ContentValues values = new ContentValues();
        values.put(ContentPi.SONG_TITLE, title);
        values.put(ContentPi.SONG_ARTIST, artist);
        values.put(ContentPi.SONG_ALBUM, album);
        values.put(ContentPi.SONG_PATH, path);
        values.put(ContentPi.SONG_YEAR, year);
        context.getContentResolver().insert(ContentPi.CONTENT_URI, values);

        // contentPi.debugContent();

        ArrayList<Song> songs = new ArrayList<Song>();
        contentPi.getTrackList(songs);
        Log.d("MPRADIO", "DB SONGS: " + songs.size());

        client.release();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        /* Inflate the desired layout first */
        view = inflater.inflate(R.layout.actions_fragment, container, false);

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
        rvLibrary = view.findViewById(R.id.rvLibrary);
        // Initialize items
        songs = Song.createTrackList(0);
        // Create adapter passing in the sample user data
        itemAdapter = new ItemAdapter(this.getContext(), songs,this);
        // Attach the adapter to the recyclerview to populate items
        rvLibrary.setAdapter(itemAdapter);
        // Set layout manager to position the items
        rvLibrary.setLayoutManager(new LinearLayoutManager(this.getContext()));
        // add line separator between recycler view items
        rvLibrary.addItemDecoration(new DividerItemDecoration(rvLibrary.getContext(), 1));
        // add swype and drag gestures
        SwipeAndDragHelper swipeAndDragHelper = new SwipeAndDragHelper(itemAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(swipeAndDragHelper);
        touchHelper.attachToRecyclerView(rvLibrary);
        /* Return the inflated view to the activity that called it */
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main4, menu);
        super.onCreateOptionsMenu(menu,inflater);

        /** SEARCH BUTTON Configuration */
        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) this.getContext().getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(this.getActivity().getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        // listening to search query text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // filter recycler view when query submitted
                itemAdapter.getFilter().filter(query);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String query) {
                // filter recycler view when text is changed
                itemAdapter.getFilter().filter(query);
                return false;
            }
        });
    }

    private void skip(){
        mpradioBTHelper.sendMessage("next");
        try {
            sleep(2000);
            new AsyncUIUpdate().execute("song_name");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void stop(){
        mpradioBTHelper.sendMessage("pause");
    }
    private void start(){
        mpradioBTHelper.sendMessage("resume");
    }
    private void restart(){
        mpradioBTHelper.sendMessage("system systemctl restart mpradio");
    }
    private void shutdown(){
        giveFeedback("Hang on...");
        mpradioBTHelper.sendMessage("system poweroff");
    }
    private void reboot(){
        giveFeedback("Hang on...");
        mpradioBTHelper.sendMessage("system reboot");
    }

    private void seekBackwards(){
        mpradioBTHelper.sendMessage("SEEK -10");
    }
    private void seekForward(){
        mpradioBTHelper.sendMessage("SEEK +10");
    }

    private void giveFeedback(String message){
        Log.d("MPRADIO", "Feedback: "+message);
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }


    /** SELECTED ITEM ACTION (from ItemAdapterListener) */
    @Override
    public void onItemSelected(Song song) {
        Toast.makeText(this.getContext().getApplicationContext(), "Selected: " + song.getItemPath(), Toast.LENGTH_LONG).show();

        Log.d("MPRADIO", "SELECTED: "+ song.getTitle()+ " PATH: " + song.getItemPath() +" NAME: "+ song.getArtist());


        if(song.getItemPath().equals("/..")) {
            reloadRemotePlaylist("/pirateradio");
            return;
        }
        Log.d("MPRADIO", "play: "+ song.getJson());
        mpradioBTHelper.sendMessage("play", song.getJson());
        try {
            sleep(2000);
            Log.d("MPRADIO", "updating song name...");
            new AsyncUIUpdate().execute("song_name");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onItemSwiped(Song song) {
        Toast.makeText(this.getContext().getApplicationContext(), "Selected folder: " +
                song.getTitle(), Toast.LENGTH_LONG).show();

        Log.d("MPRADIO", "SWIPED: FOLDER: "+ song.getTitle()+ " PATH: " + song.getItemPath() +" NAME: "+ song.getArtist());

        if(song.getItemPath().equals("/..")) {
            reloadRemotePlaylist("/pirateradio");
            return;
        }

        reloadRemotePlaylist(song.getTitle());
    }


    private void reloadRemotePlaylist(String path){
        //mpradioBTHelper.sendMessage("system rm /pirateradio/playlist ; rm /pirateradio/ps ; systemctl restart mpradio");  //LEGACY
        try {
            mpradioBTHelper.sendMessage("SCAN "+path);
            sleep(2000);
            new AsyncUIUpdate().execute("song_name");
            new AsyncUIUpdate().execute("library");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
