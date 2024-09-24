package com.hb216.recipewizard;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Displays a profile page complete with the user's favorite recipes and custom-made collections.
 * The collections utilize a nested RecyclerView of dynamically updated collections, each with dynamically
 * updated recipes contained within.
 *
 * @author Jeffrey Remy
 */
public class ProfileActivity extends AppCompatActivity {

    /** A button for navigating to other pages. */
    Button btnLogout, btnBack, btnPantryPage;

    /** Prompts the user to create a collection when clicked on. */
    Button btnCreateCollection;

    /** Exits out of the recipe adding menu. */
    Button btnCancelRecipe;

    /** A button to reach the admin page, only displayed if the user is an admin. */
    Button btnAdminPage;

    /** Allows user to go to the add ingredient page. */
    FloatingActionButton faAddIngredient;

    /** Displays the user's username. */
    TextView tvProfileName;

    /** Notifies the user about what collection they are adding a recipe to. */
    TextView tvRecipeSearchPrompt;

    /** The RecyclerView used for displaying a user's collections dynamically. */
    RecyclerView rvCollectionList;

    /** A list of all recipes in the database that can be searched through. */
    ListView lvRecipeList;

    /** A search bar for either searching collections or recipes. */
    SearchView svSearchBar;

    /** The current user's username. */
    String username;

    /** The url of the server used for backend requests. */
    private String baseUrl = "http://coms-309-006.class.las.iastate.edu:8080";
    //private String baseUrl = "http://10.0.2.2:8080";

    /** A url associated with a certain Volley request. */
    private String getCollectionsUrl, deleteCollectionUrl, postCollectionUrl, getRecipesUrl, putCollectionUrl,
            getFavoritesUrl, putFavoriteUrl, getAdminUrl, getMemberUrl, getThemeUrl;

    /** A list of collection items that can be used for the collectionAdapter. */
    List<CollectionListItem> collectionAdapterList = new ArrayList<CollectionListItem>();

    /** The adapter that allows for dynamic displaying of the user's collections. */
    CollectionListAdapter collectionAdapter;

    /** JSON list of collections associated with user; used for more detailed info when necessary. */
    List<JSONObject> collectionObjectList = new ArrayList<JSONObject>();

    /** The index of the collection to change based on user input (delete, add recipe, etc.) */
    int collectionChangeIndex;

    /** The name of the collection that is currently being updated. */
    String collectionToChangeName;

    /** A list of titles of all recipes, used for searching. */
    List<String> recipeSearchList = new ArrayList<String>();

    /** An adapter used to display all recipe titles dynamically. */
    ArrayAdapter<String> recipeSearchAdapter;

    /** A list of the user's favorite recipes, by titles. */
    List<String> favoriteRecipeTitles = new ArrayList<String>();

    /** Flag for searching collections or recipes, true by default. */
    boolean searchForCollections = true;

    /** Flag to track if user is admin, false by default. */
    boolean userIsAdmin = false;

    RatingBar rbProfileRating;
    FloatingActionButton faChatPage;

    /** Initializes the display of the profile page with all appropriate elements.
     *  Retrieves info about the user like their favorite recipes and custom collections, and checks
     *  if they are an admin. Collections are displayed via a RecyclerView. Click listeners
     *  are set for various buttons.
     * @param savedInstanceState a saved instance of this page that can be re-created
     *  */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // get locally stored username
        SharedPreferences sharedPref = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        username = sharedPref.getString("username", "TestingAdmin");

        // set urls
        getCollectionsUrl =     baseUrl + "/collections/"   + username;
        deleteCollectionUrl =   baseUrl + "/collections/"   + username + "/";
        postCollectionUrl =     baseUrl + "/collections";
        getRecipesUrl =         baseUrl + "/recipes";
        putCollectionUrl =      baseUrl + "/collections/"   + username + "/";
        getFavoritesUrl =       baseUrl + "/favorites/"     + username;
        putFavoriteUrl =        baseUrl + "/favorites/add/" + username + "/";
        getAdminUrl =           baseUrl + "/admins/";
        getMemberUrl =          baseUrl + "/member/"        + username;
        getThemeUrl =           baseUrl + "/Theme";

        // default elements for profile page
        btnLogout = findViewById(R.id.btnLogOut);
        btnBack = findViewById(R.id.btnBack);
        faAddIngredient = findViewById(R.id.faAddIngredientBtn);
        tvProfileName = findViewById(R.id.tvProfileNameTitle);
        rvCollectionList = findViewById(R.id.rvProfileCollectionList);
        btnCreateCollection = findViewById(R.id.btnCreateCollection);
        btnPantryPage = findViewById(R.id.btnPantryPage);
        svSearchBar = findViewById(R.id.svSearchInProfile);
        rbProfileRating = findViewById(R.id.rbProfileRating);
        faChatPage = findViewById(R.id.faProfileToChat);

        // elements that only display when adding recipes to collections
        btnCancelRecipe = findViewById(R.id.btnCancelRecipeProfile);
        tvRecipeSearchPrompt = findViewById(R.id.tvRecipeSearchProfile);
        lvRecipeList = findViewById(R.id.recipeSearchListProfile);

        // button that allows admins, and only admins, to access admin page
        btnAdminPage = findViewById(R.id.btnProfileToAdmin);

        // initialize layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(ProfileActivity.this);

        // initialize collection adapter
        collectionAdapter = new CollectionListAdapter(collectionAdapterList);

        // set adapter and layout manager for RecyclerView
        rvCollectionList.setAdapter(collectionAdapter);
        rvCollectionList.setLayoutManager(layoutManager);

        // get favorite recipes and add them to the start of the adapter
        getFavoriteRecipes(getFavoritesUrl);

        // check if user is admin, then display admin button if so
        isUserAdmin(getAdminUrl, username);

        // set click listener for when user clicks on a collection
        collectionAdapter.setOnItemClickListener(new CollectionListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, boolean clickedDelete, int collectionPosition, int recipePosition) {
                // check if user clicked on a recipe within a collection, then
                // take user to ViewRecipe page associated with recipe name they clicked on
                // collectionChangeIndex = collectionPosition;
                if (recipePosition != -1) {
                    String recipeName = "";
                    int recipeId = 0;
                    try {
                        // get recipe name and id based on where user clicked
                        JSONArray recipeList = (JSONArray) collectionObjectList.get(collectionPosition).get("recipes");
                        JSONObject recipeObject = (JSONObject) recipeList.get(recipePosition);
                        recipeId = (int) recipeObject.get("id");
                    }
                    catch (JSONException e) {
                        Toast.makeText(ProfileActivity.this, "Error viewing recipe: " + e, Toast.LENGTH_LONG).show();
                    }
                    Intent intent = new Intent(ProfileActivity.this, ViewRecipeActivity.class);

                    // send recipe id info to ViewRecipeActivity so it can display the right recipe
                    intent.putExtra("id", recipeId);

                    // send last activity info to ViewRecipeActivity for back button functionality
                    intent.putExtra("lastActivity", "profile");

                    // send user to view recipe page for the recipe they clicked on
                    startActivity(intent);
                }
                // prompt user to delete specified collection
                else if (clickedDelete) {
                    // get the collection the user wants to delete
                    collectionChangeIndex = collectionPosition;

                    // user cannot delete Favorites (first and default collection)
                    if (collectionChangeIndex == 0) {
                        Toast.makeText(ProfileActivity.this, "Cannot delete Favorites!", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        // confirm if user wants to delete collection
                        deleteCollectionConfirmation();
                    }
                }
                // prompt user to add a recipe to the specified collection
                else {
                    // get collection that the user wants to add a recipe to
                    collectionChangeIndex = collectionPosition;

                    // enter recipe search/add display
                    toggleRecipeSearchDisplay(true);

                    // get all recipes in database
                    getAllRecipesRequest(getRecipesUrl);
                }
            }
        });

        // based on current username, update profile page
        tvProfileName.setText(username);

        // get theme and display theme colors over username
        displayTheme(getThemeUrl);

        // display member rating
        getAndSetRating(getMemberUrl);

        // get user's previously created collections and display them on profile page
        getCollectionRequest(getCollectionsUrl);

        searchForCollections();

        // create collection with POST request
        btnCreateCollection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // prompt user for title, then send POST request with that info
                createCollectionInputDialog();
            }
        });

        // go back to main/home page
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });

        // log out of user account, go back to login page
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // add ingredient to user's pantry
        faAddIngredient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, AddIngredientActivity.class);
                intent.putExtra("lastActivity", "profile");
                startActivity(intent);
            }
        });

        // go to chat page
        faChatPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, ChatActivity.class);
                startActivity(intent);
            }
        });

        // exit recipe search/add display
        btnCancelRecipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // toggle recipe search/add display off
                toggleRecipeSearchDisplay(false);
            }
        });

        btnAdminPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // go to admin page
                Intent intent = new Intent(ProfileActivity.this, AdminActivity.class);
                startActivity(intent);
            }
        });

        btnPantryPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, PantryActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Retrieves the theme via GET and, on response, display the theme colors on the profile name.
     * @param url the url associated with the GET theme request
     */
    private void displayTheme(String url) {
        RequestQueue queue = Volley.newRequestQueue(ProfileActivity.this);

        JSONObject body = null;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, body,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    String textString = "#000000";
                    String bgString =   "#ffffff";
                    // get colors from theme object
                    try {
                        textString = (String) response.get("primaryColor");
                        bgString = (String) response.get("secondaryColor");
                    } catch (JSONException e) {
                        Toast.makeText(ProfileActivity.this, "Error displaying theme: " + e, Toast.LENGTH_LONG).show();
                    }
                    // convert to color ints
                    int textColor = Color.parseColor(bgString);
                    int bgColor = Color.parseColor(textString);

                    // display theme colors on name
                    tvProfileName.setTextColor(textColor);
                    tvProfileName.setBackgroundColor(bgColor);
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(ProfileActivity.this, "Error getting theme: " + error, Toast.LENGTH_LONG).show();
                }
            });

        queue.add(request);
    }

    /**
     * Gets the Member object associated with this member profile, and specifically displays the rating
     * that is stored in the object.
     * @param url
     */
    private void getAndSetRating(String url) {
        RequestQueue queue = Volley.newRequestQueue(ProfileActivity.this);

        JSONObject body = null;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // get the rating stored in the Member object
                        int rating = 0;
                        try {
                            rating = (int) response.get("memberRating");
                        } catch (JSONException e) {
                            Toast.makeText(ProfileActivity.this, "Error getting rating: " + e, Toast.LENGTH_SHORT).show();
                        }

                        // set and display the rating on the page
                        rbProfileRating.setRating((float) rating);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ProfileActivity.this, "Error getting member info: " + error, Toast.LENGTH_LONG).show();
                    }
                });

        queue.add(request);
    }

    /**
     * Checks if a given username is associated with an admin via a GET request. On response,
     * the userIsAdmin flag is set to true and the button to reach the admin page is displayed.
     *
     * @param adminUrl The url associated with the GET admin request
     * @param adminName The username of the possible admin to verify
     */
    private void isUserAdmin(String adminUrl, String adminName) {
        RequestQueue queue = Volley.newRequestQueue(ProfileActivity.this);

        String url = adminUrl + adminName;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // check if server verified the given admin name
                        if (response.equals(adminName + " is an Admin")) {
                            userIsAdmin = true;
                            // display admin page button
                            btnAdminPage.setVisibility(View.VISIBLE);
                        }
                        else {
                            userIsAdmin = false;
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ProfileActivity.this, "Error checking admin: " + error, Toast.LENGTH_LONG).show();
                    }
                });

        queue.add(request);
    }

    /**
     * Gets a user's favorite recipes via a GET request.
     *
     * @param getFavoritesUrl the url associated with the GET favorites request
     */
    private void getFavoriteRecipes(String getFavoritesUrl) {
        RequestQueue queue = Volley.newRequestQueue(ProfileActivity.this);

        JSONArray body = null;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, getFavoritesUrl, body, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                // fakeFavorites is a dummy JSONObject that represents Favorites in the collectionObjectList
                // Favorite is a separate type from Collection, but we can pretend that it has the same
                // properties like a recipe list and collection title for convenience's sake
                JSONObject fakeFavorites = new JSONObject();
                // store recipe titles
                for (int i = 0; i < response.length(); i++) {
                    try {
                        // add recipe names to our dummy object
                        JSONObject recipe = (JSONObject) response.get(i);
                        String recipeTitle = (String) recipe.get("title");
                        favoriteRecipeTitles.add(recipeTitle);

                        fakeFavorites.put("recipes", response);
                        fakeFavorites.put("collectionName", "Favorites");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
                // add favorites to adapter as a CollectionListItem
                CollectionListItem favoriteCollection = new CollectionListItem("Favorites", favoriteRecipeTitles);
                collectionAdapterList.add(0, favoriteCollection); // favorites must always be first

                collectionObjectList.add(0, fakeFavorites); // favorite object inserted into first position

                collectionAdapter.notifyItemInserted(0);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(ProfileActivity.this, "Error getting favorites: " + error, Toast.LENGTH_LONG).show();
            }
        });

        queue.add(request);
    }

    /**
     * Appends a recipe to a certain collection of a certain user, specified in putCollectionUrl.
     * The name of the recipe is appended onto putCollectionUrl, which is then used for a PUT request.
     * On response, the recipe that was PUT is used to update the page's adapters to dynamically display
     * the new recipe in the collection it was added to.
     *
     * @param putCollectionUrl the url associated with the PUT collection request
     * @param recipeTitle the name of the recipe to add
     */
    private void putRecipeInCollection(String putCollectionUrl, String recipeTitle) {
        RequestQueue queue = Volley.newRequestQueue(ProfileActivity.this);

        String url;

        // change url based on whether this is a PUT for favorites or a user-created collection
        if (collectionChangeIndex == 0) {
            url = putCollectionUrl + recipeTitle;
        }
        else {
            // get specified collection name
            String collectionName = collectionAdapterList.get(collectionChangeIndex).getCollectionTitle();

            // construct url with specified recipe and collection
            url = putCollectionUrl + collectionName + "/" + recipeTitle;
        }

        JSONArray body = null;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.PUT, url, body,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Toast.makeText(ProfileActivity.this, "Recipe added to collection", Toast.LENGTH_SHORT).show();
                        // insert recipe into JSON list
                        try {
                            JSONObject newRecipe;
                            // different response for favorites and generic collections
                            if (collectionChangeIndex == 0) {
                                // response from favorites should be the recipe array in favorites
                                newRecipe = (JSONObject) response.get(response.length()-1); // get inserted recipe
                            }
                            else {
                                // generic collection case - append onto the end of the list
                                JSONObject collectionJSONObject = (JSONObject) response.get(collectionChangeIndex-1);
                                JSONArray recipeJSONArray = (JSONArray) collectionJSONObject.get("recipes");
                                newRecipe = (JSONObject) recipeJSONArray.get(recipeJSONArray.length() - 1);
                            }
                            collectionObjectList.get(collectionChangeIndex).put("recipes", newRecipe);
                        } catch (JSONException e) {
                            // System.out.println(e);
                            Toast.makeText(ProfileActivity.this, "Error adding recipe: " + e, Toast.LENGTH_LONG).show();
                        }
                        // modify recipe list of collection list and notify the adapter
                        List<String> newCollectionRecipeList = collectionAdapterList.get(collectionChangeIndex).getCollectionList();
                        newCollectionRecipeList.add(recipeTitle);
                        collectionAdapterList.get(collectionChangeIndex).setCollectionList(newCollectionRecipeList);

                        collectionAdapter.notifyItemChanged(collectionChangeIndex);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ProfileActivity.this, "Error adding recipe: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
        queue.add(request);
    }

    /**
     * Sets the profile page's search bar to take in input for collection searching.
     * Updating the searchbar with new user queries updates the filter of the collection adapter.
     *
     * @author Jeffrey Remy
     */
    private void searchForCollections() {
        // update adapter so that searching works properly (prevents errors after searching for recipes)
        svSearchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if (searchForCollections) {
                    // apply filter based on user input to display matching collections
                    collectionAdapter.getFilter().filter(s.toLowerCase());
                    collectionAdapter.notifyDataSetChanged();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (searchForCollections) {
                    // apply filter based on user input to display matching collections
                    collectionAdapter.getFilter().filter(s.toLowerCase());
                    collectionAdapter.notifyDataSetChanged();
                }
                return false;
            }
        });
    }

    /**
     * Sets the profile page's search bar to take in input for recipe searching.
     * Updating the searchbar with new user queries updates the filter of a list of recipes.
     */
    private void searchForRecipes() {
        // use adapter for list view so we can make search queries
        recipeSearchAdapter = new ArrayAdapter<String>(ProfileActivity.this,
                android.R.layout.simple_list_item_1, recipeSearchList);
        lvRecipeList.setAdapter(recipeSearchAdapter);

        // item click listener for when user clicks on a recipe
        lvRecipeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long l) {
                // get name of recipe that was clicked on
                String clickedRecipe = (String) parent.getAdapter().getItem(position);
                // make PUT request with clickedRecipe and currently selected collection
                // change depending on whether it is a generic collection or favorite
                if (collectionChangeIndex == 0) {
                    // add recipe to favorites
                    putRecipeInCollection(putFavoriteUrl, clickedRecipe);
                }
                else {
                    // add recipe to a regular collection
                    putRecipeInCollection(putCollectionUrl, clickedRecipe);
                }
                collectionAdapter.notifyDataSetChanged();
                recipeSearchAdapter.notifyDataSetChanged();
                // exit add recipe view once recipe is added
                toggleRecipeSearchDisplay(false);
            }
        });

        // search bar for searching through recipes
        svSearchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if (recipeSearchList.contains(s.toLowerCase())) {
                    // filter recipe list based on query
                    if (!searchForCollections) {
                        recipeSearchAdapter.getFilter().filter(s);
                        recipeSearchAdapter.notifyDataSetChanged();
                    }
                }
                else {
                    Toast.makeText(ProfileActivity.this, "Recipe not found", Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            // continuously change list view based on what the user has inputted
            @Override
            public boolean onQueryTextChange(String s) {
                if (!searchForCollections) {
                    recipeSearchAdapter.getFilter().filter(s);
                    recipeSearchAdapter.notifyDataSetChanged();
                }
                return false;
            }
        });
    }

    /**
     * Toggles the profile page's UI elements on and off based on if the user wants to search
     * for recipes or collections. Searching initializations are made based on the type of searching
     * and the relevant buttons/elements are made visible, while irrelevant elements are made invisible.
     *
     * @param recipeSearchMode true if we search for recipes, false if we are searching for collections
     */
    private void toggleRecipeSearchDisplay(boolean recipeSearchMode) {
        if (recipeSearchMode) {
            // show recipe searching elements
            btnCancelRecipe.setVisibility(View.VISIBLE);
            tvRecipeSearchPrompt.setVisibility(View.VISIBLE);
            lvRecipeList.setVisibility(View.VISIBLE);

            searchForCollections = false;
            // clear searchbar query of any collection queries
            svSearchBar.setQuery("", true);
            svSearchBar.clearFocus();
            collectionAdapter.notifyDataSetChanged();
            // change searchbar hint
            svSearchBar.setQueryHint("Search for recipes...");

            // specify which collection the user is adding a recipe to
            String addingRecipeToCollectionTitle = collectionAdapterList.get(collectionChangeIndex).getCollectionTitle();
            tvRecipeSearchPrompt.setText("Add recipe to " + addingRecipeToCollectionTitle);

            // hide non-recipe searching elements
            btnPantryPage.setVisibility(View.INVISIBLE);
            faAddIngredient.setVisibility(View.INVISIBLE);
            btnCreateCollection.setVisibility(View.INVISIBLE);
            rvCollectionList.setVisibility(View.INVISIBLE);
            btnAdminPage.setVisibility(View.INVISIBLE);
            rbProfileRating.setVisibility(View.INVISIBLE);
            faChatPage.setVisibility(View.INVISIBLE);
        }
        else {
            // hide recipe searching elements
            btnCancelRecipe.setVisibility(View.INVISIBLE);
            tvRecipeSearchPrompt.setVisibility(View.INVISIBLE);
            lvRecipeList.setVisibility(View.INVISIBLE);

            // clear displayed list of recipes
            recipeSearchList.clear();
            recipeSearchAdapter.notifyDataSetChanged();

            // re-enable collection searching
            searchForCollections = true;
            searchForCollections();

            // clear search bar of any recipe queries
            svSearchBar.setQuery("", true);
            svSearchBar.clearFocus();
            collectionAdapter.notifyDataSetChanged();
            // change searchbar hint
            svSearchBar.setQueryHint("Search for collections...");

            // show admin page button if user is admin
            if (userIsAdmin) {
                btnAdminPage.setVisibility(View.VISIBLE);
            }

            // show collection elements
            btnPantryPage.setVisibility(View.VISIBLE);
            faAddIngredient.setVisibility(View.VISIBLE);
            btnCreateCollection.setVisibility(View.VISIBLE);
            rvCollectionList.setVisibility(View.VISIBLE);
            rbProfileRating.setVisibility(View.VISIBLE);
            faChatPage.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Gets all recipes stored in the database via GET request, then adds their names to a list
     * for searching purposes. On response, searching for recipes is enabled.
     *
     * @param getRecipesUrl the url associated with the GET recipes request
     */
    private void getAllRecipesRequest(String getRecipesUrl) {
        RequestQueue queue = Volley.newRequestQueue(ProfileActivity.this);

        JSONArray body = null;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, getRecipesUrl, body,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        // store all recipes in a list that the user can look through
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                // add recipe name to list
                                JSONObject recipe = (JSONObject) response.get(i);
                                recipeSearchList.add((String) recipe.get("title"));
                            }
                            catch (JSONException e) {
                                Toast.makeText(ProfileActivity.this, "Error getting recipes: " + e, Toast.LENGTH_LONG).show();
                            }
                        }
                        // then let user search for recipes
                        searchForCollections = false;
                        searchForRecipes();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ProfileActivity.this, "Error getting recipes: " + error, Toast.LENGTH_LONG).show();
                    }
                });
        queue.add(request);
    }

    /**
     * Creates a new empty collection via POST request. On response, calls addEmptyCollectionToDisplay()
     * to update the collection adapter to dynamically display the new collection on the page.
     *
     * @param postCollectionUrl the url associated with POSTing collections
     */
    private void postCollectionRequest(String postCollectionUrl) {
        RequestQueue queue = Volley.newRequestQueue(ProfileActivity.this);

        // make collection body to POST
        String requestBody = "{\"username\": \"" + username + "\", " +
                "\"collectionName\": \"" + collectionToChangeName + "\"}" +
                "\"recipes\": []";
        JSONObject body = null;
        try {
            body = new JSONObject(requestBody);
        } catch (JSONException e) {
            Toast.makeText(ProfileActivity.this, "Error making collection: " + e, Toast.LENGTH_LONG).show();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, postCollectionUrl, body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(ProfileActivity.this, "Collection created", Toast.LENGTH_SHORT).show();
                        // add new collection to lists and update display
                        collectionObjectList.add(response);
                        addEmptyCollectionToDisplay(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ProfileActivity.this, "Error making collection: " + error, Toast.LENGTH_LONG).show();
                    }
                });
        queue.add(request);
    }

    /**
     * Creates an input dialog to ask the user for a name for a new collection.
     * The user can either input a name and press submit to POST a collection with their
     * inputted name (via postCollectionRequest()), or press cancel to exit out of the input dialog
     * and cancel the POST request.
     */
    private void createCollectionInputDialog() {
        // builder that can create an alert dialog and display it on the screen
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);

        builder.setTitle("Enter a name for your collection");

        // assign UI elements in alert dialog
        View dialogView = LayoutInflater.from(ProfileActivity.this)
                .inflate(R.layout.alertdialog_collectiontitleinput,
                        (ViewGroup) findViewById(R.id.collectionDialogFrameLayout), false);
        EditText input = (EditText) dialogView.findViewById(R.id.etCollectionDialog);
        builder.setView(dialogView);

        // when user presses submit to create a collection
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // clear alert dialog
                dialog.dismiss();
                collectionToChangeName = input.getText().toString();
                // send POST request based on collection title
                postCollectionRequest(postCollectionUrl);
            }
        });
        // user cancels collection creation
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    /**
     * Creates a dialog to confirm if a user wants to delete a specified collection. If the user cancels,
     * the dialog disappears and no DELETE request is made. If the user confirms, the previously
     * specified collection is deleted via deleteCollectionRequest() and the page's adapters are updated
     * to dynamically display the removal of the collection.
     */
    private void deleteCollectionConfirmation() {
        // create an alert when the user tries to delete a collection
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setCancelable(true);
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Do you really want to delete this collection?");
        // user confirms that they want to delete a collection
        builder.setPositiveButton("Confirm",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // send DELETE request for collection if confirmed
                        try {
                            // get name of collection that will be deleted
                            collectionToChangeName = (String) collectionObjectList.get(collectionChangeIndex).get("collectionName");
                        } catch (JSONException e) {
                            Toast.makeText(ProfileActivity.this, "JSONException: " + e, Toast.LENGTH_LONG).show();
                        }
                        // send deletion request
                        deleteCollectionRequest(deleteCollectionUrl + collectionToChangeName);

                        // then delete the same collection from the lists, and update the display
                        collectionObjectList.remove(collectionChangeIndex);
                        collectionAdapterList.remove(collectionChangeIndex);

                        collectionAdapter.notifyItemRemoved(collectionChangeIndex);
                        collectionAdapter.notifyDataSetChanged();

                        // exit out of any search queries to display that collection was removed
                        svSearchBar.setQuery("", true);
                        svSearchBar.clearFocus();
                        collectionAdapter.notifyDataSetChanged();

                        // scroll to where the collection used to be
                        rvCollectionList.smoothScrollToPosition(collectionChangeIndex-1);
                    }
                });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // exit out of the confirmation menu if cancelled
            }
        });
        AlertDialog deleteConfirmation = builder.create();

        // show alert dialog
        deleteConfirmation.show();
    }

    /**
     * Deletes a specified collection from a user's collections based on the request url.
     * Displays a success Toast message on response.
     *
     * @param url the url associated with the DELETE collection request, including the name of the collection to be deleted
     */
    private void deleteCollectionRequest(String url) {
        RequestQueue queue = Volley.newRequestQueue(ProfileActivity.this);

        JSONArray body = null;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.DELETE, url, body, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                Toast.makeText(ProfileActivity.this, "Collection deleted", Toast.LENGTH_SHORT).show();
            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ProfileActivity.this, "Error deleting collection: " + error, Toast.LENGTH_LONG).show();
                    }
                });
        queue.add(request);
    }

    /**
     * Gets all collections associated with a certain user via GET request, as specified in the request url.
     * On response, the collections are added to the profile page with addCollectionListToDisplay().
     *
     * @param url the url associated with the GET collections request, including the username of the creator of the desired collections
     */
    private void getCollectionRequest(String url) {
        RequestQueue queue = Volley.newRequestQueue(ProfileActivity.this);

        JSONArray body = null;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, body,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        // add received collections to profile page
                        addCollectionListToDisplay(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(ProfileActivity.this, "Failed to get member collections: " + error, Toast.LENGTH_LONG).show();
            }
        });
        queue.add(request);
    }

    /**
     * Uses a JSONArray of collections to populate the profile page with the user's previously made collections.
     * Each collection in the array is added with addCollectionToDisplay and then added to a JSONArray to store
     * raw JSON data in the profile page.
     *
     * @param jsonArray the array of all previously-made collections associated with the current user
     */
    private void addCollectionListToDisplay(JSONArray jsonArray) {
        // store retrieved collections
        for (int i = 0; i < jsonArray.length(); i++) {
            // add collection
            try {
                // display collection and add it to our detailed JSONObject list
                addCollectionToDisplay((JSONObject) jsonArray.get(i));
                collectionObjectList.add((JSONObject) jsonArray.get(i));
            } catch (JSONException e) {
                System.out.println("Failed to add member collections: " + e);
            }
        }
    }

    /**
     * Based on a JSONObject that represents a collection, a collection is added to the profile page using
     * the JSONObject's data. The collection adapter is updated to reflect the newly added collection,
     * including the name of the collection and recipes contained within.
     *
     * @param jsonCollection the previously-made collection to be added to the display of profile page
     */
    private void addCollectionToDisplay(JSONObject jsonCollection) {
        // retrieve list of recipes from jsonCollection
        JSONArray recipeListFromCollection = new JSONArray();
        try {
            recipeListFromCollection = (JSONArray) jsonCollection.get("recipes");
        } catch (JSONException e) {
            Toast.makeText(ProfileActivity.this, "Error adding collection: " + e, Toast.LENGTH_SHORT).show();
        }

        // get title of collection
        String title = "";
        try {
            title = (String) jsonCollection.get("collectionName");
        } catch (JSONException e) {
            Toast.makeText(ProfileActivity.this, "Error adding collection: " + e, Toast.LENGTH_SHORT).show();
        }

        List<String> recipeStringList = new ArrayList<String>();
        // add recipes from jsonCollection into string list
        for (int i = 0; i < recipeListFromCollection.length(); i++) {
            JSONObject recipeObject = null;
            String recipe = "";
            try {
                // get JSONObject representing recipe from recipe list
                recipeObject = (JSONObject) recipeListFromCollection.get(i);
                // get recipe title from recipe list
                recipe = (String) recipeObject.get("title");
            } catch (JSONException e) {
                Toast.makeText(ProfileActivity.this, "Error adding collection: " + e, Toast.LENGTH_SHORT).show();
            }

            recipeStringList.add(recipe);
        }

        // create new CollectionListItem based on recipe list and title
        CollectionListItem newCollection = new CollectionListItem(title, recipeStringList);
        // add to adapter list
        collectionAdapterList.add(newCollection);

        // notify RecyclerView that a new collection was added
        collectionAdapter.notifyItemInserted(collectionAdapterList.size()-1);
    }

    /**
     * Based on a JSONObject that represents a collection with no recipes, add an empty collection to
     * the profile page and display the new collection by updating the adapter.
     *
     * @param jsonCollection the empty collection to be added to the profile page's display
     */
    private void addEmptyCollectionToDisplay(JSONObject jsonCollection) {
        // get title of collection
        String title = "";
        try {
            title = (String) jsonCollection.get("collectionName");
        } catch (JSONException e) {
            Toast.makeText(ProfileActivity.this, "Error adding collection: " + e, Toast.LENGTH_SHORT).show();
        }

        // create new CollectionListItem based on recipe list and title
        CollectionListItem newCollection = new CollectionListItem(title, new ArrayList<String>());

        // clear any search queries to display all user collections
        svSearchBar.setQuery("", true);
        svSearchBar.clearFocus();
        collectionAdapter.notifyDataSetChanged();

        // add to the full adapter list
        collectionAdapterList.add(newCollection);

        // notify RecyclerView that a new collection was added
        collectionAdapter.notifyItemInserted(collectionAdapterList.size()-1);

        // then scroll to the bottom to show the new collection
        rvCollectionList.smoothScrollToPosition(collectionAdapterList.size()-1);
    }
}
