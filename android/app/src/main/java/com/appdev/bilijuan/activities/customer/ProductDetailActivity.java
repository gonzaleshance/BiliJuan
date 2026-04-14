package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.adapters.CommentAdapter;
import com.appdev.bilijuan.databinding.ActivityProductDetailBinding;
import com.appdev.bilijuan.models.CartItem;
import com.appdev.bilijuan.models.Comment;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.utils.CartBottomSheet;
import com.appdev.bilijuan.utils.CartHelper;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.ImageHelper;
import com.appdev.bilijuan.utils.NotificationHelper;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.appdev.bilijuan.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {

    private ActivityProductDetailBinding binding;
    private String productId;
    private String currentUid;
    private Product product;
    private final List<Comment> comments = new ArrayList<>();
    private CommentAdapter commentAdapter;
    private float selectedStars = 0f;
    private int quantity = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        productId  = getIntent().getStringExtra("productId");
        currentUid = FirebaseHelper.getCurrentUid();

        if (productId == null) { finish(); return; }

        setupComments();
        setupClickListeners();
        loadProduct();
        loadComments();
        updateCartUI();
    }

    private void setupComments() {
        commentAdapter = new CommentAdapter(comments);
        binding.rvComments.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComments.setAdapter(commentAdapter);
        binding.rvComments.setNestedScrollingEnabled(false);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnReport.setOnClickListener(v -> showReportSheet());
        binding.btnLike.setOnClickListener(v -> toggleLike());

        View.OnClickListener starListener = v -> {
            int id = v.getId();
            if      (id == binding.star1.getId()) selectedStars = 1f;
            else if (id == binding.star2.getId()) selectedStars = 2f;
            else if (id == binding.star3.getId()) selectedStars = 3f;
            else if (id == binding.star4.getId()) selectedStars = 4f;
            else if (id == binding.star5.getId()) selectedStars = 5f;
            updateStarDisplay();
        };
        binding.star1.setOnClickListener(starListener);
        binding.star2.setOnClickListener(starListener);
        binding.star3.setOnClickListener(starListener);
        binding.star4.setOnClickListener(starListener);
        binding.star5.setOnClickListener(starListener);

        binding.btnSubmitComment.setOnClickListener(v -> submitComment());

        // Quantity Listeners
        binding.btnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                updateQuantityDisplay();
            }
        });
        binding.btnPlus.setOnClickListener(v -> {
            quantity++;
            updateQuantityDisplay();
        });

        binding.btnOrder.setOnClickListener(v -> {
            if (product == null) return;
            if (!product.isAvailable()) {
                Toast.makeText(this, "This item is currently unavailable.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, PinLocationActivity.class);
            intent.putExtra("productId", productId);
            intent.putExtra("quantity", quantity);
            startActivity(intent);
        });

        binding.btnAddToCart.setOnClickListener(v -> handleAddToCart());

        binding.btnCart.setOnClickListener(v -> {
            CartBottomSheet.show(this, this::updateCartUI);
        });
    }

    private void updateQuantityDisplay() {
        binding.tvQuantity.setText(String.valueOf(quantity));
        if (product != null) {
            double total = product.getPrice() * quantity;
            binding.tvTotalPrice.setText(String.format(Locale.getDefault(), "₱%.0f", total));
        }
    }

    private void handleAddToCart() {
        if (product == null) return;
        if (!product.isAvailable()) {
            Toast.makeText(this, "This item is currently unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }

        String cartSellerId = CartHelper.getCartSellerId(this);
        if (cartSellerId != null && !cartSellerId.equals(product.getSellerId())) {
            new AlertDialog.Builder(this)
                    .setTitle("Replace Cart?")
                    .setMessage("Your cart contains items from another store. Do you want to clear your current cart and add this item?")
                    .setPositiveButton("Clear and Add", (dialog, which) -> {
                        CartHelper.clearCart(this);
                        addToCart();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            addToCart();
        }
    }

    private void addToCart() {
        CartItem item = new CartItem(
                product.getProductId(),
                product.getName(),
                product.getPrice(),
                quantity,
                product.getSellerId(),
                product.getSellerName(),
                product.getImageBase64()
        );
        CartHelper.addToCart(this, item);
        animateCartButton();
        updateCartUI();
        Toast.makeText(this, "Added to cart!", Toast.LENGTH_SHORT).show();
    }

    private void updateCartUI() {
        int count = CartHelper.getCartCount(this);
        if (count > 0) {
            binding.btnCart.setVisibility(View.VISIBLE);
            binding.tvCartBadge.setVisibility(View.VISIBLE);
            binding.tvCartBadge.setText(String.valueOf(count));
        } else {
            binding.btnCart.setVisibility(View.GONE);
            binding.tvCartBadge.setVisibility(View.GONE);
        }
    }

    private void animateCartButton() {
        binding.btnCart.setVisibility(View.VISIBLE);
        binding.btnCart.clearAnimation();
        binding.btnCart.setScaleX(1f);
        binding.btnCart.setScaleY(1f);

        binding.btnCart.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(150)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    binding.btnCart.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                }).start();
    }

    private void loadProduct() {
        FirebaseHelper.getDb().collection("products").document(productId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { finish(); return; }
                    product = doc.toObject(Product.class);
                    product.setProductId(doc.getId());
                    bindProduct();
                });
    }

    private void bindProduct() {
        binding.tvProductName.setText(product.getName());
        binding.tvSellerName.setText("by " + product.getSellerName());
        binding.tvCategory.setText(product.getCategory());
        binding.tvPrice.setText(String.format("₱%.0f", product.getPrice()));
        binding.tvDescription.setText(product.getDescription());
        binding.tvStars.setText(String.format("%.1f", product.getStars()));
        binding.tvLikeCount.setText(String.valueOf(product.getLikes()));
        binding.tvRatingCount.setText("(" + product.getRatingCount() + " ratings)");

        updateQuantityDisplay();

        boolean available = product.isAvailable();
        binding.tvAvailability.setText(available ? "Available" : "Unavailable");
        binding.tvAvailability.setTextColor(getColor(available ? R.color.success : R.color.error));
        
        // Show/Hide unavailable notice and disable buttons
        binding.cardUnavailableNotice.setVisibility(available ? View.GONE : View.VISIBLE);
        binding.cardActionButtons.setAlpha(available ? 1.0f : 0.5f);
        binding.btnOrder.setEnabled(available);
        binding.btnAddToCart.setEnabled(available);
        binding.btnPlus.setEnabled(available);
        binding.btnMinus.setEnabled(available);

        if (product.getImageBase64() != null && !product.getImageBase64().isEmpty()) {
            Bitmap bm = ImageHelper.base64ToBitmap(product.getImageBase64());
            if (bm != null) binding.ivProduct.setImageBitmap(bm);
        }
    }

    private void toggleLike() {
        if (currentUid == null) return;
        long newLikes = product.getLikes() + 1;
        FirebaseHelper.getDb().collection("products").document(productId)
                .update("likes", FieldValue.increment(1),
                        "shopScore", FieldValue.increment(0.5))
                .addOnSuccessListener(v -> {
                    binding.tvLikeCount.setText(String.valueOf(newLikes));
                    binding.btnLike.setAlpha(0.5f);
                    binding.btnLike.setEnabled(false);
                });
    }

    private void updateStarDisplay() {
        android.widget.ImageView[] views = {
                binding.star1, binding.star2, binding.star3, binding.star4, binding.star5
        };
        for (int i = 0; i < 5; i++) {
            views[i].setAlpha(i < selectedStars ? 1.0f : 0.3f);
        }
    }

    private void submitComment() {
        String text = binding.etComment.getText() != null
                ? binding.etComment.getText().toString().trim() : "";
        if (text.isEmpty()) {
            binding.etComment.setError("Write something first");
            return;
        }
        if (selectedStars == 0) {
            Toast.makeText(this, "Please select a star rating", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUid == null) return;

        binding.btnSubmitComment.setEnabled(false);

        FirebaseHelper.getDb().collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    String userName = doc.exists() ? doc.getString("name") : "Anonymous";
                    Comment comment = new Comment(productId, currentUid, userName, text, selectedStars);

                    FirebaseHelper.getDb().collection("comments").add(comment)
                            .addOnSuccessListener(ref -> {
                                ref.update("commentId", ref.getId());
                                updateProductRating();
                                binding.etComment.setText("");
                                selectedStars = 0;
                                updateStarDisplay();
                                binding.btnSubmitComment.setEnabled(true);
                                Toast.makeText(this, "Review submitted!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                binding.btnSubmitComment.setEnabled(true);
                                Toast.makeText(this, "Failed to submit", Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void updateProductRating() {
        FirebaseHelper.getDb().collection("comments")
                .whereEqualTo("productId", productId).get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return;
                    float total = 0;
                    for (QueryDocumentSnapshot d : snap) {
                        Comment c = d.toObject(Comment.class);
                        total += c.getStars();
                    }
                    float avg = total / snap.size();
                    double shopScore = avg + (product.getLikes() * 0.5);
                    FirebaseHelper.getDb().collection("products").document(productId)
                            .update("stars", avg,
                                    "ratingCount", snap.size(),
                                    "shopScore", shopScore);
                });
    }

    private void loadComments() {
        FirebaseHelper.getDb().collection("comments")
                .whereEqualTo("productId", productId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    comments.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Comment c = doc.toObject(Comment.class);
                        c.setCommentId(doc.getId());
                        comments.add(c);
                    }
                    commentAdapter.notifyDataSetChanged();
                    binding.emptyComments.setVisibility(
                            comments.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void showReportSheet() {
        if (product == null) return;
        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(
                        this, R.style.BottomSheetStyle);
        android.view.View v = android.view.LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_report, null);
        sheet.setContentView(v);

        String[] reasons = com.appdev.bilijuan.models.Report.REASONS;
        final int[] selected = {-1};

        android.widget.ListView listView = v.findViewById(R.id.listReasons);
        android.widget.ArrayAdapter<String> adapter =
                new android.widget.ArrayAdapter<>(this,
                        android.R.layout.simple_list_item_single_choice, reasons);
        listView.setAdapter(adapter);
        listView.setChoiceMode(android.widget.ListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener((p, vi, pos, id) -> selected[0] = pos);

        v.findViewById(R.id.btnSubmitReport).setOnClickListener(btn -> {
            if (selected[0] == -1) {
                Toast.makeText(this, "Please select a reason", Toast.LENGTH_SHORT).show();
                return;
            }
            sheet.dismiss();
            submitReport(reasons[selected[0]]);
        });

        v.findViewById(R.id.btnCancelReport).setOnClickListener(btn -> sheet.dismiss());
        sheet.show();
    }

    private void submitReport(String reason) {
        if (currentUid == null || product == null) return;

        FirebaseHelper.getDb().collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    String customerName = doc.exists()
                            ? doc.getString("name") : "Customer";

                    com.appdev.bilijuan.models.Report report =
                            new com.appdev.bilijuan.models.Report(
                                    productId, product.getName(),
                                    product.getSellerId(), product.getSellerName(),
                                    currentUid, customerName, reason, "");

                    FirebaseHelper.getDb().collection("reports").add(report)
                            .addOnSuccessListener(ref -> {
                                String reportId = ref.getId();
                                ref.update("reportId", reportId);

                                FirebaseHelper.getDb().collection("users")
                                        .document(product.getSellerId())
                                        .update("reportCount", FieldValue.increment(1));

                                NotificationHelper.notifyNewReport(reportId, customerName, product.getSellerName());

                                Toast.makeText(this,
                                        "Report submitted. Thank you for your feedback.",
                                        Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to submit report",
                                            Toast.LENGTH_SHORT).show());
                });
    }
}
