package com.hb216.recipewizard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AdminDevActivity extends AppCompatActivity {

    Button btnBackToAdmin;
    // theme elements
    TextView tvCurrentThemeTitle, tvBackgroundColorTitle, tvTextColorTitle;
    Spinner themeSpinner;
    Button btnCreateTheme, btnChangeTheme;
    // empty textview elements with background colors that represent colors in a theme
    TextView backgroundColor, textColor;

    // recipe of the week elements
    TextView tvROTWTitle, tvCurrentROTW;
    Button btnChangeROTW;

    // only display when searching for recipes to set as ROTW
    TextView tvSetROTWTitle;
    ListView lvRecipeList;
    Button btnCancelROTW;
    SearchView svRecipeSearch;

    /** The username of the admin currently on the page. */
    private String adminName;

    // list of JSON objects that represent theme objects
    JSONArray themeObjectList = new JSONArray();

    // list of all theme names
    ArrayList<String> themeTitles = new ArrayList<String>();

    // list and adapter used to display all recipe titles dynamically
    List<String> recipeSearchList = new ArrayList<String>();
    ArrayAdapter<String> recipeSearchAdapter;

    /** The localhost or remote url that hosts the server. */
    private String baseUrl = // "http://10.0.2.2:8080";
           "http://coms-309-006.class.las.iastate.edu:8080";

    // urls for Volley requests
    private String getAllThemesUrl = baseUrl + "/allThemes";
    private String getThemeUrl = baseUrl + "/Theme";
    private String putThemeUrl = baseUrl + "/Theme/";
    private String setROTWUrl = baseUrl + "/set-recipe-of-the-week/";
    private String getROTWUrl = baseUrl + "/recipe-of-the-week";
    private String getRecipesUrl = baseUrl + "/recipes";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admindev);

        // get locally stored username
        SharedPreferences sharedPref = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        adminName = sharedPref.getString("username", "TestingAdmin");

        // update url(s)
        putThemeUrl += adminName + "/";

        // elements that are displayed by default
        btnBackToAdmin = findViewById(R.id.btnDevToAdmin);
        btnCreateTheme = findViewById(R.id.btnDevCreateTheme);
        btnChangeTheme = findViewById(R.id.btnDevChangeTheme);
        themeSpinner = findViewById(R.id.spinThemes);
        tvCurrentThemeTitle = findViewById(R.id.tvDevCurrentThemeTitle);
        tvBackgroundColorTitle = findViewById(R.id.tvBackgroundColorTitle);
        tvTextColorTitle = findViewById(R.id.tvTextColorTitle);
        backgroundColor = findViewById(R.id.themeBackgroundColor);
        textColor = findViewById(R.id.themeTextColor);

        tvROTWTitle = findViewById(R.id.tvDevROTWTitle);
        tvCurrentROTW = findViewById(R.id.tvDevCurrentROTW);
        btnChangeROTW = findViewById(R.id.btnChangeROTW);

        // elements that only display when setting the recipe of the week to a new recipe
        tvSetROTWTitle = findViewById(R.id.tvSetROTWTitle);
        btnCancelROTW = findViewById(R.id.btnCancelAddingROTW);
        lvRecipeList = findViewById(R.id.lvDevRecipeList);
        svRecipeSearch = findViewById(R.id.svRecipeSearchAdmin);

        // get all the existing themes and populate the spinner, and also set the selected spinner item
        // to the current global theme
        initializeSpinner(getAllThemesUrl);

        // get the recipe of the week and display it
        getRecipeOfTheWeek(getROTWUrl);

        // display colors of currently selected theme
        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            // change the color preview squares when a new theme is selected
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // get the selected theme
                String selectedTheme = (String) themeSpinner.getSelectedItem();

                // set the global theme via PUT
                changeGlobalTheme(putThemeUrl + selectedTheme);

                // then update the color preview based on the new global theme
                changeColorPreview();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        // create a theme
        btnCreateTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // go to theme editor page to make a new theme
                Intent intent = new Intent(AdminDevActivity.this, ThemeEditorActivity.class);
                // specify that we want to create a theme in the editor
                intent.putExtra("themeToChange", "");
                startActivity(intent);
            }
        });

        // change the currently selected theme
        btnChangeTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // go to theme editor page to change theme
                Intent intent = new Intent(AdminDevActivity.this, ThemeEditorActivity.class);
                // get the currently selected theme
                String selectedTheme = (String) themeSpinner.getSelectedItem();
                // specify the theme that we want to change in the editor page
                intent.putExtra("themeToChange", selectedTheme);
                startActivity(intent);
            }
        });

        // enter recipe searching menu if admin wants to set a new recipe of the week
        btnChangeROTW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // enter recipe search menu
                toggleRecipeSearchDisplay(true);

                // get all recipes in database
                getAllRecipesRequest(getRecipesUrl);
            }
        });

        // exit recipe search menu
        btnCancelROTW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // exit recipe search menu, display dev elements
                toggleRecipeSearchDisplay(false);
            }
        });

        // go back to the admin page
        btnBackToAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AdminDevActivity.this, AdminActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Set the recipe of the week based on a currently existing recipe title via POST request.
     * @param url the url associated with the recipe of the week POST request
     * @param recipeTitle the name of the recipe that we are setting as the recipe of the week
     */
    private void setRecipeOfTheWeek(String url, String recipeTitle) {
        RequestQueue queue = Volley.newRequestQueue(AdminDevActivity.this);

        // construct url based on recipe title
        String setROTWUrl = url + recipeTitle + "/" + adminName;

        StringRequest request = new StringRequest(Request.Method.POST, setROTWUrl,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // update display
                    tvCurrentROTW.setText(recipeTitle);
                    Toast.makeText(AdminDevActivity.this, "Updated ROTW", Toast.LENGTH_SHORT).show();
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(AdminDevActivity.this, "Error setting ROTW: " + error, Toast.LENGTH_SHORT).show();
                }
            });

        queue.add(request);
    }

    /**
     * Get the recipe of the week via GET and display it on the page.
     * @param url the url associated with getting the recipe of the week
     */
    private void getRecipeOfTheWeek(String url) {
        RequestQueue queue = Volley.newRequestQueue(AdminDevActivity.this);

        JSONObject body = null;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, body,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    // display the title of the recipe of the week
                    String title = "";
                    try {
                        title = (String) response.get("title");
                    }
                    catch (JSONException e) {
                        Toast.makeText(AdminDevActivity.this, "JSON error: " + e, Toast.LENGTH_LONG).show();
                    }
                    tvCurrentROTW.setText(title);
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(AdminDevActivity.this, "Failed to get ROTW: " + error, Toast.LENGTH_LONG).show();
                }
            });

        queue.add(request);
    }

    /**
     * Get all of the themes via GET and populate the theme spinner with a list of them on response.
     * Also, it gets the global theme on response and sets the spinner selection to that theme.
     * @param url the url associated with getting all themes
     */
    private void initializeSpinner(String url) {
        RequestQueue queue = Volley.newRequestQueue(AdminDevActivity.this);

        JSONArray body = null;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, body,
            new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    // store info of all themes, including colors
                    themeObjectList = response;

                    // get all theme titles
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject themeObject = (JSONObject) response.get(i);
                            themeTitles.add((String) themeObject.get("themeName"));
                        }
                    }
                    catch (JSONException e) {
                        Toast.makeText(AdminDevActivity.this, "JSON error: " + e, Toast.LENGTH_LONG).show();
                    }
                    // create adapter for spinner based on theme array
                    ArrayAdapter<String> themeAdapter = new ArrayAdapter<String>(AdminDevActivity.this, android.R.layout.simple_spinner_dropdown_item, themeTitles);
                    // populate spinner with array of themes
                    themeSpinner.setAdapter(themeAdapter);

                    // get current theme and display the current theme via the spinner
                    getGlobalTheme(getThemeUrl);
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(AdminDevActivity.this, "Error getting themes: " + error, Toast.LENGTH_LONG).show();
                    System.out.println("Error getting themes: " + error);
                }
            });

        queue.add(request);
    }

    /**
     * Toggles the current display to either show or hide elements for searching recipes to set
     * the recipe of the week.
     * @param enterRecipeSearch flag that determines whether or not the recipe search elements should be displayed
     */
    private void toggleRecipeSearchDisplay(boolean enterRecipeSearch) {
        // toggle visibility of elements based on whether or not we are searching for recipes
        int recipeSearchVisibility = enterRecipeSearch ? View.VISIBLE : View.INVISIBLE;
        int otherElementVisibility = enterRecipeSearch ?  View.INVISIBLE : View.VISIBLE;

        // clear recipe searchbar
        svRecipeSearch.setQuery("", true);
        svRecipeSearch.clearFocus();
        // reset displayed list of recipes
        recipeSearchList.clear();
        if (recipeSearchAdapter != null) {
            recipeSearchAdapter.notifyDataSetChanged();
        }

        // toggle visibility of elements
        tvSetROTWTitle.setVisibility(recipeSearchVisibility);
        svRecipeSearch.setVisibility(recipeSearchVisibility);
        btnCancelROTW.setVisibility(recipeSearchVisibility);
        lvRecipeList.setVisibility(recipeSearchVisibility);

        btnBackToAdmin.setVisibility(otherElementVisibility);
        btnCreateTheme.setVisibility(otherElementVisibility);
        btnChangeTheme.setVisibility(otherElementVisibility);
        themeSpinner.setVisibility(otherElementVisibility);
        tvCurrentThemeTitle.setVisibility(otherElementVisibility);
        tvBackgroundColorTitle.setVisibility(otherElementVisibility);
        tvTextColorTitle.setVisibility(otherElementVisibility);
        backgroundColor.setVisibility(otherElementVisibility);
        textColor.setVisibility(otherElementVisibility);
        tvCurrentROTW.setVisibility(otherElementVisibility);
        tvROTWTitle.setVisibility(otherElementVisibility);
        btnChangeROTW.setVisibility(otherElementVisibility);
    }

    /**
     * Changes the global theme of the app via a PUT request. On response, the new global theme is displayed.
     * @param putThemeUrl the url associated with the theme PUT request
     */
    private void changeGlobalTheme(String putThemeUrl) {
        RequestQueue queue = Volley.newRequestQueue(AdminDevActivity.this);

        JSONObject body = null;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, putThemeUrl, body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Toast.makeText(AdminDevActivity.this, "Theme changed", Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(AdminDevActivity.this, "Failed to change theme: " + error, Toast.LENGTH_LONG).show();
                    }
                });

        queue.add(request);
    }

    /**
     * Gets the global theme of the app as a String. On response, it displays the theme.
     * @param getThemeUrl the url associated with the theme GET request
     */
    private void getGlobalTheme(String getThemeUrl) {
        RequestQueue queue = Volley.newRequestQueue(AdminDevActivity.this);

        JSONObject body = null;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, getThemeUrl, body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // get position of the current theme in the spinner adapter
                        int position;
                        try {
                            String name = (String) response.get("themeName");
                            position = themeTitles.indexOf(name);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        // display global theme
                        themeSpinner.setSelection(position);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(AdminDevActivity.this, "Failed to get global theme: " + error, Toast.LENGTH_LONG).show();
                    }
                });

        queue.add(request);
    }

    /**
     * Sets the profile page's search bar to take in input for recipe searching.
     * Updating the searchbar with new user queries updates the filter of a list of recipes.
     */
    private void searchForRecipes() {
        // use adapter for list view so we can make search queries
        recipeSearchAdapter = new ArrayAdapter<String>(AdminDevActivity.this,
                android.R.layout.simple_list_item_1, recipeSearchList);
        lvRecipeList.setAdapter(recipeSearchAdapter);

        // item click listener for when user clicks on a recipe
        lvRecipeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long l) {
                // get name of recipe that was clicked on
                String clickedRecipe = (String) parent.getAdapter().getItem(position);
                // set recipe of the week with recipe title that was clicked on
                setRecipeOfTheWeek(setROTWUrl, clickedRecipe);
                // exit add recipe view once recipe is added
                toggleRecipeSearchDisplay(false);
            }
        });

        // search bar for searching through recipes
        svRecipeSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if (recipeSearchList.contains(s.toLowerCase())) {
                    // filter recipe list based on query
                    recipeSearchAdapter.getFilter().filter(s);
                    recipeSearchAdapter.notifyDataSetChanged();
                }
                else {
                    Toast.makeText(AdminDevActivity.this, "Recipe not found", Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            // continuously change list view based on what the user has inputted
            @Override
            public boolean onQueryTextChange(String s) {
                recipeSearchAdapter.getFilter().filter(s);
                recipeSearchAdapter.notifyDataSetChanged();
                return false;
            }
        });
    }

    /**
     * Gets all recipes stored in the database via GET request, then adds their names to a list
     * for searching purposes. On response, searching for recipes is enabled.
     *
     * @param url the url associated with the GET recipes request
     */
    private void getAllRecipesRequest(String url) {
        RequestQueue queue = Volley.newRequestQueue(AdminDevActivity.this);

        JSONArray body = null;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, body,
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
                                Toast.makeText(AdminDevActivity.this, "Error getting recipes: " + e, Toast.LENGTH_LONG).show();
                            }
                        }
                        // then let user search for recipes
                        searchForRecipes();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(AdminDevActivity.this, "Error getting recipes: " + error, Toast.LENGTH_LONG).show();
                    }
                });
        queue.add(request);
    }

    /**
     * Changes the color previews for the currently selected theme on the page based on the theme info.
     */
    private void changeColorPreview() {
        String backgroundHexcode = "";
        String textHexcode = "";

        // get position of currently selected theme
        String currentTheme = (String) themeSpinner.getSelectedItem();
        int position = themeTitles.indexOf(currentTheme);

        try {
            // get colors of currently selected theme
            JSONObject theme = (JSONObject) themeObjectList.get(position);
            backgroundHexcode = (String) theme.get("primaryColor");
            textHexcode = (String) theme.get("secondaryColor");
        } catch (JSONException e) {
            Toast.makeText(AdminDevActivity.this, "Error storing themes: " + e, Toast.LENGTH_LONG).show();
        }

        // then parse the hexcodes as colors that can be passed in
        int bgColorValue = Color.parseColor(backgroundHexcode);
        int txtColorValue = Color.parseColor(textHexcode);

        // set the color previews to the retrieved colors
        backgroundColor.setBackgroundColor(bgColorValue);
        textColor.setBackgroundColor(txtColorValue);
    }
}
