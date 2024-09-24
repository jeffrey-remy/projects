package com.hb216.recipewizard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AdminModerateActivity extends AppCompatActivity {
    // default elements
    RadioButton rbRecipeView, rbMemberView;
    Button btnBack;

    // member elements
    ListView lvMemberList;
    TextView tvMemberListTitle;
    SearchView svMemberSearch;

    // recipe elements
    TextView tvPendingRecipesTitle;
    ListView lvIngredientList;
    TextView tvSelectedRecipe, tvUploaderTitle, tvUploaderName, tvIngredientsTitle, tvInstructionsTitle, tvInstructions;
    Button btnApprove, btnDeny;
    ImageView ivPendingRecipeImage;

    // list and adapter to store all member names
    List<String> memberNameList = new ArrayList<String>();
    ArrayAdapter<String> memberSearchAdapter;

    // list and adapter to store ingredients within the current pending recipe
    List<String> ingredientList = new ArrayList<String>();
    ArrayAdapter<String> ingredientAdapter;

    // urls associated with Volley requests
    String baseUrl = // "http://10.0.2.2:8080";
            "http://coms-309-006.class.las.iastate.edu:8080";
    String getMembersUrl = baseUrl + "/members";
    String getPendingRecipeUrl = baseUrl + "/moderate/";
    String putApproveUrl = baseUrl + "/moderate/approve/";
    String deleteDenyUrl = baseUrl + "/moderate/deny/";

    String adminName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adminmoderate);

        // get locally stored username
        SharedPreferences sharedPref = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        adminName = sharedPref.getString("username", "TestingAdmin");

        rbRecipeView = findViewById(R.id.rbModerateRecipes);
        rbMemberView = findViewById(R.id.rbModerateMembers);
        lvMemberList = findViewById(R.id.lvModerateUserList);
        tvMemberListTitle = findViewById(R.id.tvModerateUserListTitle);
        svMemberSearch = findViewById(R.id.svModerateUserSearch);
        tvPendingRecipesTitle = findViewById(R.id.tvModerateRecipeTitle);
        tvSelectedRecipe = findViewById(R.id.tvCurrentPendingRecipe);
        tvUploaderTitle = findViewById(R.id.tvPendingUploaderTitle);
        tvUploaderName = findViewById(R.id.tvPendingUploaderName);
        ivPendingRecipeImage = findViewById(R.id.ivPendingRecipeImage);
        tvIngredientsTitle = findViewById(R.id.tvPendingRecipeIngredientTitle);
        lvIngredientList = findViewById(R.id.lvPendingRecipeIngredientList);
        tvInstructionsTitle = findViewById(R.id.tvPendingRecipeInstructionTitle);
        tvInstructions = findViewById(R.id.tvPendingRecipeInstructions);
        btnApprove = findViewById(R.id.btnApprovePendingRecipe);
        btnDeny = findViewById(R.id.btnDenyPendingRecipe);
        btnBack = findViewById(R.id.btnModerateBackToAdmin);

        // show elements about pending recipes
        rbRecipeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // show elements related to pending recipes
                toggleMemberRecipeDisplay(false);
            }
        });

        // show elements about members
        rbMemberView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // show elements related to members
                toggleMemberRecipeDisplay(true);

                // use a GET request to get all the members in the database, then display them in a list
                getAllMembers(getMembersUrl);
            }
        });

        // go to member info page if an individual member name is clicked
        lvMemberList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // get username of member that was clicked on
                String username = (String) adapterView.getAdapter().getItem(i);

                // go to ModerateMemberActivity for this specific member
                Intent intent = new Intent(AdminModerateActivity.this, ModerateMemberActivity.class);
                intent.putExtra("username", username);
                startActivity(intent);
            }
        });

        // approve the currently pending recipe if available
        btnApprove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // send PUT request to approve recipe
                String url = putApproveUrl + tvSelectedRecipe.getText() + "/" + adminName;
                approvePendingRecipe(url);
            }
        });

        // deny the currently pending recipe if available
        btnDeny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // send PUT request to deny recipe
                String url = deleteDenyUrl + tvSelectedRecipe.getText() + "/" + adminName;
                denyPendingRecipe(url);
            }
        });

        // exit page back to admin page
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AdminModerateActivity.this, AdminActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Sets the OnQueryTextListener for the member searchbar.
     */
    private void searchForMembers() {
        // respond to queries on the member searchbar
        svMemberSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                memberSearchAdapter.getFilter().filter(s.toLowerCase());
                memberSearchAdapter.notifyDataSetChanged();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                memberSearchAdapter.getFilter().filter(s.toLowerCase());
                memberSearchAdapter.notifyDataSetChanged();
                return false;
            }
        });
    }

    /**
     * Approves the currently pending recipe via PUT request. On response, will
     * try to GET another pending recipe.
     * @param url the url associated with the PUT pending recipe request
     */
    private void approvePendingRecipe(String url) {
        RequestQueue queue = Volley.newRequestQueue(AdminModerateActivity.this);

        StringRequest request = new StringRequest(Request.Method.PUT, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // try to get the next pending recipe since we are done with this one
                    getPendingRecipe(getPendingRecipeUrl + adminName);
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(AdminModerateActivity.this, "Failed to approve: " + error, Toast.LENGTH_LONG).show();
                }
            });

        queue.add(request);
    }

    /**
     * Denies the currently displayed pending recipe via DELETE request. On response, will
     * try to GET another pending recipe.
     * @param url the url associated with the DELETE pending recipe request
     */
    private void denyPendingRecipe(String url) {
        RequestQueue queue = Volley.newRequestQueue(AdminModerateActivity.this);

        StringRequest request = new StringRequest(Request.Method.DELETE, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // try to get the next pending recipe since we are done with this one
                        getPendingRecipe(getPendingRecipeUrl + adminName);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(AdminModerateActivity.this, "Failed to deny: " + error, Toast.LENGTH_LONG).show();
                    }
                });

        queue.add(request);
    }

    /**
     * Changes the display of the page between member elements and pending recipe elements.
     * If switching to pending recipe display, will try to get a pending recipe via GET request.
     * @param showMembers true if we want to show member elements, false if showing recipe elements
     */
    private void toggleMemberRecipeDisplay(boolean showMembers) {
        // determine which elements should be displayed
        int memberVisibility = showMembers ? View.VISIBLE : View.INVISIBLE;
        int recipeVisibility = showMembers ? View.INVISIBLE : View.VISIBLE;

        // clear out any lists before re-populating them
        memberNameList.clear();

        // clear member search bar of queries
        svMemberSearch.setQuery("", true);
        svMemberSearch.clearFocus();

        if (memberSearchAdapter != null) {
            memberSearchAdapter.notifyDataSetChanged();
        }

        // toggle visibilities
        tvMemberListTitle.setVisibility(memberVisibility);
        lvMemberList.setVisibility(memberVisibility);
        svMemberSearch.setVisibility(memberVisibility);

        // if we are displaying recipes, try to get a pending recipe and display it
        if (!showMembers) {
            getPendingRecipe(getPendingRecipeUrl + adminName);
        }
        // otherwise, make recipe elements invisible
        else {
            tvPendingRecipesTitle.setVisibility(recipeVisibility);
            tvSelectedRecipe.setVisibility(recipeVisibility);
            tvUploaderTitle.setVisibility(recipeVisibility);
            tvUploaderName.setVisibility(recipeVisibility);
            ivPendingRecipeImage.setVisibility(recipeVisibility);
            tvIngredientsTitle.setVisibility(recipeVisibility);
            lvIngredientList.setVisibility(recipeVisibility);
            tvInstructionsTitle.setVisibility(recipeVisibility);
            tvInstructions.setVisibility(recipeVisibility);
            btnApprove.setVisibility(recipeVisibility);
            btnDeny.setVisibility(recipeVisibility);
        }
    }

    /**
     * Displays a pending recipe on the page based on a JSONObject representing that recipe.
     * @param recipe a JSONObject that contains info about a specific recipe
     * @throws JSONException
     */
    private void displayPendingRecipe(JSONObject recipe) throws JSONException {
        // clear out ingredient list if it has previous pending recipe's ingredients
        if (ingredientAdapter != null) {
            ingredientList.clear();
            ingredientAdapter.notifyDataSetChanged();
        }
        String title = "";
        try {
            title = (String) recipe.get("title");
        } catch (ClassCastException e) {
            recipe = null;
        }

        // if recipe is null, then there are no pending recipes to display
        if (recipe == null) {
            // inform admin that there are no pending recipes
            displayNoPendingRecipes();
        }
        // otherwise, display the recipe as normal
        else {
            // display recipe title
            tvSelectedRecipe.setText(title);

            // display name of uploader, if possible
            String uploaderString = recipe.get("uploader").toString();
            JSONObject uploaderMember = null;
            if (uploaderString.isEmpty()) {
                uploaderMember = new JSONObject(uploaderString);
            }

            String uploaderName = "";
            if (uploaderMember != null) {
                uploaderName = (String) uploaderMember.get("username");
            }
            tvUploaderName.setText(uploaderName);

            // display image, if possible
            String imageUrl = "";
            String imageUrlString = recipe.get("picture").toString();
            if (imageUrlString.isEmpty()) {
                imageUrl = (String) recipe.get("picture");
            }
            setRecipeImage(imageUrl);

            System.out.println("image");

            // add ingredients to listview
            JSONArray ingredientArray = (JSONArray) recipe.get("ingredients");
            for (int i = 0; i < ingredientArray.length(); i++) {
                // add ingredient title to list for listview
                JSONObject ingredient = (JSONObject) ingredientArray.get(i);
                JSONObject ingredientObject = (JSONObject) ingredient.get("ingredient");
                String ingredientTitle = (String) ingredientObject.get("ingredientName");
                ingredientList.add(ingredientTitle);
            }
            // set adapter and listview
            ingredientAdapter = new ArrayAdapter<>(AdminModerateActivity.this,
                    android.R.layout.simple_list_item_1, ingredientList);
            lvIngredientList.setAdapter(ingredientAdapter);

            System.out.println("ingredients");

            // display instructions, if possible
            String instructions = "";
            String instructionString = recipe.get("instructions").toString();
            if (instructionString.isEmpty()) {
                instructions = (String) recipe.get("instructions");
            }
            tvInstructions.setText(instructions);

            System.out.println("instructions");

            // finally, make all of these recipe elements visible
            tvPendingRecipesTitle.setVisibility(View.VISIBLE);
            tvSelectedRecipe.setVisibility(View.VISIBLE);
            tvUploaderTitle.setVisibility(View.VISIBLE);
            tvUploaderName.setVisibility(View.VISIBLE);
            ivPendingRecipeImage.setVisibility(View.VISIBLE);
            tvIngredientsTitle.setVisibility(View.VISIBLE);
            lvIngredientList.setVisibility(View.VISIBLE);
            tvInstructionsTitle.setVisibility(View.VISIBLE);
            tvInstructions.setVisibility(View.VISIBLE);
            btnApprove.setVisibility(View.VISIBLE);
            btnDeny.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Changes the display/elements of the page to indicate that there are no pending recipes.
     */
    private void displayNoPendingRecipes() {
        tvPendingRecipesTitle.setText("No pending recipes");

        tvPendingRecipesTitle.setVisibility(View.VISIBLE);

        tvSelectedRecipe.setVisibility(View.INVISIBLE);
        tvUploaderTitle.setVisibility(View.INVISIBLE);
        tvUploaderName.setVisibility(View.INVISIBLE);
        ivPendingRecipeImage.setVisibility(View.INVISIBLE);
        tvIngredientsTitle.setVisibility(View.INVISIBLE);
        lvIngredientList.setVisibility(View.INVISIBLE);
        tvInstructionsTitle.setVisibility(View.INVISIBLE);
        tvInstructions.setVisibility(View.INVISIBLE);
        btnApprove.setVisibility(View.INVISIBLE);
        btnDeny.setVisibility(View.INVISIBLE);
    }

    /**
     * Checks if there are any pending recipes, and if so, displays the first one available on the page.
     * @param url the url associated with the GET pending recipe request
     */
    private void getPendingRecipe(String url) {
        RequestQueue queue = Volley.newRequestQueue(AdminModerateActivity.this);

        JSONObject body = null;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, body,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    // display info about the pending recipe that we received as a response
                    try {
                        displayPendingRecipe(response);
                    } catch (JSONException e) {
                        Toast.makeText(AdminModerateActivity.this, "Error displaying pending recipe: " + e, Toast.LENGTH_LONG).show();
                        System.out.println(e);
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(AdminModerateActivity.this, "Error getting pending recipe: " + error, Toast.LENGTH_LONG).show();
                }
            });

        queue.add(request);
    }

    /**
     * Retrieves all current members via GET request and stores them locally.
     * On response, the OnQueryTextListener for the member searchbar is initialized
     * @param url the url associated with the GET members request
     */
    private void getAllMembers(String url) {
        RequestQueue queue = Volley.newRequestQueue(AdminModerateActivity.this);

        JSONArray body = null;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, body,
            new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    // add elements to list of members
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            // add member username to list
                            JSONObject member = (JSONObject) response.get(i);
                            memberNameList.add((String) member.get("username"));
                        }
                        catch (JSONException e) {
                            Toast.makeText(AdminModerateActivity.this, "Error displaying members: " + e, Toast.LENGTH_LONG).show();
                        }
                    }
                    // then allow for searching for members with the new member list
                    memberSearchAdapter = new ArrayAdapter<String>(AdminModerateActivity.this,
                            android.R.layout.simple_list_item_1, memberNameList);
                    lvMemberList.setAdapter(memberSearchAdapter);

                    // then let the user search for members
                    searchForMembers();
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(AdminModerateActivity.this, "Failed to get members: " + error, Toast.LENGTH_LONG).show();
                }
            });

        queue.add(request);
    }

    /**
     * set the recipe image based on a given url
     * @param imageUrl the url of the image
     */
    private void setRecipeImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get().load(imageUrl).into(ivPendingRecipeImage);
        }
    }

}