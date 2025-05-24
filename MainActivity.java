package com.example.eyec;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.eyec.HomeActivity;
import com.example.eyec.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static int SPLASH_TIMER = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            Log.d(TAG, "Starting MainActivity");
            setContentView(R.layout.activity_main);
            
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "Attempting to start HomeActivity");
                        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting HomeActivity", e);
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, 
                            "Error starting app: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show());
                    }
                }
            }, SPLASH_TIMER);
        } catch (Exception e) {
            Log.e(TAG, "Error in MainActivity onCreate", e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
        }
    }
}
