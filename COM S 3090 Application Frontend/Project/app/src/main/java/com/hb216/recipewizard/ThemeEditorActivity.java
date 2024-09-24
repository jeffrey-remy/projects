package com.hb216.recipewizard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

public class ThemeEditorActivity extends AppCompatActivity {
    // shared elements between both creating and editing themes
    TextView tvEditorTitle, tvThemePreview;
    EditText etBackgroundHexcode, etTextHexcode;
    // empty textviews that display colors
    TextView backgroundColor, textColor;
    Button btnCancel;

    // elements only displayed when creating themes
    EditText etThemeName;
    Button btnCreateTheme, btnCreateApplyTheme;

    // elements only displayed when editing existing themes
    TextView tvThemeName;
    Button btnApplyTheme, btnDeleteTheme;

    // flag to determine whether to display elements for editing or creating themes
    boolean inCreatingDisplay;
    // The name of the theme that we are currently changing. Empty if we are creating a new theme.
    String themeToChange;

    String adminName;

    String baseUrl = //"http://10.0.2.2:8080";
           "http://coms-309-006.class.las.iastate.edu:8080";
    // Volley request urls
    String postThemeUrl = baseUrl + "/Theme";
    String setGlobalThemeUrl = baseUrl + "/Theme/";
    String changeThemeUrl = baseUrl + "/Theme/";
    String getGlobalThemeUrl = baseUrl + "/Theme";
    String deleteThemeUrl = baseUrl + "/Theme/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_themeeditor);

        // get locally stored username
        SharedPreferences sharedPref = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        adminName = sharedPref.getString("username", "TestingAdmin");

        // update urls
        setGlobalThemeUrl += adminName + "/";
        changeThemeUrl += adminName;

        // get the specified theme that we are editing
        themeToChange = getIntent().getStringExtra("themeToChange");

        // if it is empty, then we are creating a new theme
        inCreatingDisplay = themeToChange.isEmpty();

        // elements that are always displayed
        tvEditorTitle = findViewById(R.id.tvThemeEditorTitle);
        etBackgroundHexcode = findViewById(R.id.etBackgroundHexcode);
        etTextHexcode = findViewById(R.id.etTextHexcode);
        backgroundColor = findViewById(R.id.themeEditBackgroundColor);
        textColor = findViewById(R.id.themeEditTextColor);
        tvThemePreview = findViewById(R.id.tvThemeColorPreview);
        btnCancel = findViewById(R.id.btnEditorCancel);

        // elements for creating themes
        etThemeName = findViewById(R.id.etCreateThemeName);
        btnCreateTheme = findViewById(R.id.btnEditorCreateTheme);
        btnCreateApplyTheme = findViewById(R.id.btnEditorCreateApplyTheme);

        // elements for changing themes
        tvThemeName = findViewById(R.id.tvChangeThemeName);
        btnApplyTheme = findViewById(R.id.btnEditorApplyTheme);
        btnDeleteTheme = findViewById(R.id.btnEditorDeleteTheme);

        // listeners that should always be active
        etBackgroundHexcode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                editColorPreviews(etBackgroundHexcode, backgroundColor);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                editColorPreviews(etBackgroundHexcode, backgroundColor);
            }
        });

        etTextHexcode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                editColorPreviews(etTextHexcode, textColor);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                editColorPreviews(etTextHexcode, textColor);
            }
        });

        // go back to dev page
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ThemeEditorActivity.this, AdminDevActivity.class);
                startActivity(intent);
            }
        });

        // elements and corresponding listeners that only show when we are creating a theme
        if (inCreatingDisplay) {
            // set title
            tvEditorTitle.setText("Create New Theme");

            // create a theme with POST request based on current input
            btnCreateTheme.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    useInputForNewTheme(false);
                }
            });

            // create a theme with POST, then set that theme as the global theme
            btnCreateApplyTheme.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    useInputForNewTheme(true);
                }
            });
        }
        // elements and corresponding listeners that only show when we are editing an existing theme
        else {
            // toggle visibility of elements
            showEditingElements();

            // set title
            tvEditorTitle.setText("Change Theme");
            // set name of theme we are changing
            tvThemeName.setText(themeToChange);

            // set color previews based on the given theme we are updating
            getGlobalTheme(getGlobalThemeUrl);

            // apply changes made to this theme via PUT request
            btnApplyTheme.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // get user input
                    String backgroundColorString = etBackgroundHexcode.getText().toString();
                    String textColorString = etTextHexcode.getText().toString();

                    // check if color inputs are valid
                    if (isValidHexcode(backgroundColorString) && isValidHexcode(textColorString)) {
                        // then, make a PUT request with the new info to update the selected theme
                        changeTheme(changeThemeUrl, backgroundColorString, textColorString);
                    }
                    // notify user that their color inputs are invalid
                    else {
                        Toast.makeText(ThemeEditorActivity.this, "Invalid hexcode input", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // delete this theme
            btnDeleteTheme.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // delete the theme we are currently editing
                    deleteTheme(deleteThemeUrl + themeToChange + "/" + adminName);
                }
            });
        }
    }

    /**
     * Deletes the theme that the admin is currently editing via DELETE request.
     * @param url the url associated with the theme DELETE request
     */
    private void deleteTheme(String url) {
        RequestQueue queue = Volley.newRequestQueue(ThemeEditorActivity.this);

        JSONArray body = null;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.DELETE, url, body,
            new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    Toast.makeText(ThemeEditorActivity.this, "Deleted theme", Toast.LENGTH_SHORT).show();
                    // go back to dev page
                    Intent intent = new Intent(ThemeEditorActivity.this, AdminDevActivity.class);
                    startActivity(intent);
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(ThemeEditorActivity.this, "Error deleting theme: " + error, Toast.LENGTH_LONG).show();
                }
            });

        queue.add(request);
    }

    /**
     * Gets the global theme of the app as a String. On response, it displays the theme.
     * @param getThemeUrl the url associated with the theme GET request
     */
    private void getGlobalTheme(String getThemeUrl) {
        RequestQueue queue = Volley.newRequestQueue(ThemeEditorActivity.this);

        JSONObject body = null;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, getThemeUrl, body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // update color text inputs
                        String backgroundHexcode;
                        String textHexcode;
                        try {
                            backgroundHexcode = (String) response.get("primaryColor");
                            textHexcode = (String) response.get("secondaryColor");
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                        etBackgroundHexcode.setText(backgroundHexcode);
                        etTextHexcode.setText(textHexcode);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ThemeEditorActivity.this, "Failed to get theme: " + error, Toast.LENGTH_LONG).show();
                    }
                });

        queue.add(request);
    }

    /**
     * Changes a theme with new info via PUT request
     * @param url the url associated with the theme PUT request
     * @param newBackgroundHexcode the new background color hexcode for the theme
     * @param newTextHexcode the new text color hexcode for the theme
     */
    private void changeTheme(String url, String newBackgroundHexcode, String newTextHexcode) {
        RequestQueue queue = Volley.newRequestQueue(ThemeEditorActivity.this);

        String bodyString = "{\"themeName\": \"" + themeToChange + "\", "
                + "\"primaryColor\": \"" + newBackgroundHexcode + "\", "
                + "\"secondaryColor\": \"" + newTextHexcode + "\"}";

        JSONObject body;
        try {
            body = new JSONObject(bodyString);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, body,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Toast.makeText(ThemeEditorActivity.this, "Updated theme", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ThemeEditorActivity.this, AdminDevActivity.class);
                    startActivity(intent);
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(ThemeEditorActivity.this, "Failed to update theme: " + error, Toast.LENGTH_LONG).show();
                }
            });

        queue.add(request);
    }

    /**
     * Changes the global theme of the app via a PUT request. On response, the new global theme is displayed.
     * @param putThemeUrl the url associated with the theme PUT request
     */
    private void changeGlobalTheme(String putThemeUrl) {
        RequestQueue queue = Volley.newRequestQueue(ThemeEditorActivity.this);

        StringRequest request = new StringRequest(Request.Method.PUT, putThemeUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(ThemeEditorActivity.this, "Theme changed", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ThemeEditorActivity.this, AdminDevActivity.class);
                        startActivity(intent);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ThemeEditorActivity.this, "Failed to change theme: " + error, Toast.LENGTH_LONG).show();
                    }
                });

        queue.add(request);
    }

    /**
     * Creates a new theme via POST request, and possibly sets it as the global theme.
     * @param url the url associated with the theme POST request
     * @param themeName the name of the new theme
     * @param backgroundHexcode the hexcode of the background color of the theme
     * @param textHexcode the hexcode of the text color of the theme
     * @param setAsGlobal flag that determines whether the created theme should be set as the global theme
     */
    private void createTheme(String url, String themeName, String backgroundHexcode, String textHexcode, boolean setAsGlobal) {
        RequestQueue queue = Volley.newRequestQueue(ThemeEditorActivity.this);

        // construct body with given info
        String bodyString = "{\"themeName\": \"" + themeName + "\", "
                     + "\"primaryColor\": \"" + backgroundHexcode + "\", "
                     + "\"secondaryColor\": \"" + textHexcode + "\"}";

        JSONObject body;
        try {
            body = new JSONObject(bodyString);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    // send user back to dev page
                    if (setAsGlobal) {
                        changeGlobalTheme(setGlobalThemeUrl + themeName);
                    }
                    else {
                        Intent intent = new Intent(ThemeEditorActivity.this, AdminDevActivity.class);
                        startActivity(intent);
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(ThemeEditorActivity.this, "Error making theme: " + error, Toast.LENGTH_LONG).show();
                }
            });

        queue.add(request);
    }

    /**
     * Creates a theme based on the current input on the theme editor page.
     * @param setAsGlobal flag to determine if the new theme should be set as the global theme after being created
     */
    private void useInputForNewTheme(boolean setAsGlobal) {
        // get user input
        String newName = etThemeName.getText().toString();
        String backgroundColorString = etBackgroundHexcode.getText().toString();
        String textColorString = etTextHexcode.getText().toString();

        // check if color inputs are valid
        if (isValidHexcode(backgroundColorString) && isValidHexcode(textColorString)) {
            // then, make a POST request with the current info, and possibly set the new theme as the global theme
            createTheme(postThemeUrl, newName, backgroundColorString, textColorString, setAsGlobal);
        }
        // notify user that their color inputs are invalid
        else {
            Toast.makeText(ThemeEditorActivity.this, "Invalid hexcode input", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Hide elements for creating themes, and show elements for changing themes.
     */
    private void showEditingElements() {
        // disable visibility of elements for creating themes
        etThemeName.setVisibility(View.INVISIBLE);
        btnCreateTheme.setVisibility(View.INVISIBLE);
        btnCreateApplyTheme.setVisibility(View.INVISIBLE);

        // enable visibility of elements for changing themes
        tvThemeName.setVisibility(View.VISIBLE);
        btnApplyTheme.setVisibility(View.VISIBLE);
        btnDeleteTheme.setVisibility(View.VISIBLE);
    }

    /**
     * Tries to change the color previews on the page to reflect new input. If input is invalid,
     * an error symbol is displayed instead of the color.
     * @param textInput the EditText that contains the user input for the new color
     * @param colorView the TextView that displays the color entered in textInput
     */
    private void editColorPreviews(EditText textInput, TextView colorView) {
        // get the input that the user entered
        String hexcodeInput = textInput.getText().toString();

        // if input is a valid hexcode, display it on the page
        if (isValidHexcode(hexcodeInput)) {
            int inputColor = Color.parseColor(hexcodeInput);
            colorView.setBackgroundColor(inputColor);
            colorView.setText("");

            // then, change the text preview with the new color info
            int bgColor, txtColor;
            ColorDrawable drawable = (ColorDrawable) backgroundColor.getBackground();
            if (drawable == null) {
                // make background color of preview white if it is not instantiated
                bgColor = 0;
            } else {
                bgColor = drawable.getColor();
            }

            drawable = (ColorDrawable) textColor.getBackground();
            if (drawable == null) {
                // make text color of preview white if it is not instantiated
                txtColor = 0;
            } else {
                txtColor = drawable.getColor();
            }

            tvThemePreview.setBackgroundColor(bgColor);
            tvThemePreview.setTextColor(txtColor);
        }
        // otherwise, indicate that the hexcode is invalid
        else {
            colorView.setText("?");
            colorView.setBackgroundColor(0);
        }
    }

    /**
     * Check if a given string is formatted in hexcode notation (ex. #00ff00)
     * @param input the given String
     * @return true if input matches a hexcode format, false otherwise
     */
    private boolean isValidHexcode(String input) {
        // regex pattern that corresponds to a valid hexcode formatted String
        String hexcodeMatch = "^#[0-9a-f]+";

        // check string against the hexcode regex
        if (input.length() == 7) {
            return input.matches(hexcodeMatch);
        }

        return false;
    }
}
