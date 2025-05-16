package com.example.rentease;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MyPropertyAdapter extends ArrayAdapter<Property> implements Filterable {
    private static final String TAG = "MyPropertyAdapter";
    private static final boolean DEBUG = false; // Set to false in production

    private Context context;
    private List<Property> propertyList;
    private List<Property> filteredPropertyList;
    private Properties_Fragment fragmentReference;
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
    private final LayoutInflater inflater;
    private final int viewResourceId = R.layout.property_delete_item_card;

    // ViewHolder pattern for smoother scrolling
    private static class ViewHolder {
        ImageView imageView;
        TextView name;
        TextView location;
        TextView propertyType;
        TextView houseType;
        TextView furnishing;
        ImageButton deleteButton;
    }

    public MyPropertyAdapter(Context context, List<Property> propertyList) {
        super(context, 0, propertyList);
        this.context = context;
        this.inflater = LayoutInflater.from(context);

        // Initialize with empty lists if null is passed
        this.propertyList = new ArrayList<>();
        if (propertyList != null && !propertyList.isEmpty()) {
            this.propertyList.addAll(propertyList);
        }
        this.filteredPropertyList = new ArrayList<>(this.propertyList);

        if (DEBUG) Log.d(TAG, "MyPropertyAdapter initialized with " + this.propertyList.size() + " properties");
    }

    public void setFragmentReference(Properties_Fragment fragment) {
        this.fragmentReference = fragment;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        try {
            if (convertView == null) {
                convertView = inflater.inflate(viewResourceId, parent, false);

                // Set up ViewHolder to avoid repeated findViewById calls
                holder = new ViewHolder();
                holder.imageView = convertView.findViewById(R.id.propertyImage);
                holder.name = convertView.findViewById(R.id.propertyName);
                holder.location = convertView.findViewById(R.id.propertyLocation);
                holder.propertyType = convertView.findViewById(R.id.propertyType);
                holder.houseType = convertView.findViewById(R.id.propertyHouseType);
                holder.furnishing = convertView.findViewById(R.id.propertyFurnishing);
                holder.deleteButton = convertView.findViewById(R.id.deletePropertyButton);

                convertView.setTag(holder);
                if (DEBUG) Log.d(TAG, "Created new view for position " + position);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (position >= filteredPropertyList.size()) {
                if (DEBUG) Log.e(TAG, "Position out of bounds: " + position + ", list size: " + filteredPropertyList.size());
                return convertView;
            }

            final Property currentProperty = filteredPropertyList.get(position);
            if (currentProperty == null) {
                if (DEBUG) Log.e(TAG, "Property at position " + position + " is null");
                return convertView;
            }

            // Set text data with null checks
            holder.name.setText(currentProperty.getPropertyName());
            holder.location.setText(currentProperty.getCity() + ", " + currentProperty.getAddress());
            holder.propertyType.setText(currentProperty.getPropertyType());
            holder.houseType.setText(currentProperty.getHouseType());
            holder.furnishing.setText(currentProperty.getFurnishing());

            // Load image using Glide with optimized settings
            String imageUrl = currentProperty.getFirstImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.ic_home_placeholder)
                                .error(R.drawable.ic_home_placeholder)
                                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC) // More efficient caching strategy
                                .centerCrop()  // Better image fitting
                                .override(300, 300)) // Control image size to prevent large loads
                        .into(holder.imageView);
            } else {
                holder.imageView.setImageResource(R.drawable.ic_home_placeholder);
            }

            // Set click listener for delete button - only set it once
            final int propertyPosition = position;
            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (fragmentReference != null) {
                        // Get the original position in the unfiltered list
                        int originalPosition = propertyList.indexOf(currentProperty);
                        if (originalPosition != -1) {
                            fragmentReference.deleteProperty(currentProperty.getId(), originalPosition);
                        } else {
                            if (DEBUG) Log.e(TAG, "Property not found in original list");
                        }
                    } else {
                        if (DEBUG) Log.e(TAG, "Fragment reference is null, cannot delete property");
                    }
                }
            });

        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "Error in getView: " + e.getMessage(), e);
            // Return a default view if there's an error
            if (convertView == null) {
                convertView = inflater.inflate(viewResourceId, parent, false);
            }
        }

        return convertView;
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

    public void updatePropertyList(final List<Property> newList) {
        // Handle UI updates on the main thread, but process data in background
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final List<Property> validProperties = new ArrayList<>();

                if (newList != null) {
                    for (Property property : newList) {
                        if (property != null) {
                            validProperties.add(property);
                            // Only log in debug mode to prevent performance impact
                            if (DEBUG) property.logAllFields();
                        }
                    }
                }

                // Update the UI on the main thread
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            propertyList.clear();
                            propertyList.addAll(validProperties);
                            filteredPropertyList.clear();
                            filteredPropertyList.addAll(propertyList);
                            notifyDataSetChanged();
                            if (DEBUG) Log.d(TAG, "Property list updated, now contains " + propertyList.size() + " items");
                        }
                    });
                }
            }
        });
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
                    // Convert constraint to lowercase once to avoid repeated conversions
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    for (Property property : propertyList) {
                        if (property == null) continue;

                        // Check if property matches filter criteria
                        if (matchesFilter(property, filterPattern)) {
                            filteredList.add(property);
                        }
                    }
                }

                results.values = filteredList;
                results.count = filteredList.size();
                return results;
            }

            private boolean matchesFilter(Property property, String filterPattern) {
                // Pre-convert property fields to lowercase to avoid multiple conversions
                String propertyName = property.getPropertyName().toLowerCase();
                String city = property.getCity().toLowerCase();
                String address = property.getAddress().toLowerCase();
                String propertyType = property.getPropertyType().toLowerCase();
                String houseType = property.getHouseType().toLowerCase();

                return propertyName.contains(filterPattern) ||
                        city.contains(filterPattern) ||
                        address.contains(filterPattern) ||
                        propertyType.contains(filterPattern) ||
                        houseType.contains(filterPattern);
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredPropertyList.clear();
                if (results != null && results.values != null) {
                    //noinspection unchecked
                    filteredPropertyList.addAll((List<Property>) results.values);
                }
                if (DEBUG) Log.d(TAG, "Filter results updated, showing " + filteredPropertyList.size() + " properties");
                notifyDataSetChanged();
            }
        };
    }
}