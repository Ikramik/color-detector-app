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
import android.text.InputType;
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

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.TensorImage;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadActivity extends AppCompatActivity {

    private static final String TAG = "UploadActivity";
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 101;
    private static final int INPUT_SIZE = 224;
    private static final int MIN_CONFIDENCE = 60; // Minimum confidence percentage

    private Uri imageUri;
    private ImageView imageView;
    private TextView resultTextView;
    private Button btnCancel, btnSave;
    private String detectedColor = "Unknown";
    private String clothingType = "Unknown";
    
    // Color detection thresholds
    private static final float SATURATION_THRESHOLD = 0.15f;
    private static final float VALUE_THRESHOLD_DARK = 0.15f;
    private static final float VALUE_THRESHOLD_LIGHT = 0.85f;
    
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

    private Interpreter tflite;
    private final List<String> clothingLabels = Arrays.asList("Shirt", "Pants", "Dress", "Jacket", "Skirt", "Shorts");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        initializeViews();
        setupButtons();

        try {
            tflite = new Interpreter(loadModelFile("clothing_model.tflite"));
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load model", Toast.LENGTH_SHORT).show();
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
                    bitmap = (Bitmap) data.getExtras().get("data");
                } else if (requestCode == GALLERY_REQUEST_CODE) {
                    imageUri = data.getData();
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
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
        analyzeClothingType(bitmap);
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

    private void analyzeClothingType(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        int sampleSize = 20;
        int[][] samples = new int[sampleSize][sampleSize];
        
        for (int x = 0; x < sampleSize; x++) {
            for (int y = 0; y < sampleSize; y++) {
                int pixelX = (width * x) / sampleSize;
                int pixelY = (height * y) / sampleSize;
                samples[x][y] = bitmap.getPixel(pixelX, pixelY);
            }
        }
        
        float aspectRatio = (float) height / width;
        boolean hasVerticalPattern = detectVerticalPattern(samples);
        boolean hasHorizontalPattern = detectHorizontalPattern(samples);
        
        if (aspectRatio > 1.5f) {
            clothingType = "Dress";
        } else if (aspectRatio < 0.8f) {
            clothingType = hasHorizontalPattern ? "Pants" : "Shorts";
        } else {
            clothingType = hasVerticalPattern ? "Shirt" : "Jacket";
        }
        
        updateResults();
    }

    private boolean detectVerticalPattern(int[][] samples) {
        int changes = 0;
        for (int x = 0; x < samples.length; x++) {
            for (int y = 1; y < samples[x].length; y++) {
                if (isDifferentColor(samples[x][y], samples[x][y-1])) {
                    changes++;
                }
            }
        }
        return changes > (samples.length * samples[0].length * 0.3);
    }

    private boolean detectHorizontalPattern(int[][] samples) {
        int changes = 0;
        for (int y = 0; y < samples[0].length; y++) {
            for (int x = 1; x < samples.length; x++) {
                if (isDifferentColor(samples[x][y], samples[x-1][y])) {
                    changes++;
                }
            }
        }
        return changes > (samples.length * samples[0].length * 0.3);
    }

    private boolean isDifferentColor(int color1, int color2) {
        int threshold = 30;
        int r1 = Color.red(color1), r2 = Color.red(color2);
        int g1 = Color.green(color1), g2 = Color.green(color2);
        int b1 = Color.blue(color1), b2 = Color.blue(color2);
        
        return Math.abs(r1 - r2) > threshold ||
               Math.abs(g1 - g2) > threshold ||
               Math.abs(b1 - b2) > threshold;
    }

    private void updateResults() {
        String result = String.format("Type: %s\nColor: %s", clothingType, detectedColor);
        resultTextView.setText(result);
    }

    private void saveResults() {
        if (detectedColor.equals("Unknown") || clothingType.equals("Unknown")) {
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
                String id = etDialogId.getText().toString().trim();
                if (TextUtils.isEmpty(id)) {
                    etDialogId.setError("Please enter an ID");
                    return;
                }

                SharedPreferences prefs = getSharedPreferences("ClothingPrefs", MODE_PRIVATE);
                if (prefs.contains(id)) {
                    new AlertDialog.Builder(this)
                            .setTitle("ID Already Exists")
                            .setMessage("An item with this ID already exists. Do you want to overwrite it?")
                            .setPositiveButton("Yes", (innerDialog, which) -> {
                                saveItem(id);
                                dialog.dismiss();
                            })
                            .setNegativeButton("No", null)
                            .show();
                } else {
                    saveItem(id);
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void saveItem(String id) {
        SharedPreferences prefs = getSharedPreferences("ClothingPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(id, clothingType + "|" + detectedColor);
        editor.apply();
        Toast.makeText(this, "Item saved successfully", Toast.LENGTH_SHORT).show();
        finish();
    }

    private MappedByteBuffer loadModelFile(String modelPath) throws IOException {
        FileInputStream inputStream = new FileInputStream(getAssets().openFd(modelPath).getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = getAssets().openFd(modelPath).getStartOffset();
        long declaredLength = getAssets().openFd(modelPath).getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}