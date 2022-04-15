package com.example.clockin;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.clockin.databinding.InfoEditBinding;
import com.example.clockin.volley.VolleyDataRequester;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Objects;

public class EditPage extends AppCompatActivity {
    private int NAME = 0;
    private int PHONE = 1;
    private int EMAIL = 2;
    private int WAGE = 3;
    private InfoEditBinding binding;

    private String HOST = "https://52.139.218.209:443/user/edit_user_profile";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = InfoEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        int purpose = getIntent().getIntExtra("PURPOSE", 0);
        try {
            JSONObject info = new JSONObject(getIntent().getStringExtra("INFO"));
            switch (purpose) {
                case 0:
                    binding.textView.setText(info.getString("NAME"));
                    break;
                case 1:
                    binding.textView.setText(info.getString("PHONE"));
                    break;
                case 2:
                    binding.textView.setText(info.getString("MAIL"));
                    break;
                case 3:
                    binding.textView.setText(info.getString("WAGE"));
                    break;
                case 4:
                    binding.textView.setText(getString(R.string.manager, info.getString("MANAGER")));
                case 5:
                    binding.textView.setText(getString(R.string.active, info.getString("ENABLE")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        binding.button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HashMap<String, String> mapBody = new HashMap<>();
                try {
                    JSONObject jsonObject = new JSONObject(getIntent().getStringExtra("INFO"));
                    mapBody.put("name", jsonObject.getString("NAME"));
                    mapBody.put("phone", jsonObject.getString("PHONE"));
                    mapBody.put("mail", jsonObject.getString("MAIL"));
                    mapBody.put("manager", jsonObject.getString("MANAGER"));
                    mapBody.put("sex", jsonObject.getString("SEX"));
                    mapBody.put("birthday",  jsonObject.getString("BIRTHDAY").replaceAll("\\.", "/"));
                    mapBody.put("enable", "");
                    switch (purpose) {
                        case 0:
                            // users should not be able to change username
                            /**mapBody.put("name", binding.textView.getText().toString());
                            sendInfo(mapBody);
                             */
                            break;
                        case 1:
                            mapBody.put("phone", binding.textView.getText().toString());
                            sendInfo(mapBody);
                            break;
                        case 2:
                            mapBody.put("mail", binding.textView.getText().toString());
                            sendInfo(mapBody);
                            break;
                        case 3:
                            mapBody.put("wage", binding.textView.getText().toString());
                            sendInfo(mapBody);
                            break;
                        case 4:
                            String manager = binding.textView.getText().toString();
                            if (!manager.equals("false")) {
                                manager = "true";
                            } else {
                                manager = "false";
                            }
                            mapBody.put("manager", manager);
                        case 5:
                            String active = binding.textView.getText().toString();
                            if (!active.equals("false")) {
                                active = "true";
                            } else {
                                active = "false";
                            }
                            mapBody.put("enable", active);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    @SuppressLint("SetTextI18n")
    public void sendInfo(HashMap<String, String> body) {
        body.put("account", getIntent().getExtras().getString("ACCOUNT"));
        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(HOST)
                .setBody(body)
                .setMethod(VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    try {
                        if (!response.getBoolean("status")) {
                            Toast.makeText(this, R.string.error_connecting, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, R.string.editing_success, Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }).requestJson();
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
