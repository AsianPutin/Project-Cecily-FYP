package com.app.projectcecily;

import android.content.Context;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.FirebaseDatabase;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PhotoCycleActivity extends AppCompatActivity {

    private MaterialButtonToggleGroup togglePhotoStream;
    private ViewPager2 viewPagerPhotos;
    private Button btnSendPhoto;
    private Button btnStreamPartner;

    private List<PhotoItem> myPhotosList = new ArrayList<>();
    private List<PhotoItem> partnerPhotosList = new ArrayList<>();
    private boolean isViewingMyPhotos = true;
    private boolean isUpdatingViewPagerProgrammatically = false;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_cycle);

        View appBarView = findViewById(R.id.appBarLayout);
        UIUtils.applyTopInsetToAppBar(appBarView);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Photo Cycle");
            }
        }

        togglePhotoStream = findViewById(R.id.togglePhotoStream);
        viewPagerPhotos = findViewById(R.id.viewPagerPhotos);
        btnSendPhoto = findViewById(R.id.btnSendPhoto);
        btnStreamPartner = findViewById(R.id.btnStreamPartner);

        setupLaunchers();
        updatePartnerButtonLabel();

        PhotoCyclePagerAdapter adapter = new PhotoCyclePagerAdapter();
        if (viewPagerPhotos != null) {
            viewPagerPhotos.setAdapter(adapter);
            viewPagerPhotos.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    isViewingMyPhotos = (position == 0);
                    isUpdatingViewPagerProgrammatically = true;
                    if (togglePhotoStream != null) {
                        togglePhotoStream.check(position == 0 ? R.id.btnStreamMe : R.id.btnStreamPartner);
                    }
                    isUpdatingViewPagerProgrammatically = false;
                }
            });
        }

        if (togglePhotoStream != null) {
            togglePhotoStream.check(R.id.btnStreamMe);
            togglePhotoStream.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked || isUpdatingViewPagerProgrammatically) return;
                int page = (checkedId == R.id.btnStreamMe) ? 0 : 1;
                isViewingMyPhotos = (page == 0);
                if (viewPagerPhotos != null) {
                    viewPagerPhotos.setCurrentItem(page, true);
                }
            });
        }

        if (btnSendPhoto != null) {
            btnSendPhoto.setOnClickListener(v -> showImageSourceChooser());
        }

        loadPhotoStreams();
    }

    private void updatePartnerButtonLabel() {
        if (btnStreamPartner == null) return;
        String partnerId = UserManager.getPartnerId(this);
        if (partnerId.isEmpty()) {
            btnStreamPartner.setText("Partner's Photos");
            return;
        }

        FirebaseDatabase.getInstance("https://i-must-finish-this-in-2026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("profiles").child(partnerId).child("name").get().addOnSuccessListener(snapshot -> {
            String name = snapshot.getValue(String.class);
            if (name != null && !name.trim().isEmpty()) {
                btnStreamPartner.setText(name.trim() + "'s Photos");
            } else {
                btnStreamPartner.setText("Partner's Photos");
            }
        }).addOnFailureListener(e -> btnStreamPartner.setText("Partner's Photos"));
    }

    private Uri tempCameraUri;

    private void setupLaunchers() {
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && tempCameraUri != null) {
                Bitmap photo = PhotoCycleManager.decodeHighResBitmap(this, tempCameraUri);
                if (photo != null) {
                    // Save unedited full-res photo to device gallery Pictures/ProjectCecily
                    PhotoCycleManager.saveUneditedPhotoToGallery(this, photo);
                    showCropEditorDialog(photo);
                }
            }
        });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri selectedImage = result.getData().getData();
                if (selectedImage != null) {
                    Bitmap bitmap = PhotoCycleManager.decodeHighResBitmap(this, selectedImage);
                    if (bitmap != null) {
                        showCropEditorDialog(bitmap);
                    }
                }
            }
        });
    }

    private void showImageSourceChooser() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_photo_cycle_options, null);
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        sheet.setContentView(view);

        View btnViewPhotos = view.findViewById(R.id.optionViewPhotos);
        if (btnViewPhotos != null) btnViewPhotos.setVisibility(View.GONE);

        view.findViewById(R.id.optionCamera).setOnClickListener(v -> {
            sheet.dismiss();
            launchCameraIntent();
        });

        view.findViewById(R.id.optionGallery).setOnClickListener(v -> {
            sheet.dismiss();
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        sheet.show();
    }

    private void launchCameraIntent() {
        tempCameraUri = PhotoCycleManager.createTempCameraUri(this);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (tempCameraUri != null) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, tempCameraUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        cameraLauncher.launch(intent);
    }

    private void showCropEditorDialog(Bitmap rawBitmap) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_crop_profile_pic, null);
        CropImageView ivCropTarget = view.findViewById(R.id.ivCropTarget);
        CircularOverlayView overlayView = view.findViewById(R.id.circularOverlayView);
        Button btnRotate = view.findViewById(R.id.btnRotateCrop);
        Button btnSave = view.findViewById(R.id.btnSaveCroppedPic);

        ivCropTarget.setImageAndReset(rawBitmap);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setNegativeButton("Cancel", null)
                .create();

        btnRotate.setOnClickListener(v -> ivCropTarget.rotate90());

        btnSave.setOnClickListener(v -> {
            Bitmap cropped = ivCropTarget.getCroppedCircleBitmap(overlayView.getCircleRect());
            Bitmap finalUpload = (cropped != null) ? cropped : rawBitmap;
            uploadCroppedPhoto(finalUpload);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void uploadCroppedPhoto(Bitmap bitmap) {
        PhotoCycleManager.uploadPhoto(this, bitmap, new PhotoCycleManager.UploadCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(PhotoCycleActivity.this, "Photo sent to partner's stream! 📸✨", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(PhotoCycleActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPhotoStreams() {
        String myUserId = UserManager.getUserId(this);
        String partnerId = UserManager.getPartnerId(this);

        if (!myUserId.isEmpty()) {
            PhotoCycleManager.listenActivePhotos(this, myUserId, photos -> {
                myPhotosList = (photos != null) ? photos : new ArrayList<>();
                if (viewPagerPhotos != null && viewPagerPhotos.getAdapter() != null) {
                    viewPagerPhotos.getAdapter().notifyDataSetChanged();
                }
            });
        }

        if (!partnerId.isEmpty()) {
            PhotoCycleManager.listenActivePhotos(this, partnerId, photos -> {
                partnerPhotosList = (photos != null) ? photos : new ArrayList<>();
                if (viewPagerPhotos != null && viewPagerPhotos.getAdapter() != null) {
                    viewPagerPhotos.getAdapter().notifyDataSetChanged();
                }
            });
        }
    }

    private Bitmap createBlurredBitmap(Bitmap original, int radius) {
        if (original == null) return null;
        try {
            Bitmap input = Bitmap.createScaledBitmap(original, Math.max(1, original.getWidth() / 4), Math.max(1, original.getHeight() / 4), false);
            Bitmap output = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Bitmap.Config.ARGB_8888);

            android.renderscript.RenderScript rs = android.renderscript.RenderScript.create(this);
            android.renderscript.ScriptIntrinsicBlur script = android.renderscript.ScriptIntrinsicBlur.create(rs, android.renderscript.Element.U8_4(rs));
            android.renderscript.Allocation allocIn = android.renderscript.Allocation.createFromBitmap(rs, input);
            android.renderscript.Allocation allocOut = android.renderscript.Allocation.createFromBitmap(rs, output);

            script.setRadius(Math.min(25, Math.max(1, radius)));
            script.setInput(allocIn);
            script.forEach(allocOut);
            allocOut.copyTo(output);
            rs.destroy();
            return output;
        } catch (Exception e) {
            return original;
        }
    }

    private void showFullPhotoDialog(Bitmap photoBitmap) {
        if (photoBitmap == null) return;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_view_full_image, null);
        ImageView ivBlurBg = view.findViewById(R.id.ivBlurBg);
        ImageView ivFull = view.findViewById(R.id.ivFullImage);
        View cardHolder = view.findViewById(R.id.cardImageHolder);

        Bitmap blurSrc = null;
        try {
            View rootView = getWindow().getDecorView().getRootView();
            rootView.setDrawingCacheEnabled(true);
            Bitmap cache = rootView.getDrawingCache();
            if (cache != null) {
                blurSrc = Bitmap.createBitmap(cache);
            }
            rootView.setDrawingCacheEnabled(false);
        } catch (Exception ignored) {}

        if (blurSrc == null) blurSrc = photoBitmap;

        Bitmap blurred = createBlurredBitmap(blurSrc, 25);
        if (blurred != null && ivBlurBg != null) {
            ivBlurBg.setImageBitmap(blurred);
        }

        if (photoBitmap != null && ivFull != null) {
            ivFull.setImageBitmap(photoBitmap);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                .setView(view)
                .create();

        view.setOnClickListener(v -> dialog.dismiss());
        if (cardHolder != null) {
            cardHolder.setOnClickListener(v -> {});
        }

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

    private class PhotoCyclePagerAdapter extends RecyclerView.Adapter<PhotoCyclePagerAdapter.PageViewHolder> {

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo_cycle_page, parent, false);
            return new PageViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            boolean isMyPage = (position == 0);
            List<PhotoItem> photos = isMyPage ? myPhotosList : partnerPhotosList;

            if (photos.isEmpty()) {
                holder.rvGrid.setVisibility(View.GONE);
                holder.layoutEmpty.setVisibility(View.VISIBLE);
                if (isMyPage) {
                    holder.tvEmpty.setText("No active photos sent by you.\nTap 'Send Photo' to start!");
                } else {
                    holder.tvEmpty.setText("No active photos from partner yet.\nAsk them to send one!");
                }
            } else {
                holder.layoutEmpty.setVisibility(View.GONE);
                holder.rvGrid.setVisibility(View.VISIBLE);

                holder.rvGrid.setLayoutManager(new GridLayoutManager(PhotoCycleActivity.this, 2));
                PhotoGridAdapter gridAdapter = new PhotoGridAdapter(photos, isMyPage);
                holder.rvGrid.setAdapter(gridAdapter);
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        class PageViewHolder extends RecyclerView.ViewHolder {
            RecyclerView rvGrid;
            View layoutEmpty;
            TextView tvEmpty;

            PageViewHolder(View v) {
                super(v);
                rvGrid = v.findViewById(R.id.rvPagePhotoGrid);
                layoutEmpty = v.findViewById(R.id.layoutPageEmpty);
                tvEmpty = v.findViewById(R.id.tvPageEmptyText);
            }
        }
    }

    private class PhotoGridAdapter extends RecyclerView.Adapter<PhotoGridAdapter.CardViewHolder> {
        private final List<PhotoItem> photos;
        private final boolean isMyStream;

        PhotoGridAdapter(List<PhotoItem> photos, boolean isMyStream) {
            this.photos = photos;
            this.isMyStream = isMyStream;
        }

        @NonNull
        @Override
        public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo_card, parent, false);
            return new CardViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
            PhotoItem item = photos.get(position);

            try {
                String clean = item.photoBase64.contains(",") ? item.photoBase64.split(",")[1] : item.photoBase64;
                byte[] bytes = Base64.decode(clean, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                holder.ivPhoto.setImageBitmap(bmp);

                holder.ivPhoto.setOnClickListener(v -> showFullPhotoDialog(bmp));
            } catch (Exception e) {
                holder.ivPhoto.setImageBitmap(null);
            }

            long elapsed = System.currentTimeMillis() - item.timestamp;
            long hoursLeft = Math.max(0, 24 - (elapsed / (60 * 60 * 1000L)));
            holder.tvTimeAgo.setText(hoursLeft + "h left");

            if (isMyStream) {
                holder.btnDelete.setVisibility(View.VISIBLE);
                holder.btnDelete.setOnClickListener(v -> {
                    new MaterialAlertDialogBuilder(PhotoCycleActivity.this)
                            .setTitle("Delete Photo?")
                            .setMessage("Are you sure you want to delete this photo from the 24-hour stream?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                String userId = UserManager.getUserId(PhotoCycleActivity.this);
                                PhotoCycleManager.deletePhoto(PhotoCycleActivity.this, userId, item.photoId, () -> {
                                    Toast.makeText(PhotoCycleActivity.this, "Photo deleted!", Toast.LENGTH_SHORT).show();
                                    loadPhotoStreams();
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            } else {
                holder.btnDelete.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return photos.size();
        }

        class CardViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPhoto;
            TextView tvTimeAgo;
            ImageView btnDelete;

            CardViewHolder(View v) {
                super(v);
                ivPhoto = v.findViewById(R.id.ivItemPhoto);
                tvTimeAgo = v.findViewById(R.id.tvItemTimeAgo);
                btnDelete = v.findViewById(R.id.btnItemDelete);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeUtils.applyAccentToActivity(this);

        int accentColor = ThemeUtils.getAccentColor(this);
        if (btnSendPhoto != null) {
            btnSendPhoto.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
