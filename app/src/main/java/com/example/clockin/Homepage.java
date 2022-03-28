package com.example.clockin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.clockin.databinding.HomepageBinding;
import com.example.clockin.punch_sections.Punches;
import com.example.clockin.volley.VolleyDataRequester;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class Homepage extends AppCompatActivity implements View.OnClickListener {
    private String RECORD_HOST = "https://52.139.218.209:443/record/get_user_record";
    private String CLOCK_HOST = "https://52.139.218.209:443/identify/clockin";
    private boolean clockedin = false;
    private HomepageBinding binding;

    private DateTimeFormatter BASE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm");


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = HomepageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.action_bar, null);
        actionBar.setCustomView(v);
        v.findViewById(R.id.manage_bttn).setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), ManagementMenu.class);
            intent.putExtra("ACCOUNT", getIntent().getStringExtra("ACCOUNT"));
            showManagementDialog(intent);
        });
        binding.clockinButton.setOnClickListener(this);
        binding.clockoutButton.setOnClickListener(this);
        binding.viewpunches.setOnClickListener(this);
        binding.companyinfo.setOnClickListener(this);
        binding.logout.setOnClickListener(this);
        welcomeMessage();
        startTimer();

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.clockin_button:
                if (!clockedin) {
                    showAlertDialog("ON");
                } else {
                    Toast.makeText(this, getString(R.string.already_clocked_in), Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.clockout_button:
                if (clockedin) {
                    showAlertDialog("OFF");
                } else {
                    Toast.makeText(this, getString(R.string.not_currently_clocked_in), Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.viewpunches:
                Intent intent = new Intent(this, Punches.class);
                intent.putExtra("ACCOUNT", getIntent().getStringExtra("ACCOUNT"));
                intent.putExtra("USERNAME", getIntent().getStringExtra("USERNAME"));
                startActivity(intent);
                break;
            case R.id.companyinfo:
                Intent profile_intent = new Intent(this, UserProfile.class);
                profile_intent.putExtra("ACCOUNT", getIntent().getStringExtra("ACCOUNT"));
                profile_intent.putExtra("USERNAME", getIntent().getStringExtra("USERNAME"));
                profile_intent.putExtra("MANAGER", getIntent().getBooleanExtra("MANAGER", false));
                startActivity(profile_intent);
                break;
            case R.id.logout:
                finish();
        }
    }



    /* This function assumes if user is clocked in, that they clocked in within the past 30 days */
    private void startTimer() {
        HashMap<String, String> body = new HashMap<String, String>(){{
            put("account", getIntent().getStringExtra("ACCOUNT"));
            LocalDateTime curr = LocalDateTime.now();
            put("starttime", curr.format(DAY_FORMAT));
            put("endtime", curr.minusDays(30).format(DAY_FORMAT));
        }};
        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(RECORD_HOST)
                .setBody(body)
                .setMethod(VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    // jsonObject will hold most recent clockin/out record of target employee
                    final JSONObject[] jsonObject = new JSONObject[1];
                    try {
                        if (!response.getBoolean("status")) {
                            Toast.makeText(getApplicationContext(), getString(R.string.error_connecting), Toast.LENGTH_LONG).show();
                        } else {
                            Log.e("Homepage", response.toString());
                            // iterate through records to find most recent record with matching username
                            JSONArray jsonArray = response.getJSONArray("result");
                            LocalDateTime earliest = LocalDateTime.parse("1900/01/01 00:00:00", BASE_FORMAT);
                            String username = getIntent().getStringExtra("USERNAME");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject temp = jsonArray.getJSONObject(i);
                                if (temp.getString("user").equals(username)) {
                                    LocalDateTime curr = LocalDateTime.parse(temp.getString("date"), BASE_FORMAT);
                                    if (earliest.isBefore(curr)) {
                                        jsonObject[0] = temp;
                                        earliest = curr;
                                    }
                                }
                            }
                            if (jsonObject[0] == null) {
                                Toast.makeText(getApplicationContext(), "No data", Toast.LENGTH_LONG).show();
                            } else if (jsonObject[0].getString("status").equals("OFF")) {
                                clockedin = false;
                            } else {
                                clockedin = true;
                                binding.clockinTime.setText(jsonObject[0].getString("date"));
                                binding.clockinButton.setBackground(getResources().getDrawable(R.drawable.active_button));
                            }
                        }
                    } catch (JSONException | NullPointerException e) {
                        binding.chronometer.setText(R.string.clocked_out);
                    }
                }).requestJson();
    }


    private void showAlertDialog(String status) {
        AlertDialog alertDialog = new AlertDialog.Builder(Homepage.this, R.style.AlertDialogTheme).create();
        alertDialog.setMessage(getString(R.string.clockin_confirm) + LocalDateTime.now().format(BASE_FORMAT));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes),
                (dialog, which) -> {
                    clock(status);
                    dialog.dismiss();
                }
        );
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }


    private void clock(String status) {
        HashMap<String, String> body = new HashMap<>();
        body.put("account", getIntent().getStringExtra("ACCOUNT"));
        body.put("user_object_id", getIntent().getStringExtra("USER_OBJECT_ID"));
        body.put("date", LocalDateTime.now().format(BASE_FORMAT));
        body.put("record_object_id", getIntent().getStringExtra("RECORD_OBJECT_ID"));
        body.put("status", status);
        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(CLOCK_HOST)
                .setBody(body)
                .setMethod(VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    Log.e("Clocked", response.toString());
                    try {
                        if (response.getBoolean("status")) {
                            if (status.equals("ON")) {
                                Toast.makeText(this, getString(R.string.clockin_success), Toast.LENGTH_LONG).show();
                                clockedin = true;
                                binding.clockinButton.setBackground(getResources().getDrawable(R.drawable.active_button));
                                binding.clockinTime.setText(LocalDateTime.now().format(HOUR_FORMAT));
                            } else {
                                Toast.makeText(this, getString(R.string.clockin_success), Toast.LENGTH_LONG).show();
                                clockedin = false;
                                binding.clockinButton.setBackground(getResources().getDrawable(R.drawable.inactive));
                                binding.clockinTime.setText("- - : - -");
                            }
                        } else {
                            Toast.makeText(this, getString(R.string.error_connecting), Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, getString(R.string.error_connecting), Toast.LENGTH_LONG).show();
                    }
                }).requestJson();
    }

    private void welcomeMessage() {
        String username = getIntent().getStringExtra("USERNAME");
        int hour = LocalDateTime.now().getHour();
        if ((hour >= 23) || (hour < 6)) {
            binding.welcome.setText(getString(R.string.midnight, username));
        } else if (hour < 12) {
            binding.welcome.setText(getString(R.string.morning, username));
        } else if (hour < 18) {
            binding.welcome.setText(getString(R.string.afternoon, username));
        } else {
            binding.welcome.setText(getString(R.string.evening, username));
        }
    }

    private void showManagementDialog(Intent intent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        final View customLayout = getLayoutInflater().inflate(R.layout.management_dialog, null);
        EditText first = customLayout.findViewById(R.id.first);
        EditText second = customLayout.findViewById(R.id.second);
        EditText third = customLayout.findViewById(R.id.third);
        EditText fourth = customLayout.findViewById(R.id.fourth);
        first.requestFocus();
        first.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (first.getText().toString().length() >= 1) {
                    second.requestFocus();
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        second.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (second.getText().toString().length() >= 1) {
                    third.requestFocus();
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        third.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (third.getText().toString().length() >= 1) {
                    fourth.requestFocus();
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

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
}
