package com.app.projectcecily;

public class DateCard {
    public String id;
    public String title;
    public String category;
    public String description;
    public String imageUrl;
    public boolean isLdrCard;

    public DateCard() {} // Firebase

    public DateCard(String id, String title, String category, String description, String imageUrl, boolean isLdrCard) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.imageUrl = imageUrl;
        this.isLdrCard = isLdrCard;
    }
}