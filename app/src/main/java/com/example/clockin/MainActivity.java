package com.example.clockin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.clockin.volley.VolleyDataRequester;
import com.example.clockin.volley.VolleyHelperApplication;

import org.json.JSONException;

import java.net.MalformedURLException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private String host = "https://52.139.218.209:443/login/login";
    public static final String POST = "/post";
    private EditText email;
    private EditText password;
    Context context;

    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 200;

    private View.OnClickListener loginClickListener = view -> {
        try {
            loginButtonClicked();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    };

    private View.OnClickListener registrationClickListener = view -> registrationButtonClicked();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            email.setAutofillHints(View.AUTOFILL_HINT_EMAIL_ADDRESS);
            password.setAutofillHints(View.AUTOFILL_HINT_PASSWORD);
        }
        context = VolleyHelperApplication.getInstance();

        // attach click listeners
        Button loginButton = findViewById(R.id.login);
        Button registrationButton = findViewById(R.id.register);
        loginButton.setOnClickListener(loginClickListener);
        registrationButton.setOnClickListener(registrationClickListener);

        if (!hasPermissions()) {
            // request camera permissions
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE_CAMERA_PERMISSION);
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

    private boolean isEmpty(EditText editText) {
        CharSequence txt = editText.getText().toString();
        return TextUtils.isEmpty(txt);
    }

    private void loginButtonClicked() throws MalformedURLException {
        if (isEmpty(email)) {
            email.setError("Please enter your email");
        } else if (isEmpty(password)) {
            password.setError("Please enter your password");
        } else {
            HashMap<String, String> body = new HashMap<>();
            body.put("password", password.getText().toString());
            body.put("account", email.getText().toString());
            Log.v("Response", body.toString());
            VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                    .setUrl( host)
                    .setBody( body )
                    .setMethod( VolleyDataRequester.Method.POST )
                    .setJsonResponseListener(response -> {
                        Log.v("Response", response.toString());
                        try {
                            if (response.getBoolean("status")) {
                                Toast.makeText(getApplicationContext(), "Login successful", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(getApplicationContext(), FaceClockIn.class);
                                intent.putExtra("Purpose", "Identify");
                                intent.putExtra("company_number", email.getText().toString());
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(getApplicationContext(), "Login unsuccessful", Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(), "Error occurred", Toast.LENGTH_LONG).show();
                        }
                    })
                    .requestJson();
        }

    }

    private void registrationButtonClicked() {
        Intent intent = new Intent(this, CompanyRegistrationWindow.class);
        intent.putExtra("Purpose", "Identify");
        startActivity(intent);
    }
}