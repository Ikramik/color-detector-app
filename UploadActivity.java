package com.example.eyec;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadActivity extends AppCompatActivity {

    private static final String TAG = "UploadActivity";
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 101;

    private Uri imageUri;
    private ImageView imageView;
    private TextView resultTextView;
    private Button btnCancel, btnSave;
    private String detectedColor = "Unknown";
    private Bitmap currentBitmap;

    // Color detection thresholds
    private static final float SATURATION_THRESHOLD = 0.15f;
    private static final float VALUE_THRESHOLD_DARK = 0.15f;
    private static final float VALUE_THRESHOLD_LIGHT = 0.85f;
    private static final String IMAGE_DIR = "clothing_images";

    // Color ranges for better detection
    private static final Map<String, float[]> COLOR_RANGES = new HashMap<String, float[]>() {{
        put("Red", new float[]{-20f, 20f});
        put("Orange", new float[]{21f, 40f});
        put("Yellow", new float[]{41f, 65f});
        put("Green", new float[]{66f, 160f});
        put("Blue", new float[]{161f, 260f});
        put("Purple", new float[]{261f, 290f});
        put("Pink", new float[]{291f, 340f});
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        initializeViews();
        setupButtons();

        Intent intent = getIntent();
        if (intent != null && "camera".equals(intent.getStringExtra("source"))) {
            Bundle extras = intent.getExtras();
            if (extras != null && extras.containsKey("data")) {
                Bitmap cameraImage = (Bitmap) extras.get("data");
                if (cameraImage != null) {
                    currentBitmap = cameraImage;
                    imageView.setImageBitmap(cameraImage);
                    analyzeImage(cameraImage);
                }
            }
        } else if (intent != null && "gallery".equals(intent.getStringExtra("source"))) {
            openGallery();
        } else {
            showUploadOptions();
        }
    }

    private void initializeViews() {
        imageView = findViewById(R.id.imageView);
        resultTextView = findViewById(R.id.textView);
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSave);
    }

    private void setupButtons() {
        Button btnUpload = findViewById(R.id.btnUpload);
        btnUpload.setOnClickListener(v -> showUploadOptions());
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveResults());
    }

    private void showUploadOptions() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openCamera();
                    else openGallery();
                })
                .show();
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            try {
                Bitmap bitmap = null;
                if (requestCode == CAMERA_REQUEST_CODE) {
                    Bundle extras = data.getExtras();
                    if (extras != null && extras.containsKey("data")) {
                        bitmap = (Bitmap) extras.get("data");
                        currentBitmap = bitmap;
                    }
                } else if (requestCode == GALLERY_REQUEST_CODE) {
                    imageUri = data.getData();
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    currentBitmap = bitmap;
                }

                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    analyzeImage(bitmap);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing image", e);
                Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void analyzeImage(Bitmap bitmap) {
        analyzeColorAdvanced(bitmap);
    }

    private void analyzeColorAdvanced(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette == null) {
                detectedColor = "Unknown";
                updateResults();
                return;
            }

            List<Palette.Swatch> swatches = new ArrayList<>();
            if (palette.getDominantSwatch() != null) swatches.add(palette.getDominantSwatch());
            if (palette.getVibrantSwatch() != null) swatches.add(palette.getVibrantSwatch());
            if (palette.getDarkVibrantSwatch() != null) swatches.add(palette.getDarkVibrantSwatch());
            if (palette.getLightVibrantSwatch() != null) swatches.add(palette.getLightVibrantSwatch());

            if (swatches.isEmpty()) {
                detectedColor = "Unknown";
                updateResults();
                return;
            }

            Map<String, Integer> colorVotes = new HashMap<>();
            for (Palette.Swatch swatch : swatches) {
                String color = getColorName(swatch.getRgb());
                colorVotes.put(color, colorVotes.getOrDefault(color, 0) + 1);
            }

            detectedColor = colorVotes.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Unknown");

            updateResults();
        });
    }

    private String getColorName(int colorInt) {
        float[] hsv = new float[3];
        Color.colorToHSV(colorInt, hsv);
        float hue = hsv[0];
        float saturation = hsv[1];
        float value = hsv[2];

        if (saturation < SATURATION_THRESHOLD) {
            if (value < VALUE_THRESHOLD_DARK) return "Black";
            if (value > VALUE_THRESHOLD_LIGHT) return "White";
            return value < 0.5f ? "Dark Gray" : "Light Gray";
        }

        if (hue < 0) hue += 360f;

        for (Map.Entry<String, float[]> entry : COLOR_RANGES.entrySet()) {
            float[] range = entry.getValue();
            if (range[0] <= hue && hue <= range[1]) {
                String prefix = value < 0.3f ? "Dark " : value > 0.7f ? "Light " : "";
                return prefix + entry.getKey();
            }
        }

        return "Unknown";
    }

    private void updateResults() {
        String result = String.format("Color: %s", detectedColor);
        resultTextView.setText(result);
    }

    private void saveResults() {
        if (detectedColor.equals("Unknown") || currentBitmap == null) {
            Toast.makeText(this, "No results to save", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_id_input, null);
        EditText etDialogId = dialogView.findViewById(R.id.etDialogId);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Save Item")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String itemId = etDialogId.getText().toString().trim();
                if (TextUtils.isEmpty(itemId)) {
                    etDialogId.setError("Please enter an ID");
                    return;
                }

                SharedPreferences prefs = getSharedPreferences("ClothingPrefs", MODE_PRIVATE);
                if (prefs.contains(itemId)) {
                    new AlertDialog.Builder(this)
                            .setTitle("ID Already Exists")
                            .setMessage("An item with this ID already exists. Do you want to overwrite it?")
                            .setPositiveButton("Yes", (innerDialog, which) -> {
                                saveItem(itemId, currentBitmap);
                                dialog.dismiss();
                            })
                            .setNegativeButton("No", null)
                            .show();
                } else {
                    saveItem(itemId, currentBitmap);
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private String saveImageToStorage(Bitmap bitmap, String id) {
        try {
            File directory = new File(getFilesDir(), IMAGE_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File imageFile = new File(directory, id + ".jpg");
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
            return null;
        }
    }

    private void saveItem(String id, Bitmap imageBitmap) {
        SharedPreferences prefs = getSharedPreferences("ClothingPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String imagePath = saveImageToStorage(imageBitmap, id);
        // Save only color information (no type)
        String data = "Clothing|" + detectedColor + "|" + imagePath;

        editor.putString(id, data);
        editor.apply();
        Toast.makeText(this, "Item saved successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}
