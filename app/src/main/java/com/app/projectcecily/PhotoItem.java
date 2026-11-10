package com.app.projectcecily;

public class PhotoItem {
    public String photoId;
    public String senderId;
    public String photoBase64;
    public long timestamp;

    public PhotoItem() {
        // Default constructor for Firebase RTDB
    }

    public PhotoItem(String photoId, String senderId, String photoBase64, long timestamp) {
        this.photoId = photoId;
        this.senderId = senderId;
        this.photoBase64 = photoBase64;
        this.timestamp = timestamp;
    }
}
