package com.hb216.recipewizard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

/**
 * Displays the admin page, including various features exclusive to admins.
 *
 * @author Jeffrey Remy
 */
public class AdminActivity extends AppCompatActivity {

    /** A button for navigating to other pages in the app. */
    Button btnProfile, btnHome;
    /** Toggles the type of mode that the admin is currently in. */
    RadioButton rbDevMode, rbModMode, rbNormMode;
    /** Displays the current global theme. */
    TextView tvCurrentTheme;
    TextView tvCurrentThemeTitle;

    TextView tvROTWTitle;
    TextView tvCurrentROTW;

    TextView tvPendingRecipeTitle, tvPendingRecipeStatus, tvMemberCountTitle, tvMemberCount;

    TextView tvModeTitle;
    RadioGroup rgChangeMode;

    Button btnDevPage, btnModeratePage;

    ListView lvAdminLog;
    TextView tvAdminLogTitle;

    List<String> logStringList = new ArrayList<String>();
    ArrayAdapter logAdapter;

    /** The localhost or remote url that hosts the server. */
    private String baseUrl = //"http://10.0.2.2:8080";
        "http://coms-309-006.class.las.iastate.edu:8080";
    /** The username of the admin currently on the page. */
    private String adminName;

    /** The url associated with the GET theme request. */
    private String getThemeUrl = baseUrl + "/Theme";
    /** The url associated with the PUT admin mode request. */
    private String adminModeUrl = baseUrl + "/admins/";

    private String getLogUrl = baseUrl + "/log";
    private String getROTWUrl = baseUrl + "/recipe-of-the-week";
    private String getMembersUrl = baseUrl + "/members";
    private String getPendingRecipeUrl = baseUrl + "/moderate/";

    /** Flags for the admin's current mode, false by default. */
    boolean devModeEnabled, modModeEnabled, normModeEnabled = false;

    /**
     * Initializes the display of the admin page, and sets responses for user input such as pressing
     * buttons or entering text.
     * @param savedInstanceState a saved instance of this page that can be re-created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // get locally stored username
        SharedPreferences sharedPref = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        adminName = sharedPref.getString("username", "MRTA");
        // adminName = "MRTA";

        // update url(s)
        getPendingRecipeUrl += adminName;

        // elements that are always displayed
        btnProfile = findViewById(R.id.btnAdminToProfile);
        btnHome = findViewById(R.id.btnAdminToHome);
        tvModeTitle = findViewById(R.id.tvModeTitle);
        rgChangeMode = findViewById(R.id.rgChangeAdminMode);
        rbDevMode = findViewById(R.id.rbDevMode);
        rbModMode = findViewById(R.id.rbModMode);
        rbNormMode = findViewById(R.id.rbNormMode);

        // elements that only display in normal mode (admin log)
        tvAdminLogTitle = findViewById(R.id.tvAdminLogTitle);
        lvAdminLog = findViewById(R.id.lvAdminLogList);

        // elements that only display in dev mode, like the theme or recipe of the week
        tvCurrentTheme = findViewById(R.id.tvAdminCurrentTheme);
        tvCurrentThemeTitle = findViewById(R.id.tvCurrentThemeTitle);
        tvCurrentROTW = findViewById(R.id.tvCurrentROTW);
        tvROTWTitle = findViewById(R.id.tvROTWTitle);

        // elements that only display in moderate mode, like pending recipe status and member count
        tvPendingRecipeTitle = findViewById(R.id.tvAdminPendingRecipeTitle);
        tvPendingRecipeStatus = findViewById(R.id.tvAdminPendingRecipeStatus);
        tvMemberCountTitle = findViewById(R.id.tvMemberCountTitle);
        tvMemberCount = findViewById(R.id.tvMemberCount);

        btnDevPage = findViewById(R.id.btnDevPage);
        btnModeratePage = findViewById(R.id.btnModeratePage);

        // go to profile page
        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AdminActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        // go to home page
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AdminActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });

        // enter dev mode via PUT request
        rbDevMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // make PUT request to change current admin into dev mode
                changeAdminMode(adminModeUrl, adminName, "devMode");
            }
        });

        // enter moderate mode via PUT request
        rbModMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // make PUT request to change current admin into mod mode
                changeAdminMode(adminModeUrl, adminName, "modMode");
            }
        });

        // enter normal mode via PUT request
        rbNormMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // make PUT request to change current admin into normal mode
                changeAdminMode(adminModeUrl, adminName, "normMode");
            }
        });

        // go to dev page
        btnDevPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AdminActivity.this, AdminDevActivity.class);
                startActivity(intent);
            }
        });

        // go to moderate page
        btnModeratePage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AdminActivity.this, AdminModerateActivity.class);
                startActivity(intent);
            }
        });

        // if admin is already in one of the modes, re-initialize appropriate elements
        if (rbDevMode.isChecked()) {
            rbDevMode.performClick();
        }
        else if (rbModMode.isChecked()) {
            rbModMode.performClick();
        }
        else if (rbNormMode.isChecked()) {
            rbNormMode.performClick();
        }
    }

    /**
     * Changes the currently displayed elements on the page based on the current admin mode.
     */
    private void displayModeElements() {
        // disable any previous elements
        lvAdminLog.setVisibility(View.INVISIBLE);
        tvAdminLogTitle.setVisibility(View.INVISIBLE);

        tvCurrentROTW.setVisibility(View.INVISIBLE);
        tvROTWTitle.setVisibility(View.INVISIBLE);
        tvCurrentTheme.setVisibility(View.INVISIBLE);
        tvCurrentThemeTitle.setVisibility(View.INVISIBLE);

        tvPendingRecipeTitle.setVisibility(View.INVISIBLE);
        tvPendingRecipeStatus.setVisibility(View.INVISIBLE);
        tvMemberCountTitle.setVisibility(View.INVISIBLE);
        tvMemberCount.setVisibility(View.INVISIBLE);

        btnDevPage.setVisibility(View.INVISIBLE);
        btnModeratePage.setVisibility(View.INVISIBLE);

        // display dev mode elements
        if (devModeEnabled) {
            tvCurrentTheme.setVisibility(View.VISIBLE);
            tvCurrentThemeTitle.setVisibility(View.VISIBLE);
            tvCurrentROTW.setVisibility(View.VISIBLE);
            tvROTWTitle.setVisibility(View.VISIBLE);
            btnDevPage.setVisibility(View.VISIBLE);

            // get current theme and edit tvCurrentTheme to display the current theme
            getGlobalTheme(getThemeUrl);

            // get current recipe of the week and edit tvCurrentROTW to display the current ROTW
            getRecipeOfTheWeek(getROTWUrl);
        }
        // display moderate mode elements
        if (modModeEnabled) {
            tvPendingRecipeTitle.setVisibility(View.VISIBLE);
            tvPendingRecipeStatus.setVisibility(View.VISIBLE);
            tvMemberCountTitle.setVisibility(View.VISIBLE);
            tvMemberCount.setVisibility(View.VISIBLE);
            btnModeratePage.setVisibility(View.VISIBLE);

            // check if there is a pending recipe and notify admin if there is one
            getPendingRecipe(getPendingRecipeUrl);

            // get member count and display on page
            getMembers(getMembersUrl);
        }
        // display normal mode elements
        if (normModeEnabled) {
            // populate admin log list and then display it
            getAdminLog(getLogUrl);
            lvAdminLog.setVisibility(View.VISIBLE);
            tvAdminLogTitle.setVisibility(View.VISIBLE);
        }
        // otherwise, clear out the logs if they are populated
        else if (!logStringList.isEmpty()) {
            logStringList.clear();
            logAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Gets the log of admin actions via GET, then displays the list of logs on response.
     * @param url the url associated with the GET admin log request
     */
    private void getAdminLog(String url) {
        RequestQueue queue = Volley.newRequestQueue(AdminActivity.this);

        JSONArray body = null;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, body,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        // populate log listview with retrieved admin log
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject log = (JSONObject) response.get(i);
                                String logString = (String) log.get("logString");
                                logStringList.add(0, logString);
                            } catch (JSONException e) {
                                Toast.makeText(AdminActivity.this, "Error displaying log: " + e, Toast.LENGTH_LONG).show();
                            }
                        }
                        // then initialize the log adapter with the populated list of logs
                        logAdapter = new ArrayAdapter(AdminActivity.this,
                                android.R.layout.simple_list_item_1, logStringList);
                        lvAdminLog.setAdapter(logAdapter);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(AdminActivity.this, "Error getting logs: " + error, Toast.LENGTH_LONG).show();
                    }
                });

        queue.add(request);
    }

    /**
     * Changes the admin's current mode via a PUT request. On response, the local flags for
     * the admin's mode are changed.
     * @param adminModeUrl the url associated with the admin mode PUT request
     * @param adminName the username of the admin that wants to change modes
     * @param requestedMode the desired mode that the admin wants to change to
     */
    private void changeAdminMode(String adminModeUrl, String adminName, String requestedMode) {
        RequestQueue queue = Volley.newRequestQueue(AdminActivity.this);

        // change url based on requested mode
        String url = adminModeUrl + requestedMode + "/" + adminName;

        JSONObject body = null;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // update locally stored mode flags
                        try {
                            devModeEnabled = (boolean) response.get("devMode");
                            modModeEnabled = (boolean) response.get("moderateMode");
                            // user is in dev mode
                            if (devModeEnabled) {
                                Toast.makeText(AdminActivity.this, "Dev mode enabled", Toast.LENGTH_SHORT).show();
                            }
                            // user is in mod mode
                            if (modModeEnabled) {
                                Toast.makeText(AdminActivity.this, "Mod mode enabled", Toast.LENGTH_SHORT).show();
                            }
                            // normal mode case
                            if (!devModeEnabled && !modModeEnabled) {
                                normModeEnabled = true;
                                Toast.makeText(AdminActivity.this, "Normal mode enabled", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                normModeEnabled = false;
                            }
                        } catch (JSONException e) {
                            Toast.makeText(AdminActivity.this, "Error changing mode: " + e, Toast.LENGTH_LONG).show();
                        }
                        // then, reflect changes on the admin page by displaying relevant UI elements for the mode
                        displayModeElements();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(AdminActivity.this, "Error changing mode: " + error, Toast.LENGTH_LONG).show();
                    }
                });

        queue.add(request);
    }

    /**
     * Tries to retrieve a pending recipe via GET request. If the returned recipe has a title,
     * the page is updated to signify that a pending recipe exists. Otherwise, it will signify that there
     * are no pending recipes.
     * @param url
     */
    private void getPendingRecipe(String url) {
        RequestQueue queue = Volley.newRequestQueue(AdminActivity.this);

        JSONObject body = null;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // if the pending recipe has a title (is valid), display "Yes", otherwise display "No"
                        if (!response.isNull("title")) {
                            tvPendingRecipeStatus.setText("Yes");
                        }
                        else {
                            tvPendingRecipeStatus.setText("No");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(AdminActivity.this, "Error checking pending recipe: " + error, Toast.LENGTH_LONG).show();
                    }
                });

        queue.add(request);
    }

    /**
     * Gets all members in the database via GET, then displays the number of members that exist.
     * @param url the url associated with the GET members request
     */
    private void getMembers(String url) {
        RequestQueue queue = Volley.newRequestQueue(AdminActivity.this);

        JSONArray body = null;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, body,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        // get number of members
                        int memberCount = response.length();

                        // display number of members
                        tvMemberCount.setText(Integer.toString(memberCount));
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(AdminActivity.this, "Failed to get members: " + error, Toast.LENGTH_LONG).show();
                    }
                });

        queue.add(request);
    }

    /**
     * Retrieves the recipe of the week via GET request. On response, the ROTW is displayed on the page.
     * @param url the url associated with the GET recipe of the week request
     */
    private void getRecipeOfTheWeek(String url) {
        RequestQueue queue = Volley.newRequestQueue(AdminActivity.this);

        JSONObject body = null;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // get the name of the ROTW and display it
                        try {
                            String rotwName = (String) response.get("title");
                            tvCurrentROTW.setText(rotwName);
                        } catch (JSONException e) {
                            Toast.makeText(AdminActivity.this, "Error displaying ROTW: " + e, Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(AdminActivity.this, "Failed to get ROTW: " + error, Toast.LENGTH_LONG).show();
                    }
                });

        queue.add(request);
    }

    /**
     * Based on a JSONObject that represents a theme, display the theme information on the page
     * @param theme the JSONObject that represents a theme
     */
    private void displayTheme(JSONObject theme) throws JSONException {
        String themeName = (String) theme.get("themeName");
        String primaryColor = (String) theme.get("primaryColor");
        String secondaryColor = (String) theme.get("secondaryColor");

        // display theme name
        tvCurrentTheme.setText(themeName);

        // edit text with theme colors
        int bgColor = Color.parseColor(primaryColor);
        int textColor = Color.parseColor(secondaryColor);

        tvCurrentTheme.setTextColor(textColor);
        tvCurrentTheme.setBackgroundColor(bgColor);
    }

    /**
     * Gets the global theme of the app as a String. On response, it displays the theme.
     * @param getThemeUrl the url associated with the theme GET request
     */
    private void getGlobalTheme(String getThemeUrl) {
        RequestQueue queue = Volley.newRequestQueue(AdminActivity.this);

        JSONObject body = null;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, getThemeUrl, body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // display theme and colors on page
                        try {
                            displayTheme(response);
                        } catch (JSONException e) {
                            Toast.makeText(AdminActivity.this, "Failed to display theme: " + e, Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(AdminActivity.this, "Failed to get theme: " + error, Toast.LENGTH_LONG).show();
                    }
                });

        queue.add(request);
    }
}
