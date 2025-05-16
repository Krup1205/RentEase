package com.example.rentease;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class Property {
    private static final String TAG = "Property";

    // Private property ID field for Firebase operations
    private String id;

    // Make sure property names match EXACTLY what's in Firebase
    public String propertyName, address, propertyType, houseType, city, ownerContact, furnishing;
    public List<String> facilities;
    public List<String> imageUrls;

    // Required empty constructor for Firebase
    public Property() {
        // Initialize lists to prevent null pointer exceptions
        facilities = new ArrayList<>();
        imageUrls = new ArrayList<>();
        Log.d(TAG, "Empty constructor called");
    }

    public Property(String propertyName, String address, String propertyType, String houseType, String city,
                    String ownerContact, String furnishing, List<String> facilities, List<String> imageUrls) {
        this.propertyName = propertyName;
        this.address = address;
        this.propertyType = propertyType;
        this.houseType = houseType;
        this.city = city;
        this.ownerContact = ownerContact;
        this.furnishing = furnishing;
        this.facilities = facilities != null ? facilities : new ArrayList<>();
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        Log.d(TAG, "Created property: " + propertyName);
    }

    // Getter and setter for ID (needed for Firebase operations)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        Log.d(TAG, "Set property ID: " + id + " for property: " + getPropertyName());
    }

    // Getters with null safety
    public String getPropertyName() {
        return propertyName != null ? propertyName : "Unnamed Property";
    }

    public String getAddress() {
        return address != null ? address : "Address not available";
    }

    public String getPropertyType() {
        return propertyType != null ? propertyType : "Not specified";
    }

    public String getHouseType() {
        return houseType != null ? houseType : "Not specified";
    }

    public String getCity() {
        return city != null ? city : "City not specified";
    }

    public String getOwnerContact() {
        return ownerContact != null ? ownerContact : "Contact not available";
    }

    public String getFurnishing() {
        return furnishing != null ? furnishing : "Not specified";
    }

    public List<String> getFacilities() {
        return facilities != null ? facilities : new ArrayList<>();
    }

    public List<String> getImageUrls() {
        return imageUrls != null ? imageUrls : new ArrayList<>();
    }

    // Get the first image URL for thumbnail display with null safety
    public String getFirstImageUrl() {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            String url = imageUrls.get(0);
            Log.d(TAG, "First image URL: " + url);
            return url;
        }
        Log.d(TAG, "No image URLs available for property: " + getPropertyName());
        return null;
    }

    // Added for debugging - print all fields
    public void logAllFields() {
        Log.d(TAG, "Property Details - " +
                "ID: " + id +
                ", Name: " + propertyName +
                ", Address: " + address +
                ", City: " + city +
                ", Type: " + propertyType +
                ", House Type: " + houseType +
                ", Furnishing: " + furnishing +
                ", Facilities: " + (facilities != null ? facilities.size() : 0) +
                ", Images: " + (imageUrls != null ? imageUrls.size() : 0));
    }

    @Override
    public String toString() {
        return "Property{" +
                "id='" + id + '\'' +
                ", propertyName='" + propertyName + '\'' +
                ", city='" + city + '\'' +
                ", propertyType='" + propertyType + '\'' +
                ", imageUrls.size=" + (imageUrls != null ? imageUrls.size() : 0) +
                '}';
    }
}