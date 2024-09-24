package com.hb216.recipewizard;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RatingBar;
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

import java.util.ArrayList;
import java.util.List;

public class ModerateMemberActivity extends AppCompatActivity {

    TextView tvMemberName, tvFullName;
    RatingBar rbRating;
    ListView lvMessageList;
    Button btnDeleteMessage, btnBan, btnBack;

    // list and adapter for messages from the specified chat member
    List<String> messageList = new ArrayList<String>();
    ArrayAdapter<String> messageAdapter;

    // list of message ids corresponding to messageList, so that we can make DELETE requests
    List<Integer> messageIdList = new ArrayList<Integer>();
    // id currently selected message
    long selectedMessageId = -1;
    // position in messageList of currently selected message
    int messageDeletionIndex = -1;

    // name of the admin that is checking info about the member
    String adminName;

    // email of the member, used for sending a ban notification email
    String memberEmail;

    // urls for Volley requests
    String baseUrl =  "http://10.0.2.2:8080";
        // "http://coms-309-006.class.las.iastate.edu:8080";
    String getMemberUrl = baseUrl + "/member/";
    String getMessagesUrl = baseUrl + "/allMessages/";
    String deleteMessageUrl = baseUrl + "/myMessages/";
    String deleteMemberUrl = baseUrl + "/banHammer/";
    String getAdminUrl = baseUrl + "/member/";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moderatemember);

        tvMemberName = findViewById(R.id.tvModerateMemberName);
        tvFullName = findViewById(R.id.tvModerateMemberFullname);
        rbRating = findViewById(R.id.rbModerateMemberRating);
        lvMessageList = findViewById(R.id.lvModerateMessageList);
        btnDeleteMessage = findViewById(R.id.btnModerateDeleteMessage);
        btnBan = findViewById(R.id.btnBanMember);
        btnBack = findViewById(R.id.btnBackToModerate);

        // get locally stored username
        SharedPreferences sharedPref = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        adminName = sharedPref.getString("username", "MRTA");

        // get member username that we were given via extra
        String memberName = getIntent().getStringExtra("username");

        // update urls
        getMemberUrl += memberName;
        getMessagesUrl += memberName + "/" + adminName;
        deleteMessageUrl += memberName + "/";
        deleteMemberUrl += adminName + "/" + memberName;
        getAdminUrl += adminName;

        // get member info and update display
        getMember(getMemberUrl);

        // get member messages and update display
        getMessages(getMessagesUrl);

        // listener for clicking on individual messages in the message list
        lvMessageList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // un-highlight any previously selected messages
                for (int j = 0; j < messageList.size(); j++) {
                    lvMessageList.getChildAt(j).setBackgroundColor(Color.WHITE);
                }

                // highlight the selected message
                view.setBackgroundColor(Color.YELLOW);

                // get the id of the selected message and store it
                selectedMessageId = messageIdList.get(i);

                // store position of message that should be deleted
                messageDeletionIndex = i;
            }
        });

        // delete a message that was clicked on in the message list
        btnDeleteMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // check if an id was selected
                if (selectedMessageId != -1) {
                    // specify that we want to delete this message
                    String url = deleteMessageUrl += selectedMessageId;
                    deleteMessage(url);
                }
            }
        });

        // delete/ban the specified member
        btnBan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // prompt admin with a reason for the ban before continuing, then delete the member and exit
                displayBanPrompt();
            }
        });

        // exit page back to moderate page
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ModerateMemberActivity.this, AdminModerateActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Displays a dialog prompt when the admin tries to the ban the member. The admin should
     * enter a reason for the ban before confirming.
     */
    private void displayBanPrompt() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ModerateMemberActivity.this);
        builder.setCancelable(true);
        builder.setTitle("Ban Reason");
        builder.setMessage("Please explain why you are banning this user:");

        // text input for ban reason
        EditText reasonInput = new EditText(ModerateMemberActivity.this);
        reasonInput.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(reasonInput);

        // on confirmation, get ban reason input and delete the member
        builder.setPositiveButton("Confirm ban",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // get ban reason
                        String input = reasonInput.getText().toString();
                        // construct an email notifying the member that they were banned
                        sendBanEmail(input);

                        // send DELETE request for member
                        deleteMember(deleteMemberUrl);
                    }
                });

        // exit option to cancel ban
        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

        builder.show();
    }

    /**
     * Helps compose an email notifying the member that they were banned. Prompts user to open up any available
     * email client that has a valid account.
     * @param input String containing the admin's reason why the member was banned
     */
    private void sendBanEmail(String input) {
        // create intent to send admin to email app
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/plain");

        String banReason = "You were banned from Recipe Wizard for the following reason: \n" + input;

        // must pass in array of recipients to EXTRA_EMAIL below
        String[] recipientList = new String[1];
        recipientList[0] = memberEmail;

        // send email info like recipient, subject, and body
        emailIntent.putExtra(Intent.EXTRA_EMAIL, recipientList);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Ban from Recipe Wizard");
        emailIntent.putExtra(Intent.EXTRA_TEXT, banReason);

        // exit back to moderate page first
        Intent intent = new Intent(ModerateMemberActivity.this, AdminModerateActivity.class);
        startActivity(intent);

        // make admin choose an email app to send the ban email with
        startActivity(Intent.createChooser(emailIntent, "Send ban notification email"));
    }

    /**
     * Uses a PUT request to ban/remove a member entirely. Exits the page on response.
     * @param url the url associated with the PUT member request
     */
    private void deleteMember(String url) {
        RequestQueue queue = Volley.newRequestQueue(ModerateMemberActivity.this);

        StringRequest request = new StringRequest(Request.Method.PUT, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    System.out.println(response);
                    Toast.makeText(ModerateMemberActivity.this, "Member banned", Toast.LENGTH_SHORT).show();
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(ModerateMemberActivity.this, "Error banning member: " + error, Toast.LENGTH_LONG).show();
                }
            });

        queue.add(request);
    }

    /**
     * Deletes a specific message via DELETE request
     * @param url the url associated with the DELETE message request
     */
    private void deleteMessage(String url) {
        RequestQueue queue = Volley.newRequestQueue(ModerateMemberActivity.this);

        StringRequest request = new StringRequest(Request.Method.DELETE, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // remove the specified message from the adapter and update the display
                    messageList.remove(messageDeletionIndex);
                    messageAdapter.notifyDataSetChanged();
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(ModerateMemberActivity.this, "Error deleting message: " + error, Toast.LENGTH_LONG).show();
                }
            });

        queue.add(request);
    }

    /**
     * Retrieves all messages from a specific member via GET request
     * and initializes the message adapter/listview with the messages.
     * @param url the url associated with the GET messages request
     */
    private void getMessages(String url) {
        RequestQueue queue = Volley.newRequestQueue(ModerateMemberActivity.this);

        JSONArray body = null;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, body,
            new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    // iterate through all messages
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            // get message contents and add it to the list
                            JSONObject message = (JSONObject) response.get(i);
                            String messageText = (String) message.get("content");
                            messageList.add(messageText);

                            // also, get the associated message id in case we want to delete this message
                            int id = (int) message.get("id");
                            messageIdList.add(id);
                        }
                        catch (JSONException e) {
                            Toast.makeText(ModerateMemberActivity.this, "Error viewing message: " + e, Toast.LENGTH_LONG).show();
                        }
                    }
                    // set adapter with list of messages
                    messageAdapter = new ArrayAdapter<>(ModerateMemberActivity.this,
                            android.R.layout.simple_list_item_1, messageList);
                    lvMessageList.setAdapter(messageAdapter);
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(ModerateMemberActivity.this, "Failed to get messages: " + error, Toast.LENGTH_LONG).show();
                }
            });

        queue.add(request);
    }

    /**
     * Given a JSONObject that represents a member, update the display elements on the page appropriately
     * @param member the JSONObject that contains info about a specific member
     * @throws JSONException
     */
    private void displayMemberInfo(JSONObject member) throws JSONException {
        // get member's username
        String username = (String) member.get("username");

        // get member's first and last name
        String firstName = "[first name]";
        String lastName = "[last name]";
        // check if either of the optional name fields are null
        if (!member.get("firstName").toString().equals("null")) {
            firstName = (String) member.get("firstName");
        }
        if (!member.get("lastName").toString().equals("null")) {
            lastName = (String) member.get("lastName");
        }

        String fullName = firstName + " " + lastName;

        // display username and potential first/last names
        tvMemberName.setText(username);
        tvFullName.setText(fullName);

        // get member rating and display
        int rating = (int) member.get("memberRating");
        rbRating.setRating((float) rating);
    }

    /**
     * Retrieves a Member object via GET request and updates the page elements with the info in the object.
     * @param url the url associated with the GET member request
     */
    private void getMember(String url) {
        RequestQueue queue = Volley.newRequestQueue(ModerateMemberActivity.this);

        JSONObject body = null;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, body,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    // update page with member info
                    try {
                        // store email info just in case
                        memberEmail = (String) response.get("email");
                        // update display of page
                        displayMemberInfo(response);
                    } catch (JSONException e) {
                        Toast.makeText(ModerateMemberActivity.this, "Error displaying info: " + e, Toast.LENGTH_LONG).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(ModerateMemberActivity.this, "Error getting info: " + error, Toast.LENGTH_LONG).show();
                }
            });

        queue.add(request);
    }
}
