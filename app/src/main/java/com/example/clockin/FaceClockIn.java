package com.example.clockin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;

import com.example.clockin.volley.VolleyDataRequester;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.View;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceClockIn extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 200;

    private static String HOST = "https://52.139.218.209:443/";

    // camera variables
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private CameraSelector cameraSelector;
    private ExecutorService analysisExecutor;
    private ExecutorService captureExecutor;
    private FaceDetector faceDetector;
    private int rotation;

    // nav drawer
    public DrawerLayout drawerLayout;
    public ActionBarDrawerToggle actionBarDrawerToggle;
    public NavigationView navigationView;

    // intent we pass back to user registration window
    private Intent user_intent;
    private String purpose;

    private Bitmap bitmap;
    private CardView cameraWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.faceclockin);
        Intent intent = getIntent();
        cameraWrapper = findViewById(R.id.clockIncameraWrapper);
        // only display nav drawer when we're using this for identification
        if (intent.getExtras().get("Purpose").equals("Identify")) {
            purpose = "Identify";
            drawerLayout = findViewById(R.id.my_drawer_layout);
            actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);
            drawerLayout.addDrawerListener(actionBarDrawerToggle);
            actionBarDrawerToggle.syncState();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            navigationView = findViewById(R.id.base_navigation_view);
            navigationView.setNavigationItemSelectedListener(this);

            // set user_intent parameters
            user_intent = new Intent(this, Homepage.class);
            user_intent.putExtra("company_number", intent.getExtras().getString("company_number"));
            user_intent.putExtra("Purpose", "Identify");
        } else if (intent.getExtras().get("Purpose").equals("Register")) {
            purpose = "Register";
            user_intent = new Intent(this, UserRegistrationWindow.class);
            user_intent.putExtras(intent.getExtras());
            user_intent.putExtra("Purpose", "Register");
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
                ImageCapture imageCapture =
                        new ImageCapture.Builder()
                                .setTargetRotation(rotation).build();
                Button captureButton = findViewById(R.id.shutter);
                captureExecutor = Executors.newSingleThreadExecutor();
                captureButton.setOnClickListener(v ->
                        imageCapture.takePicture(captureExecutor, new ImageCapture.OnImageCapturedCallback() {
                            @Override
                            public void onCaptureSuccess(@NonNull ImageProxy image) {
                                if (livenessAnalyzer.getLiveness()) {
                                    cropFace(image);
                                } else {
                                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Please blink to ensure live image", Toast.LENGTH_LONG).show());
                                }
                                image.close();
                            }
                        }));
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, imageCapture, preview);
            } catch (ExecutionException | InterruptedException e) {
                // Should never be reached
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void cropFace(ImageProxy image) {
        this.bitmap = FileUtils.toBitmap(image);
        InputImage inputImage = InputImage.fromBitmap(bitmap, rotation);
        faceDetector = FaceDetection.getClient();
        faceDetector.process(inputImage).addOnSuccessListener(faces -> {
            if (!faces.isEmpty()) {
                Face face = faces.get(0);
                Rect rect = face.getBoundingBox();
                try {
                    Bitmap croppedBmp = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
                    Bitmap compressedBitmap = Bitmap.createScaledBitmap(croppedBmp, 150, 150, false);
                    String base64 = FileUtils.getBase64String(compressedBitmap);
                    send(compressedBitmap, base64);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(getApplicationContext(), "No live faces detected", Toast.LENGTH_LONG).show();
            }
        });
    }


    private void send(Bitmap bitmap, String base64) {
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
            if (purpose.equals("Register")) {
                user_intent.putExtra("landmark", Arrays.toString(arr));
                user_intent.putExtra("photo", base64);
                startActivity(user_intent);
                finish();
            } else {
                String company_number = user_intent.getExtras().getString("company_number");
                HashMap<String, String> body = new HashMap<>();
                body.put("account", company_number);
                body.put("landmark", Arrays.toString(arr));
                body.put("cropimage", base64);
                VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                        .setUrl(HOST+"identify/identify")
                        .setBody(body)
                        .setMethod( VolleyDataRequester.Method.POST )
                        .setJsonResponseListener(response -> {
                            try {
                                if (response.get("status").toString().equals("false")) {
                                    Toast.makeText(getApplicationContext(), "Identification unsuccessful. Try again", Toast.LENGTH_LONG).show();
                                } else {
                                    JSONObject jsonObject = response.getJSONObject("result");
                                    String username = jsonObject.getString("username");
                                    user_intent.putExtra("username", username);
                                    user_intent.putExtra("manager", jsonObject.getBoolean("manager"));
                                    user_intent.putExtra("user_object_id", jsonObject.getString("user_object_id"));
                                    user_intent.putExtra("record_object_id", jsonObject.getString("record_object_id"));
                                    showAlertDialog(username);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        })
                        .requestJson();
            }

        });
    }

    private void showAlertDialog(String username) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View customLayout = getLayoutInflater().inflate(R.layout.confirm_dialog, null);
        builder.setView(customLayout);
        ImageView imageView = customLayout.findViewById(R.id.dialog_photo);
        TextView textView = customLayout.findViewById(R.id.dialog_username);
        textView.setText(username);
        imageView.setImageBitmap(bitmap);
        // add a button
        builder.setPositiveButton("Login", (dialog, which) -> {
            startActivity(user_intent);
        });
        builder.setNegativeButton("Not you?", (dialog, which) -> {
            dialog.dismiss();
        });
        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this,
                        "Please enable all permissions to use this app",
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



    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_account:
                Intent temp = new Intent(this, UserRegistrationWindow.class);
                temp.putExtras(user_intent.getExtras());
                startActivity(new Intent(temp));
            case R.id.base_settings:
                // todo
            case R.id.base_logout:
                finish();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}



