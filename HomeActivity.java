package com.example.eyec;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int CAMERA_PERMISSION_CODE = 201;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            Log.d(TAG, "Starting HomeActivity");
            setContentView(R.layout.activity_home);

            Button btnTable = findViewById(R.id.menuButton);
            Button btnCamera = findViewById(R.id.cameraButton);
            Button btnUpload = findViewById(R.id.uploadButton);

            if (btnTable == null || btnUpload == null || btnCamera == null) {
                Log.e(TAG, "Failed to find buttons in layout");
                Toast.makeText(this, "Error: UI elements not found", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            btnTable.setOnClickListener(v -> {
                try {
                    Log.d(TAG, "Starting ColorTableActivity");
                    startActivity(new Intent(HomeActivity.this, ColorTableActivity.class));
                } catch (Exception e) {
                    Log.e(TAG, "Error starting ColorTableActivity", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

            btnUpload.setOnClickListener(v -> {
                try {
                    Log.d(TAG, "Starting UploadActivity with gallery intent");
                    Intent intent = new Intent(HomeActivity.this, UploadActivity.class);
                    intent.putExtra("source", "gallery");
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error starting UploadActivity", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

            btnCamera.setOnClickListener(v -> {
                try {
                    Log.d(TAG, "Camera button clicked");
                    checkCameraPermission();
                } catch (Exception e) {
                    Log.e(TAG, "Error starting camera", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

            Log.d(TAG, "HomeActivity setup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in HomeActivity onCreate", e);
            Toast.makeText(this, "Error initializing home screen: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting camera permission");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
        } else {
            Log.d(TAG, "Camera permission already granted");
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted");
                openCamera();
            } else {
                Log.d(TAG, "Camera permission denied");
                Toast.makeText(this, "Camera permission is required to take pictures", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        try {
            Log.d(TAG, "Opening camera");
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            } else {
                Log.e(TAG, "No camera app found");
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera", e);
            Toast.makeText(this, "Error opening camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            try {
                Log.d(TAG, "Camera result received");
                // Pass the camera image to UploadActivity
                Intent intent = new Intent(HomeActivity.this, UploadActivity.class);
                intent.putExtra("source", "camera");

                Bundle extras = data.getExtras();
                if (extras != null && extras.containsKey("data")) {
                    intent.putExtras(extras);
                }

                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error passing camera image to UploadActivity", e);
                Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_CANCELED) {
            Log.d(TAG, "Camera operation cancelled by user");
        }
    }
}
