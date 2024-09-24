package com.hb216.recipewizard;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class shoppingListActivity extends AppCompatActivity{

    private TextView shoppingTitle;
    private SearchView shoppingSV;
    private RecyclerView shoppingRV;
    private List<JSONObject> listOfAllIngredients;
    private LinearLayoutManager linearLayoutManager;
    private shoppingAdapter shoppingAdapter;
    private Button backBtn;

    //The url to be called for volley purposes
    private String url =
            "http://10.0.2.2:8080";
    // "http://coms-309-006.class.las.iastate.edu:8080";
    // "https://65919b70-858c-4edc-b8ad-52ce312dd3cf.mock.pstmn.io";

    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_list);

        shoppingTitle = findViewById(R.id.shoppingTitle);
        shoppingSV = findViewById(R.id.shoppingSV);
        shoppingRV = findViewById(R.id.shoppingRV);
        backBtn = findViewById(R.id.backToPantry);

        // get locally stored username
        SharedPreferences sharedPref = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        username = sharedPref.getString("username", "User");

        listOfAllIngredients = new ArrayList<>();
        getShoppingList();
        shoppingSV.clearFocus();

        //Changes the seach view list based on text entered into search bar
        shoppingSV.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                filterList(s);
                return false;
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(shoppingListActivity.this, PantryActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Sets filteredList array (the array of items displayed on search) to a filtered set of items based on text entry
     * @param text text entered by user
     * @throws JSONException Occurs if given object doesn't have given attribute
     */
    private void filterList(String text) {
        List<JSONObject> filteredList = new ArrayList<>();
        String objName;
        for(JSONObject object : listOfAllIngredients){
            try {
                objName = object.getJSONObject("recipeIngredient").getJSONObject("ingredient").getString("ingredientName");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            if(objName.toLowerCase().contains(text.toLowerCase())){
                filteredList.add(object);
            }
        }
        shoppingAdapter.setFilteredList(filteredList);
    }

    /**
     * Gets the shopping list for the current user
     */
    private void getShoppingList() {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest jsonObjReq = new JsonArrayRequest(Request.Method.GET,
                url + "/shopping-list/" + username, null,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            //gets the list of shopping ingredients and adds it to listOfAllIngredients
                            for (int i = 0; i < response.length(); i++) {
                                listOfAllIngredients.add(response.getJSONObject(i));
                            }

                            //Initializing recycler view components
                            linearLayoutManager = new LinearLayoutManager(shoppingListActivity.this, LinearLayoutManager.VERTICAL, false);
                            shoppingAdapter = new shoppingAdapter(listOfAllIngredients);
                            shoppingRV.setLayoutManager(linearLayoutManager);
                            shoppingRV.setAdapter(shoppingAdapter);

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(shoppingListActivity.this, "Getting shopping list FAILED: " + error, Toast.LENGTH_LONG).show();
            }
        });

        queue.add(jsonObjReq);
    }

    /**
     * Adapter updates recycler view when displayed items change
     * @author Maddie Sells
     */
    class shoppingAdapter extends RecyclerView.Adapter<shoppingListActivity.shoppingAdapter.holder>{
        List<JSONObject> data;

        /**
         * Contructer for rvAdapter
         * @param data arrayList of given recipe/profile data
         */
        public shoppingAdapter(List<JSONObject> data){
            this.data = data;
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
        public shoppingListActivity.shoppingAdapter.holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(shoppingListActivity.this).inflate(R.layout.single_ingredient_with_amounts_template, parent, false);
            return new shoppingListActivity.shoppingAdapter.holder(view);
        }

        /**
         * sets the title and image for each item in each recycler view
         *
         * @param holder The ViewHolder which should be updated to represent the contents of the
         *        item at the given position in the data set.
         * @param position The position of the item within the adapter's data set.
         */
        @Override
        public void onBindViewHolder(@NonNull shoppingListActivity.shoppingAdapter.holder holder, int position) {
            try {
                //setting the title
                String text = data.get(position).getJSONObject("recipeIngredient").getJSONObject("ingredient").getString("ingredientName");
                holder.ingTitle.setText(text);

                //setting amount
                int amount = data.get(position).getInt("amountOz");
                String amountTitle = amount + "oz";
                holder.amount.setText(amountTitle);

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
            TextView ingTitle;
            TextView amount;

            /**
             * Constructor for holder class
             * @param itemView given itemView
             */
            public holder(@NonNull View itemView) {
                super(itemView);
                ingTitle = itemView.findViewById(R.id.ingTitle);
                amount = itemView.findViewById(R.id.amount);
            }
        }
    }
}