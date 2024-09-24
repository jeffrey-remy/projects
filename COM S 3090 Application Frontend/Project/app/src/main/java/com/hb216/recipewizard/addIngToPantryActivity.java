package com.hb216.recipewizard;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class addIngToPantryActivity extends AppCompatActivity implements recyclerInterface_MS{

    private TextView addPantryTitle;
    private SearchView addPantrySV;
    private RecyclerView addPantryRV;
    private List<JSONObject> listOfAllIngredients, ingToAdd;
    private LinearLayoutManager linearLayoutManager;
    private allIngredientsAdapter addPantryAdapter;
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
        setContentView(R.layout.activity_add_ing_to_pantry);

        addPantryTitle = findViewById(R.id.addPantryTitle);
        addPantrySV = findViewById(R.id.addPantrySV);
        addPantryRV = findViewById(R.id.addPantryRV);
        backBtn = findViewById(R.id.backBtnAddPantry);


        // get locally stored username
        SharedPreferences sharedPref = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        username = sharedPref.getString("username", "User");

        ingToAdd = new ArrayList<>();

        listOfAllIngredients = new ArrayList<>();
        getAllIngredients();

        addPantrySV.clearFocus();

        //Changes the seach view list based on text entered into search bar
        addPantrySV.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
                Intent intent = new Intent(addIngToPantryActivity.this, PantryActivity.class);
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
                objName = object.getString("ingredientName");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            if(objName.toLowerCase().contains(text.toLowerCase())){
                filteredList.add(object);
            }
        }
        addPantryAdapter.filterList(filteredList);
    }

    /**
     * Gets all the ingredients in the database
     */
    private void getAllIngredients() {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest jsonObjReq = new JsonArrayRequest(Request.Method.GET,
                url + "/ingredients", null,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            //Gets list of ingredients and stores them in listOfAllIngredients
                            for (int i = 0; i < response.length(); i++) {
                                listOfAllIngredients.add(response.getJSONObject(i));
                            }

                            //Initializing recycler view components
                            linearLayoutManager = new LinearLayoutManager(addIngToPantryActivity.this, LinearLayoutManager.VERTICAL, false);
                            addPantryAdapter = new allIngredientsAdapter(listOfAllIngredients, addIngToPantryActivity.this);
                            addPantryRV.setLayoutManager(linearLayoutManager);
                            addPantryRV.setAdapter(addPantryAdapter);

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(addIngToPantryActivity.this, "Getting all ingredient list FAILED: " + error, Toast.LENGTH_LONG).show();
            }
        });

        queue.add(jsonObjReq);
    }

    /**
     * Posts ingredients to add to pantry
     */
    private void postIngredient(String ingName, int ingAmount) {
        RequestQueue queue = Volley.newRequestQueue(this);

        String string = "{'amountOz': '" + ingAmount + "'}";
        JSONObject body = null;
        try {
            body = new JSONObject(string);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                url + "/pantry-ingredients/" + username + "/" + ingName, body,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(addIngToPantryActivity.this, "ingredient added", Toast.LENGTH_LONG).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(addIngToPantryActivity.this, "adding ingredients to pantry FAILED: " + error, Toast.LENGTH_LONG).show();
            }
        });
        queue.add(jsonObjReq);
    }

    @Override
    public void onItemClick(int position, int whatDataset){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Amount (in oz)");

        // Set up the input
        final EditText input = new EditText(this);

        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int amount = Integer.parseInt(input.getText().toString());
                String ingName;
                try {
                    ingName = listOfAllIngredients.get(position).getString("ingredientName");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                //call post ingredient method
                postIngredient(ingName, amount);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}