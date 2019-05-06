package com.example.morro.telecomando.UI;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.example.morro.telecomando.Core.Item;
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

    private List<Item> itemListFiltered;
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
                    listener.onItemSelected(itemListFiltered.get(getAdapterPosition()));
                }
            });
        }
    }

    /**
     * Now we need to begin filling in our adapter
     */

    // Store a member variable for the items
    private List<Item> itemList;
    // Store the context for easy access
    private Context mContext;

    // Pass in the contact array into the constructor
    public ItemAdapter(Context context, List<Item> items, ItemAdapterListener listener) {
        itemList = items;
        this.listener = listener;
        mContext = context;
        this.itemListFiltered = itemList;
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
        Item item = itemListFiltered.get(position);      //we always use ListFiltered due to search implementation

        // Set item views based on your views and data model
        viewHolder.titleTextView.setText(item.getTitle());
        viewHolder.artistTextView.setText(item.getArtist());
        viewHolder.albumTextView.setText(item.getAlbum());
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return itemListFiltered.size();
    }


    /** handle moving and swiping gestures on the view (Perform actions) */
    @Override
    public void onViewMoved(int oldPosition, int newPosition) {
        Item item = itemListFiltered.get(oldPosition);
        itemListFiltered.remove(oldPosition);
        itemListFiltered.add(newPosition, item);
        notifyItemMoved(oldPosition, newPosition);
    }

    @Override
    public void onViewSwiped(int position) {
        listener.onItemSwiped(itemListFiltered.get(position));
        itemListFiltered.remove(position);
        notifyItemRemoved(position);
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
                    itemListFiltered = itemList;
                } else {
                    List<Item> filteredList = new ArrayList<>();    // collect all results a List
                    for (Item row : itemList) {
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
                    itemListFiltered = filteredList;
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = itemListFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                itemListFiltered = (ArrayList<Item>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    public interface ItemAdapterListener {
        void onItemSelected(Item item);
        void onItemSwiped(Item item);
    }

}