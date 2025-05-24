package com.example.eyec;


public class ClothingItem {
    private String id;
    private String color;
    private String type;

    public ClothingItem(String id, String color, String type) {
        this.id = id;
        this.color = color;
        this.type = type;
    }

    public String getId() { return id; }
    public String getColor() { return color; }
    public String getType() { return type; }
}