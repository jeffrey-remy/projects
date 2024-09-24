package com.hb216.recipewizard;

import org.json.JSONException;

/**
 * An interface for establishing the clicking function for the recycler views
 */
public interface recyclerInterface_MS {

    /**
     * Does something (usually goes to viewRecipe page) when the given item in a recycler view is clicked
     * @param position position in the dataset of the item clicked
     * @param whatDataset a number representing in which dataset an item was clicked
     * @throws JSONException throws exception if unable to get specific parameter from given object
     */
    void onItemClick(int position, int whatDataset) throws JSONException;
}


