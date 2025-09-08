package com.example.eyec;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;
import java.io.File;

public class ClothingGridAdapter extends ArrayAdapter<ClothingItem> {

    public ClothingGridAdapter(Context context, List<ClothingItem> items) {
        super(context, R.layout.grid_item_clothing, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.grid_item_clothing, parent, false);
        }

        ClothingItem item = getItem(position);
        if (item == null) {
            return convertView;
        }

        ImageView imageView = convertView.findViewById(R.id.imageView);
        TextView tvId = convertView.findViewById(R.id.tvId);
        TextView tvColor = convertView.findViewById(R.id.tvColor);

        if (item.getImagePath() != null) {
            try {
                File imgFile = new File(item.getImagePath());
                if (imgFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(item.getImagePath());
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    } else {
                        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                } else {
                    imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } catch (Exception e) {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        tvId.setText("Item #" + item.getId());
        tvColor.setText("Color: " + item.getColor());

        return convertView;
    }
}
