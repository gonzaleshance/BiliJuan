package com.appdev.bilijuan.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Comment {

    private String commentId;
    private String productId;
    private String userId;
    private String userName;
    private String text;
    private float stars;   // 1–5 star rating left with comment

    @ServerTimestamp
    private Date timestamp;

    // Required empty constructor for Firestore
    public Comment() {}

    public Comment(String productId, String userId, String userName,
                   String text, float stars) {
        this.productId = productId;
        this.userId    = userId;
        this.userName  = userName;
        this.text      = text;
        this.stars     = stars;
    }

    // Getters
    public String getCommentId()  { return commentId; }
    public String getProductId()  { return productId; }
    public String getUserId()     { return userId; }
    public String getUserName()   { return userName; }
    public String getText()       { return text; }
    public float getStars()       { return stars; }
    public Date getTimestamp()    { return timestamp; }

    // Setters
    public void setCommentId(String commentId)    { this.commentId = commentId; }
    public void setProductId(String productId)    { this.productId = productId; }
    public void setUserId(String userId)          { this.userId = userId; }
    public void setUserName(String userName)      { this.userName = userName; }
    public void setText(String text)              { this.text = text; }
    public void setStars(float stars)             { this.stars = stars; }
    public void setTimestamp(Date timestamp)      { this.timestamp = timestamp; }
}