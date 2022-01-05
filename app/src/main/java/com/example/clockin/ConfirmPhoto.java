package com.example.clockin;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;

import com.example.clockin.volley.VolleyDataRequester;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;

import androidx.drawerlayout.widget.DrawerLayout;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

public class ConfirmPhoto extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private String filename;
    private ImageView imageView;
    private Bitmap image;
    private Button button;
    private Intent intent;
    private String purpose;

    private String HOST = "https://52.139.218.209:443/identify/identify";

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private NavigationView navigationView;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.confirm_photo);
        imageView = findViewById(R.id.photoView);
        button = findViewById(R.id.confirm_photo_button);
        filename = getIntent().getExtras().getString("filename");
        assert getSupportActionBar() != null;   //null check
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);   //show back button

        /*drawerLayout = findViewById(R.id.confirm_photo_drawer);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        navigationView = findViewById(R.id.photo_navigation_view);

        navigationView.inflateMenu(R.menu.manager_navigation);
        navigationView.setNavigationItemSelectedListener(this);*/

        //display photo
        File photo = FileUtils.getPhotoFileUri(filename, this);
        image = BitmapFactory.decodeFile(photo.getAbsolutePath());
        imageView.setImageBitmap(image);

        intent = getIntent();
        purpose = intent.getExtras().getString("Purpose");
        button.setOnClickListener(view -> cropFace());
    }

    private void cropFace() {
        Bitmap image = this.image;
        InputImage inputImage = InputImage.fromBitmap(image, 0);
        FaceDetector faceDetector = FaceDetection.getClient();
        faceDetector.process(inputImage).addOnSuccessListener(faces -> {
            if (!faces.isEmpty()) {
                Face face = faces.get(0);
                Rect rect = face.getBoundingBox();
                try {
                    Bitmap croppedBmp = Bitmap.createBitmap(image, rect.left, rect.top, rect.width(), rect.height());
                    Bitmap compressedBitmap = Bitmap.createScaledBitmap(croppedBmp, 150, 150, false);
                    String base64 = FileUtils.getBase64String(compressedBitmap);
                    send(compressedBitmap, base64);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
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
            Intent photo_intent;
            if (purpose.equals("Register")) {
                photo_intent = new Intent(this, UserRegistrationWindow.class);
                photo_intent.putExtras(intent.getExtras());
                photo_intent.putExtra("landmarks", Arrays.toString(arr));
                photo_intent.putExtra("photo", base64);
                startActivity(photo_intent);
            } else {
                String company_number = intent.getExtras().getString("company_number");
                HashMap<String, String> body = new HashMap<>();
                body.put("account", company_number);
                body.put("cropimage", base64);
                body.put("landmark", Arrays.toString(arr));

                VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                        .setUrl(HOST)
                        .setBody(body)
                        .setMethod( VolleyDataRequester.Method.POST )
                        .setJsonResponseListener(response -> {
                            try {
                                if (response.get("status").toString().equals("false")) {
                                    Toast.makeText(getApplicationContext(), "Identification unsuccessful. Try again", Toast.LENGTH_LONG).show();
                                } else {
                                    JSONObject jsonObject = response.getJSONObject("result");
                                    Intent homepage = new Intent(this, Homepage.class);
                                    homepage.putExtra("username", jsonObject.getString("username"));
                                    homepage.putExtra("manager", jsonObject.getBoolean("manager"));
                                    homepage.putExtra("user_object_id", jsonObject.getString("user_object_id"));
                                    homepage.putExtra("record_object_id", jsonObject.getString("record_object_id"));
                                    homepage.putExtra("company_number", company_number);
                                    startActivity(homepage);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        })
                        .requestJson();
            }

        });
    }


    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }
}
