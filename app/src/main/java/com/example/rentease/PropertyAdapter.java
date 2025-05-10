package com.example.rentease;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class PropertyAdapter extends ArrayAdapter<Property> implements Filterable {
    private static final String TAG = "PropertyAdapter";
    private Context context;
    private List<Property> propertyList;
    private List<Property> filteredPropertyList;

    public PropertyAdapter(Context context, List<Property> propertyList) {
        super(context, 0, propertyList);
        this.context = context;
        // Initialize with empty lists if null is passed
        this.propertyList = new ArrayList<>();
        if (propertyList != null && !propertyList.isEmpty()) {
            this.propertyList.addAll(propertyList);
        }
        this.filteredPropertyList = new ArrayList<>(this.propertyList);
        Log.d(TAG, "PropertyAdapter initialized with " + this.propertyList.size() + " properties");
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;

        try {
            if (listItem == null) {
                listItem = LayoutInflater.from(context).inflate(R.layout.property_item, parent, false);
                Log.d(TAG, "Created new view for position " + position);
            }

            if (position >= filteredPropertyList.size()) {
                Log.e(TAG, "Position out of bounds: " + position + ", list size: " + filteredPropertyList.size());
                return listItem;
            }

            Property currentProperty = filteredPropertyList.get(position);
            if (currentProperty == null) {
                Log.e(TAG, "Property at position " + position + " is null");
                return listItem;
            }

            Log.d(TAG, "Getting view for property: " + currentProperty.getPropertyName() + " at position " + position);

            ImageView imageView = listItem.findViewById(R.id.propertyImage);
            TextView name = listItem.findViewById(R.id.propertyName);
            TextView location = listItem.findViewById(R.id.propertyLocation);
            TextView propertyType = listItem.findViewById(R.id.propertyType);
            TextView houseType = listItem.findViewById(R.id.propertyHouseType);
            TextView furnishing = listItem.findViewById(R.id.propertyFurnishing);

            // Set text data with null checks
            name.setText(currentProperty.getPropertyName());
            location.setText(currentProperty.getCity() + ", " + currentProperty.getAddress());
            propertyType.setText(currentProperty.getPropertyType());
            houseType.setText(currentProperty.getHouseType());
            furnishing.setText(currentProperty.getFurnishing());

            // Load image using Glide with error handling
            String imageUrl = currentProperty.getFirstImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Log.d(TAG, "Loading image from URL: " + imageUrl);
                Glide.with(context)
                        .load(imageUrl)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.ic_home_placeholder)
                                .error(R.drawable.ic_home_placeholder)
                                .diskCacheStrategy(DiskCacheStrategy.ALL))
                        .into(imageView);
            } else {
                Log.d(TAG, "No image URL, using placeholder");
                imageView.setImageResource(R.drawable.ic_home_placeholder);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getView: " + e.getMessage(), e);
            // Return a default view if there's an error
            if (listItem == null) {
                listItem = LayoutInflater.from(context).inflate(R.layout.property_item, parent, false);
            }
        }

        return listItem;
    }

    @Override
    public int getCount() {
        return filteredPropertyList.size();
    }

    @Override
    public Property getItem(int position) {
        if (position >= 0 && position < filteredPropertyList.size()) {
            return filteredPropertyList.get(position);
        }
        return null;
    }

    public void updatePropertyList(List<Property> newList) {
        Log.d(TAG, "Updating property list with " + (newList != null ? newList.size() : 0) + " items");
        this.propertyList.clear();
        if (newList != null) {
            for (Property property : newList) {
                if (property != null) {
                    this.propertyList.add(property);
                    property.logAllFields(); // Log each property for debugging
                }
            }
        }
        this.filteredPropertyList.clear();
        this.filteredPropertyList.addAll(this.propertyList);
        notifyDataSetChanged();
        Log.d(TAG, "Property list updated, now contains " + this.propertyList.size() + " items");
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<Property> filteredList = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    // No filter, return all items
                    filteredList.addAll(propertyList);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (Property property : propertyList) {
                        if (property == null) continue;

                        // Filter by property name, city, address, or property type
                        if (property.getPropertyName().toLowerCase().contains(filterPattern) ||
                                property.getCity().toLowerCase().contains(filterPattern) ||
                                property.getAddress().toLowerCase().contains(filterPattern) ||
                                property.getPropertyType().toLowerCase().contains(filterPattern) ||
                                property.getHouseType().toLowerCase().contains(filterPattern)) {
                            filteredList.add(property);
                        }
                    }
                }

                results.values = filteredList;
                results.count = filteredList.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredPropertyList.clear();
                if (results != null && results.values != null) {
                    filteredPropertyList.addAll((List<Property>) results.values);
                }
                Log.d(TAG, "Filter results updated, showing " + filteredPropertyList.size() + " properties");
                notifyDataSetChanged();
            }
        };
    }
}