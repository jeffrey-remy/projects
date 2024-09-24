package com.hb216.recipewizard;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Creates/Displays the login page for the app
 * @author Maddie Sells
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * The given username and password entered by the user
     */
    private String username, password;

    private EditText usernameInput;
    private EditText passwordInput;

   private Button submitBtn, cancelButton, regButton;

    /**
     * URL used for Volley functionality
     */
    private String url =
    //"http://10.0.2.2:8080/login";
    "http://coms-309-006.class.las.iastate.edu:8080/login";

    /**
     * Initializes and sets up the page, includes various click listeners
     * @param savedInstanceState current state of application
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameInput = (EditText) findViewById(R.id.username);
        passwordInput = (EditText) findViewById(R.id.password);
        submitBtn = (Button) findViewById(R.id.submitBtn);
        cancelButton = findViewById(R.id.cancelButton);
        regButton = findViewById(R.id.regButton);

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                username = usernameInput.getText().toString();
                password = passwordInput.getText().toString();

                postRequest();
            }
        });

        /**
         * When cancel is pushed, goes back to main hub
         */
        cancelButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        //Goes to registration page
        regButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
            }
        });

    }

    /**
     * Shows given text for a short length of time
     * @param text given text
     */
    private void showToast(String text){
        Toast.makeText(LoginActivity.this, text, Toast.LENGTH_LONG).show();
    }

    /**
     * shows given text for a long length of time
     * @param text given text
     */
    private void showLongToast(String text){
        Toast.makeText(LoginActivity.this, text, Toast.LENGTH_LONG).show();
    }

    /**
     * POSTs username and password to see if user is part of the database. If yes, displays success text.
     * If not displays error text.
     */
    private void postRequest() {
        RequestQueue queue = Volley.newRequestQueue(LoginActivity.this);

        // Convert input to JSONObject
        JSONObject body = null;
        try{
            // etRequest should contain a JSON object string as your POST body
            // similar to what you would have in POSTMAN-body field
            // and the fields should match with the object structure of @RequestBody on sb
            String jsonString = "{'username': '" + username + "', 'password': '" + password + "'}";
            body = new JSONObject(jsonString);
        } catch (Exception e){
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        showToast("Login Successful");
                        // store username in app for use in other pages
                        SharedPreferences sharedPref = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("username", username).apply();

                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.networkResponse != null) {
                            try {
                                // Parse the error response to get the error message
                                //finds and stores error message
                                String errorResponse = new String(error.networkResponse.data, "UTF-8");
                                JSONObject errorObject = new JSONObject(errorResponse);
                                String errorMessage = errorObject.getString("message");
                                Log.d("AAA", errorMessage);

                                //just prints that you had an incorrect user or pass
                                showLongToast("Incorrect username or password :(");
                                // Handle the error message
                            } catch (JSONException | UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }else{
                            showToast("Some Other Error: " + error);
                        }
                    }
                }
        );
        queue.add(request); // send request
    }
}