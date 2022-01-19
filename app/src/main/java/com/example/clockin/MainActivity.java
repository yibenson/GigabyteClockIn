package com.example.clockin;

import androidx.appcompat.app.AlertDialog;
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

import com.example.clockin.databinding.ActivityMainBinding;
import com.example.clockin.volley.VolleyDataRequester;
import com.example.clockin.volley.VolleyHelperApplication;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;

import java.net.MalformedURLException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private String host = "https://52.139.218.209:443/";
    private ActivityMainBinding binding;

    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 200;
    GoogleSignInClient mGoogleSignInClient;
    private final int RC_SIGN_IN = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        binding.email.setAutofillHints(View.AUTOFILL_HINT_EMAIL_ADDRESS);
        binding.password.setAutofillHints(View.AUTOFILL_HINT_PASSWORD);

        binding.signInButton.setOnClickListener(view -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        binding.login.setOnClickListener(view -> {
            try {
                loginButtonClicked();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        });
        binding.register.setOnClickListener(view -> registrationButtonClicked());

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE_CAMERA_PERMISSION);
        }
    }

    private void loginButtonClicked() throws MalformedURLException {
        if (isEmpty(binding.email)) {
            binding.email.setError(getString(R.string.company_user_empty));
        } else if (isEmpty(binding.password)) {
            binding.password.setError(getString(R.string.password_empty));
        } else {
            HashMap<String, String> body = new HashMap<>();
            body.put("password", binding.password.getText().toString());
            body.put("account", binding.email.getText().toString());
            VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                    .setUrl( host + "login/login")
                    .setBody( body )
                    .setMethod( VolleyDataRequester.Method.POST )
                    .setJsonResponseListener(response -> {
                        try {
                            if (response.getBoolean("status")) {
                                Intent intent = new Intent(getApplicationContext(), FaceClockIn.class);
                                intent.putExtra("Purpose", "Identify");
                                intent.putExtra("company_number", binding.email.getText().toString());
                                startActivity(intent);
                            } else {
                                // user/pw invalid
                                showAlertDialog("login_error");
                            }
                        } catch (JSONException e) {
                            // error connecting to server
                            showAlertDialog("error");
                        }
                    })
                    .requestJson();

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        } else {
            Log.v("Response", "Something errored");
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                String name = account.getDisplayName();
                String personEmail = account.getEmail();
                String personId = account.getId();
                HashMap<String, String> map = new HashMap<>();
                map.put("name", name);
                map.put("mail", personEmail);
                map.put("id", personId);
                VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                        .setUrl( host + "login/google_login")
                        .setBody( map )
                        .setMethod( VolleyDataRequester.Method.POST )
                        .setJsonResponseListener(response -> {
                            try {
                                if (response.getBoolean("status")) {
                                    Intent intent = new Intent(getApplicationContext(), FaceClockIn.class);
                                    intent.putExtra("Purpose", "Identify");
                                    intent.putExtra("company_number", personId);
                                    startActivity(intent);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        });
            }
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("Response", "signInResult:failed code=" + e.getStatusCode());
            // updateUI(null);
        }
    }


    private void registrationButtonClicked() {
        Intent intent = new Intent(this, CompanyRegistrationWindow.class);
        startActivity(intent);
    }

    private boolean isEmpty(EditText editText) {
        return TextUtils.isEmpty(editText.getText().toString());
    }

    private void showAlertDialog(String status) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        if (status.equals("login_error")) {
            builder.setMessage(getString(R.string.company_login_failed))
                    .setPositiveButton("Ok", null);
        } else {
            builder.setMessage(getString(R.string.error_connecting))
                    .setPositiveButton("Ok", null);
        }
        builder.create().show();
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

}