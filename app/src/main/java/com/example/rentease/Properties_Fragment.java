package com.example.rentease;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private static final boolean DEBUG = true; // Set to false in production

    // UI Components
    private Button addButton;
    private ListView propertyListView;
    private TextView emptyView;
    private SearchView searchView;
    private View loadingOverlay;
    private TextView loadingText;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Data
    private List<Property> propertyList;
    private MyPropertyAdapter adapter;

    // Firebase references
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference mDatabase;
    private String currentUserId;

    // Add timeout handling
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            // This will run if loading takes too long (10 seconds)
            if (isAdded() && getContext() != null) {
                hideLoadingUI();
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                Toast.makeText(getContext(), "Loading timed out. Please check your internet connection.",
                        Toast.LENGTH_LONG).show();

                showEmptyState("Connection timed out. Please try again.");
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_properties_, container, false);
        if (DEBUG) Log.d(TAG, "onCreateView: Initializing Properties Fragment");

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Get current user ID and verify it's not null
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            if (DEBUG) Log.d(TAG, "Current user ID: " + currentUserId);
        } else {
            Log.e(TAG, "User is not logged in");
            Toast.makeText(getContext(), "Please log in to view your properties", Toast.LENGTH_LONG).show();
        }

        // Initialize views
        initializeViews(view);

        return view;
    }

    private void initializeViews(View view) {
        addButton = view.findViewById(R.id.add);
        propertyListView = view.findViewById(R.id.myPropertyListView);
        emptyView = view.findViewById(R.id.myPropertiesEmptyView);
        searchView = view.findViewById(R.id.myPropertiesSearchView);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);

        // Initialize optional SwipeRefreshLayout if available in your layout
        try {
            swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        if (DEBUG) Log.d(TAG, "onRefresh called");
                        if (currentUserId != null) {
                            loadUserProperties();
                        } else {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                });
                swipeRefreshLayout.setColorSchemeResources(
                        android.R.color.holo_blue_bright,
                        android.R.color.holo_green_light,
                        android.R.color.holo_orange_light,
                        android.R.color.holo_red_light);
            }
        } catch (Exception e) {
            Log.d(TAG, "SwipeRefreshLayout not found in layout");
        }

        // Initialize loading text if available
        try {
            loadingText = view.findViewById(R.id.loadingText);
        } catch (Exception e) {
            // It's fine if this doesn't exist
            Log.d(TAG, "Loading text view not found");
        }

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
                if (selectedProperty != null) {
                    navigateToPropertyDetails(selectedProperty);
                }
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
                    if (adapter != null) {
                        adapter.getFilter().filter(newText);
                    }
                    return true;
                }
            });
        }

        // Set click listener for add button
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (DEBUG) Log.d(TAG, "Add button clicked");
                Intent intent = new Intent(getContext(), add_property.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Load properties when the view is created
        if (currentUserId != null) {
            loadUserProperties();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.d(TAG, "onResume: Refreshing property list");
        // Refresh property list when fragment resumes
        if (currentUserId != null) {
            loadUserProperties();
        }
    }

    private void loadUserProperties() {
        // Show loading UI
        showLoadingUI();

        // Set timeout to handle loading failures
        timeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutHandler.postDelayed(timeoutRunnable, 10000); // 10 second timeout

        // Check connection first
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected != null && connected) {
                    // Connected to Firebase, proceed with loading
                    fetchPropertiesFromFirebase();
                } else {
                    // Not connected to Firebase
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    hideLoadingUI();
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "No internet connection. Please check your network.",
                                Toast.LENGTH_SHORT).show();
                    }
                    showEmptyState("No internet connection. Please check your network.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Connection check failed: " + error.getMessage());
                timeoutHandler.removeCallbacks(timeoutRunnable);
                hideLoadingUI();
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                showEmptyState("Connection error: " + error.getMessage());
            }
        });
    }

    private void fetchPropertiesFromFirebase() {
        // Reference to the user's properties
        DatabaseReference propertiesRef = mDatabase.child("properties").child(currentUserId);
        if (DEBUG) Log.d(TAG, "Loading user properties from: " + propertiesRef.toString());

        propertiesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Clear timeout as we got data
                timeoutHandler.removeCallbacks(timeoutRunnable);

                try {
                    List<Property> newPropertyList = new ArrayList<>();

                    if (DEBUG) {
                        Log.d(TAG, "onDataChange: Properties node exists: " + dataSnapshot.exists());
                        Log.d(TAG, "onDataChange: Found " + dataSnapshot.getChildrenCount() + " properties");
                    }

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            if (DEBUG) Log.d(TAG, "Property key: " + snapshot.getKey());

                            Property property = snapshot.getValue(Property.class);
                            if (property != null) {
                                // Store the property ID for deletion
                                String propertyId = snapshot.getKey();
                                if (DEBUG) Log.d(TAG, "Loaded property: " + property.getPropertyName() + " with ID: " + propertyId);
                                property.setId(propertyId);
                                newPropertyList.add(property);
                            } else {
                                Log.e(TAG, "Failed to parse property at key: " + snapshot.getKey());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing property data: " + e.getMessage(), e);
                        }
                    }

                    // Update adapter on the UI thread
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Update the adapter with the new list
                                if (adapter != null) {
                                    adapter.updatePropertyList(newPropertyList);
                                }

                                // Show/hide empty view
                                if (newPropertyList.isEmpty()) {
                                    if (DEBUG) Log.d(TAG, "No properties found after parsing, showing empty view");
                                    showEmptyState("You don't have any properties yet. Click the '+' button to add one.");
                                } else {
                                    if (DEBUG) Log.d(TAG, "Found " + newPropertyList.size() + " properties, hiding empty view");
                                    hideEmptyState();
                                }

                                // Hide refresh indicator if present
                                if (swipeRefreshLayout != null) {
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing property data: " + e.getMessage());
                    showEmptyState("Error loading properties. Please try again.");
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                } finally {
                    // Always hide loading UI when done
                    hideLoadingUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Cancel timeout
                timeoutHandler.removeCallbacks(timeoutRunnable);

                Log.e(TAG, "loadProperties:onCancelled", databaseError.toException());
                hideLoadingUI();

                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load properties: " + databaseError.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }

                showEmptyState("Error loading properties: " + databaseError.getMessage());
            }
        });
    }

    // Delete property from Firebase
    public void deleteProperty(final String propertyId, final int position) {
        if (currentUserId == null || propertyId == null) {
            Log.e(TAG, "deleteProperty: Invalid user ID or property ID");
            return;
        }

        if (DEBUG) Log.d(TAG, "Deleting property with ID: " + propertyId);

        // Get the property before removing from list (for image deletion)
        Property propertyToDelete = null;
        if (position >= 0 && position < propertyList.size()) {
            propertyToDelete = propertyList.get(position);
        }

        final Property finalPropertyToDelete = propertyToDelete;

        // Reference to the property to delete
        DatabaseReference propertyRef = mDatabase.child("properties").child(currentUserId).child(propertyId);

        propertyRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Delete success
                if (DEBUG) Log.d(TAG, "Property deleted successfully from database");

                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Property deleted successfully", Toast.LENGTH_SHORT).show();
                }

                // Also delete the property images if they exist
                if (finalPropertyToDelete != null) {
                    deletePropertyImages(finalPropertyToDelete);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Delete failure
                Log.e(TAG, "Failed to delete property: " + e.getMessage());

                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Failed to delete property: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Delete property images from Firebase Storage
    private void deletePropertyImages(Property property) {
        if (property == null || property.getImageUrls() == null || property.getImageUrls().isEmpty()) {
            if (DEBUG) Log.d(TAG, "No images to delete");
            return;
        }

        for (String imageUrl : property.getImageUrls()) {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                    if (DEBUG) Log.d(TAG, "Deleting image: " + imageUrl);

                    imageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if (DEBUG) Log.d(TAG, "Image deleted successfully");
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

    private void showLoadingUI() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoadingUI() {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (loadingOverlay != null) {
                    loadingOverlay.setVisibility(View.GONE);
                }
            });
        }
    }

    private void showEmptyState(String message) {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (emptyView != null) {
                    emptyView.setText(message);
                    emptyView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void hideEmptyState() {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (emptyView != null) {
                    emptyView.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Remove any pending timeout callbacks
        timeoutHandler.removeCallbacks(timeoutRunnable);
    }
}