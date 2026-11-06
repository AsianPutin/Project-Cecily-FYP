package com.app.projectcecily;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
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

    private ViewPager2 vpWishlist;
    private WishlistPagerAdapter pageAdapter;
    private List<WishlistItem> allRawItems = new ArrayList<>();
    private DatabaseReference dbRef;
    private String userId;
    private boolean isUserTabClicking = true;

    private String[] selectedBase64Images = {"", "", ""};
    private ImageView[] slotImageViews = new ImageView[3];
    private ImageView ivHeroBoxPreview;
    private int currentSlotIndex = 0;
    private int activeHeroIndex = 0;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (currentSlotIndex == 0) {
                        processUriWithCrop(imageUri);
                    } else {
                        processImageAuto(imageUri, currentSlotIndex);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);

        userId = UserManager.getUserId(this);
        String partnerId = UserManager.getPartnerId(this);
        String pairKey = partnerId.isEmpty() ? userId : (userId.compareTo(partnerId) < 0 ? userId + "_" + partnerId : partnerId + "_" + userId);
        dbRef = FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("wishlists").child(pairKey);

        View appBarView = findViewById(R.id.appBarLayout);
        UIUtils.applyTopInsetToAppBar(appBarView);

        View toolbarView = findViewById(R.id.toolbar);
        if (toolbarView instanceof com.google.android.material.appbar.MaterialToolbar) {
            setSupportActionBar((com.google.android.material.appbar.MaterialToolbar) toolbarView);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Shared Wishlist");
            }
        }

        vpWishlist = findViewById(R.id.vpWishlist);
        pageAdapter = new WishlistPagerAdapter();
        vpWishlist.setAdapter(pageAdapter);

        MaterialButtonToggleGroup tabToggle = findViewById(R.id.wishlistTabToggle);
        if (tabToggle != null) {
            tabToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked && isUserTabClicking) {
                    int targetPage = (checkedId == R.id.btnMyWishlistTab) ? 0 : 1;
                    if (vpWishlist != null && vpWishlist.getCurrentItem() != targetPage) {
                        vpWishlist.setCurrentItem(targetPage, true);
                    }
                }
            });
        }

        vpWishlist.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (tabToggle != null) {
                    isUserTabClicking = false;
                    tabToggle.check(position == 0 ? R.id.btnMyWishlistTab : R.id.btnPartnerWishlistTab);
                    isUserTabClicking = true;
                }
            }
        });

        View fab = findViewById(R.id.fabAddWishlist);
        fab.setOnClickListener(v -> showItemDialog(null));

        View cardBannerAd = findViewById(R.id.cardBannerAd);
        View btnDismissAd = findViewById(R.id.btnDismissAd);
        TextView tvAdText = findViewById(R.id.tvAdBannerText);

        BannerAdHelper.setupBanner(this, cardBannerAd, btnDismissAd, tvAdText, () -> {
            if (fab != null) {
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp =
                        (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) fab.getLayoutParams();
                lp.bottomToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
                lp.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                lp.bottomMargin = (int) (24 * getResources().getDisplayMetrics().density);
                fab.setLayoutParams(lp);
            }
        });

        UIUtils.applyBottomMargin(fab);
        listenForWishlist();
    }

    private void listenForWishlist() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allRawItems.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    WishlistItem item = ds.getValue(WishlistItem.class);
                    if (item != null) allRawItems.add(item);
                }
                if (pageAdapter != null) {
                    pageAdapter.updateItems(allRawItems);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        String partnerId = UserManager.getPartnerId(this);
        if (!partnerId.isEmpty()) {
            FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .getReference("profiles").child(partnerId).child("name").get().addOnSuccessListener(snapshot -> {
                String pName = snapshot.getValue(String.class);
                if (pName != null && !pName.isEmpty()) {
                    Button btnPartnerTab = findViewById(R.id.btnPartnerWishlistTab);
                    if (btnPartnerTab != null) {
                        btnPartnerTab.setText(pName + "'s Wishlist");
                    }
                }
            });
        }
    }

    private class WishlistPagerAdapter extends RecyclerView.Adapter<WishlistPagerAdapter.PageViewHolder> {
        private final List<WishlistItem> myItems = new ArrayList<>();
        private final List<WishlistItem> partnerItems = new ArrayList<>();
        private final WishlistAdapter myAdapter;
        private final WishlistAdapter partnerAdapter;

        public WishlistPagerAdapter() {
            myAdapter = new WishlistAdapter(myItems, userId, WishlistActivity.this);
            partnerAdapter = new WishlistAdapter(partnerItems, userId, WishlistActivity.this);
        }

        public void updateItems(List<WishlistItem> allItems) {
            myItems.clear();
            partnerItems.clear();
            for (WishlistItem item : allItems) {
                boolean isOwner = item.ownerId != null && item.ownerId.equals(userId);
                if (isOwner) {
                    myItems.add(item);
                } else {
                    partnerItems.add(item);
                }
            }
            Collections.sort(myItems, (a, b) -> Boolean.compare(a.isBought || a.isReceived, b.isBought || b.isReceived));
            Collections.sort(partnerItems, (a, b) -> Boolean.compare(a.isBought || a.isReceived, b.isBought || b.isReceived));

            myAdapter.notifyDataSetChanged();
            partnerAdapter.notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            RecyclerView rv = new RecyclerView(parent.getContext());
            rv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            rv.setClipToPadding(false);
            int padding = (int) (8 * parent.getContext().getResources().getDisplayMetrics().density);
            rv.setPadding(padding, padding, padding, padding);
            rv.setLayoutManager(new LinearLayoutManager(parent.getContext()));
            UIUtils.applyBottomPadding(rv);
            return new PageViewHolder(rv);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            if (position == 0) {
                holder.rvPage.setAdapter(myAdapter);
            } else {
                holder.rvPage.setAdapter(partnerAdapter);
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        class PageViewHolder extends RecyclerView.ViewHolder {
            RecyclerView rvPage;

            public PageViewHolder(@NonNull View itemView) {
                super(itemView);
                rvPage = (RecyclerView) itemView;
            }
        }
    }

    private void showItemDialog(WishlistItem existingItem) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_wishlist, null);
        TextInputEditText etName = view.findViewById(R.id.etItemName);
        TextInputEditText etLink = view.findViewById(R.id.etItemLink);

        ivHeroBoxPreview = view.findViewById(R.id.ivHeroBoxPreview);
        slotImageViews[0] = view.findViewById(R.id.ivSlot1);
        slotImageViews[1] = view.findViewById(R.id.ivSlot2);
        slotImageViews[2] = view.findViewById(R.id.ivSlot3);

        selectedBase64Images = new String[]{"", "", ""};
        activeHeroIndex = 0;

        if (existingItem != null) {
            etName.setText(existingItem.name != null ? existingItem.name : "");
            etLink.setText(existingItem.link != null ? existingItem.link : "");

            selectedBase64Images[0] = existingItem.img1 != null ? existingItem.img1 : "";
            selectedBase64Images[1] = existingItem.img2 != null ? existingItem.img2 : "";
            selectedBase64Images[2] = existingItem.img3 != null ? existingItem.img3 : "";

            activeHeroIndex = existingItem.heroIndex;
        }

        updateDialogSlotPreviews();

        String initialTitle = etName.getText() != null ? etName.getText().toString().trim() : "";
        String initialLink = etLink.getText() != null ? etLink.getText().toString().trim() : "";
        String initialImg1 = selectedBase64Images[0];
        String initialImg2 = selectedBase64Images[1];
        String initialImg3 = selectedBase64Images[2];
        int initialHero = activeHeroIndex;

        for (int i = 0; i < 3; i++) {
            final int slotIdx = i;
            View cardSlot = view.findViewById(slotIdx == 0 ? R.id.cardSlot1 : (slotIdx == 1 ? R.id.cardSlot2 : R.id.cardSlot3));
            cardSlot.setOnClickListener(v -> {
                if (selectedBase64Images[slotIdx].isEmpty()) {
                    currentSlotIndex = slotIdx;
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    galleryLauncher.launch(intent);
                } else {
                    activeHeroIndex = slotIdx;
                    updateDialogSlotPreviews();
                    Toast.makeText(WishlistActivity.this, "Set photo as Hero Image!", Toast.LENGTH_SHORT).show();
                }
            });

            cardSlot.setOnLongClickListener(v -> {
                if (!selectedBase64Images[slotIdx].isEmpty()) {
                    selectedBase64Images[slotIdx] = "";
                    if (activeHeroIndex == slotIdx) activeHeroIndex = 0;
                    updateDialogSlotPreviews();
                    Toast.makeText(WishlistActivity.this, "Photo removed", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(existingItem == null ? "Add to Wishlist" : "Edit Item")
                .setView(view)
                .setPositiveButton(existingItem == null ? "Add" : "Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        java.util.function.Supplier<Boolean> hasUnsavedChanges = () -> {
            String curTitle = etName.getText() != null ? etName.getText().toString().trim() : "";
            String curLink = etLink.getText() != null ? etLink.getText().toString().trim() : "";
            return !curTitle.equals(initialTitle) ||
                   !curLink.equals(initialLink) ||
                   !selectedBase64Images[0].equals(initialImg1) ||
                   !selectedBase64Images[1].equals(initialImg2) ||
                   !selectedBase64Images[2].equals(initialImg3) ||
                   activeHeroIndex != initialHero;
        };

        Runnable promptDiscardChanges = () -> {
            if (hasUnsavedChanges.get()) {
                new MaterialAlertDialogBuilder(WishlistActivity.this)
                        .setTitle("Discard Unsaved Changes?")
                        .setMessage("You have unsaved changes. Are you sure you want to discard them?")
                        .setPositiveButton("Discard", (d, which) -> dialog.dismiss())
                        .setNegativeButton("Keep Editing", null)
                        .show();
            } else {
                dialog.dismiss();
            }
        };

        dialog.show();

        Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnSave.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String link = etLink.getText() != null ? etLink.getText().toString().trim() : "";

            if (name.isEmpty()) {
                Toast.makeText(WishlistActivity.this, "Please enter an item name.", Toast.LENGTH_SHORT).show();
                return;
            }

            String id = (existingItem != null) ? existingItem.id : dbRef.push().getKey();
            WishlistItem item = new WishlistItem(id, name, link, userId,
                    selectedBase64Images[0], selectedBase64Images[1], selectedBase64Images[2], activeHeroIndex);

            if (existingItem != null) {
                item.isBought = existingItem.isBought;
                item.isReceived = existingItem.isReceived;
            }

            if (id != null) {
                dbRef.child(id).setValue(item);
                Toast.makeText(WishlistActivity.this, existingItem == null ? "Item added!" : "Item saved!", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        Button btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        btnCancel.setOnClickListener(v -> promptDiscardChanges.run());

        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                promptDiscardChanges.run();
                return true;
            }
            return false;
        });
    }

    private void updateDialogSlotPreviews() {
        List<Integer> filledIndices = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (!selectedBase64Images[i].isEmpty()) {
                filledIndices.add(i);
                displayBase64InImageView(slotImageViews[i], selectedBase64Images[i]);
            } else {
                slotImageViews[i].setImageResource(android.R.drawable.ic_menu_camera);
            }
        }

        if (filledIndices.isEmpty()) {
            activeHeroIndex = 0;
            ivHeroBoxPreview.setImageResource(R.drawable.ic_blank_user);
        } else {
            if (!filledIndices.contains(activeHeroIndex)) {
                activeHeroIndex = filledIndices.get(0);
            }
            displayBase64InImageView(ivHeroBoxPreview, selectedBase64Images[activeHeroIndex]);
        }
    }

    private void processUriWithCrop(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap rawBitmap = BitmapFactory.decodeStream(is);
            if (rawBitmap != null) {
                showSquareCropDialog(rawBitmap);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSquareCropDialog(Bitmap rawBitmap) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_crop_profile_pic, null);
        CropImageView ivCropTarget = view.findViewById(R.id.ivCropTarget);
        CircularOverlayView overlayView = view.findViewById(R.id.circularOverlayView);
        Button btnRotate = view.findViewById(R.id.btnRotateCrop);
        Button btnSave = view.findViewById(R.id.btnSaveCroppedPic);

        ivCropTarget.setImageAndReset(rawBitmap);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Crop Square Image")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .create();

        btnRotate.setOnClickListener(v -> ivCropTarget.rotate90());

        btnSave.setOnClickListener(v -> {
            Bitmap cropped = ivCropTarget.getCroppedCircleBitmap(overlayView.getCircleRect());
            if (cropped == null) cropped = rawBitmap;
            saveProcessedBitmapToSlot(cropped, currentSlotIndex);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void processImageAuto(Uri uri, int slotIndex) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap rawBitmap = BitmapFactory.decodeStream(is);
            if (rawBitmap != null) {
                saveProcessedBitmapToSlot(rawBitmap, slotIndex);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProcessedBitmapToSlot(Bitmap bitmap, int slotIndex) {
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 400, 400, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos);
        byte[] bytes = baos.toByteArray();

        selectedBase64Images[slotIndex] = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.DEFAULT);
        updateDialogSlotPreviews();
    }

    private void displayBase64InImageView(ImageView iv, String base64Str) {
        try {
            String clean = base64Str.contains(",") ? base64Str.split(",")[1] : base64Str;
            byte[] bytes = Base64.decode(clean, Base64.DEFAULT);
            Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bm != null) {
                iv.setImageBitmap(bm);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onBought(WishlistItem item) {
        if (item.isBought) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Undo Bought Status?")
                    .setMessage("Are you sure you want to unmark '" + item.name + "' as bought?")
                    .setPositiveButton("Yes, Undo", (dialog, which) -> {
                        dbRef.child(item.id).child("isBought").setValue(false);
                        Toast.makeText(WishlistActivity.this, "Item unmarked as bought", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Mark as Bought?")
                    .setMessage("Are you sure you bought '" + item.name + "' for your partner?")
                    .setPositiveButton("Yes, Mark Bought", (dialog, which) -> {
                        dbRef.child(item.id).child("isBought").setValue(true);
                        Toast.makeText(WishlistActivity.this, "Marked as bought!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    @Override
    public void onReceived(WishlistItem item) {
        if (item.isReceived) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Undo Received Status?")
                    .setMessage("Are you sure you want to unmark '" + item.name + "' as received?")
                    .setPositiveButton("Yes, Undo", (dialog, which) -> {
                        dbRef.child(item.id).child("isReceived").setValue(false);
                        Toast.makeText(WishlistActivity.this, "Item unmarked as received", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Mark as Received?")
                    .setMessage("Are you sure you received '" + item.name + "'?")
                    .setPositiveButton("Yes, Mark Received", (dialog, which) -> {
                        dbRef.child(item.id).child("isReceived").setValue(true);
                        Toast.makeText(WishlistActivity.this, "Marked as received!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    @Override
    public void onEdit(WishlistItem item) {
        showItemDialog(item);
    }

    @Override
    public void onDelete(WishlistItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Wishlist Item?")
                .setMessage("Are you sure you want to delete '" + item.name + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbRef.child(item.id).removeValue();
                    Toast.makeText(WishlistActivity.this, "Item deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
