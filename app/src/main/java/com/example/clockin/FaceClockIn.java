package com.example.clockin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;

import com.example.clockin.databinding.FaceClockinBinding;
import com.example.clockin.volley.VolleyDataRequester;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceClockIn extends AppCompatActivity {
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 200;

    private static String HOST = "https://52.139.218.209:443/";
    private static final String IDENTIFY = "IDENTIFY";
    private static final String REGISTER = "REGISTER";
    private static final String EDIT = "EDIT";

    // camera variables
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private CameraSelector cameraSelector;
    private ExecutorService analysisExecutor;
    private ExecutorService captureExecutor;
    private FaceDetector faceDetector;
    private int rotation;

    // layout variables
    private FaceClockinBinding binding;
    public ActionBarDrawerToggle actionBarDrawerToggle;

    // bitmap = photo we take
    private Bitmap bitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FaceClockinBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // only display nav drawer when we're using this for identification
        if (getIntent().getStringExtra("PURPOSE").equals("IDENTIFY")) {
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayShowCustomEnabled(true);
            LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.action_bar_buttonless, null);
            actionBar.setCustomView(v);
        }
        if (!hasPermissions()) {
            // request camera permissions
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE_CAMERA_PERMISSION);
        } else {
            // CameraX preview and image capture use cases
            bindUseCases();
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void bindUseCases() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();
                cameraProvider.unbindAll();

                // bind preview
                PreviewView previewView = findViewById(R.id.previewView);
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // bind image analysis
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
                analysisExecutor = Executors.newSingleThreadExecutor();
                LivenessAnalyzer livenessAnalyzer = new LivenessAnalyzer();
                imageAnalysis.setAnalyzer(analysisExecutor, livenessAnalyzer);

                // bind image capture
                rotation = getWindowManager().getDefaultDisplay().getRotation();
                ImageCapture imageCapture = new ImageCapture.Builder()
                        .setTargetRotation(rotation).build();
                captureExecutor = Executors.newSingleThreadExecutor();
                binding.shutter.setOnClickListener(v ->
                        imageCapture.takePicture(captureExecutor, new ImageCapture.OnImageCapturedCallback() {
                            @Override
                            public void onCaptureSuccess(@NonNull ImageProxy image) {
                                if (livenessAnalyzer.getLiveness()) {
                                    cropFace(image);
                                } else {
                                    runToast(getString(R.string.please_blink));
                                }
                                image.close();
                            }


                        }));
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, imageCapture, preview);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void cropFace(ImageProxy image) {
        this.bitmap = FileUtils.toBitmap(image);
        InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
        faceDetector = FaceDetection.getClient();
        faceDetector.process(inputImage).addOnSuccessListener(faces -> {
            if (!faces.isEmpty()) {
                Face face = faces.get(0);
                Rect rect = face.getBoundingBox();
                Bitmap croppedBmp = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
                Bitmap compressedBitmap = Bitmap.createScaledBitmap(croppedBmp, 150, 150, false);
                String base64 = FileUtils.getBase64String(compressedBitmap);
                send(compressedBitmap, base64);
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.no_faces_detected), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void send(Bitmap bitmap, String base64) {
        // we pass bitmap through face detection again so landmark coordinates correspond to cropped img
        InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL).build();
        FaceDetector faceDetector = FaceDetection.getClient(options);
        int[] arr = new int[4];
        faceDetector.process(inputImage).addOnSuccessListener(faces -> {
            if (!faces.isEmpty()) {
                Face face = faces.get(0);
                try {
                    FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
                    FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
                    arr[0] = (int) leftEye.getPosition().x;
                    arr[1] = (int) leftEye.getPosition().y;
                    arr[2] = (int) rightEye.getPosition().x;
                    arr[3] = (int) rightEye.getPosition().y;
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }).addOnCompleteListener(task -> {
            switch (getIntent().getStringExtra("PURPOSE")) {
                case REGISTER:
                    // if registering a user for the first time
                    Intent intent = new Intent(getApplicationContext(), UserRegistrationWindow.class);
                    intent.putExtras(getIntent().getExtras());
                    intent.putExtra("LANDMARK", Arrays.toString(arr));
                    intent.putExtra("PHOTO", base64);
                    showAlertDialog(null, intent, null);
                    break;
                case IDENTIFY:
                    // if identifying a user to login
                    HashMap<String, String> body = new HashMap<>();
                    body.put("account", getIntent().getStringExtra("ACCOUNT"));
                    body.put("cropimage", base64);
                    body.put("landmark", Arrays.toString(arr));
                    VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                            .setUrl(HOST+"identify/identify")
                            .setBody(body)
                            .setMethod(VolleyDataRequester.Method.POST)
                            .setJsonResponseListener(response -> {
                                try {
                                    if (response.get("status").toString().equals("false")) {
                                        runToast(getString(R.string.identification_unsuccessful));
                                    } else {
                                        Intent id_intent = new Intent(getApplicationContext(), Homepage.class);
                                        JSONObject jsonObject = response.getJSONObject("result");
                                        id_intent.putExtra("ACCOUNT", getIntent().getStringExtra("ACCOUNT"));
                                        id_intent.putExtra("USERNAME", jsonObject.getString("username"));
                                        id_intent.putExtra("MANAGER", jsonObject.getBoolean("manager"));
                                        id_intent.putExtra("USER_OBJECT_ID", jsonObject.getString("user_object_id"));
                                        id_intent.putExtra("RECORD_OBJECT_ID", jsonObject.getString("record_object_id"));
                                        showAlertDialog(jsonObject.getString("username"), id_intent, null);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            })
                            .requestJson();
                    break;
                case EDIT:
                    // if ediitng the login photo for a user
                    HashMap<String, String> mapBody = new HashMap<>();
                    try {
                        JSONObject jsonObject = new JSONObject(getIntent().getStringExtra("INFO"));
                        String name = jsonObject.getString("name");
                        mapBody.put("account", getIntent().getExtras().getString("ACCOUNT"));
                        mapBody.put("name", jsonObject.getString("NAME"));
                        mapBody.put("phone", jsonObject.getString("PHONE"));
                        mapBody.put("mail", jsonObject.getString("MAIL"));
                        mapBody.put("manager", jsonObject.getString("MANAGER"));
                        mapBody.put("wage", jsonObject.getString("WAGE"));
                        mapBody.put("sex", jsonObject.getString("SEX"));
                        mapBody.put("birthday",  jsonObject.getString("BIRTHDAY").replaceAll("\\.", "/"));
                        mapBody.put("enable", "true");
                        mapBody.put("face", base64);
                        mapBody.put("landmark", Arrays.toString(arr));
                        showAlertDialog(name, null, mapBody);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
            }
        });
    }

    private void showManagementDialog(Intent intent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View customLayout = getLayoutInflater().inflate(R.layout.management_dialog, null);
        customLayout.findViewById(R.id.first);
        builder.setView(customLayout);
        builder.setPositiveButton(getString(R.string.login), (dialog, which) -> {
            startActivity(intent);
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
            dialog.dismiss();
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showAlertDialog(String username, Intent intent, HashMap<String, String> body) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        final View customLayout = getLayoutInflater().inflate(R.layout.confirm_dialog, null);
        builder.setView(customLayout);
        ImageView imageView = customLayout.findViewById(R.id.dialog_photo);
        TextView textView = customLayout.findViewById(R.id.dialog_username);
        imageView.setImageBitmap(bitmap);
        textView.setText(getString(R.string.your_photo));

        // body and username null if registering user for first time; only body null if identifying user
        if ((body == null)) {
            if (username != null) {
                textView.setText(username);
            }
            builder.setPositiveButton(getString(R.string.confirm_photo), (dialog, which) -> {
                dialog.dismiss();
                startActivity(intent);
            });
            builder.setNegativeButton(getString(R.string.retry), (dialog, which) -> {
                dialog.dismiss();
            });
        // only body null if identifying user
        } else {
            builder.setPositiveButton("Confirm Photo", (dialog, which) -> {
                VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                        .setUrl(HOST+"user/edit_user_profile")
                        .setBody(body)
                        .setMethod(VolleyDataRequester.Method.POST)
                        .setJsonResponseListener(response -> {
                            try {
                                if (response.get("status").toString().equals("false")) {
                                    runToast(getString(R.string.error_connecting));
                                } else {
                                    runToast(getString(R.string.editing_success));
                                    finish();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }).requestJson();
            });
            builder.setNegativeButton(getString(R.string.retry), (dialog, which) -> {
                dialog.dismiss();
            });
        }
        AlertDialog dialog = builder.create();
        dialog.show();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // todo: maybe move to alert dialog
                Toast.makeText(this,
                        getString(R.string.please_grant_permissions),
                        Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            bindUseCases();
        }
    }

    private boolean hasPermissions() {
        for (String s: PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, s) !=
                    PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void runToast(String msg) {
        final String str = msg;
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show());
    }



}



