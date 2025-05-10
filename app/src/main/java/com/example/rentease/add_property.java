package com.example.rentease;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class add_property extends AppCompatActivity {

    private static final int MIN_IMAGE_COUNT = 5;
    private Button btnSelectImages;
    private RecyclerView rvPropertyImages;
    private ImageAdapter imageAdapter;
    private ArrayList<Uri> imageList = new ArrayList<>();

    Spinner spinnerPropertyType, spinnerHouseType, spinnerCity;
    String selectedPropertyType, selectedHouseType;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                            imageList.add(imageUri);
                        }
                    } else if (result.getData().getData() != null) {
                        imageList.add(result.getData().getData());
                    }
                    imageAdapter.notifyDataSetChanged();
                    if (imageList.size() < MIN_IMAGE_COUNT) {
                        showToast("Please select at least 5 images!");
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_property);

        btnSelectImages = findViewById(R.id.btnSelectImages);
        rvPropertyImages = findViewById(R.id.rvPropertyImages);
        imageAdapter = new ImageAdapter(imageList, this);
        rvPropertyImages.setLayoutManager(new GridLayoutManager(this, 3));
        rvPropertyImages.setHasFixedSize(true);
        rvPropertyImages.setNestedScrollingEnabled(true);
        rvPropertyImages.setAdapter(imageAdapter);

        btnSelectImages.setOnClickListener(v -> openGallery());

        spinnerPropertyType = findViewById(R.id.spinnerPropertyType);
        spinnerHouseType = findViewById(R.id.spinnerHouseType);
        spinnerCity = findViewById(R.id.spinnerCity);

        spinnerPropertyType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Apartment", "Villa", "Independent House", "Penthouse", "Studio"}));
        spinnerHouseType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"1 BHK", "2 BHK", "3 BHK", "4 BHK", "5 BHK"}));
        spinnerCity.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new String[]{"Vadodara", "Surat"}));

        spinnerPropertyType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPropertyType = parent.getItemAtPosition(position).toString();
                // Removed unnecessary Toast
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        findViewById(R.id.btnSubmit).setOnClickListener(v -> {
            if (imageList.size() < MIN_IMAGE_COUNT) {
                showToast("Please select at least 5 images!");
                return;
            }
            uploadProperty();
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadProperty() {
        String propertyName = ((TextInputEditText) findViewById(R.id.etPropertyName)).getText().toString().trim();
        String address = ((TextInputEditText) findViewById(R.id.etAddress)).getText().toString().trim();
        String ownerContact = ((TextInputEditText) findViewById(R.id.etOwnerContact)).getText().toString().trim();
        String city = spinnerCity.getSelectedItem().toString();
        String houseType = spinnerHouseType.getSelectedItem().toString();

        RadioGroup rgFurnishing = findViewById(R.id.rgFurnishingType);
        int selectedFurnishingId = rgFurnishing.getCheckedRadioButtonId();
        RadioButton selectedFurnishing = findViewById(selectedFurnishingId);
        String furnishing = selectedFurnishing != null ? selectedFurnishing.getText().toString() : "Not Specified";

        List<String> facilities = new ArrayList<>();
        int[] checkBoxIds = {
                R.id.cbParking, R.id.cbWifi, R.id.cbSecurity, R.id.cbGym, R.id.cbPool,
                R.id.cbGarden, R.id.cbLift, R.id.cbBackup, R.id.cbCCTV, R.id.cbClubhouse
        };

        for (int id : checkBoxIds) {
            CheckBox cb = findViewById(id);
            if (cb.isChecked()) facilities.add(cb.getText().toString());
        }

        StorageReference storageRef = FirebaseStorage.getInstance().getReference("property_images");
        List<String> imageUrls = new ArrayList<>();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Properties");
        String propertyId = dbRef.push().getKey();

        if (propertyId == null) {
            showToast("Error generating property ID");
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        final int[] uploadCount = {0};

        for (int i = 0; i < imageList.size(); i++) {
            Uri imageUri = imageList.get(i);
            StorageReference imgRef = storageRef.child(propertyId + "_img_" + i);

            imgRef.putFile(imageUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return imgRef.getDownloadUrl();
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            imageUrls.add(downloadUri.toString());
                            uploadCount[0]++;

                            if (uploadCount[0] == imageList.size()) {
                                Property property = new Property(propertyName, address, selectedPropertyType,
                                        houseType, city, ownerContact, furnishing, facilities, imageUrls);

                                dbRef.child(propertyId).setValue(property).addOnCompleteListener(task2 -> {
                                    progressDialog.dismiss();
                                    if (task2.isSuccessful()) {
                                        showToast("Property submitted successfully!");
                                        clearForm();
                                    } else {
                                        showToast("Failed to submit property.");
                                    }
                                });
                            }
                        } else {
                            progressDialog.dismiss();
                            showToast("Image upload failed: " + task.getException().getMessage());
                        }
                    });
        }
    }

    private void clearForm() {
        ((TextInputEditText) findViewById(R.id.etPropertyName)).setText("");
        ((TextInputEditText) findViewById(R.id.etAddress)).setText("");
        ((TextInputEditText) findViewById(R.id.etOwnerContact)).setText("");
        spinnerPropertyType.setSelection(0);
        spinnerHouseType.setSelection(0);
        spinnerCity.setSelection(0);

        RadioGroup rgFurnishing = findViewById(R.id.rgFurnishingType);
        rgFurnishing.clearCheck();

        int[] checkBoxIds = {
                R.id.cbParking, R.id.cbWifi, R.id.cbSecurity, R.id.cbGym, R.id.cbPool,
                R.id.cbGarden, R.id.cbLift, R.id.cbBackup, R.id.cbCCTV, R.id.cbClubhouse
        };

        for (int id : checkBoxIds) {
            ((CheckBox) findViewById(id)).setChecked(false);
        }

        imageList.clear();
        imageAdapter.notifyDataSetChanged();
    }

    private void showToast(String message) {
        Toast.makeText(add_property.this, message, Toast.LENGTH_SHORT).show();
    }

    public class Property {
        public String propertyName, address, propertyType, houseType, city, ownerContact, furnishing;
        public List<String> facilities;
        public List<String> imageUrls;

        public Property() {}

        public Property(String propertyName, String address, String propertyType, String houseType, String city,
                        String ownerContact, String furnishing, List<String> facilities, List<String> imageUrls) {
            this.propertyName = propertyName;
            this.address = address;
            this.propertyType = propertyType;
            this.houseType = houseType;
            this.city = city;
            this.ownerContact = ownerContact;
            this.furnishing = furnishing;
            this.facilities = facilities;
            this.imageUrls = imageUrls;
        }
    }
}
