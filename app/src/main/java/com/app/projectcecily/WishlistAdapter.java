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
import java.util.List;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.ViewHolder> {

    private List<WishlistItem> items;
    private String currentUserId;
    private OnItemActionListener listener;

    public interface OnItemActionListener {
        void onBought(WishlistItem item);
        void onReceived(WishlistItem item);
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
        holder.tvLink.setText(item.link);

        boolean isOwner = item.ownerId.equals(currentUserId);
        
        // Image loading logic
        loadImage(holder.iv1, item.img1);
        loadImage(holder.iv2, item.img2);
        loadImage(holder.iv3, item.img3);

        // Visibility Logic
        // Buy button is for both if not bought
        holder.btnBought.setVisibility(!item.isBought ? View.VISIBLE : View.GONE);
        
        // Received is for owner if bought but not received
        holder.btnReceived.setVisibility(isOwner && item.isBought && !item.isReceived ? View.VISIBLE : View.GONE);

        // Delete is ALWAYS for Owner, but never for partner
        holder.btnDelete.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        // Styling
        if (item.isBought) {
            holder.card.setAlpha(0.6f);
            if (!item.name.contains("(Bought!)")) {
                holder.tvName.setText(item.name + " (Bought!)");
            }
        } else {
            holder.card.setAlpha(1.0f);
        }

        if (item.isReceived) {
            holder.card.setAlpha(0.3f);
        }

        holder.btnBought.setOnClickListener(v -> listener.onBought(item));
        holder.btnReceived.setOnClickListener(v -> listener.onReceived(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    private void loadImage(ImageView iv, String url) {
        if (url != null && !url.isEmpty()) {
            iv.setVisibility(View.VISIBLE);
            Glide.with(iv.getContext()).load(url).placeholder(R.mipmap.ic_launcher).into(iv);
        } else {
            iv.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLink;
        Button btnBought, btnReceived, btnDelete;
        ImageView iv1, iv2, iv3;
        MaterialCardView card;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvLink = itemView.findViewById(R.id.tvItemLink);
            btnBought = itemView.findViewById(R.id.btnMarkBought);
            btnReceived = itemView.findViewById(R.id.btnMarkReceived);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            iv1 = itemView.findViewById(R.id.iv1);
            iv2 = itemView.findViewById(R.id.iv2);
            iv3 = itemView.findViewById(R.id.iv3);
            card = itemView.findViewById(R.id.wishlistCard);
        }
    }
}