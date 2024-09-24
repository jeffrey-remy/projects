package com.hb216.recipewizard;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class PantryActivity extends AppCompatActivity {

    private TextView pantryTitle;
    private SearchView pantrySV;
    private RecyclerView pantryRV;
    private RecyclerView ingTypeImageRV;
    private List<JSONObject> curIngList;
    private List<Integer> curIngAmountsList, pantryIdList;
    private LinearLayoutManager linearLayoutManager;
    private rvAdapter pantryAdapter;
    private imageRVAdapter imageRVAdapter;
    private Button backBtn, addIngToPantryBtn, shoppingBtn;

    //The url to be called for volley purposes
    private String url =
            "http://10.0.2.2:8080";
     // "http://coms-309-006.class.las.iastate.edu:8080";
     //"https://65919b70-858c-4edc-b8ad-52ce312dd3cf.mock.pstmn.io";

    private String username;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantry);

        pantryTitle = findViewById(R.id.pantryTitle);
        pantrySV = findViewById(R.id.pantrySV);
        pantryRV = findViewById(R.id.pantryRV);
        backBtn = findViewById(R.id.backBtnPantry);
        addIngToPantryBtn = findViewById(R.id.addIngToPantryBtn);
        shoppingBtn = findViewById(R.id.shoppingBtn);

        // get locally stored username
        SharedPreferences sharedPref = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        username = sharedPref.getString("username", "User");

        curIngList = new ArrayList<>();
        curIngAmountsList = new ArrayList<>();
        pantryIdList = new ArrayList<>();

        //getting the current pantry list to be displayed
        getCurPantryList();

        pantrySV.clearFocus();

        //Changes the search view list based on text entered into search bar
        pantrySV.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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

        addIngToPantryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PantryActivity.this, addIngToPantryActivity.class);
                startActivity(intent);
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PantryActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        shoppingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PantryActivity.this, shoppingListActivity.class);
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
            for(JSONObject object : curIngList){
                try {
                    objName = object.getString("ingredientName");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                if(objName.toLowerCase().contains(text.toLowerCase())){
                    filteredList.add(object);
                }
            }
        pantryAdapter.filterList(filteredList);
    }

    /**
     * Gets the current list of ingredients in a users pantry
     */
    private void getCurPantryList() {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest jsonObjReq = new JsonArrayRequest(Request.Method.GET,
                url + "/pantry-ingredients/" + username, null,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            //goes through each ingredient in the response and adds the ingredient
                            //its amount, and its id the their respective lists
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject ingredient = response.getJSONObject(i).getJSONObject("ingredient");
                                curIngList.add(ingredient);
                                curIngAmountsList.add(response.getJSONObject(i).getInt("amountOz"));

                                int pantryId = response.getJSONObject(i).getInt("id");
                                pantryIdList.add(pantryId);
                            }

                            //Initializing recycler view components
                            linearLayoutManager = new LinearLayoutManager(PantryActivity.this, LinearLayoutManager.VERTICAL, false);
                            pantryAdapter = new PantryActivity.rvAdapter(curIngList, curIngAmountsList);
                            pantryRV.setLayoutManager(linearLayoutManager);
                            pantryRV.setAdapter(pantryAdapter);

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(PantryActivity.this, "Getting current pantry list FAILED: " + error, Toast.LENGTH_LONG).show();
            }
        });

        queue.add(jsonObjReq);
    }

    /**
     * Changes the amount of an ingredient a user has in their pantry
     */
    private void changeIngAmount(int ingId, int ingAmount) {
        RequestQueue queue = Volley.newRequestQueue(this);

        String string = "{'id': '" + ingId + "', 'amountOz': '" + ingAmount + "'}";
        JSONObject body = null;
        try {
            body = new JSONObject(string);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.PUT,
                url + "/pantry-ingredients/" + username, body,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(PantryActivity.this, "Change ingredient amount FAILED: " + error, Toast.LENGTH_LONG).show();
            }
        });
        queue.add(jsonObjReq);
    }

    /**
     * Adapter updates recycler view when displayed items change
     *
     * @author Maddie Sells
     */
    class rvAdapter extends RecyclerView.Adapter<PantryActivity.rvAdapter.holder> {
        List<JSONObject> data;
        List<Integer> amounts;

        /**
         * Contructer for rvAdapter
         *
         * @param data arrayList of given recipe/profile data
         */
        public rvAdapter(List<JSONObject> data, List<Integer> amounts) {
            this.data = data;
            this.amounts = amounts;
        }

        /**
         * Sets search view list based on given filteredList
         * @param filteredList list of objects to change data to
         */
        private void filterList(List<JSONObject> filteredList) {
            this.data = filteredList;
            notifyDataSetChanged();
        }

        /**
         * @param parent   The ViewGroup into which the new View will be added after it is bound to
         *                 an adapter position.
         * @param viewType The view type of the new View.
         * @return holder
         */
        @NonNull
        @Override
        public PantryActivity.rvAdapter.holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(PantryActivity.this).inflate(R.layout.single_pantry_ingredient, parent, false);
            return new PantryActivity.rvAdapter.holder(view);
        }

        /**
         * sets the title and image for each item in each recycler view
         *
         * @param holder   The ViewHolder which should be updated to represent the contents of the
         *                 item at the given position in the data set.
         * @param position The position of the item within the adapter's data set.
         */
        @Override
        public void onBindViewHolder(@NonNull PantryActivity.rvAdapter.holder holder, int position) {
            ArrayList<String> ingAttributesList = new ArrayList<>();
            try {
                //setting the title
                String ingName = data.get(position).getString("ingredientName");
                holder.ingTitle.setText(ingName);

                //setting the amount
                int amount = amounts.get(position);
                String strAmount = amount + "";
                holder.editTextNumber.setText(strAmount);

                //Saving Id
                holder.id = pantryIdList.get(position);

                //creating the ingAttributesList for image RV purposes
                if(data.get(position).getBoolean("lactoseFree")){
                    ingAttributesList.add("lactoseFree");
                }
                if(data.get(position).getBoolean("glutenFree")){
                    ingAttributesList.add("glutenFree");
                }
                if(data.get(position).getBoolean("vegetarian")){
                    ingAttributesList.add("vegetarian");
                }
                if(data.get(position).getBoolean("vegan")){
                    ingAttributesList.add("vegan");
                }
                if(data.get(position).getBoolean("nutFree")){
                    ingAttributesList.add("nutFree");
                }
                if(data.get(position).getBoolean("shellFishFree")){
                    ingAttributesList.add("shellFishFree");
                }

                //sets up recyclerView for pictures
                linearLayoutManager = new LinearLayoutManager(PantryActivity.this, LinearLayoutManager.HORIZONTAL, false);
                imageRVAdapter = new imageRVAdapter(ingAttributesList);
                ingTypeImageRV.setLayoutManager(linearLayoutManager);
                ingTypeImageRV.setAdapter(imageRVAdapter);

                holder.editTextNumber.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        try {
                            int changedAmt = Integer.parseInt(holder.editTextNumber.getEditableText().toString());

                            //changes ingredient amount
                            changeIngAmount(holder.id, changedAmt);

                        }catch (NumberFormatException e){
                            if(!holder.editTextNumber.getEditableText().toString().equals("")){
                                Toast.makeText(PantryActivity.this, "Please only enter a number", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    @Override
                    public void afterTextChanged(Editable editable) {
                    }
                });
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
         * Class for the single recycler view templates
         *
         * @author Maddie Sells
         */
        class holder extends RecyclerView.ViewHolder {
            TextView ingTitle, ozText;
            EditText editTextNumber;
            int id;


            /**
             * Constructor for holder class
             *
             * @param itemView given itemView
             */
            public holder(@NonNull View itemView) {
                super(itemView);
                //Initializing recycler view components
                ingTypeImageRV = itemView.findViewById(R.id.imagesRV);

                ingTitle = itemView.findViewById(R.id.ingTitle);
                editTextNumber = itemView.findViewById(R.id.editTextNumber);
                ozText = itemView.findViewById(R.id.ozText);
                ozText.setText("oz");
            }
        }
    }

    /**
     * Adapter updates recycler view when displayed items change
     *
     * @author Maddie Sells
     */
    class imageRVAdapter extends RecyclerView.Adapter<PantryActivity.imageRVAdapter.imageHolder> {
        List<String> data;

        /**
         * Contructer for rvAdapter
         *
         * @param data arrayList of given recipe/profile data
         */
        public imageRVAdapter(List<String> data) {
            this.data = data;
        }

        /**
         * @param parent   The ViewGroup into which the new View will be added after it is bound to
         *                 an adapter position.
         * @param viewType The view type of the new View.
         * @return holder
         */
        @NonNull
        @Override
        public PantryActivity.imageRVAdapter.imageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(PantryActivity.this).inflate(R.layout.single_ingredient_type_images, parent, false);
            return new PantryActivity.imageRVAdapter.imageHolder(view);
        }

        /**
         * sets the title and image for each item in each recycler view
         *
         * @param holder   The ViewHolder which should be updated to represent the contents of the
         *                 item at the given position in the data set.
         * @param position The position of the item within the adapter's data set.
         */
        @Override
        public void onBindViewHolder(@NonNull PantryActivity.imageRVAdapter.imageHolder holder, int position) {
            //setting images for each ingredient attribute
            if(data.get(position).equals("lactoseFree")){
                holder.setImage("https://media.istockphoto.com/id/1312934705/vector/lactose-free-sign-icon-logo-round-badge-with-milk-bottle-crossed-out.jpg?s=612x612&w=0&k=20&c=tT8voQJE7KRNPfzS02PH1v_zKiISa_S-5_Rnhana9eA=");
            }
            if(data.get(position).equals("glutenFree")){
                holder.setImage("https://content.health.harvard.edu/wp-content/uploads/2022/03/1a1b8e24-3224-41f5-a613-00f28f27cc06.jpg");
            }
            if(data.get(position).equals("vegetarian")){
                holder.setImage("https://t3.ftcdn.net/jpg/05/33/16/84/360_F_533168476_ZwcHp3VXYOAr8RaAHxt2p1FwT0TbXTzj.jpg");
            }
            if(data.get(position).equals("vegan")){
                holder.setImage("https://img.freepik.com/free-vector/grunge-vegan-seal-stamp-rubber-look-rectangular_78370-673.jpg");
            }
            if(data.get(position).equals("nutFree")){
                holder.setImage("https://yt3.googleusercontent.com/ytc/APkrFKYQdUJKBOLxZOFztdw4Gv0D4gylw_yN1gIyMyWH=s900-c-k-c0x00ffffff-no-rj");
            }
            if(data.get(position).equals("shellFishFree")){
                holder.setImage("https://static.vecteezy.com/system/resources/previews/000/343/594/original/vector-shellfish-free-icon.jpg");
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
         *
         * @param filteredList list of objects to change data to
         */
        public void setFilteredList(List<String> filteredList) {
            this.data = filteredList;
            notifyDataSetChanged();
        }

        /**
         * Class for the single recycler view templates
         *
         * @author Maddie Sells
         */
        class imageHolder extends RecyclerView.ViewHolder {
            ImageView image;


            /**
             * Constructor for holder class
             *
             * @param itemView given itemView
             */
            public imageHolder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.typeImage);
            }

            /**
             * Displays the given photo url in the imageView
             * @param url The url of the photo to be displayed
             */
            private void setImage(String url){
                Picasso.get().load(url).centerCrop();
                if(url != null && url.length() > 0){
                    Picasso.get().load(url).into(image);
                }else{
                    Picasso.get().load("https://images.app.goo.gl/YKgyzQhuCVbK43To9").into(image);
                }
            }
        }
    }

}