package com.hb216.recipewizard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CollectionListAdapter acts as an adapter to dynamically display a user's collections, including the name
 * and contained recipes within each collection. It stores a list of CollectionListItem objects that can be filtered
 * via the custom getFilter() class.
 *
 * @author Jeffrey Remy
 */
public class CollectionListAdapter extends RecyclerView.Adapter<CollectionListAdapter.CollectionListViewHolder> implements Filterable {

    /** Allows for sharing between recipe and collection views. */
    private RecyclerView.RecycledViewPool viewPool = new RecyclerView.RecycledViewPool();

    /** List of collections initialized at constructor */
    private List<CollectionListItem> initialCollectionList;

    /** List of collections after applying filters (for searching) */
    private List<CollectionListItem> filteredCollectionList;

    /** Recipe adapter that is nested within our collection adapter. */
    RecipeCardAdapter recipeAdapter;

    /**
     * Constructor for CollectionListAdapter that initializes the initial and filtered collection lists.
     * @param list a list of CollectionListItem
     */
    CollectionListAdapter(List<CollectionListItem> list) {
        initialCollectionList = list;
        filteredCollectionList = list;
    }

    /**
     * Returns a filter that we can use for searching through collections.
     * @return a Filter that can be used to sort through the CollectionListAdapter and display certain collections
     */
    @Override
    public Filter getFilter() {
        Filter collectionFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                FilterResults filterResults = new FilterResults();

                // get filter (query) that was passed-in
                String filter = charSequence.toString().toLowerCase();

                // filter based on the initial list that has  all collections
                List<CollectionListItem> initialList = initialCollectionList;

                ArrayList<CollectionListItem> filterList = new ArrayList<CollectionListItem>();

                // only apply filter if it is empty
                if (!filter.isEmpty()) {
                    // check if any CollectionListItems in the adapter match
                    for (CollectionListItem c : initialList) {
                        if (c.getCollectionTitle().toLowerCase().contains(filter)) {
                            filterList.add(c);
                        }
                    }
                }
                // otherwise, display all collections
                else {
                    filterList = (ArrayList<CollectionListItem>) initialCollectionList;
                }

                // return the results of our filtering
                filterResults.values = filterList;
                filterResults.count = filterList.size();
                return filterResults;
            }

            // assign our filter results to the current filtered list
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                filteredCollectionList = (ArrayList<CollectionListItem>) filterResults.values;
                notifyDataSetChanged();
            }
        };

        return collectionFilter;
    }

    /**
     * An interface that can be implemented in the profile activity so that item clicks in ViewHolder
     * are received in ProfileActivity.
     */
    public interface OnItemClickListener {
        // send info about what the user clicked on, the collection they clicked on, and the recipe they might
        // have clicked on
        void onItemClick(View itemView, boolean clickedDelete, int collectionPosition, int recipePosition);
    }

    /** A listener for clicks on individual collections in the CollectionListAdapter.*/
    private OnItemClickListener listener;

    /**
     * Sets the OnItemClickListener for the CollectionListAdapter, allowing for individual collections
     * to be clicked on.
     * @param listener the OnItemClickListener that carries out actions in response to an item being clicked
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * The CollectIonListViewHolder acts as the main display/UI for the collection adapter, displaying
     * collections dynamically with the appropriate UI elements. Included in its constructor are listeners
     * for clicking on the delete or add recipe buttons of each collection element.
     */
    class CollectionListViewHolder extends RecyclerView.ViewHolder {
        private TextView collectionTitle;
        private RecyclerView recipeRecyclerView;

        public Button btnDelete;
        public Button btnAddRecipe;

        public CollectionListViewHolder(@NonNull View itemView) {
            super(itemView);

            // initialize UI elements of the collection
            collectionTitle = itemView.findViewById(R.id.tvCollectionTitle);
            recipeRecyclerView = itemView.findViewById(R.id.rvRecipesInCollection);
            btnDelete = itemView.findViewById(R.id.btnDeleteCollectionProfile);
            btnAddRecipe = itemView.findViewById(R.id.btnAddRecipeToCollection);

            // delete collection listener
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // check which collection the user clicked on, accounting for possible filtering by checking og list
                    int collectionPosition = initialCollectionList.indexOf(filteredCollectionList.get(getAdapterPosition()));

                    if (listener != null && collectionPosition != RecyclerView.NO_POSITION) {
                        // indicate to ProfileActivity that the delete button was clicked, and where it was clicked
                        listener.onItemClick(itemView, true, collectionPosition, -1);
                    }
                }
            });

            // add recipe to collection listener
            btnAddRecipe.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // check which collection the user clicked on, accounting for possible filtering by checking og list
                    int collectionPosition = initialCollectionList.indexOf(filteredCollectionList.get(getAdapterPosition()));
                    if (listener != null && collectionPosition != RecyclerView.NO_POSITION) {
                        // indicate to ProfileActivity that the delete button was *not* clicked, and where it was clicked
                        listener.onItemClick(itemView, false, collectionPosition, -1);
                    }
                }
            });
        }
    }

    /**
     * Creates a new CollectionListViewHolder and inflates the layout with a new collection.
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     *               an adapter position.
     * @param viewType The view type of the new View.
     *
     * @return the new ViewHolder that will display a collection on the profile page
     */
    @NonNull
    @Override
    public CollectionListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // inflate layout with UI element that represents a collection
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.collection_listitem, parent, false);
        return new CollectionListViewHolder(view);
    }

    /**
     * Assigns a CollectionListViewHolder to a certain collection in the adapter based on a given position
     * in the list of collections.
     * @param holder The ViewHolder which should be updated to represent the contents of the
     *        item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull CollectionListViewHolder holder, int position) {
        // set title of collection in TextView
        CollectionListItem collectionItem = filteredCollectionList.get(position);
        holder.collectionTitle.setText(collectionItem.getCollectionTitle());

        // get a layout manager for the nested recipe adapter within the collection
        LinearLayoutManager layoutManager = new LinearLayoutManager(holder.recipeRecyclerView.getContext(),
                LinearLayoutManager.HORIZONTAL, false);

        layoutManager.setInitialPrefetchItemCount(collectionItem.getCollectionList().size());

        // create a new adapter for collection's respective recipe list
        recipeAdapter = new RecipeCardAdapter(collectionItem.getCollectionList());

        // setting RecyclerView for collection's respective recipe list
        holder.recipeRecyclerView.setLayoutManager(layoutManager);
        holder.recipeRecyclerView.setAdapter(recipeAdapter);
        holder.recipeRecyclerView.setRecycledViewPool(viewPool);

        // listener for when a recipe is clicked
        recipeAdapter.setOnItemClickListener(new RecipeCardAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int recipePosition) {
                // check within which collection the user clicked on, accounting for possible filtering by checking og list
                int collectionPosition = initialCollectionList.indexOf(filteredCollectionList.get(holder.getAdapterPosition()));
                if (listener != null && collectionPosition != RecyclerView.NO_POSITION) {
                    // indicate position of collection and recipe that was clicked
                    listener.onItemClick(holder.itemView, false, collectionPosition, recipePosition);
                }
            }
        });
    }

    /**
     * Returns the number of collections in the filtered collection list aka the currently displayed list.
     * @return number of items in the currently filtered collection list
     */
    @Override
    public int getItemCount() {
        if (filteredCollectionList != null) {
            return filteredCollectionList.size();
        }
        else {
            return 0;
        }
    }
}