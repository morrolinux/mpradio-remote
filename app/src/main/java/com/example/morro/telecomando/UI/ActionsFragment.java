package com.example.morro.telecomando.UI;

import android.app.SearchManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.morro.telecomando.Core.Item;
import com.example.morro.telecomando.Core.MpradioBTHelper;
import com.example.morro.telecomando.R;

import java.util.ArrayList;

public class ActionsFragment extends Fragment implements ItemAdapter.ItemAdapterListener {
    private View view = null;
    ArrayList<Item> items;
    ItemAdapter itemAdapter;
    private MpradioBTHelper mpradioBTHelper;
    private View.OnClickListener mainClickListener;
    RecyclerView rvLibrary;
    SearchView searchView;

    private class AsyncUIUpdate extends AsyncTask<String,Integer,String> {
        String action;
        @Override
        protected String doInBackground(String... strings) {
            //return mpradioBTHelper.fetch(strings[0]);
            action = strings[0];
            return mpradioBTHelper.sendMessageGetReply(strings[0]);
        }

        protected void onProgressUpdate(Integer... progress) {}

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(action.equals("now_playing")) {
                result = (result.substring(0,result.indexOf("\n")-1)).substring(result.indexOf("=")+2);
                ((TextView) view.findViewById(R.id.lblNow_playing)).setText(result);
            }else if(action.equals("playlist")){
                createTrackList(result,items);
                itemAdapter.notifyDataSetChanged();
            }
        }
    }


    public static void createTrackList(String content,ArrayList<Item> items) {
        String[] lines = content.split(System.getProperty("line.separator"));
        int nLines = lines.length;
        items.clear();                      //CLEAR instead of adding duplicates
        items.add(new Item("/.."));
        for(int i=0; i < nLines ; i++){
            System.out.println("Adding "+lines[i]);
            items.add(new Item(lines[i]));
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
        new AsyncUIUpdate().execute("now_playing");
        new AsyncUIUpdate().execute("playlist");
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
        items = Item.createTrackList(0);
        // Create adapter passing in the sample user data
        itemAdapter = new ItemAdapter(this.getContext(), items,this);
        // Attach the adapter to the recyclerview to populate items
        rvLibrary.setAdapter(itemAdapter);
        // Set layout manager to position the items
        rvLibrary.setLayoutManager(new LinearLayoutManager(this.getContext()));
        // add swype and drag gestures
        SwipeAndDragHelper swipeAndDragHelper = new SwipeAndDragHelper(itemAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(swipeAndDragHelper);
        touchHelper.attachToRecyclerView(rvLibrary);
        /* Return the inflated view to the activity who called it */
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
        mpradioBTHelper.sendMessage("SKIP");
        try {
            Thread.sleep(2000);
            new AsyncUIUpdate().execute("now_playing");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void stop(){
        mpradioBTHelper.sendMessage("system systemctl stop mpradio");
    }
    private void start(){
        mpradioBTHelper.sendMessage("system systemctl start mpradio");
    }
    private void restart(){
        mpradioBTHelper.sendMessage("system systemctl restart mpradio");
    }
    private void shutdown(){
        giveFeedback("Hang on...");
        mpradioBTHelper.sendMessage("system systemctl stop mpradio && shutdown -h now");
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
        System.out.println("Feedback: "+message);
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }


    /** SELECTED ITEM ACTION (from ItemAdapterListener) */
    @Override
    public void onItemSelected(Item item) {
        Toast.makeText(this.getContext().getApplicationContext(), "Selected: " + item.getItemPath(), Toast.LENGTH_LONG).show();

        System.out.println("SELECTED: FOLDER: "+ item.getItemFolder()+ " PATH: " + item.getItemPath() +" NAME: "+ item.getItemName());


        if(item.getItemPath().equals("/..")) {
            reloadRemotePlaylist("/pirateradio");
            return;
        }

        mpradioBTHelper.sendMessage("PLAY "+item.getItemPath());
        try {
            Thread.sleep(1500);
            new AsyncUIUpdate().execute("now_playing");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onItemSwiped(Item item) {
        Toast.makeText(this.getContext().getApplicationContext(), "Selected folder: " +
                item.getItemFolder(), Toast.LENGTH_LONG).show();

        System.out.println("SWIPED: FOLDER: "+ item.getItemFolder()+ " PATH: " + item.getItemPath() +" NAME: "+ item.getItemName());

        if(item.getItemPath().equals("/..")) {
            reloadRemotePlaylist("/pirateradio");
            return;
        }

        reloadRemotePlaylist(item.getItemFolder());
    }


    private void reloadRemotePlaylist(String path){
        //mpradioBTHelper.sendMessage("system rm /pirateradio/playlist ; rm /pirateradio/ps ; systemctl restart mpradio");  //LEGACY
        try {
            mpradioBTHelper.sendMessage("SCAN "+path);
            Thread.sleep(2000);
            new AsyncUIUpdate().execute("now_playing");
            new AsyncUIUpdate().execute("playlist");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
