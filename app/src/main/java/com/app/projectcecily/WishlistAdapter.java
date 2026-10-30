package com.app.projectcecily;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.ViewHolder> {

    private List<WishlistItem> items;
    private String currentUserId;
    private OnItemActionListener listener;

    public interface OnItemActionListener {
        void onBought(WishlistItem item);
        void onReceived(WishlistItem item);
        void onEdit(WishlistItem item);
        void onDelete(WishlistItem item);
    }

    public WishlistAdapter(List<WishlistItem> items, String currentUserId, OnItemActionListener listener) {
        this.items = items;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wishlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WishlistItem item = items.get(position);
        holder.tvName.setText(item.name);
        holder.tvLink.setText(item.link != null && !item.link.trim().isEmpty() ? item.link : "");
        holder.tvLink.setVisibility(item.link != null && !item.link.trim().isEmpty() ? View.VISIBLE : View.GONE);

        boolean isOwner = item.ownerId != null && item.ownerId.equals(currentUserId);

        bindImages(holder, item);

        if (isOwner) {
            // ITEM CREATOR / OWNER:
            // Secret Gift Rule: NEVER show bought status or dimmed card to the creator (preserve surprise!)
            holder.btnMarkBought.setVisibility(View.GONE);
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.card.setAlpha(1.0f);
        } else {
            // PARTNER / GIFT BUYER:
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
            holder.btnMarkBought.setVisibility(View.VISIBLE);

            if (item.isBought) {
                holder.btnMarkBought.setText("Bought! (Undo)");
                holder.btnMarkBought.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#448AFF")));
                holder.card.setAlpha(0.6f);
            } else {
                holder.btnMarkBought.setText("Mark Bought");
                holder.btnMarkBought.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#25D366")));
                holder.card.setAlpha(1.0f);
            }
        }

        holder.btnMarkBought.setOnClickListener(v -> listener.onBought(item));
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    private void bindImages(ViewHolder holder, WishlistItem item) {
        List<String> validImages = new ArrayList<>();
        if (item.img1 != null && !item.img1.trim().isEmpty()) validImages.add(item.img1);
        if (item.img2 != null && !item.img2.trim().isEmpty()) validImages.add(item.img2);
        if (item.img3 != null && !item.img3.trim().isEmpty()) validImages.add(item.img3);

        if (validImages.isEmpty()) {
            holder.layoutSoloContainer.setVisibility(View.GONE);
            holder.layoutCollageContainer.setVisibility(View.GONE);
            return;
        }

        int hIdx = item.heroIndex;
        if (hIdx < 0 || hIdx >= validImages.size()) hIdx = 0;

        String heroUrl = validImages.remove(hIdx);

        if (validImages.isEmpty()) {
            // Solo Image: Centered square in middle of card
            holder.layoutSoloContainer.setVisibility(View.VISIBLE);
            holder.layoutCollageContainer.setVisibility(View.GONE);
            loadImage(holder.ivSoloImage, heroUrl);
        } else {
            // Collage Layout (2 or 3 photos)
            holder.layoutSoloContainer.setVisibility(View.GONE);
            holder.layoutCollageContainer.setVisibility(View.VISIBLE);

            loadImage(holder.ivHeroImage, heroUrl);

            if (validImages.size() == 1) {
                // 2 Photos Total: Left Hero (60%), Right 2nd photo (40%)
                holder.cardSideTop.setVisibility(View.VISIBLE);
                holder.cardSideBottom.setVisibility(View.GONE);
                loadImage(holder.ivSideTop, validImages.get(0));
            } else {
                // 3 Photos Total: Left Hero (60%), Right 2 photos vertically stacked (40%)
                holder.cardSideTop.setVisibility(View.VISIBLE);
                holder.cardSideBottom.setVisibility(View.VISIBLE);
                loadImage(holder.ivSideTop, validImages.get(0));
                loadImage(holder.ivSideBottom, validImages.get(1));
            }
        }
    }

    private void loadImage(ImageView iv, String url) {
        if (url != null && !url.isEmpty()) {
            Glide.with(iv.getContext()).load(url).placeholder(R.mipmap.ic_launcher).into(iv);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLink;
        Button btnMarkBought, btnEdit, btnDelete;
        View layoutSoloContainer, layoutCollageContainer;
        ImageView ivSoloImage, ivHeroImage, ivSideTop, ivSideBottom;
        MaterialCardView card, cardSideTop, cardSideBottom;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvLink = itemView.findViewById(R.id.tvItemLink);
            btnMarkBought = itemView.findViewById(R.id.btnMarkBought);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            layoutSoloContainer = itemView.findViewById(R.id.layoutSoloContainer);
            layoutCollageContainer = itemView.findViewById(R.id.layoutCollageContainer);

            ivSoloImage = itemView.findViewById(R.id.ivSoloImage);
            ivHeroImage = itemView.findViewById(R.id.ivHeroImage);
            ivSideTop = itemView.findViewById(R.id.ivSideTop);
            ivSideBottom = itemView.findViewById(R.id.ivSideBottom);

            cardSideTop = itemView.findViewById(R.id.cardSideTop);
            cardSideBottom = itemView.findViewById(R.id.cardSideBottom);

            card = itemView.findViewById(R.id.wishlistCard);
        }
    }
}
