package com.app.projectcecily;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WishlistActivity extends AppCompatActivity implements WishlistAdapter.OnItemActionListener {

    private WishlistAdapter adapter;
    private List<WishlistItem> itemList = new ArrayList<>();
    private DatabaseReference dbRef;
    private String userId;
    
    private String[] selectedBase64Images = {"", "", ""};
    private ImageView[] dialogImageViews = new ImageView[3];
    private int currentImageIndex = 0;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    processImage(imageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);

        userId = UserManager.getUserId(this);
        dbRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("wishlist");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Shared Wishlist");
        }

        RecyclerView         rv = findViewById(R.id.rvWishlist);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WishlistAdapter(itemList, userId, this);
        rv.setAdapter(adapter);

        View fab = findViewById(R.id.fabAddWishlist);
        fab.setOnClickListener(v -> showItemDialog(null));
        
        UIUtils.applyBottomMargin(fab);
        UIUtils.applyBottomPadding(rv);
        UIUtils.applyTopInsetPadding(rv);

        listenForWishlist();
    }

    private void listenForWishlist() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    WishlistItem item = ds.getValue(WishlistItem.class);
                    if (item != null) itemList.add(item);
                }
                Collections.sort(itemList, (a, b) -> Boolean.compare(a.isBought, b.isBought));
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showItemDialog(WishlistItem existingItem) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_wishlist, null);
        TextInputEditText etName = view.findViewById(R.id.etItemName);
        TextInputEditText etLink = view.findViewById(R.id.etItemLink);
        TextInputEditText etImg1 = view.findViewById(R.id.etImg1);
        
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggleMediaSource);
        LinearLayout layoutUrl = view.findViewById(R.id.layoutUrlInputs);
        LinearLayout layoutGallery = view.findViewById(R.id.layoutGalleryInputs);

        dialogImageViews[0] = view.findViewById(R.id.ivSelect1);
        dialogImageViews[1] = view.findViewById(R.id.ivSelect2);
        dialogImageViews[2] = view.findViewById(R.id.ivSelect3);

        // Reset selected images
        selectedBase64Images = new String[]{"", "", ""};

        if (existingItem != null) {
            etName.setText(existingItem.name);
            etLink.setText(existingItem.link);
            
            // Handle existing images
            if (existingItem.img1 != null && existingItem.img1.startsWith("data:")) {
                selectedBase64Images[0] = existingItem.img1;
                toggle.check(R.id.btnSourceGallery);
                layoutGallery.setVisibility(View.VISIBLE);
                layoutUrl.setVisibility(View.GONE);
                // Preview logic would go here, but for now we'll just keep the base64
            } else if (existingItem.img1 != null && !existingItem.img1.isEmpty()) {
                etImg1.setText(existingItem.img1);
                toggle.check(R.id.btnSourceUrl);
            }
            
            selectedBase64Images[1] = existingItem.img2 != null ? existingItem.img2 : "";
            selectedBase64Images[2] = existingItem.img3 != null ? existingItem.img3 : "";
        }

        for (int i = 0; i < 3; i++) {
            final int index = i;
            dialogImageViews[i].setOnClickListener(v -> {
                currentImageIndex = index;
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(intent);
            });
        }

        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnSourceUrl) {
                    layoutUrl.setVisibility(View.VISIBLE);
                    layoutGallery.setVisibility(View.GONE);
                } else {
                    layoutUrl.setVisibility(View.GONE);
                    layoutGallery.setVisibility(View.VISIBLE);
                }
            }
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle(existingItem == null ? "Add to Wishlist" : "Edit Item")
                .setView(view)
                .setPositiveButton(existingItem == null ? "Add" : "Save", (dialog, which) -> {
                    String name = etName.getText().toString();
                    String link = etLink.getText().toString();
                    
                    String id = (existingItem != null) ? existingItem.id : dbRef.push().getKey();
                    String img1, img2, img3;

                    if (toggle.getCheckedButtonId() == R.id.btnSourceUrl) {
                        img1 = etImg1.getText().toString();
                        img2 = ""; img3 = "";
                    } else {
                        img1 = selectedBase64Images[0];
                        img2 = selectedBase64Images[1];
                        img3 = selectedBase64Images[2];
                    }

                    if (!name.isEmpty()) {
                        WishlistItem item = new WishlistItem(id, name, link, userId, img1, img2, img3);
                        // Preserve bought/received status if editing
                        if (existingItem != null) {
                            item.isBought = existingItem.isBought;
                            item.isReceived = existingItem.isReceived;
                        }
                        dbRef.child(id).setValue(item);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void processImage(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            
            // Resize for prototype (stay under 100kb for RTDB safety)
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] bytes = baos.toByteArray();
            
            selectedBase64Images[currentImageIndex] = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.DEFAULT);
            dialogImageViews[currentImageIndex].setImageBitmap(scaled);
            dialogImageViews[currentImageIndex].setScaleType(ImageView.ScaleType.CENTER_CROP);
            
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBought(WishlistItem item) {
        dbRef.child(item.id).child("isBought").setValue(true);
        Toast.makeText(this, "Marked as bought!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReceived(WishlistItem item) {
        dbRef.child(item.id).child("isReceived").setValue(true);
        Toast.makeText(this, "Gift received!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEdit(WishlistItem item) {
        showItemDialog(item);
    }

    @Override
    public void onDelete(WishlistItem item) {
        dbRef.child(item.id).removeValue();
        Toast.makeText(this, "Item removed", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeUtils.applyAccentToActivity(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}