package com.hb216.recipewizard;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Displays a registration page to the user where they can create a new account.
 *
 * @author Jeffrey Remy
 */
public class RegistrationActivity extends AppCompatActivity {

    /** A customizable element of the member to be registered. */
    EditText etUsername, etName, etPassword, etEmail;

    /** Sends a POST request for a new member when clicked on. */
    Button btnCreate;

    /** Sends user to the login page. */
    Button btnLogin;

    /** The url associated with the POST request for creating new members. */
    private String requestUrl = "http://coms-309-006.class.las.iastate.edu:8080/newMember";

    /**
     * Initializes the display of the registration page, setting click listeners for creating new members
     * based on user input or accessing the login page.
     * @param savedInstanceState a saved instance of this page that can be re-created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        etUsername = findViewById(R.id.etUsername);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnCreate = findViewById(R.id.btnCreateAccount);
        btnLogin = findViewById(R.id.btnBackToLogin);

        // attempt to create new member based on user input; send POST request
        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // create request for new member based on user entries for fields
                String requestBody = "{\"username\":\"" + etUsername.getText().toString() + "\", "
                        + "\"firstName\":\"" + etName.getText().toString() + "\", "
                        + "\"email\":\"" + etEmail.getText().toString() + "\", "
                        + "\"password\":\"" + etPassword.getText().toString()
                        + "\"}";

                // send post request
                postMemberRequest(requestBody, requestUrl);
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Creates a new member in the database via a POST request. On response, a success message is displayed.
     *
     * @param requestBody a String in JSON format containing info for the new member
     * @param requestUrl the url associated with the member POST request
     */
    private void postMemberRequest(String requestBody, String requestUrl) {
        RequestQueue queue = Volley.newRequestQueue(RegistrationActivity.this);

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
                        Toast.makeText(RegistrationActivity.this, "Account created!", Toast.LENGTH_SHORT).show();
                        System.out.println(response);

                        Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                        startActivity(intent);
                    }
                },
                // failed to complete post request
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(RegistrationActivity.this, "Failed to make account: " + error, Toast.LENGTH_SHORT).show();
                        System.out.println(error);
                    }
                }
        );
        queue.add(request);
    }
}