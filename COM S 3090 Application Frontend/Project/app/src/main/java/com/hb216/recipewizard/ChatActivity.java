package com.hb216.recipewizard;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.handshake.ServerHandshake;

import java.util.ArrayList;

/**
 * Displays a chat page that allows the user to send new messages to other users as well as
 * re-visit previously messaged users.
 *
 * @author Jeffrey Remy
 */
public class ChatActivity extends AppCompatActivity implements WebSocketListener {

    /** The username of the current user. */
    String username;

    /** The url of the WebSocket used for the chat page. */
    private String websocketUrl = //"ws://10.0.2.2:8080/chat/";
                    "ws://coms-309-006.class.las.iastate.edu:8080/chat/";

    /** A field for entering a new username that the user wants to talk to. */
    EditText etUsername;
    /** A field for entering a message to send to the websocket. */
    EditText etMessage;

    /** A button for navigating to another page. */
    Button  btnBack, btnProfile;
    /** Changes the chat display to show a new conversation with the username entered in etUsername. */
    Button btnConnect;
    /** Sends a message entered in etMessage to the websocket. */
    Button btnSendMessage;

    /** Displays the chat history. */
    TextView tvMsgDisplay;
    /** Displays the username that the user is currently talking to. */
    TextView tvTalkingToName;

    /** Allows for scrolling through the chat history. */
    ScrollView svMessages;
    /** Used to display usernames in the chat menu dynamically. */
    RecyclerView rvChatMenu;

    /** The username of the other user that our current user is currently talking to. */
    String currentlyTalkingTo;

    /**
     * Contains the entire chat history of the websocket, that can be filtered according to the current
     * username and currentlyTalkingTo username.
     */
    String fullHistory = "";

    /** Stores any usernames that the user has mentioned or been mentioned by in chat history. */
    ArrayList<String> nameHistory = new ArrayList<String>();

    /** The ChatAdapter that dynamically displays usernames that this user has talked to before. */
    ChatAdapter adapter = new ChatAdapter(nameHistory);

    /** Flag that indicates whether the menu or actual chat history should be displayed currently. */
    boolean inMenu = true;

    /**
     * Initializes the chat page display, setting click listeners for navigation and chat menu elements.
     * The WebSocket connection is established, and the chat menu containing previously talked to usernames
     * is initialized and displayed via the ChatAdapter.
     * @param savedInstanceState a saved instance of this page that can be re-created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // get locally stored username
        SharedPreferences sharedPref = getSharedPreferences("Preferences", Context.MODE_PRIVATE);
        username = sharedPref.getString("username", "JEFF");
        // update websocket url
        websocketUrl += username;

        etMessage = findViewById(R.id.etSendMessageChat);
        btnSendMessage = findViewById(R.id.btnSendMessageChat);
        tvMsgDisplay = findViewById(R.id.tvChatMessageDisplay);
        svMessages = findViewById(R.id.svChat);
        etUsername = findViewById(R.id.etUsernameChat);
        btnConnect = findViewById(R.id.btnConnectChat);
        rvChatMenu = findViewById(R.id.rvChatMenu);
        btnBack = findViewById(R.id.btnBackToChatMenu);
        btnProfile = findViewById(R.id.btnProfileFromChat);
        tvTalkingToName = findViewById(R.id.tvChatNameTalkingTo);

        // automatically create connection to websocket with user's stored username
        // Establish WebSocket connection and set listener
        WebSocketManager.getInstance().connectWebSocket(websocketUrl);
        WebSocketManager.getInstance().setWebSocketListener(ChatActivity.this);

        // assign adapter for displaying names in chat menu
        rvChatMenu.setAdapter(adapter);
        // layout manager to position items
        rvChatMenu.setLayoutManager(new LinearLayoutManager(this));

        // filter messages to only those of a certain username
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // update chat display to be a conversation with entered username
                changeChatDisplay();

                // display chat history page
                inMenu = false;
                switchBetweenDisplays();
            }
        });

        // send message to websocket
        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    // insert currentlyTalkingTo username before message to send DM to that specific person
                    String fullMessage = "@" + currentlyTalkingTo + " " + etMessage.getText().toString();

                    // send message
                    WebSocketManager.getInstance().sendMessage(fullMessage);
                } catch (Exception e) {
                    Log.d("ExceptionSendMessage:", e.getMessage().toString());
                }

                // TODO: Fix bug - After a certain size of chat history, scrollview no longer scrolls to the bottom
                // scroll to the bottom to display new message
                svMessages.scrollTo(0, svMessages.getBottom());
            }
        });

        // set on click listener for adapter (when an individual item is clicked)
        adapter.setOnItemClickListener(new ChatAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                // get the username that the user wants to talk to
                String username = nameHistory.get(position);

                // retrieve messages pertaining to the selected user
                etUsername.setText(username);
                changeChatDisplay();
                etUsername.setText("");

                // display chat history page
                inMenu = false;
                switchBetweenDisplays();
            }
        });

        // go back to menu from chat history page (toggle displays)
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // display chat menu
                inMenu = true;
                switchBetweenDisplays();
            }
        });

        // go to profile page
        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChatActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });
    }


    /**
     * Receive a message from the chat WebSocket. Upon receiving a message, the message
     * is edited and stored before being displayed on the screen.
     * @param message The received WebSocket message.
     */
    @Override
    public void onWebSocketMessage(String message) {
        runOnUiThread(() -> {
            // add message to history
            fullHistory += "\n" + message;

            // check if message mentions the user, add to name history if so
            addToNameHistory(message);

            // re-initialize the names in the main chat menu to reflect any new message-senders
            initializeMenuNames();

            // check and edit message appropriately
            String editedMessage = checkAndTrimMessage(message);
            // if new message is not empty, then it is a new valid message
            if (!editedMessage.isEmpty()) {
                // get currently displayed text
                String currentDisplay = tvMsgDisplay.getText().toString();
                // update display
                tvMsgDisplay.setText(currentDisplay + "\n" + editedMessage);
            }

            // scroll to the bottom to display new message
            svMessages.smoothScrollTo(0, svMessages.getBottom());
        });
    }

    /**
     * Takes action when the WebSocket closes. Upon closure, a message is displayed stating
     * why the WebSocket closed.
     * @param code   The status code indicating the reason for closure.
     * @param reason A human-readable explanation for the closure.
     * @param remote Indicates whether the closure was initiated by the remote endpoint.
     */
    @Override
    public void onWebSocketClose(int code, String reason, boolean remote) {
        // explain from where the server was closed
        String closedBy = remote ? "server" : "local";
        runOnUiThread(() -> {
            // notify user that the connection was closed and for what reason
            String s = tvMsgDisplay.getText().toString();
            tvMsgDisplay.setText(s + "---\nconnection closed by " + closedBy + "\nreason: " + reason);
        });
    }

    /**
     * Takes action when the WebSocket is opened.
     * @param handshakedata Information about the server handshake.
     */
    @Override
    public void onWebSocketOpen(ServerHandshake handshakedata) {}

    /**
     * Takes action when the WebSocket experiences an error. The error message is displayed when it occurs.
     * @param ex The exception that describes the error.
     */
    @Override
    public void onWebSocketError(Exception ex) {
        Toast.makeText(ChatActivity.this, "Websocket error: " + ex.toString(), Toast.LENGTH_SHORT).show();
    }

    // trims a given message by checking if it is a message sent between the current user and currentlyTalkingTo user
    // remove elements like the @ mention from the message

    /**
     * Trims a given message by checking if it is a message sent between the current user and currentlyTalkingTo user.
     * It removes elements like the @ mention from the message.
     * @param message the String of a message to be trimmed
     * @return the given message String, with any elements like "@" or "[DM]" removed
     */
    public String checkAndTrimMessage(String message) {
        String mentionedUsername = username;

        // case 1: currentlyTalkingTo user sends a message to current user
        int messageSenderIndex = message.indexOf(currentlyTalkingTo + ":");
        int mentionedNameIndex = message.indexOf("@" + username + " ");

        // if case 1 is invalid, try case 2: current user sends a message to currentlyTalkingTo user
        if (messageSenderIndex == -1 || mentionedNameIndex == -1) {
            messageSenderIndex = message.indexOf(username + ":");
            mentionedNameIndex = message.indexOf("@" + currentlyTalkingTo + " ");

            // update mentioned username appropriately
            mentionedUsername = currentlyTalkingTo;
        }

        // if both indices exist in the string, this is a valid message to display
        if (messageSenderIndex != -1 && mentionedNameIndex != -1) {
            // check if [DM] is in message
            int dmIndex = message.indexOf("[DM]");
            // edit message based on the mentioned username (depending on presence of [DM] in message)
            String editedMessage;
            if (dmIndex == 0) {
                // [DM] is in front of message, so remove it
                editedMessage = message.substring(5, mentionedNameIndex) + message.substring(mentionedNameIndex + 1 + mentionedUsername.length());
            }
            else {
                editedMessage = message.substring(0, mentionedNameIndex) + message.substring(mentionedNameIndex + 1 + mentionedUsername.length());
            }
            // return new trimmed message
            return editedMessage;
        }

        // if message is not valid, return empty string
        return "";
    }

    /**
     * Initialize names in the chat menu based on the initial history of the WebSocket's messages.
     */
    public void initializeMenuNames() {
        String[] messages = fullHistory.split("\n");

        for (String message : messages) {
            // check if names in message are new and store them if so
            if (message.contains(":")) {
                addToNameHistory(message);
            }
        }
    }

    /**
     * Adds a username to nameHistory if the given message mentions the user or is mentioned by the user and has not
     * been seen in previous messages. Updates the chat menu display to show the new username.
     * @param message the message String
     */
    public void addToNameHistory(String message) {
        int colonIndex = message.indexOf(":");
        int dmIndex = message.indexOf("[DM]");
        int mentionStartIndex = message.indexOf("@");
        System.out.println("Message: " + message);
        if (colonIndex != -1 && mentionStartIndex != -1) {
            // get sender name
            String sender;
            // remove [DM] that appears at the start of messages if it is there
            if (dmIndex == 0) {
                sender = message.substring(5, colonIndex);
            }
            else {
                sender = message.substring(0, colonIndex);
            }

            int mentionEndIndex = message.indexOf(" ", mentionStartIndex); // find end of mentioned name in string
            // get mentioned name (from right after @ to the next space)
            String mentioned = message.substring(mentionStartIndex + 1, mentionEndIndex);


            // check if sender username is new, then add it to the nameHistory if so
            if (mentioned.equals(username) && !nameHistory.contains(sender)) {
                nameHistory.add(sender);
                // notify the adapter that an item was inserted at last position
                adapter.notifyItemInserted(nameHistory.size() - 1);
            }

            // check if mentioned username is new, then add it to the nameHistory if so
            if (sender.equals(username) && !nameHistory.contains(mentioned)) {
                nameHistory.add(mentioned);
                // notify the adapter that an item was inserted at last position
                adapter.notifyItemInserted(nameHistory.size() - 1);
            }
        }
    }

    /**
     * Updates the display to show chat messages where the user has talked to the currentlyTalkingTo username.
     */
    public void changeChatDisplay() {
        // check if new username is different than the current one before carrying out anything
        if (currentlyTalkingTo == null || !currentlyTalkingTo.equals(etUsername.getText().toString())) {
            // change who we are currently talking to
            currentlyTalkingTo = etUsername.getText().toString();

            // wipe message display
            tvMsgDisplay.setText("");

            // split the current chat history into separate messages (separated by newlines)
            String[] messages = fullHistory.split("\n");

            // chat history to be displayed
            String displayHistory = "";

            // go through the message history and only include those where the current user
            // and the currentlyTalkingTo user are mentioning each other
            for (String message : messages) {
                // check if message is relevant and trim it down it if so
                String editedMessage = checkAndTrimMessage(message);
                // if new message is not empty, then it is a new valid message
                if (!editedMessage.isEmpty()) {
                    displayHistory += "\n" + editedMessage;
                }

                // check if names in message are new and store them if so
                if (message.contains(":")) {
                    addToNameHistory(message);
                }
            }

            // get currently displayed text
            String currentDisplay = tvMsgDisplay.getText().toString();
            // update display with new history
            tvMsgDisplay.setText(currentDisplay + displayHistory);
        }
        // scroll to the bottom to display new history
        svMessages.scrollTo(0, svMessages.getBottom());
    }

    /**
     * Switches between the chat menu that displays usernames and the actual chat messages with a certain username.
     * Accomplishes this by toggling visibility of elements on and off.
     */
    public void switchBetweenDisplays() {
        // menu -> chat case
        if (!inMenu) {
            // disable menu elements
            rvChatMenu.setVisibility(View.INVISIBLE);
            etUsername.setVisibility(View.INVISIBLE);
            btnConnect.setVisibility(View.INVISIBLE);

            // enable chat elements
            btnSendMessage.setVisibility(View.VISIBLE);
            etMessage.setVisibility(View.VISIBLE);
            svMessages.setVisibility(View.VISIBLE);
            btnBack.setVisibility(View.VISIBLE);
            tvTalkingToName.setVisibility(View.VISIBLE);

            // set currently talking to name in display
            tvTalkingToName.setText(currentlyTalkingTo);
        }
        // chat -> menu case
        else {
            // disable chat elements
            btnSendMessage.setVisibility(View.INVISIBLE);
            etMessage.setVisibility(View.INVISIBLE);
            svMessages.setVisibility(View.INVISIBLE);
            btnBack.setVisibility(View.INVISIBLE);
            tvTalkingToName.setVisibility(View.INVISIBLE);

            // enable menu elements
            rvChatMenu.setVisibility(View.VISIBLE);
            etUsername.setVisibility(View.VISIBLE);
            btnConnect.setVisibility(View.VISIBLE);
        }
    }
}
