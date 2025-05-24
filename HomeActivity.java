package com.example.eyec;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            Log.d(TAG, "Starting HomeActivity");
            setContentView(R.layout.activity_home);

            Button btnTable = findViewById(R.id.table);
            Button btnUpload = findViewById(R.id.upload);

            if (btnTable == null || btnUpload == null) {
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
                    Log.d(TAG, "Starting UploadActivity");
                    startActivity(new Intent(HomeActivity.this, UploadActivity.class));
                } catch (Exception e) {
                    Log.e(TAG, "Error starting UploadActivity", e);
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
}