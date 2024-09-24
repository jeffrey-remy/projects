package com.hb216.recipewizard;

import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RecipeCardAdapter is an adapter for recipes to be stored within collections. These recipe lists
 * can be dynamically updated to account for new recipes being added into the collection.
 *
 * @author Jeffrey Remy
 */
public class RecipeCardAdapter extends RecyclerView.Adapter<RecipeCardAdapter.RecipeViewHolder> {

    /** A list of recipes by titles. */
    private List<String> recipeList;

    /**
     * Initialize RecipeCardAdapter with a list of recipe titles, recipeList
     * @param recipeList list of recipe titles
     */
    RecipeCardAdapter(List<String> recipeList) {
        this.recipeList = recipeList;
    }

    /**
     * An interface that can be implemented in the profile activity so that we can send info to higher classes.
     */
    public interface OnItemClickListener {
        void onItemClick(int recipePosition);
    }

    /** A listener for clicks on individual recipe elements in the RecipeCardAdapter.*/
    private OnItemClickListener listener;

    /**
     * Sets the OnItemClickListener of this recipe adapter, so that clicks on individual recipes can be detected.
     * @param listener the OnItemClickListener that carries out actions in response to an item being clicked
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Acts as the main display/UI element of individual recipes, displaying appropriate info about
     * each recipe and providing a way to click on individual recipes in the adapter.
     */
    class RecipeViewHolder extends RecyclerView.ViewHolder {
        // customizable recipe title
        TextView recipeItemTitle;

        // RecipeViewHolder constructor, initializing the recipe title and a listener for clicking on recipes
        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeItemTitle = itemView.findViewById(R.id.etRecipeTitleInCollection);

            // listener for when the recipe is clicked
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // send click to be received by interface
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            // call onItemClick in our interface, so that a parent adapter can receive
                            // the click in its own scope
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }

    /**
     * Creates a new RecipeViewHolder and inflates the layout with this new RecipeViewHolder element.
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     *               an adapter position.
     * @param viewType The view type of the new View.
     *
     * @return the new RecipeViewHolder that will display a recipe
     */
    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // inflate layout with recipe UI element
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.collection_item, parent, false);

        return new RecipeViewHolder(view);
    }

    /**
     * Binds a RecipeViewHolder to a given recipe in the list via a given position.
     * In other words, this sets a display element for a certain recipe.
     * @param holder The ViewHolder which should be updated to represent the contents of the
     *        item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull RecipeCardAdapter.RecipeViewHolder holder, int position) {
        // get corresponding recipe based on position in list
        String recipeName = recipeList.get(position);

        // set TextView
        TextView textView = holder.recipeItemTitle;
        textView.setText(recipeName);
    }

    /**
     * Gets the amount of recipes stored in the recipe list.
     * @return number of recipes in recipeList
     */
    @Override
    public int getItemCount() {
        return recipeList.size();
    }
}
