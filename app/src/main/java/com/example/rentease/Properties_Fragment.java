package com.example.rentease;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SearchView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class Properties_Fragment extends Fragment {
    private static final String TAG = "Properties_Fragment";

    // UI Components
    private Button addButton;
    private ListView propertyListView;
    private TextView emptyView;
    private SearchView searchView;
    private ProgressDialog progressDialog;

    // Data
    private List<Property> propertyList;
    private MyPropertyAdapter adapter;

    // Firebase references
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_properties_, container, false);
        Log.d(TAG, "onCreateView: Initializing Properties Fragment");

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Get current user ID and verify it's not null
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
            Log.d(TAG, "Current user ID: " + currentUserId);
        } else {
            Log.e(TAG, "User is not logged in");
            Toast.makeText(getContext(), "Please log in to view your properties", Toast.LENGTH_LONG).show();
        }

        // Initialize views
        addButton = view.findViewById(R.id.add);
        propertyListView = view.findViewById(R.id.myPropertyListView);
        emptyView = view.findViewById(R.id.myPropertiesEmptyView);
        searchView = view.findViewById(R.id.myPropertiesSearchView); // Make sure to add this to your layout
        propertyList = new ArrayList<>();

        // Set empty view for ListView
        propertyListView.setEmptyView(emptyView);

        // Initialize adapter with proper context
        adapter = new MyPropertyAdapter(getContext(), propertyList);
        adapter.setFragmentReference(this);
        propertyListView.setAdapter(adapter);

        // Set item click listener
        propertyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Property selectedProperty = adapter.getItem(position);
                navigateToPropertyDetails(selectedProperty);
            }
        });

        // Set up search functionality
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    adapter.getFilter().filter(newText);
                    return true;
                }
            });
        }

        // Set click listener for add button
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Add button clicked");
                Intent intent = new Intent(getContext(), add_property.class);
                startActivity(intent);
            }
        });

        // Load properties from Firebase
        if (currentUserId != null) {
            loadUserProperties();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Refreshing property list");
        // Refresh property list when fragment resumes
        if (currentUserId != null) {
            loadUserProperties();
        }
    }

    private void loadUserProperties() {
        // Show loading indicator
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Loading your properties...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Reference to the user's properties
        DatabaseReference propertiesRef = mDatabase.child("properties").child(currentUserId);
        Log.d(TAG, "Loading user properties from: " + propertiesRef.toString());

        propertiesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                propertyList.clear();
                Log.d(TAG, "onDataChange: Properties node exists: " + dataSnapshot.exists());
                Log.d(TAG, "onDataChange: Found " + dataSnapshot.getChildrenCount() + " properties");

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Log.d(TAG, "Property key: " + snapshot.getKey());

                        Property property = snapshot.getValue(Property.class);
                        if (property != null) {
                            // Store the property ID for deletion
                            String propertyId = snapshot.getKey();
                            Log.d(TAG, "Loaded property: " + property.getPropertyName() + " with ID: " + propertyId);
                            property.setId(propertyId);
                            propertyList.add(property);
                        } else {
                            Log.e(TAG, "Failed to parse property at key: " + snapshot.getKey());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing property data: " + e.getMessage(), e);
                    }
                }

                // Update adapter and dismiss loading dialog
                adapter.updatePropertyList(propertyList);

                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                // Show/hide empty view
                if (propertyList.isEmpty()) {
                    Log.d(TAG, "No properties found after parsing, showing empty view");
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    Log.d(TAG, "Found " + propertyList.size() + " properties, hiding empty view");
                    emptyView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "loadProperties:onCancelled", databaseError.toException());

                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                Toast.makeText(getContext(), "Failed to load properties: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();

                emptyView.setText("Error loading properties. Please try again.");
                emptyView.setVisibility(View.VISIBLE);
            }
        });
    }

    // Delete property from Firebase
    public void deleteProperty(final String propertyId, final int position) {
        if (currentUserId == null || propertyId == null) {
            Log.e(TAG, "deleteProperty: Invalid user ID or property ID");
            return;
        }

        Log.d(TAG, "Deleting property with ID: " + propertyId);

        // Reference to the property to delete
        DatabaseReference propertyRef = mDatabase.child("properties").child(currentUserId).child(propertyId);

        propertyRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Delete success
                Log.d(TAG, "Property deleted successfully from database");
                Toast.makeText(getContext(), "Property deleted successfully", Toast.LENGTH_SHORT).show();

                // Also delete the property images if they exist
                Property property = propertyList.get(position);
                deletePropertyImages(property);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Delete failure
                Log.e(TAG, "Failed to delete property: " + e.getMessage());
                Toast.makeText(getContext(), "Failed to delete property: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Delete property images from Firebase Storage
    private void deletePropertyImages(Property property) {
        if (property == null || property.getImageUrls() == null || property.getImageUrls().isEmpty()) {
            Log.d(TAG, "No images to delete");
            return;
        }

        for (String imageUrl : property.getImageUrls()) {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                    Log.d(TAG, "Deleting image: " + imageUrl);

                    imageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Image deleted successfully");
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Failed to delete image: " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing image URL: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Method to navigate to property details page
     */
    private void navigateToPropertyDetails(Property property) {
        // Example implementation (uncomment and modify as needed for your app)
        /*
        Intent intent = new Intent(getContext(), PropertyDetailsActivity.class);
        // Pass property data as serializable or in bundle
        intent.putExtra("PROPERTY_ID", property.getId());
        // Add any other data you need to pass
        startActivity(intent);
        */

        // For now, just show more details in a toast
        String details = property.getPropertyName() + "\n" +
                "Location: " + property.getCity() + ", " + property.getAddress() + "\n" +
                "Type: " + property.getPropertyType() + " - " + property.getHouseType() + "\n" +
                "Furnishing: " + property.getFurnishing();

        Toast.makeText(getContext(), details, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Dismiss dialog if it's showing to prevent leaks
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}