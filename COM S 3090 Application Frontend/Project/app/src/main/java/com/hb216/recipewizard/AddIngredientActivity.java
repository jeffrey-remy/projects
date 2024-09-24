package com.hb216.recipewizard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Displays a page that allows the user to add a new ingredient to the database.
 *
 * @author Jeffrey Remy
 */
public class AddIngredientActivity extends AppCompatActivity {

    /** A customizable element of the ingredient to be added. */
    EditText etName, etAmount, etCals;

    /** Toggles a flag for the ingredient's dietary restrictions. */
    CheckBox cbLactoseFree, cbGlutenFree, cbNutFree, cbVegetarian, cbVegan, cbShellfishFree;

    /** Sends a POST request when clicked on with the currently entered ingredient info. */
    Button btnAdd;

    /** Exits out of the add ingredient page. */
    Button btnBack;

    /** The url associated with POSTing ingredients. */
    private String requestUrl = "http://10.0.2.2:8080/ingredients";

    //"http://coms-309-006.class.las.iastate.edu:8080/ingredients";

    /**
     * Initializes the display of the add ingredient page, setting click listeners for when the user
     * wants to add an ingredient or leave the page.
     * @param savedInstanceState a saved instance of this page that can be re-created
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addingredient);

        etName = findViewById(R.id.etIngredientName);
        etAmount = findViewById(R.id.etIngredientAmount);
        etCals = findViewById(R.id.etCaloriesPerOz);
        cbLactoseFree = findViewById(R.id.cbLactoseFree);
        cbGlutenFree = findViewById(R.id.cbGlutenFree);
        cbNutFree = findViewById(R.id.cbNutFree);
        cbVegetarian = findViewById(R.id.cbVegetarian);
        cbVegan = findViewById(R.id.cbVegan);
        cbShellfishFree = findViewById(R.id.cbShellfishFree);
        btnAdd = findViewById(R.id.btnAddIngredient);
        btnBack = findViewById(R.id.btnHome);

        // add ingredient via POST request based on user input
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // construct request body based on currently entered info
                String requestBody = "{\"ingredientName\":\"" + etName.getText().toString() + "\", "
                        + "\"calories\":\"" + etCals.getText().toString() + "\", "
                        + "\"lactoseFree\":\"" + cbLactoseFree.isChecked() + "\", "
                        + "\"glutenFree\":\"" + cbGlutenFree.isChecked() + "\", "
                        + "\"vegetarian\":\"" + cbVegetarian.isChecked() + "\", "
                        + "\"vegan\":\"" + cbVegan.isChecked() + "\", "
                        + "\"nutFree\":\"" + cbNutFree.isChecked() + "\", "
                        + "\"shellFishFree\":\"" + cbShellfishFree.isChecked() + "\""
                        + "}";

                // make post request
                postIngredientRequest(requestBody, requestUrl);
            }
        });

        // go back to previous page
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
        if(lastActivity.equals("home")){
            intent = new Intent(AddIngredientActivity.this, HomeActivity.class);
        }else if(lastActivity.equals("profile")){
            intent = new Intent(AddIngredientActivity.this, ProfileActivity.class);
        }else{ //This shouldn't occur
            intent = new Intent(AddIngredientActivity.this, MainActivity.class);
        }
        startActivity(intent);
    }

    /**
     * Creates a new ingredient in the database via a POST request. On response, a success message is displayed.
     *
     * @param requestBody a String with JSON formatting containing info about the new ingredient
     * @param requestUrl the url associated with the ingredient POST request
     */
    private void postIngredientRequest(String requestBody, String requestUrl) {
        RequestQueue queue = Volley.newRequestQueue(AddIngredientActivity.this);

        JSONObject body = null;
        try {
            // create JSON object from request string
            body = new JSONObject(requestBody);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        // make json object request for post request
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, requestUrl, body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(AddIngredientActivity.this, "Ingredient added!", Toast.LENGTH_SHORT).show();
                        System.out.println(response);
                        goToLastActivity();
                    }
                },
                // failed to complete post request
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(AddIngredientActivity.this, "Failed to add ingredient: " + error, Toast.LENGTH_SHORT).show();
                        System.out.println(error);
                        goToLastActivity();
                    }
                }
        );
        queue.add(request);
    }
}
