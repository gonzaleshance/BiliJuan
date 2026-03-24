package com.appdev.bilijuan.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageHelper {

    public static String uriToBase64(Context context, Uri imageUri) {
        try {
            InputStream inputStream = context.getContentResolver()
                    .openInputStream(imageUri);
            Bitmap original = BitmapFactory.decodeStream(inputStream);
            Bitmap resized = resizeBitmap(original, 500, 500);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);

            byte[] imageBytes = baos.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap base64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0,
                    decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bitmap resizeBitmap(Bitmap source, int maxWidth, int maxHeight) {
        int width  = source.getWidth();
        int height = source.getHeight();

        float ratio = Math.min(
                (float) maxWidth  / width,
                (float) maxHeight / height
        );

        int newWidth  = Math.round(width  * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true);
    }
}