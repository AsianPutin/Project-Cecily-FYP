package com.app.projectcecily;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PhotoCycleManager {

    private static final String FIREBASE_URL = "https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/";
    public static final int MAX_PHOTOS_PER_USER = 4;
    public static final long EXPIRY_MILLIS = 24 * 60 * 60 * 1000L; // 24 Hours

    public interface PhotosCallback {
        void onPhotosLoaded(List<PhotoItem> photos);
    }

    public interface UploadCallback {
        void onSuccess();
        void onError(String message);
    }

    public static String getPairKey(Context context) {
        String userId = UserManager.getUserId(context);
        String partnerId = UserManager.getPartnerId(context);
        if (partnerId.isEmpty()) return userId;
        return (userId.compareTo(partnerId) < 0) ? (userId + "_" + partnerId) : (partnerId + "_" + userId);
    }

    public static Uri createTempCameraUri(Context context) {
        if (context == null) return null;
        try {
            java.io.File cacheDir = context.getExternalCacheDir();
            if (cacheDir == null) cacheDir = context.getCacheDir();
            java.io.File tempFile = java.io.File.createTempFile("highres_camera_", ".jpg", cacheDir);
            return androidx.core.content.FileProvider.getUriForFile(context, "com.app.projectcecily.fileprovider", tempFile);
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap decodeHighResBitmap(Context context, Uri imageUri) {
        if (context == null || imageUri == null) return null;
        try (java.io.InputStream is = context.getContentResolver().openInputStream(imageUri)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeStream(is, null, options);
        } catch (Exception e) {
            return null;
        }
    }

    public static void saveUneditedPhotoToGallery(Context context, Bitmap bitmap) {
        if (context == null || bitmap == null) return;
        try {
            String filename = "Cecily_" + System.currentTimeMillis() + ".jpg";
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ProjectCecily");
                Uri imageUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (imageUri != null) {
                    fos = context.getContentResolver().openOutputStream(imageUri);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                    if (fos != null) fos.close();
                }
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/ProjectCecily";
                java.io.File file = new java.io.File(imagesDir);
                if (!file.exists()) file.mkdirs();
                java.io.File image = new java.io.File(imagesDir, filename);
                fos = new java.io.FileOutputStream(image);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                fos.close();
            }
        } catch (Exception ignored) {}
    }

    public static void uploadPhoto(Context context, Bitmap croppedBmp, UploadCallback callback) {
        String userId = UserManager.getUserId(context);
        if (userId.isEmpty()) {
            if (callback != null) callback.onError("Please log in first.");
            return;
        }

        String pairKey = getPairKey(context);
        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("photo_cycles").child(pairKey).child(userId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long now = System.currentTimeMillis();
                List<DataSnapshot> validSnaps = new ArrayList<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        Long ts = ds.child("timestamp").getValue(Long.class);
                        if (ts != null && (now - ts < EXPIRY_MILLIS)) {
                            validSnaps.add(ds);
                        } else {
                            // Automatically purge expired photos > 24 hours
                            ds.getRef().removeValue();
                        }
                    } catch (Exception ignored) {}
                }

                if (validSnaps.size() >= MAX_PHOTOS_PER_USER) {
                    if (callback != null) callback.onError("Capacity full! (Max 4 active 24h photos)");
                    return;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                croppedBmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                String base64Str = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

                String photoId = "photo_" + now;
                PhotoItem item = new PhotoItem(photoId, userId, base64Str, now);

                ref.child(photoId).setValue(item).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) callback.onSuccess();
                    } else {
                        if (callback != null) callback.onError("Upload failed. Check network.");
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) callback.onError(error.getMessage());
            }
        });
    }

    public static void listenCombinedActivePhotos(Context context, PhotosCallback callback) {
        if (context == null) {
            if (callback != null) callback.onPhotosLoaded(new ArrayList<>());
            return;
        }

        String myUserId = UserManager.getUserId(context);
        String partnerId = UserManager.getPartnerId(context);

        if (myUserId.isEmpty() && partnerId.isEmpty()) {
            if (callback != null) callback.onPhotosLoaded(new ArrayList<>());
            return;
        }

        String pairKey = getPairKey(context);
        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("photo_cycles").child(pairKey);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long now = System.currentTimeMillis();
                List<PhotoItem> combinedPhotos = new ArrayList<>();

                if (snapshot.exists()) {
                    for (DataSnapshot userSnap : snapshot.getChildren()) {
                        for (DataSnapshot photoSnap : userSnap.getChildren()) {
                            try {
                                PhotoItem item = photoSnap.getValue(PhotoItem.class);
                                if (item != null && item.timestamp > 0 && (now - item.timestamp < EXPIRY_MILLIS)) {
                                    combinedPhotos.add(item);
                                } else {
                                    photoSnap.getRef().removeValue();
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }

                // Randomize selection order without a set fixed sequence!
                java.util.Collections.shuffle(combinedPhotos);

                if (callback != null) callback.onPhotosLoaded(combinedPhotos);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void listenActivePhotos(Context context, String targetUserId, PhotosCallback callback) {
        if (context == null || targetUserId == null || targetUserId.isEmpty()) {
            if (callback != null) callback.onPhotosLoaded(new ArrayList<>());
            return;
        }

        String pairKey = getPairKey(context);
        DatabaseReference ref = FirebaseDatabase.getInstance(FIREBASE_URL).getReference("photo_cycles").child(pairKey).child(targetUserId);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long now = System.currentTimeMillis();
                List<PhotoItem> activePhotos = new ArrayList<>();

                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        try {
                            PhotoItem item = ds.getValue(PhotoItem.class);
                            if (item != null && item.timestamp > 0 && (now - item.timestamp < EXPIRY_MILLIS)) {
                                activePhotos.add(item);
                            } else {
                                // Auto-purge expired photos > 24 hours
                                ds.getRef().removeValue();
                            }
                        } catch (Exception ignored) {}
                    }
                }

                // Sort chronologically (oldest to newest)
                java.util.Collections.sort(activePhotos, (a, b) -> Long.compare(a.timestamp, b.timestamp));

                if (callback != null) callback.onPhotosLoaded(activePhotos);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void deletePhoto(Context context, String targetUserId, String photoId, Runnable onComplete) {
        if (context == null || targetUserId == null || targetUserId.isEmpty() || photoId == null || photoId.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        String pairKey = getPairKey(context);
        FirebaseDatabase.getInstance(FIREBASE_URL).getReference("photo_cycles").child(pairKey).child(targetUserId).child(photoId).removeValue().addOnCompleteListener(task -> {
            if (onComplete != null) onComplete.run();
        });
    }
}
