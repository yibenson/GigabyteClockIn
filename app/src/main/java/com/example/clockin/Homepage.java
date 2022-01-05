package com.example.clockin;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.clockin.volley.VolleyDataRequester;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class Homepage extends AppCompatActivity implements View.OnClickListener {
    private String username;
    private String RECORD_HOST = "https://52.139.218.209:443/record/get_user_record";
    private String CLOCK_HOST = "https://52.139.218.209:443/identify/clockin";
    private JSONObject userObject;
    private boolean clockedin = false;
    private Chronometer chronometer;
    private long difference;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homepage);
        getSupportActionBar().setTitle("Home");
        ImageButton clockin = findViewById(R.id.clockin);
        ImageButton clockout = findViewById(R.id.clockout);
        ImageButton viewpunches = findViewById(R.id.viewpunches);
        TextView welcome = findViewById(R.id.welcome);
        ImageButton companyinfo = findViewById(R.id.companyinfo);
        if (!getIntent().getExtras().getBoolean("manager")) {
            companyinfo.setBackgroundResource(R.drawable.info_user);
            Toast.makeText(this, "Failed line 53", Toast.LENGTH_LONG).show();
        }
        chronometer = findViewById(R.id.chronometer);
        clockin.setOnClickListener(this);
        clockout.setOnClickListener(this);
        viewpunches.setOnClickListener(this);
        companyinfo.setOnClickListener(this);
        username = getIntent().getStringExtra("username");
        welcomeMessage(welcome);
        startTimer();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.clockin:
                if (!clockedin) {
                    showAlertDialog("ON");
                } else {
                    Toast.makeText(this, "You're already clocked in", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.clockout:
                if (clockedin) {
                    showAlertDialog("OFF");
                } else {
                    Toast.makeText(this, "You're already clocked out", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.viewpunches:
                Intent intent = new Intent(this, Punches.class);
                intent.putExtra("company_number", getIntent().getStringExtra("company_number"));
                intent.putExtra("username", username);
                startActivity(intent);
                break;
                // view punches
            case R.id.companyinfo:
                if (getIntent().getExtras().getBoolean("manager")) {
                    Intent employees_intent = new Intent(this, Employees.class);
                    employees_intent.putExtra("company_number", getIntent().getStringExtra("company_number"));
                    startActivity(employees_intent);
                } else {
                    Intent profile_intent = new Intent(this, UserProfile.class);
                    profile_intent.putExtra("company_number", getIntent().getStringExtra("company_number"));
                    profile_intent.putExtra("username", username);
                    startActivity(profile_intent);
                }
                break;
                // company info

        }
    }

    private void welcomeMessage(TextView textView) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if ((hour >= 23) || (hour < 6)) {
            textView.setText(getString(R.string.midnight, username));
        } else if (hour < 12) {
            textView.setText(getString(R.string.morning, username));
        } else if (hour < 18) {
            textView.setText(getString(R.string.afternoon, username));
        } else {
            textView.setText(getString(R.string.evening, username));
        }
    }

    private void startTimer() {
        Calendar calendar = Calendar.getInstance();
        String curr_month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
        String curr = calendar.get(Calendar.YEAR) + "/" + curr_month +
                "/" + calendar.get(Calendar.DAY_OF_MONTH);
        calendar.add(Calendar.DAY_OF_YEAR, -30);
        String prev_month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
        String prev = calendar.get(Calendar.YEAR) + "/" + prev_month +
                "/" + calendar.get(Calendar.DAY_OF_MONTH);
        calendar.add(Calendar.DAY_OF_YEAR, 30);
        final JSONObject[] jsonObject = new JSONObject[1];
        String company_number = getIntent().getStringExtra("company_number");
        HashMap<String, String> body = new HashMap<String, String>(){{
            put("account", company_number);
            put("starttime", prev);
            put("endtime", curr);
        }};
        Log.v("Homepage", body.toString());
        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(RECORD_HOST)
                .setBody(body)
                .setMethod(VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    Log.v("Homepage", response.toString());
                    try {
                        if (!response.getBoolean("status")) {
                            chronometer.setText(R.string.error);
                        } else {
                            JSONArray jsonArray = response.getJSONArray("result");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                jsonObject[0] = jsonArray.getJSONObject(i);
                                if (jsonObject[0].getString("user").equals(username)) {
                                    userObject = jsonObject[0];
                                }
                            }
                            if (userObject.getString("status").equals("OFF") || jsonObject[0] == null) {
                                clockedin = false;
                                chronometer.setText(R.string.clocked_out);
                            } else {
                                clockedin = true;
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                                Date date = simpleDateFormat.parse(userObject.getString("date"));
                                Log.v("Date", date.toString());
                                Log.v("Date", calendar.getTime().toString());
                                printDifference(date, calendar.getTime());
                            }
                        }
                    } catch (JSONException | ParseException e) {
                        Log.v("Timer", response.toString());
                        Log.v("Timer", "JSONException | ParseException detected");
                        chronometer.setText(R.string.error);
                    }
                }).requestJson();
    }

    private void showAlertDialog(String status) {
        AlertDialog alertDialog = new AlertDialog.Builder(Homepage.this).create();
        alertDialog.setMessage("Would you like to clock in/out?");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                (dialog, which) -> {
                    clock(status);
                    finish();
                }
        );
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void clock(String status) {
        Calendar calendar = Calendar.getInstance();
        HashMap<String, String> body = new HashMap<>();
        body.put("account", getIntent().getStringExtra("company_number"));
        body.put("user_object_id", getIntent().getStringExtra("user_object_id"));
        String curr_month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
        String time = calendar.get(Calendar.YEAR) + "/" + curr_month + "/" +
                calendar.get(Calendar.DAY_OF_MONTH) + " " + calendar.get(Calendar.HOUR_OF_DAY) +
                ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND);
        body.put("date", time);
        body.put("record_object_id", getIntent().getStringExtra("record_object_id"));
        body.put("status", status);
        Log.v("Clocked", status);
        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(CLOCK_HOST)
                .setBody(body)
                .setMethod(VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    Log.v("Clocked", response.toString());
                    try {
                        if (response.getBoolean("status")) {
                            Toast.makeText(this, "Your punch has been recorded", Toast.LENGTH_LONG).show();
                            if (status.equals("ON")) {
                                clockedin = true;
                                chronometer.setFormat("You've been clocked in for: %s");
                                chronometer.setBase(SystemClock.elapsedRealtime());
                                chronometer.start();
                            } else {
                                clockedin = false;
                                chronometer.setBase(SystemClock.elapsedRealtime());
                                difference = 0;
                                chronometer.stop();
                                chronometer.setText(R.string.clocked_out);
                            }
                        } else {
                            Toast.makeText(this, "Error connecting to server", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error connecting to server", Toast.LENGTH_LONG).show();
                    }
                }).requestJson();
    }

    private void printDifference(Date prev, Date curr) {
        difference = curr.getTime() - prev.getTime();
        chronometer.setOnChronometerTickListener(chronometer -> {
            difference+=1000;
            int h   = (int)(difference /3600000);
            int m = (int)(difference - h*3600000)/60000;
            int s= (int)(difference - h*3600000- m*60000)/1000 ;
            String t = "You've been clocked in for: " + (h < 10 ? "0"+h: h)+":"+(m < 10 ? "0"+m: m)+":"+ (s < 10 ? "0"+s: s);
            chronometer.setText(t);
        });
        chronometer.setBase(difference);
        chronometer.setText(R.string.timer_format);
        chronometer.start();
    }
}
