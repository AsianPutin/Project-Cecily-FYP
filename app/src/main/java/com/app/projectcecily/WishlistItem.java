package com.app.projectcecily;

public class WishlistItem {
    public String id;
    public String name;
    public String link;
    public String img1, img2, img3;
    public int heroIndex = 0;
    public boolean isBought;
    public boolean isReceived;
    public String ownerId;

    public WishlistItem() {} // Required for Firebase

    public WishlistItem(String id, String name, String link, String ownerId, String img1, String img2, String img3, int heroIndex) {
        this.id = id;
        this.name = name;
        this.link = link;
        this.ownerId = ownerId;
        this.img1 = img1;
        this.img2 = img2;
        this.img3 = img3;
        this.heroIndex = heroIndex;
        this.isBought = false;
        this.isReceived = false;
    }
}