package com.example.eyec;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ColorTableActivity extends AppCompatActivity {
    private GridView gridView;
    private List<ClothingItem> clothingItems = new ArrayList<>();
    private List<ClothingItem> filteredItems = new ArrayList<>();
    private ClothingGridAdapter adapter;
    private Spinner spinnerColorFilter;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_table_grid);

        gridView = findViewById(R.id.gridView);
        spinnerColorFilter = findViewById(R.id.spinnerColorFilter);
        prefs = getSharedPreferences("ClothingPrefs", MODE_PRIVATE);

        adapter = new ClothingGridAdapter(this, filteredItems);
        gridView.setAdapter(adapter);

        loadClothingItems();
        setupColorFilter();

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            ClothingItem item = filteredItems.get(position);
            showItemDetails(item);
        });

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadClothingItems() {
        SharedPreferences prefs = getSharedPreferences("ClothingPrefs", MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        clothingItems.clear();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String[] parts = entry.getValue().toString().split("\\|");
            if (parts.length >= 2) {
                // Format is now: "Clothing|color|imagePath"
                String imagePath = parts.length >= 3 ? parts[2] : null;
                clothingItems.add(new ClothingItem(entry.getKey(), parts[1], imagePath));
            }
        }

        filteredItems.clear();
        filteredItems.addAll(clothingItems);
        adapter.notifyDataSetChanged();
    }

    private void setupColorFilter() {
        Set<String> colors = new HashSet<>();
        for (ClothingItem item : clothingItems) {
            colors.add(item.getColor());
        }

        List<String> colorList = new ArrayList<>(colors);
        colorList.add(0, "All Colors");

        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, colorList);
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerColorFilter.setAdapter(colorAdapter);

        spinnerColorFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void applyFilters() {
        String selectedColor = spinnerColorFilter.getSelectedItem().toString();

        filteredItems.clear();
        for (ClothingItem item : clothingItems) {
            boolean colorMatch = selectedColor.equals("All Colors") || item.getColor().equals(selectedColor);
            if (colorMatch) {
                filteredItems.add(item);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void showItemDetails(ClothingItem item) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_item_details, null);
        TextView tvColor = dialogView.findViewById(R.id.tvColor);

        tvColor.setText("Color: " + item.getColor());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Item: " + item.getId())
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .setNegativeButton("Delete", (dialogInterface, which) -> {
                    showDeleteConfirmation(item);
                })
                .create();

        dialog.show();
    }

    private void showDeleteConfirmation(ClothingItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    deleteItem(item);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteItem(ClothingItem item) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(item.getId());
        editor.apply();

        if (item.getImagePath() != null) {
            File imageFile = new File(item.getImagePath());
            if (imageFile.exists()) {
                if (imageFile.delete()) {
                    Toast.makeText(this, "Image file deleted", Toast.LENGTH_SHORT).show();
                }
            }
        }

        // Remove from lists and update UI
        clothingItems.remove(item);
        filteredItems.remove(item);
        adapter.notifyDataSetChanged();

        Toast.makeText(this, "Item deleted successfully", Toast.LENGTH_SHORT).show();
    }
}
