package com.example.rentease;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class dashboard extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    FrameLayout frameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        bottomNavigationView = findViewById(R.id.bottomNavigationBar);
        frameLayout = findViewById(R.id.frame_layout);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                int item_id = item.getItemId();

                if(item_id == R.id.home){
                    loadFragment(new HomeFragment(),false);
                } else if (item_id == R.id.favourite){
                    loadFragment(new FavouritesFragment(),false);
                } else if (item_id == R.id.properties) {
                    loadFragment(new Properties_Fragment(),false);
                } else {
                    loadFragment(new Profile_Fragment(),false);
                }
                return true;
            }
        });
        loadFragment(new HomeFragment(),true);
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
}