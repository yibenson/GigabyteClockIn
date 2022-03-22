package com.example.clockin;

import android.annotation.SuppressLint;
import android.content.Intent;
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

import com.example.clockin.databinding.UserprofileBinding;
import com.example.clockin.punch_sections.Punches;
import com.example.clockin.volley.VolleyDataRequester;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Objects;

public class UserProfile extends AppCompatActivity {
    private String HOST = "https://52.139.218.209:443/user/get_user_profile";

    private JSONObject info;
    private Intent intent;
    private Intent punch_intent;
    private boolean manager = false;
    private boolean enable = false;
    UserprofileBinding binding;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = UserprofileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.profile_title);
        intent = new Intent(getApplicationContext(), EditPage.class);

        binding.profilePhoto.setOnClickListener(view -> thing(view));
        binding.profileEmail.setOnClickListener(view -> thing(view));
        binding.profilePhone.setOnClickListener(view -> thing(view));
        binding.profileWage.setOnClickListener(view -> thing(view));
        binding.manager.setOnClickListener(view -> thing(view));
        binding.active.setOnClickListener(view -> thing(view));

        // populate textviews/photo with user's info
        fillInfo();
    }

    public void thing(View view) {
        intent.putExtra("ACCOUNT", getIntent().getStringExtra("ACCOUNT"));
        intent.putExtra("USERNAME", getIntent().getStringExtra("USERNAME"));
        switch (view.getId()) {
            case R.id.profile_username:
                intent.putExtra("PURPOSE", 0);
                intent.putExtra("INFO", info.toString());
                // startActivity(intent);
                break;
            case R.id.profile_email:
                intent.putExtra("PURPOSE", 2);
                intent.putExtra("INFO", info.toString());
                startActivity(intent);
                break;
            case R.id.profile_phone:
                intent.putExtra("PURPOSE", 1);
                intent.putExtra("INFO", info.toString());
                startActivity(intent);
                break;
            case R.id.profile_wage:
                intent.putExtra("PURPOSE", 3);
                intent.putExtra("INFO", info.toString());
                startActivity(intent);
                break;
            case R.id.manager:
                if (manager) {
                    intent.putExtra("PURPOSE", 4);
                    intent.putExtra("INFO", info.toString());
                    startActivity(intent);
                }
                break;
            case R.id.active:
                if (enable) {
                    intent.putExtra("PURPOSE", 5);
                    intent.putExtra("INFO", info.toString());
                    startActivity(intent);
                }
                break;
            case R.id.view_punches:
                punch_intent = new Intent(this, Punches.class);
                punch_intent.putExtras(getIntent());
                startActivity(punch_intent);
                break;
            default:
                Intent cam = new Intent(getApplicationContext(), FaceClockIn.class);
                cam.putExtra("ACCOUNT", getIntent().getStringExtra("ACCOUNT"));
                cam.putExtra("PURPOSE", "EDIT");
                cam.putExtras(intent.getExtras());
                cam.putExtra("INFO", info.toString());
                startActivity(cam);
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    public void fillInfo() {
        HashMap<String, String> body = new HashMap<>();
        body.put("account", getIntent().getExtras().getString("ACCOUNT"));
        String name = getIntent().getStringExtra("USERNAME");
        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(HOST)
                .setBody(body)
                .setMethod(VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    Log.v("Response", response.toString());
                    try {
                        if (!response.getBoolean("status")) {
                            Toast.makeText(this, R.string.error_connecting, Toast.LENGTH_LONG).show();
                        } else {
                            JSONObject jsonObject = new JSONObject();
                            JSONArray jsonArray = response.getJSONArray("result");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                if (jsonArray.getJSONObject(i).getString("name").equals(name)) {
                                    jsonObject = jsonArray.getJSONObject(i);
                                }
                            }
                            info = jsonObject;
                            binding.profileUsername.setText(getString(R.string.name, jsonObject.getString("name")));
                            binding.profilePhone.setText(getString(R.string.phone_number, jsonObject.getString("phone")));
                            binding.profileEmail.setText(getString(R.string.email, jsonObject.getString("mail")));
                            binding.profileWage.setText(getString(R.string.user_wage,jsonObject.getString("wage") ));
                            binding.manager.setText(getString(R.string.manager,jsonObject.getString("manager")));
                            manager = Boolean.parseBoolean(jsonObject.getString("manager"));
                            binding.active.setText(getString(R.string.active,jsonObject.getString("enable")));
                            enable = Boolean.parseBoolean(jsonObject.getString("enable"));
                            byte[] decodedString = Base64.decode(jsonObject.getString("face").replace("\\n", "\n"), Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            binding.profilePhoto.setImageBitmap(decodedByte);
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
