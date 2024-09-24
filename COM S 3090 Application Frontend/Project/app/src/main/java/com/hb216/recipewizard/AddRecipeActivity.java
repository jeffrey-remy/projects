package com.hb216.recipewizard;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Creates/Displays the add recipe page for the app
 * @author Maddie Sells
 */
public class AddRecipeActivity extends AppCompatActivity implements recyclerInterface_MS{

    private String title, instructions;
    private String photoURL;
    private TextView ingListTitle;
    private EditText editTitle, editInstructions,editPhotoURL;
    private RecyclerView ingRV;
    private Button submitBtn;
    private Button cancelBtn;
    private LinearLayoutManager linearLayoutManager;
    private allIngredientsAdapter addPantryAdapter;

    /**
     * The list of all ingredients in the database
     */
    private List<JSONObject> allIngredients = new ArrayList<>();

    /**
     * The ingredients to be added to the recipe
     */
     private HashSet<String> myIngredients;

    /**
     * The amounts of the ingredients to be added to the recipe
     */
     private HashSet<Integer> myIngredientAmts;

    /**
     * The URL to be called during Volley processes
     */
    private String url =
    "http://10.0.2.2:8080";
    //"http://coms-309-006.class.las.iastate.edu:8080";
    private ArrayAdapter<JSONObject> adapterForAllIngr;

    AlertDialog.Builder builder;


    /**
     * Initializes and sets up the page, includes various click listeners
     * @param savedInstanceState current state of application
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        ingListTitle = (TextView) findViewById(R.id.ingListTitle);
        editTitle = (EditText) findViewById(R.id.editTitle);
        editInstructions = (EditText) findViewById(R.id.instructions);
        editPhotoURL = (EditText) findViewById(R.id.photoURL);
        ingRV = findViewById(R.id.ingRV);
        submitBtn = (Button) findViewById(R.id.submitBtn);
        cancelBtn = (Button) findViewById(R.id.cancelBtn);

        myIngredients = new HashSet<>();
        myIngredientAmts = new HashSet<>();

        //Gets all ingredients from database
        getAllIngredients();


        //puts the list of preset ingredients into ListView
        adapterForAllIngr = new ArrayAdapter<JSONObject>(this, android.R.layout.simple_list_item_1, allIngredients);


        /**
         * When submit is pushed, POSTs given responses
         */
        submitBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                title = editTitle.getText().toString();
                instructions = editInstructions.getText().toString();
                photoURL = editPhotoURL.getText().toString();

               postRequest();
            }
        });

        /**
         * When cancel is pushed, goes back to main hub
         */
        cancelBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                goToLastActivity();
            }
        });
    }

    /**
     * Goes to last activity the user was at
     */
    private void goToLastActivity(){
        Intent lastIntent = getIntent();
        String lastActivity = lastIntent.getStringExtra("lastActivity");

        Intent intent;
        if(lastActivity != null && lastActivity.equals("home")){
            intent = new Intent(AddRecipeActivity.this, HomeActivity.class);
        }else{ //This shouldn't happen
            intent = new Intent(AddRecipeActivity.this, MainActivity.class);
        }
        startActivity(intent);
    }

    /**
     * Gets list of all ingredients in database and adds them to allIngredients list
     */
    private void getAllIngredients() {
        String ingURL = url + "/ingredients";
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET,
                ingURL, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try{
                            //adding array response to my allIngredients array list to be displayed
                            for (int i = 0; i < response.length(); i++){
                                allIngredients.add(response.getJSONObject(i));
                            }

                            //Initializing recycler view components
                            linearLayoutManager = new LinearLayoutManager(AddRecipeActivity.this, LinearLayoutManager.VERTICAL, false);
                            addPantryAdapter = new allIngredientsAdapter(allIngredients, AddRecipeActivity.this);
                            ingRV.setLayoutManager(linearLayoutManager);
                            ingRV.setAdapter(addPantryAdapter);

                        }catch (JSONException e){
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                showToast("Error getting list of ingredients: "+ error);
            }
        });
        queue.add(request);
    }

    /**
     * POSTs made recipe without ingredients
     */
    private void postRequest() {
        RequestQueue queue = Volley.newRequestQueue(AddRecipeActivity.this);

        // Creating body of user inputs to be passed
        JSONObject body = null;
        try{
            String jsonString = "{'title': '" + title + "', " +
                    "'instructions': '" + instructions+ "', " +
                    "'picture': '" + photoURL + "'}";
            body = new JSONObject(jsonString);
        } catch (Exception e){
            e.printStackTrace();
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url + "/recipes", body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //getting recipe id to be able to put ingredients into this recipe
                        int id;
                        try {
                            id = response.getInt("id");
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                        //calling putIngredientsForRecipe() method until all ingredients have been added
                        String[] myIngredientList = myIngredients.toArray(new String[myIngredients.size()]);
                        Integer[] myIngredientAmtList = myIngredientAmts.toArray(new Integer[myIngredientAmts.size()]);
                        for(int i = 0; i < myIngredientList.length; i++){

                            //putIngredientsForRecipe(id, myIngredientList[i], myIngredientAmtList[i].intValue());
                            postIngredientToRecipeIngredients(myIngredientList[i], myIngredientAmtList[i].intValue(), id);
                        }
                        showToast("Recipe submitted");

                        //go see this recipe
                        Intent intent = new Intent(AddRecipeActivity.this, ViewRecipeActivity.class);
                        intent.putExtra("id", id);

                        //just go to home anyway
                        intent.putExtra("lastActivity", "home");
                        startActivity(intent);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showToast("Error submitting recipe: "+ error);
                        goToLastActivity();
                    }
                }
        );
        queue.add(request); // send request
    }

    /**
     * POSTs given ingredient to recipeIngredients
     */
    private void postIngredientToRecipeIngredients(String ingName, int amount, int recipeId) {
        RequestQueue queue = Volley.newRequestQueue(AddRecipeActivity.this);

        // Creating body of user inputs to be passed
        String jsonString = "{'amountOz': '" + amount + "'}";
        JSONObject body = null;
        try{
            body = new JSONObject(jsonString);
        } catch (Exception e){
            e.printStackTrace();
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url + "/recipe-ingredients/" + ingName + "/" + recipeId, body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showToast("Error submitting ingredients to recipeIngredients: "+ error);
                    }
                }
        );
        queue.add(request); // send request
    }

    /**
     * Displays text for a short length
     * @param text String to be displayed
     */
    private void showToast(String text){
        Toast.makeText(AddRecipeActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemClick(int position, int whatDataset){
        builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Amount (in oz)");

        // Set up the input
        final EditText input = new EditText(this);

        input.setHint("ex. 5oz");

        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int amount;
                String ingName;
                try {
                    amount = Integer.parseInt(input.getText().toString());
                    myIngredientAmts.add(amount);

                    ingName = allIngredients.get(position).getString("ingredientName");
                    myIngredients.add(ingName);

                    ingListTitle.setText(ingListTitle.getText() + " " + ingName);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
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

    public AlertDialog.Builder getLastDialog(){
        return builder;
    }
}