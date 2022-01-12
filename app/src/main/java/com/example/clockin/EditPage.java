package com.example.clockin;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clockin.databinding.InfoEditBinding;
import com.example.clockin.volley.VolleyDataRequester;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

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
        int purpose = getIntent().getIntExtra("Purpose", 0);
        try {
            JSONObject info = new JSONObject(getIntent().getStringExtra("Info"));
            switch (purpose) {
                case 0:
                    binding.textView.setText(info.getString("name"));
                    break;
                case 1:
                    binding.textView.setText(info.getString("phone"));
                    break;
                case 2:
                    binding.textView.setText(info.getString("mail"));
                    break;
                case 3:
                    binding.textView.setText(info.getString("wage"));
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        binding.button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HashMap<String, String> mapBody = new HashMap<>();
                try {
                    JSONObject jsonObject = new JSONObject(getIntent().getStringExtra("Info"));
                    mapBody.put("name", jsonObject.getString("name"));
                    mapBody.put("phone", jsonObject.getString("phone"));
                    mapBody.put("mail", jsonObject.getString("mail"));
                    mapBody.put("manager", jsonObject.getString("manager"));
                    mapBody.put("sex", jsonObject.getString("sex"));
                    mapBody.put("birthday",  jsonObject.getString("birthday").replaceAll("\\.", "/"));
                    mapBody.put("enable", "true");
                    switch (purpose) {
                        case 0:
                            mapBody.put("name", binding.textView.getText().toString());
                            sendInfo(mapBody);
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
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    @SuppressLint("SetTextI18n")
    public void sendInfo(HashMap<String, String> body) {
        body.put("account", getIntent().getExtras().getString("company_number"));
        Log.v("EditPage", body.toString() );
        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(HOST)
                .setBody(body)
                .setMethod(VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    try {
                        Log.v("Response", response.toString());
                        if (!response.getBoolean("status")) {
                            Toast.makeText(this, R.string.error_connecting, Toast.LENGTH_LONG).show();
                        } else {
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }).requestJson();
    }

}
