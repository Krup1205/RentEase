package com.example.rentease;

import android.app.ProgressDialog;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {
    // Parameter argument names
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // Parameters
    private String mParam1;
    private String mParam2;

    // UI Components
    private ListView propertyListView;
    private SearchView searchView;
    private TextView emptyView;
    private PropertyAdapter adapter;
    private List<Property> propertyList;
    private ProgressDialog progressDialog;

    // Firebase
    private DatabaseReference propertiesRef;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        // Initialize Firebase
        propertiesRef = FirebaseDatabase.getInstance().getReference("Properties");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize UI components
        propertyListView = view.findViewById(R.id.propertyListView);
        searchView = view.findViewById(R.id.searchView);
        emptyView = view.findViewById(R.id.emptyView);

        // Set empty view for the list
        propertyListView.setEmptyView(emptyView);

        // Initialize property list
        propertyList = new ArrayList<>();

        // Initialize and set adapter
        adapter = new PropertyAdapter(getContext(), propertyList);
        propertyListView.setAdapter(adapter);

        // Set up click listener for property items
        propertyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Property selectedProperty = adapter.getItem(position);

                // Show property details or navigate to a detailed view
                Toast.makeText(getContext(), "Selected: " + selectedProperty.getPropertyName(), Toast.LENGTH_SHORT).show();

                // Navigate to property details
                navigateToPropertyDetails(selectedProperty);
            }
        });

        // Set up search functionality
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

        // Load properties from Firebase
        loadPropertiesFromFirebase();

        return view;
    }

    private void loadPropertiesFromFirebase() {
        // Show loading indicator
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Loading properties...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        propertiesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Clear existing list
                propertyList.clear();

                // Iterate through database snapshots
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Property property = snapshot.getValue(Property.class);
                    if (property != null) {
                        propertyList.add(property);
                    }
                }

                // Update adapter and dismiss loading dialog
                adapter.updatePropertyList(propertyList);

                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                // Show empty view if no properties
                if (propertyList.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    emptyView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle errors
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                Toast.makeText(getContext(), "Error loading properties: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();

                emptyView.setText("Error loading properties. Please try again.");
                emptyView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Dismiss dialog if it's showing to prevent leaks
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * Method to navigate to property details page
     * You'll need to create a PropertyDetailsActivity or Fragment to show full details
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
}