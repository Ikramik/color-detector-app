package com.example.eyec;

public class ClothingItem {
    private String id;
    private String color;
    private String imagePath;

    public ClothingItem(String id, String color, String imagePath) {
        this.id = id;
        this.color = color;
        this.imagePath = imagePath;
    }

    public String getId() { return id; }
    public String getColor() { return color; }
    public String getImagePath() { return imagePath; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ClothingItem that = (ClothingItem) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
