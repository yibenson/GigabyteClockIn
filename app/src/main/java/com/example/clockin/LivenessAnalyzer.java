package com.example.clockin;

import android.annotation.SuppressLint;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetector;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class LivenessAnalyzer implements ImageAnalysis.Analyzer {
    private LinkedList<Float> queue;
    private boolean liveness;
    private double threshold = 0.1;
    private int maxSize = 30;
    private Image lastFrame;

    private FaceDetector faceDetector;

    public LivenessAnalyzer() {
        queue = new LinkedList<>();
        FaceDetectorOptions faceDetectorOptions = new FaceDetectorOptions.Builder()
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL).build();
        faceDetector = FaceDetection.getClient(faceDetectorOptions);
    }

    // single pass STD calculation
    private void calculateLiveness() {
        Float[] arr = queue.toArray(new Float[0]);
        Float sum = 0f;
        float sq_sum = 0f;
        for (Float ai : arr) {
            sum += ai;
            sq_sum += ai * ai;
        }
        float mean = sum / arr.length;
        float variance = sq_sum / arr.length - mean * mean;
        double std = Math.sqrt(variance);
        liveness = std > threshold;

    }

    public boolean getLiveness() {
        return liveness;
    }

    public Image getFrame() {
        return lastFrame;
    }

    @Override
    public void analyze(ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError") Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            faceDetector.process(image).addOnSuccessListener(faces -> {
                if (!faces.isEmpty()) {
                    if (queue.size() > maxSize) {
                        queue.pop();
                    } try {
                        Float f = faces.get(0).getLeftEyeOpenProbability();
                        queue.add(f);
                        calculateLiveness();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
                imageProxy.close();
            }).addOnFailureListener(e -> {
                imageProxy.close();
            });
        }
    }
}
