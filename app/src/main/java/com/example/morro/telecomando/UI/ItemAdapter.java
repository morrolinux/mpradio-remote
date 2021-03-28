package com.example.morro.telecomando.UI;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.example.morro.telecomando.Core.Song;
import com.example.morro.telecomando.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by morro on 15/02/18.
 */

// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
// An adapter is needed to actually populate the data into the RecyclerView

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder>
        implements SwipeAndDragHelper.ActionCompletionContract, Filterable {

    private List<Song> songListFiltered;
    private ItemAdapterListener listener;

    private boolean searchByName = true;
    private boolean searchByBrand = true;
    private boolean searchByCategory = true;
    private boolean searchByID = true;

    public void setSearchByName(boolean searchByName) {
        this.searchByName = searchByName;
    }

    public void setSearchByBrand(boolean searchByBrand) {
        this.searchByBrand = searchByBrand;
    }

    public void setSearchByCategory(boolean searchByCategory) {
        this.searchByCategory = searchByCategory;
    }

    public void setSearchByID(boolean searchByID) {
        this.searchByID = searchByID;
    }

    // ViewHolder class: provides a direct reference to each of the views within a data item
    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private TextView artistTextView;
        private TextView albumTextView;

        // constructor that accepts an entire item row
        public ViewHolder(View itemView) {
            super(itemView);

            titleTextView = (TextView) itemView.findViewById(R.id.title);
            artistTextView = (TextView) itemView.findViewById(R.id.artist);
            albumTextView = (TextView) itemView.findViewById(R.id.album);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // send the selected item in callback
                    listener.onItemSelected(songListFiltered.get(getAdapterPosition()));
                }
            });
        }
    }

    /**
     * Now we need to begin filling in our adapter
     */

    // Store a member variable for the items
    private List<Song> songList;
    // Store the context for easy access
    private Context mContext;

    // Pass in the contact array into the constructor
    public ItemAdapter(Context context, List<Song> songs, ItemAdapterListener listener) {
        songList = songs;
        this.listener = listener;
        mContext = context;
        this.songListFiltered = songList;
    }

    // Easy access to the context object in the recyclerview
    private Context getContext() {
        return mContext;
    }

    /**
     * implement the adapter methods
     */

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public ItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.item_row, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(contactView);
        return viewHolder;
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(ItemAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        Song song = songListFiltered.get(position);      //we always use ListFiltered due to search implementation

        // Set item views based on your views and data model
        viewHolder.titleTextView.setText(song.getTitle());
        viewHolder.artistTextView.setText(song.getArtist());
        viewHolder.albumTextView.setText(song.getAlbum());
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return songListFiltered.size();
    }


    /** handle moving and swiping gestures on the view (Perform actions) */
    @Override
    public void onViewMoved(int oldPosition, int newPosition) {
        Song song = songListFiltered.get(oldPosition);
        songListFiltered.remove(oldPosition);
        songListFiltered.add(newPosition, song);
        notifyItemMoved(oldPosition, newPosition);
    }

    @Override
    public void onViewSwiped(int position, int direction) {
        listener.onItemSwiped(songListFiltered.get(position), direction);
        // songListFiltered.remove(position);
        // notifyItemRemoved(position);
    }

    /**
     * implement Filterable's Filter method
     */

    /** provides a Filter object for filtering strings on the elements*/
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    songListFiltered = songList;
                } else {
                    List<Song> filteredList = new ArrayList<>();    // collect all results a List
                    for (Song row : songList) {
                        if ( searchByName ) {
                            if (row.getItemPath().toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(row);
                                continue;           //no need to add a thing multiple times
                            }
                            if (row.getTitle().toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(row);
                                continue;           //no need to add a thing multiple times
                            }
                            if (row.getAlbum().toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(row);
                                continue;           //no need to add a thing multiple times
                            }
                            if (row.getArtist().toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(row);
                                continue;           //no need to add a thing multiple times
                            }
                        }
                    }
                    songListFiltered = filteredList;
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = songListFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                songListFiltered = (ArrayList<Song>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    public interface ItemAdapterListener {
        void onItemSelected(Song song);
        void onItemSwiped(Song song, int direction);
    }

}