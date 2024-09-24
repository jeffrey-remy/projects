package com.hb216.recipewizard;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button btnLogin;
    Button btnAddRecipe;
    Button btnRegistration;
    Button btnAddIngredient;
    Button btnProfile;
    Button btnHome;
    Button btnViewRecipe;
    Button btnChat;
    Button btnPantry;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnLogin = findViewById(R.id.btnLoginPage);
        btnAddRecipe = findViewById(R.id.btnAddRecipePage);
        btnRegistration = findViewById(R.id.btnRegistrationPage);
        btnAddIngredient = findViewById(R.id.btnAddIngredientPage);
        btnProfile = findViewById(R.id.btnProfilePage);
        btnHome = findViewById(R.id.btnHomePage);
        btnViewRecipe = findViewById(R.id.btnViewRecipePage);
        btnChat = findViewById(R.id.btnChatPage);
        btnPantry = findViewById(R.id.pantryBtn);


        // go to Maddie's login page
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // go to Maddie's add recipe page
        btnAddRecipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
                startActivity(intent);
            }
        });

        // go to Jeffrey's registration page
        btnRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, RegistrationActivity.class);
                startActivity(intent);
            }
        });

        // go to Jeffrey's add ingredient page
        btnAddIngredient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddIngredientActivity.class);
                startActivity(intent);
            }
        });

        // go to Jeffrey's profile page
        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });

        // go to Maddie's home page
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });

        // go to Maddie's view recipe page
        btnViewRecipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ViewRecipeActivity.class);
                startActivity(intent);
            }
        });

        // go to Jeffrey's chat page
        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                startActivity(intent);
            }
        });

        // go to Maddie's pantry page
        btnPantry.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, PantryActivity.class);
                startActivity(intent);
            }
        }));
    }
}