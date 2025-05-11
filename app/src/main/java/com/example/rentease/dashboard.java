package com.example.rentease;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class dashboard extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    FrameLayout frameLayout;
    Toolbar toolbar;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize and setup toolbar with black theme
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Set toolbar title color to white for better visibility on black background
        toolbar.setTitleTextColor(Color.WHITE);

        bottomNavigationView = findViewById(R.id.bottomNavigationBar);
        frameLayout = findViewById(R.id.frame_layout);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                int item_id = item.getItemId();

                if(item_id == R.id.home){
                    loadFragment(new HomeFragment(),false);
                }  else if (item_id == R.id.properties) {
                    loadFragment(new Properties_Fragment(),false);
                } else {
                    loadFragment(new Profile_Fragment(),false);
                }
                return true;
            }
        });
        loadFragment(new HomeFragment(),true);
    }

    // Method to create menu in the toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.top_menu, menu);

        // Change only the logout menu item to red color
        MenuItem logoutItem = menu.findItem(R.id.action_logout);
        if (logoutItem != null) {
            SpannableString spanString = new SpannableString(logoutItem.getTitle().toString());
            spanString.setSpan(new ForegroundColorSpan(Color.RED), 0, spanString.length(), 0);
            logoutItem.setTitle(spanString);
        }

        // Keep settings menu item black (default color)
        // No need to change anything as it will remain the default color

        return true;
    }

    // Method to handle menu item clicks
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Handle settings action
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
            // Add your settings navigation code here
            return true;
        } else if (id == R.id.action_logout) {
            // Handle logout action
            performLogout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Method to perform logout
    private void performLogout() {
        // Show a toast message to indicate logout is in progress
        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();

        // Sign out from Firebase Authentication
        mAuth.signOut();

        // Check if user is signed out successfully
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User is now logged out
            Toast.makeText(this, "Logout successful", Toast.LENGTH_SHORT).show();

            // Redirect to login activity
            Intent intent = new Intent(dashboard.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Close current activity
        } else {
            // Logout failed
            Toast.makeText(this, "Logout failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFragment(Fragment fragment, boolean isAppInitialized){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if(isAppInitialized){
            fragmentTransaction.add(R.id.frame_layout,fragment);
        } else {
            fragmentTransaction.replace(R.id.frame_layout,fragment);
        }

        fragmentTransaction.commit();
    }

    // Optional: Check if user is logged in when activity starts
    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // No user is signed in, redirect to login
            Intent intent = new Intent(dashboard.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}