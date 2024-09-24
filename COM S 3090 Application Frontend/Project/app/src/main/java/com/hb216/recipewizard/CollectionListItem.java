package com.hb216.recipewizard;

import java.util.List;

/**
 * CollectionListItem is a class to represent collections in a simple way, practical for displaying info.
 * CollectionListItem stores the title of the collection and the list of recipe titles contained in the collection.
 *
 * @author Jeffrey Remy
 */
public class CollectionListItem {

    /** The name of the collection. */
    private String collectionTitle;

    /** A list of recipe titles stored within the collection. */
    private List<String> collectionList;

    /**
     * The constructor for CollectionListItem that initializes the title and list of recipes
     * @param title the title of the collection
     * @param list a list of recipes to be stored in the collection
     */
    public CollectionListItem(String title, List<String> list) {
        collectionTitle = title;
        collectionList = list;
    }

    /**
     * Returns the name of a CollectionListItem as a String.
     * @return the name of the collection
     */
    public String getCollectionTitle() {
        return collectionTitle;
    }

    /**
     * Sets the name of a CollectionListItem with a given String.
     * @param title the new title of the collection
     */
    public void setCollectionTitle(String title) {
        collectionTitle = title;
    }

    /**
     * Returns the recipe names stored in CollectionListItem as a String list
     * @return the list of recipes of a collection
     */
    public List<String> getCollectionList() {
        return collectionList;
    }

    /**
     * Sets the recipe names in CollectionListItem to a new list of names
     * @param list the new list of recipe titles
     */
    public void setCollectionList(List<String> list) {
        collectionList = list;
    }
}
