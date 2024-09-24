package com.hb216.recipewizard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.java_websocket.handshake.ServerHandshake;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Creates/Displays the view recipe page for the app
 * @author Maddie Sells
 */
public class ViewRecipeActivity extends AppCompatActivity implements WebSocketListener{
    private ImageView image;
    private TextView title, commentSectionTitle, instrucBox, ingTitle, instrucTitle;
    private ListView ingListView, commentListView;
    private EditText messageEtxt;
    private Button postBtn, backBtn, madeBtn, goingToMake;
    /**
     * The recipe id of the recipe to be displayed
     */
    private int givenRecipeId;

    /**
     * The username of the user viewing the page
     */
    private String username;

    private ArrayList<String> listComments, ingredientList;
    private ArrayAdapter<String> commentsAdapter, ingAdapter;

    /**
     * The url to be used for Volley functionality
     */
    private String url =
            //"http://10.0.2.2:8080";
            "http://coms-309-006.class.las.iastate.edu:8080";

    /**
     * The URL to be used for websocket functionality
     */
    private String baseServerURL =
           // "ws://10.0.2.2:8080";
            "ws://coms-309-006.class.las.iastate.edu:8080";

    /**
     * Initializes and sets up the page, includes various click listeners
     * @param savedInstanceState current state of application
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewrecipe);
        image = findViewById(R.id.image);
        title = findViewById(R.id.title);
        ingListView = findViewById(R.id.ingredientList);
        instrucBox = findViewById(R.id.instructionBox);
        ingTitle = findViewById(R.id.ingredientListTitle);
        instrucTitle = findViewById(R.id.instructionsTitle);
        commentSectionTitle = findViewById(R.id.commentSectionTitle);
        commentListView = findViewById(R.id.listOfComments);
        messageEtxt = findViewById(R.id.messageEtxt);
        postBtn = findViewById(R.id.postBtn);
        backBtn = findViewById(R.id.backBtn);
        madeBtn = findViewById(R.id.madeBtn);
        goingToMake = findViewById(R.id.goingToMakeBtn);
        ingredientList = new ArrayList<>();
        listComments = new ArrayList<>();

        //adapter for Comment ListView
        commentsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listComments);
        commentListView.setAdapter(commentsAdapter);

        //adapter for ingredient ListView
        ingAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, ingredientList);
        ingListView.setAdapter(ingAdapter);

        // get locally stored username
        SharedPreferences sharedPref = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        username = sharedPref.getString("username", "User");

        Intent intent = getIntent();
        Bundle b = intent.getExtras();
        if(b != null){
            givenRecipeId = (int) b.get("id");
        }else{
            givenRecipeId = 19;
        }

        getGivenRecipe(givenRecipeId);


        //auto connects user to server when recipe is entered
        connectUserToServer();

        /**
         *When post button is clicked, the message is added to the database websocket
         **/
        postBtn.setOnClickListener(v -> {
            try {

                // send message
                WebSocketManager.getInstance().sendMessage(messageEtxt.getText().toString());
            } catch (Exception e) {
                Log.d("ExceptionSendMessage:", e.getMessage().toString());
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToLastActivity();
            }
        });

        madeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changePantryIngredients();
            }
        });

        goingToMake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addIngToShoppingList();
            }
        });

        //makes it so you can scroll down in the comment list
        commentListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                switch(action){
                    case MotionEvent.ACTION_DOWN:
                        //Disallow ScrollView to intercept touch events.
                        view.getParent().requestDisallowInterceptTouchEvent(true);
                        break;
                    case MotionEvent.ACTION_UP:
                        //Allow ScrollView to intercept touch events.
                        view.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                //Handle ListView touch events
                view.onTouchEvent(motionEvent);
                return true;
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
            intent = new Intent(ViewRecipeActivity.this, HomeActivity.class);
        }else if(lastActivity.equals("profile")){
            intent = new Intent(ViewRecipeActivity.this, ProfileActivity.class);
        }else{ //This shouldn't occur
            intent = new Intent(ViewRecipeActivity.this, MainActivity.class);
        }
        startActivity(intent);
    }

    /**
     * Adds items to listComments
     * @param s The comment to be added to listComments
     */
    private void addItems(String s){
        listComments.add(s);
        commentsAdapter.notifyDataSetChanged();
    }

    /**
     * Posts a runnable to the UI thread's message queue
     * @param message The received WebSocket message.
     */
    @Override
    public void onWebSocketMessage(String message) {
        runOnUiThread(() -> {
            String[] lines = message.split("\\R\\R");
            for(int i = 0; i < lines.length; i++){
                addItems(lines[i]);
            }
        });
    }

    /**
     * Performs basic closing operations to close the websocket
     * @param code   The status code indicating the reason for closure.
     * @param reason A human-readable explanation for the closure.
     * @param remote Indicates whether the closure was initiated by the remote endpoint.
     */
    @Override
    public void onWebSocketClose(int code, String reason, boolean remote) {
        String closedBy = remote ? "server" : "local";
        runOnUiThread(() -> {
        });
    }

    /**
     * Performs basic opening operations to open the websocket
     * @param handshakedata Information about the server handshake.
     */
    @Override
    public void onWebSocketOpen(ServerHandshake handshakedata) {}

    /**
     * Default websocket error handling
     * @param ex The exception that describes the error.
     */
    @Override
    public void onWebSocketError(Exception ex) {
        commentSectionTitle.setText(ex.toString());
    }

    /**
     * Auto connects user to server
     */
    private void connectUserToServer(){
        String serverURL = baseServerURL + "/comments/" +  givenRecipeId + "/" + username;

        WebSocketManager.getInstance().connectWebSocket(serverURL);
        WebSocketManager.getInstance().setWebSocketListener(ViewRecipeActivity.this);
    }

    /**
     * GETs the recipe that was clicked on and assigns all fields to appropriate values
     * @param recipeId The id of the recipe to be gotten data from and displayed
     */
    private void getGivenRecipe(long recipeId) {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                url + "/recipes/" + recipeId, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            //setting up the page given given recipe
                            title.setText(response.getString("title"));
                            instrucBox.setText(response.getString("instructions"));
                            String photoURL = response.getString("picture");
                            setImage(photoURL);

                            //gets list of ingredients and their attributes for the current recipe and saves
                            // it in listOfIngredients
                            ArrayList<JSONObject> listOfIngredients = new ArrayList<>();
                            for(int i = 0; i < response.getJSONArray("ingredients").length(); i++){
                                listOfIngredients.add((JSONObject) response.getJSONArray("ingredients").get(i));
                            }

                            //goes through the list of ingredients and picks out the name and amount to be
                            //displayed for current recipe
                            for(int i = 0; i < listOfIngredients.size(); i++){
                                String ingredientName = listOfIngredients.get(i).getJSONObject("ingredient").getString("ingredientName");
                                int amount = listOfIngredients.get(i).getInt("amountOz");
                                String output = ingredientName + " (" + amount + "oz)";
                                ingredientList.add(output);
                                ingAdapter.notifyDataSetChanged();
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ViewRecipeActivity.this, "Getting ingredient failed: " + error, Toast.LENGTH_LONG).show();
                    }
        });
        queue.add(jsonObjReq);
    }

    /**
     * Is called when the user *makes* the recipe
     * Changes users pantry ingredient amounts based on amounts specified in made recipe
     */
    private void changePantryIngredients() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String recipeTitle = (String) title.getText();

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.PUT,
                url + "/pantry-ingredients/" + username + "/" + recipeTitle, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(error instanceof ParseError){ //should be a parse error since a null response is returned
                    Toast.makeText(ViewRecipeActivity.this, "Ingredients updated!", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(ViewRecipeActivity.this, "Failed: " + error, Toast.LENGTH_LONG).show();
                }
            }
        });
        queue.add(jsonObjReq);
    }

    /**
     * Is called when the user *is going to make* the recipe
     * Adds ingredients that are in this recipe that a user does not have enough of in their pantry to their shopping list
     */
    private void addIngToShoppingList() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String recipeTitle = (String) title.getText();

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.PUT,
                url + "/shopping-list/" + username + "/" + recipeTitle, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(error instanceof ParseError){ //should be a parse error since a null response is returned
                    Toast.makeText(ViewRecipeActivity.this, "Ingredients needed added to your shopping cart!", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(ViewRecipeActivity.this, "Failed: " + error, Toast.LENGTH_LONG).show();
                }
            }
        });
        queue.add(jsonObjReq);
    }

    /**
     * Displays the given photo url in the imageView
     * @param url The url of the photo to be displayed
     */
    private void setImage(String url){
        if(url != null && url.length() > 0){
            Picasso.get().load(url).into(image);
        }else{
          Picasso.get().load("https://images.app.goo.gl/YKgyzQhuCVbK43To9").into(image);
        }
    }
}
