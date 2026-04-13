package com.appdev.bilijuan.activities.customer;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.databinding.ActivityReviewPromptBinding;
import com.appdev.bilijuan.models.Comment;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class ReviewPromptActivity extends AppCompatActivity {

    private ActivityReviewPromptBinding binding;
    private String orderId, productId, currentUid;
    private float selectedStars = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReviewPromptBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderId     = getIntent().getStringExtra("orderId");
        productId   = getIntent().getStringExtra("productId");
        currentUid  = FirebaseHelper.getCurrentUid();

        String productName = getIntent().getStringExtra("productName");
        String storeName   = getIntent().getStringExtra("storeName");

        binding.tvProductName.setText(productName != null ? productName : "your order");
        binding.tvStoreName.setText("from " + (storeName != null ? storeName : "the store"));

        setupStars();
        binding.btnSkip.setOnClickListener(v -> markReviewedAndFinish());
        binding.btnSubmit.setOnClickListener(v -> submitReview());
    }

    private void setupStars() {
        ImageView[] stars = {
                binding.star1, binding.star2, binding.star3,
                binding.star4, binding.star5
        };
        View.OnClickListener starListener = v -> {
            int id = v.getId();
            if      (id == R.id.star1) selectedStars = 1f;
            else if (id == R.id.star2) selectedStars = 2f;
            else if (id == R.id.star3) selectedStars = 3f;
            else if (id == R.id.star4) selectedStars = 4f;
            else if (id == R.id.star5) selectedStars = 5f;
            for (int i = 0; i < 5; i++)
                stars[i].setAlpha(i < selectedStars ? 1.0f : 0.3f);
        };
        for (ImageView star : stars) star.setOnClickListener(starListener);
    }

    private void submitReview() {
        String text = binding.etReview.getText() != null
                ? binding.etReview.getText().toString().trim() : "";

        if (selectedStars == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSubmit.setEnabled(false);

        FirebaseHelper.getDb().collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    String userName = doc.exists() ? doc.getString("name") : "Customer";
                    Comment comment = new Comment(productId, currentUid,
                            userName, text.isEmpty() ? "Great!" : text, selectedStars);

                    FirebaseHelper.getDb().collection("comments").add(comment)
                            .addOnSuccessListener(ref -> {
                                ref.update("commentId", ref.getId());
                                updateProductRating();
                                markReviewedAndFinish();
                            })
                            .addOnFailureListener(e -> {
                                binding.btnSubmit.setEnabled(true);
                                Toast.makeText(this, "Failed to submit",
                                        Toast.LENGTH_SHORT).show();
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
                        com.appdev.bilijuan.models.Comment c =
                                d.toObject(com.appdev.bilijuan.models.Comment.class);
                        total += c.getStars();
                    }
                    float avg = total / snap.size();
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("stars", avg);
                    updates.put("ratingCount", snap.size());
                    updates.put("shopScore", avg + (FieldValue.serverTimestamp()
                            instanceof FieldValue ? 0 : 0));
                    FirebaseHelper.getDb().collection("products")
                            .document(productId).update("stars", avg,
                                    "ratingCount", snap.size());
                });
    }

    private void markReviewedAndFinish() {
        FirebaseHelper.getDb().collection("orders")
                .document(orderId)
                .update("reviewed", true)
                .addOnSuccessListener(v -> finish())
                .addOnFailureListener(e -> finish());
    }
}