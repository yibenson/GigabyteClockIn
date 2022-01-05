package com.example.clockin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class FileUtils {
    // Saves image
    public static String saveImage(Bitmap bitmap, Context context) {
        String root = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root);
        if (!myDir.exists()) {
            myDir.mkdirs();
        }

        String fname = "image-" + System.currentTimeMillis() + ".jpg";
        File file = new File(myDir, fname);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fname;
    }

    // Returns the File for a photo stored on disk given the fileName
    public static File getPhotoFileUri(String fileName, Context ctx) {
        // Get safe storage directory for photos
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        // This way, we don't need to request external read/write runtime permissions.
        File mediaStorageDir = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString());

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()){
            Log.v("File", fileName + " does not exist");
        }

        // Return the file target for the photo based on filename
        File file = new File(mediaStorageDir.getPath() + File.separator + fileName);

        return file;
    }


    // ImageProxy to bitmap
    public static Bitmap toBitmap(ImageProxy image) {
        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        byteBuffer.rewind();
        byte[] bytes = new byte[byteBuffer.capacity()];
        byteBuffer.get(bytes);
        byte[] clonedBytes = bytes.clone();
        return BitmapFactory.decodeByteArray(clonedBytes, 0, clonedBytes.length);
    }

    public static String getBase64String(Bitmap bitmap) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);

        byte[] byteArray = stream.toByteArray();
        return Base64.encodeToString(byteArray,Base64.DEFAULT);
    }

}