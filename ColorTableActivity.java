package com.example.eyec;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ColorTableActivity extends AppCompatActivity {
    private ListView listView;
    private List<ClothingItem> clothingItems = new ArrayList<>();
    private ClothingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_table);

        listView = findViewById(R.id.listView);
        adapter = new ClothingAdapter(this, clothingItems);
        listView.setAdapter(adapter);

        loadClothingItems();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            ClothingItem item = clothingItems.get(position);
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
            if (parts.length == 2) {
                clothingItems.add(new ClothingItem(entry.getKey(), parts[0], parts[1]));
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void showItemDetails(ClothingItem item) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_item_details, null);
        TextView tvType = dialogView.findViewById(R.id.tvType);
        TextView tvColor = dialogView.findViewById(R.id.tvColor);

        tvType.setText("Type: " + item.getType());
        tvColor.setText("Color: " + item.getColor());

        new AlertDialog.Builder(this)
                .setTitle("Item #" + item.getId())
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }

    private static class ClothingAdapter extends ArrayAdapter<ClothingItem> {
        public ClothingAdapter(Context context, List<ClothingItem> items) {
            super(context, R.layout.list_item_clothing, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.list_item_clothing, parent, false);
            }

            ClothingItem item = getItem(position);
            TextView tvId = convertView.findViewById(R.id.tvId);
            tvId.setText("Item #" + item.getId());

            return convertView;
        }
    }
}