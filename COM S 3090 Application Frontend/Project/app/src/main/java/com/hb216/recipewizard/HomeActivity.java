package com.hb216.recipewizard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates/Displays the home page for the app
 * @author Maddie Sells
 */
public class HomeActivity extends AppCompatActivity implements recyclerInterface_MS{
    private Toolbar toolbar;
    private SearchView searchView;
    private RecyclerView recRV, rsRV, svRV;
    private Button addRecipeBtn, addIngredientBtn;
    private Switch svSwitch;

    /**
     * An array that holds given data (either recipes or profiles)
     */
    private List<JSONObject> recRecipeData, rsRecipeData, fullRecipeList, fullProfileList, filteredList;
    private LinearLayoutManager linearLayoutManager;
    private rvAdapter rvAdapter, fullRecipeAdapter, fullProfileAdapter;
    private ImageView ROTWimage;
    private TextView ROTWtitle, recentlySearchedTitle;
    private int ROTWid;

    /**
     * The username of the current logged in user
     */
    private String username;

    /**
     * The url to be called for volley purposes
     */
    private String url =
         //   "http://10.0.2.2:8080";
           // "https://65919b70-858c-4edc-b8ad-52ce312dd3cf.mock.pstmn.io";
        "http://coms-309-006.class.las.iastate.edu:8080";

    /**
     * Initializes and sets up the page, includes various click listeners
     * @param savedInstanceState current state of application
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // get locally stored username
        SharedPreferences sharedPref = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        username = sharedPref.getString("username", "User");

        toolbar = findViewById(R.id.toolbar);
        searchView = findViewById(R.id.searchView);
        ROTWimage = findViewById(R.id.ROTWphoto);
        ROTWtitle = findViewById(R.id.ROTWtitle);
        addRecipeBtn = findViewById(R.id.addRecipeBtn);
        addIngredientBtn = findViewById(R.id.addIngredientBtn);

        //initializing switch
        svSwitch = findViewById(R.id.svSwitch);
        svSwitch.setText("Searching for recipes...");


        recentlySearchedTitle = findViewById(R.id.recentlySearchedTitle);
        recRV = findViewById(R.id.recRecyclerView);
        rsRV = findViewById(R.id.rsRecyclerView);
        svRV = findViewById(R.id.svRecyclerView);

        //making search view invisible until user is searching
        svRV.setVisibility(View.INVISIBLE);
        svSwitch.setVisibility(View.INVISIBLE);

        //initializing the data source
        recRecipeData = new ArrayList<>();
        rsRecipeData = new ArrayList<>();
        fullRecipeList = new ArrayList<>();
        fullProfileList = new ArrayList<>();
        filteredList = new ArrayList<>();

        //Check to see if user has home initialized
        getHome();

        setSupportActionBar(toolbar);

        searchView.clearFocus();

        //make search view list visible when searching
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                svRV.setVisibility(View.VISIBLE);
                svSwitch.setVisibility(View.VISIBLE);
                ROTWimage.setVisibility(View.INVISIBLE);
                ROTWtitle.setVisibility(View.INVISIBLE);
                rsRV.setVisibility(View.INVISIBLE);
                recentlySearchedTitle.setVisibility(View.INVISIBLE);
                recRV.setVisibility(View.INVISIBLE);
            }
        }
        );

        //make search list disappear when you click out
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                svRV.setVisibility(View.INVISIBLE);
                svSwitch.setVisibility(View.INVISIBLE);
                ROTWimage.setVisibility(View.VISIBLE);
                ROTWtitle.setVisibility(View.VISIBLE);
                rsRV.setVisibility(View.VISIBLE);
                recentlySearchedTitle.setVisibility(View.VISIBLE);
                recRV.setVisibility(View.VISIBLE);
                return false;
            }
        });

        //Changes the seach view list based on text entered into search bar
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                try {
                    filterList(newText);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
        });

        //Changes switch text based on switch position
        svSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //clear filtered list in preparation for reassignment
                filteredList.clear();

                //changes displayed text to profile text and displayed items to profile items
                if(svSwitch.isChecked()){
                    svSwitch.setText("Searching for profiles...");
                    for(int i = 0; i < fullProfileList.size(); i++){
                        filteredList.add(fullProfileList.get(i));
                    }
                    fullProfileAdapter.setFilteredList(filteredList);
                    svRV.setAdapter(fullProfileAdapter);

                  //changes displayed text to profile text and displayed items to profile items
                }else{
                    svSwitch.setText("Searching for recipes...");
                    for(int i = 0; i < fullRecipeList.size(); i++){
                        filteredList.add(fullRecipeList.get(i));
                    }

                    fullRecipeAdapter.setFilteredList(filteredList);
                    svRV.setAdapter(fullRecipeAdapter);
                }
            }
        });

        ROTWimage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, ViewRecipeActivity.class);
                intent.putExtra("id", ROTWid);
                intent.putExtra("lastActivity", "home");
                startActivity(intent);
            }
        });

        addIngredientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, AddIngredientActivity.class);
                intent.putExtra("lastActivity", "home");
                startActivity(intent);
            }
        });

        addRecipeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, AddRecipeActivity.class);
                intent.putExtra("lastActivity", "home");
                startActivity(intent);
            }
        });


    }

    /**
     * Sets filteredList array (the array of items displayed on search) to a filtered set of items based on text entry
     * @param text text entered by user
     * @throws JSONException Occcurs if given object doesn't have given attribute
     */
    private void filterList(String text) throws JSONException {
        filteredList.clear();
        ArrayList<JSONObject> recipeIngredients = new ArrayList<>();

        //search by profile
        if(svSwitch.isChecked()){
            for(JSONObject object : fullProfileList) {
                //search by username of profile
                if (object.get("username").toString().toLowerCase().contains(text.toLowerCase())) {
                    filteredList.add(object);
                }
            }
            if(filteredList.isEmpty()){
                Toast.makeText(this, "No profiles found", Toast.LENGTH_SHORT).show();
            }else{
                fullProfileAdapter.setFilteredList(filteredList);
            }
        //search by recipe
        }else{
            //Searching recipe by name
            for(JSONObject object : fullRecipeList){
                if(object.get("title").toString().toLowerCase().contains(text.toLowerCase())){
                    filteredList.add(object);
                }

//            //searching recipe by ingredients
//            int recipeId = object.getInt("id");
//            getIngredientsForRecipe(recipeId, recipeIngredients);
//            for(JSONObject ingObject : recipeIngredients){
//                if(ingObject.get("name").toString().toLowerCase().contains(text.toLowerCase())){
//                    filteredList.add(ingObject);
//                    break;
//                }
//            }
            }
            if(filteredList.isEmpty()){
                Toast.makeText(this, "No recipes found", Toast.LENGTH_SHORT).show();
            }else{
                fullRecipeAdapter.setFilteredList(filteredList);
            }
        }
    }

    /**
     * inflates option menu
     * @param menu the menu to be inflated
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return true;
    }

    /**
     * Opens option menu and changes activity if option is clicked
     * @param item the menuItem
     * @return the item selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId() == R.id.profileBtn){
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        }else if(item.getItemId() == R.id.addIngBtn){
            Intent intent = new Intent(HomeActivity.this, AddIngredientActivity.class);
            startActivity(intent);
        }else if(item.getItemId() == R.id.addRecipeBtn){
            Intent intent = new Intent(HomeActivity.this, AddRecipeActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Gets the list of recommended recipes and adds them to recRecipeData list, initializes recycler view items
     */
    private void getRecRecipeList() {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest jsonObjReq = new JsonArrayRequest(Request.Method.GET,
                url + "/recommended-recipes/" + username, null,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for(int i = 0; i < response.length(); i++){
                                recRecipeData.add(response.getJSONObject(i));
                            }

                            linearLayoutManager = new LinearLayoutManager(HomeActivity.this, LinearLayoutManager.HORIZONTAL, false);
                            rvAdapter = new rvAdapter(recRecipeData, HomeActivity.this, 1, R.layout.single_rv_template);
                            recRV.setLayoutManager(linearLayoutManager);
                            recRV.setAdapter(rvAdapter);

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(HomeActivity.this, "Getting recommended recipes FAILED: " + error, Toast.LENGTH_LONG).show();
            }
        });

        queue.add(jsonObjReq);
    }

    /**
     * Gets the array list of 5 most recent recently searched recipes and adds them to rsRecipeData, initializes recycler view items
     */
    private void getRsRecipeList() {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest jsonObjReq = new JsonArrayRequest(Request.Method.GET,
                url + "/recently-searched-recipes/" + username + "/latest", null,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for(int i = 0; i < response.length(); i++){
                                rsRecipeData.add(response.getJSONObject(i));
                            }

                            linearLayoutManager = new LinearLayoutManager(HomeActivity.this, LinearLayoutManager.HORIZONTAL, false);
                            rvAdapter = new rvAdapter(rsRecipeData, HomeActivity.this, 2, R.layout.single_rv_template);
                            rsRV.setLayoutManager(linearLayoutManager);
                            rsRV.setAdapter(rvAdapter);

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(HomeActivity.this, "Getting recently searched recipes FAILED: " + error, Toast.LENGTH_LONG).show();
            }
        });

        queue.add(jsonObjReq);
    }

    /**
     * Gets the array list of recipes to be displayed while searching and adds them to fullRecipeList, initializes recycler view items
     */
    private void getFullRecipeList() {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest jsonObjReq = new JsonArrayRequest(Request.Method.GET,
                url + "/recipes", null,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for(int i = 0; i < response.length(); i++){
                                fullRecipeList.add(response.getJSONObject(i));

                                //setting the default filtedList to fullRecipeList
                                filteredList.add(response.getJSONObject(i));
                            }

                            //Initializing recycler view components
                            linearLayoutManager = new LinearLayoutManager(HomeActivity.this, LinearLayoutManager.VERTICAL, false);
                            fullRecipeAdapter = new rvAdapter(fullRecipeList, HomeActivity.this, 3, R.layout.search_view_single_template);
                            svRV.setLayoutManager(linearLayoutManager);
                            svRV.setAdapter(fullRecipeAdapter);

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(HomeActivity.this, "Getting recently searched recipes FAILED: " + error, Toast.LENGTH_LONG).show();
            }
        });

        queue.add(jsonObjReq);
    }

    /**
     * Gets the array list of profiles to be displayed while searching, adds them to fullProfileList, initializes recycler view items
     */
    private void getFullProfileList() {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest jsonObjReq = new JsonArrayRequest(Request.Method.GET,
                url + "/members", null,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for(int i = 0; i < response.length(); i++){
                                fullProfileList.add(response.getJSONObject(i));
                            }
                            linearLayoutManager = new LinearLayoutManager(HomeActivity.this, LinearLayoutManager.VERTICAL, false);
                            fullProfileAdapter = new rvAdapter(fullProfileList, HomeActivity.this, 4, R.layout.search_view_single_template);

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(HomeActivity.this, "Getting recently searched recipes FAILED: " + error, Toast.LENGTH_LONG).show();
            }
        });
        queue.add(jsonObjReq);
    }

    /**
     * Gets the Recipe Of The Week and sets its attributes to various activity items
     */
    private void getROTW() {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                url + "/recipe-of-the-week", null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            ROTWtitle.setText(response.getString("title"));
                            ROTWid = response.getInt("id");
                            String ROTWurl = response.getString("picture");
                            //SET PHOTO
                            if(ROTWurl != null && ROTWurl.length() > 0){
                                Picasso.get().load(ROTWurl).into(ROTWimage);
                            }else{
                                Picasso.get().load("https://images.app.goo.gl/YKgyzQhuCVbK43To9").into(ROTWimage);
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(HomeActivity.this, "Getting ROTW FAILED: " + error, Toast.LENGTH_LONG).show();
            }
        });

        queue.add(jsonObjReq);
    }

    /**
     * Gets list of ingredients for a recipe and adds it to recipeIngredient list
     * @param recipeID finds ingredients from recipe with this id
     * @param recipeIngredient adds ingredients to this list
     */
    private void getIngredientsForRecipe(int recipeID, ArrayList<JSONObject> recipeIngredient) {
        RequestQueue queue = Volley.newRequestQueue(this);

        //clears ingredients from previous recipe
        recipeIngredient.clear();

        JsonArrayRequest jsonObjReq = new JsonArrayRequest(Request.Method.GET,
                url + "/recipes/" + recipeID + "/ingredients", null,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for(int i = 0; i < response.length(); i++){
                                recipeIngredient.add(response.getJSONObject(i));
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(HomeActivity.this, "Getting ingredients for given recipe FAILED: " + error, Toast.LENGTH_LONG).show();
            }
        });

        queue.add(jsonObjReq);
    }

    /**
     *
     * POSTs a recipe when clicked on while searching
     *
     * @param title title of recipe to be posted
     */
    private void postSearchedRecipe(String title) {
        RequestQueue queue = Volley.newRequestQueue(this);

        JSONObject body = null;
        try{
            String jsonString = "{'username': '" + username + "', " + "'title': '" + title + "'}";
            body = new JSONObject(jsonString);
        } catch (Exception e){
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                url + "/recently-searched-recipes/" + username + "/" + title, body,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
         // Toast.makeText(HomeActivity.this, "Posting searched recipe FAILED: " + error, Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(jsonObjReq);
    }

    /**
     * Creates a home page for the given user if they don't have one yet
     */
    private void createHome() {
        RequestQueue queue = Volley.newRequestQueue(this);

        String string = "{'username': '"+ username + "'}";
        JSONObject body = null;
        try {
            body = new JSONObject(string);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                url + "/create-home/" + username, body,
                new Response.Listener<JSONObject>() {

                    //Once a generic home is created, call the methods to get user specific info
                    @Override
                    public void onResponse(JSONObject response) {
                        //populate recRecipeData with JsonArray of recommended recipes
                        getRecRecipeList();

                        //populate rsRecipeData with JsonArray of recently searched recipes
                        getRsRecipeList();

                        //populate fullRecipeList with JsonArray of recipes to be displayed for search
                        getFullRecipeList();

                        //populate fullProfileList with JsonArray of profiles to be displayed for search
                        getFullProfileList();

                        //gets the recipe of the week information
                         getROTW();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(HomeActivity.this, "creating home page FAILED: " + error, Toast.LENGTH_LONG).show();
            }
        });
        queue.add(jsonObjReq);
    }

    /**
     * GETs Home is user has one and calls methods to initialize user specific recipe data
     */
    private void getHome() {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                url + "/home/" + username, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        //If initialized, get the given data

                        //populate recRecipeData with JsonArray of recommended recipes
                        getRecRecipeList();

                        //populate rsRecipeData with JsonArray of recently searched recipes
                        getRsRecipeList();

                        //populate fullRecipeList with JsonArray of recipes to be displayed for search
                        getFullRecipeList();

                        //populate fullProfileList with JsonArray of profiles to be displayed for search
                        getFullProfileList();

                        //gets the recipe of the week information (NOT CURRENTLY WORKING)
                        getROTW();

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                createHome();
                //     Toast.makeText(HomeActivity.this, "Getting ingredients for given recipe FAILED: " + error, Toast.LENGTH_LONG).show();
            }
        });

        queue.add(jsonObjReq);
    }

    /**
     * goes to ViewRecipeActivity based on recipe clicked
     * @param position - position in array list that was clicked
     * @param whatDataset - what dataset was clicked
     * @throws JSONException
     */
    @Override
    public void onItemClick(int position, int whatDataset) throws JSONException {
        Intent intent = new Intent(HomeActivity.this, ViewRecipeActivity.class);
        JSONObject object;
        if(whatDataset == 1){ //recommended recipes
            object = recRecipeData.get(position);
        }else if (whatDataset == 2){ // recently searched recipes
            object = rsRecipeData.get(position);
        } else{ //whatDataset == 3 or 4 (ie. the fullProfile or fullRecipe list) - just using the currently displayed filteredList
            object = filteredList.get(position);
            if(whatDataset == 3){
                //post the recently searched recipe
                String recipeTitle = object.getString("title");
                postSearchedRecipe(recipeTitle);
            }
        }
        int id = object.getInt("id");
        intent.putExtra("id", id);
        intent.putExtra("lastActivity", "home");
        startActivity(intent);
    }

    /**
     * Adapter updates recycler view when displayed items change
     * @author Maddie Sells
     */
    class rvAdapter extends RecyclerView.Adapter<HomeActivity.rvAdapter.holder>{
        private final recyclerInterface_MS recyclerInterface;
        List<JSONObject> data;

        int whatDataset;
        int singleObjectLayout;

        /**
         * Contructer for rvAdapter
         * @param data arrayList of given recipe/profile data
         * @param recyclerInterface given interface
         * @param whatDataset number associated with the dataset given
         * @param singleObjectLayout how the items should be displayed
         */
        public rvAdapter(List<JSONObject> data, recyclerInterface_MS recyclerInterface, int whatDataset, int singleObjectLayout){
            this.data = data;
            this.recyclerInterface = recyclerInterface;
            this.whatDataset = whatDataset;
            this.singleObjectLayout = singleObjectLayout;
        }

        /**
         * @param parent The ViewGroup into which the new View will be added after it is bound to
         *               an adapter position.
         * @param viewType The view type of the new View.
         *
         * @return holder
         */
        @NonNull
        @Override
        public holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(HomeActivity.this).inflate(singleObjectLayout, parent, false);
            return new holder(view, whatDataset);
        }

        /**
         * sets the title and image for each item in each recycler view
         *
         * @param holder The ViewHolder which should be updated to represent the contents of the
         *        item at the given position in the data set.
         * @param position The position of the item within the adapter's data set.
         */
        @Override
        public void onBindViewHolder(@NonNull holder holder, int position) {
            try {
                //setting the title
                String text;
                if(whatDataset == 4){ //if profile dataset
                    text = data.get(position).getString("username");
                }else{ //if recipe dataset

                    //setting recipe title
                    text = data.get(position).getString("title");

                    //setting the picture
                    String imageURL = (data.get(position).getString("picture"));
                    holder.setImage(imageURL);
                }
                holder.tvTitle.setText(text);

            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * @return Size of dataset
         */
        @Override
        public int getItemCount() {
            return data.size();
        }

        /**
         * Sets search view list based on given filteredList
         * @param filteredList list of objects to change data to
         */
        public void setFilteredList(List<JSONObject> filteredList){
            this.data = filteredList;
            notifyDataSetChanged();
        }

        /**
         * Class for the single recycler view templates
         * @author Maddie Sells
         */
        class holder extends RecyclerView.ViewHolder{
            TextView tvTitle;
            ImageView tvImage;

            /**
             * Constructor for holder class
             * @param itemView given itemView
             * @param whatDataset what dataset is given
             */
            public holder(@NonNull View itemView, int whatDataset) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvImage = itemView.findViewById(R.id.tvImage);

                //Calls onItem click method for given position in given arrayList
                itemView.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        if(recyclerInterface != null){
                            int pos = getAdapterPosition();

                            if(pos != RecyclerView.NO_POSITION){
                                try {
                                    recyclerInterface.onItemClick(pos, whatDataset);
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                });
            }

            /**
             * displays the given photo url in the imageView
             * @param url photo url to be displayed
             */
            private void setImage(String url){
                //loads given image if there is one
                if(url != null && url.length() > 0){
                    Picasso.get().load(url).into(tvImage);

                 //otherwise loads default black image
                }else{
                    Picasso.get().load("https://images.app.goo.gl/YKgyzQhuCVbK43To9").into(tvImage);
                }
            }
        }
    }

}
