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

import com.example.clockin.databinding.HomepageBinding;
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
    private String RECORD_HOST = "https://52.139.218.209:443/record/get_user_record";
    private String CLOCK_HOST = "https://52.139.218.209:443/identify/clockin";
    private boolean clockedin = false;
    private Date earliest_date;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private HomepageBinding binding;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = HomepageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().setTitle("Home");
        if (!getIntent().getExtras().getBoolean("manager")) {
            // if user is not manager, set button background to appropriate drawable
            binding.companyinfo.setBackgroundResource(R.drawable.info_user);
        }
        binding.clockin.setOnClickListener(this);
        binding.clockout.setOnClickListener(this);
        binding.viewpunches.setOnClickListener(this);
        binding.companyinfo.setOnClickListener(this);
        welcomeMessage();
        startTimer();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.clockin:
                if (!clockedin) {
                    showAlertDialog("ON");
                } else {
                    Toast.makeText(this, getString(R.string.already_clocked_in), Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.clockout:
                if (clockedin) {
                    showAlertDialog("OFF");
                } else {
                    Toast.makeText(this, getString(R.string.not_currently_clocked_in), Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.viewpunches:
                Intent intent = new Intent(this, Punches.class);
                intent.putExtra("company_number", getIntent().getStringExtra("company_number"));
                intent.putExtra("username", getIntent().getStringExtra("username"));
                startActivity(intent);
                break;
            case R.id.companyinfo:
                if (getIntent().getExtras().getBoolean("manager")) {
                    Intent employees_intent = new Intent(this, Employees.class);
                    employees_intent.putExtra("company_number", getIntent().getStringExtra("company_number"));
                    employees_intent.putExtra("manager", getIntent().getBooleanExtra("manager", false));
                    Log.v("Response", employees_intent.toString());
                    startActivity(employees_intent);
                } else {
                    Intent profile_intent = new Intent(this, UserProfile.class);
                    profile_intent.putExtra("company_number", getIntent().getStringExtra("company_number"));
                    profile_intent.putExtra("username", getIntent().getStringExtra("username"));
                    profile_intent.putExtra("manager", getIntent().getBooleanExtra("manager", false));
                    startActivity(profile_intent);
                }
                break;
        }
    }

    // returns string representation of curr month and prev month
    private String[] getMonths() {
        Calendar calendar = Calendar.getInstance();
        // add 1 to result of calendar.get because months are counted 0-11
        String curr_month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
        String curr = calendar.get(Calendar.YEAR) + "/" + curr_month +
                "/" + calendar.get(Calendar.DAY_OF_MONTH);
        calendar.add(Calendar.DAY_OF_YEAR, -30);
        String prev_month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
        String prev = calendar.get(Calendar.YEAR) + "/" + prev_month +
                "/" + calendar.get(Calendar.DAY_OF_MONTH);
        calendar.add(Calendar.DAY_OF_YEAR, 30);
        return new String[]{curr, prev};
    }

    private void startTimer() {
        String[] months = getMonths();
        String curr_month = months[0];
        String prev_month = months[1];
        HashMap<String, String> body = new HashMap<String, String>(){{
            put("account", getIntent().getStringExtra("company_number"));
            put("starttime", prev_month);
            put("endtime", curr_month);
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
                            binding.chronometer.setText(getString(R.string.error_connecting));
                        } else {
                            // iterate through records to find most recent record with matching username
                            JSONArray jsonArray = response.getJSONArray("result");
                            earliest_date = simpleDateFormat.parse("1900/01/01 00:00:00");
                            String username = getIntent().getStringExtra("username");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject temp = jsonArray.getJSONObject(i);
                                if (temp.getString("user").equals(username)) {
                                    Date date = simpleDateFormat.parse(temp.getString("date"));
                                    if (earliest_date.compareTo(date) < 0) {
                                        jsonObject[0] = temp;
                                        earliest_date = date;
                                    }
                                }
                            }
                            if (jsonObject[0].getString("status").equals("OFF")) {
                                clockedin = false;
                                binding.chronometer.setText(R.string.clocked_out);
                            } else {
                                clockedin = true;
                                Date date = simpleDateFormat.parse(jsonObject[0].getString("date"));
                                // this will print how long user has been clocked in on chronometer
                                // date is null if no punch records exist yet (new users)
                                printDifference(date);
                            }
                        }
                    } catch (JSONException | ParseException | NullPointerException e) {
                        binding.chronometer.setText(R.string.clocked_out);
                    }
                }).requestJson();
    }


    private void showAlertDialog(String status) {
        AlertDialog alertDialog = new AlertDialog.Builder(Homepage.this).create();
        alertDialog.setMessage(getString(R.string.clockin_confirm));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes),
                (dialog, which) -> {
                    clock(status);
                    Intent intent = new Intent(this, FaceClockIn.class);
                    intent.putExtra("company_number", getIntent().getStringExtra("company_number"));
                    intent.putExtra("Purpose", "Identify");
                    startActivity(intent);
                    finish();
                }
        );
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void clock(String status) {
        // get curr time
        Calendar calendar = Calendar.getInstance();
        String curr_month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
        String time = calendar.get(Calendar.YEAR) + "/" + curr_month + "/" +
                calendar.get(Calendar.DAY_OF_MONTH) + " " + calendar.get(Calendar.HOUR_OF_DAY) +
                ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND);

        HashMap<String, String> body = new HashMap<>();
        body.put("account", getIntent().getStringExtra("company_number"));
        body.put("user_object_id", getIntent().getStringExtra("user_object_id"));
        body.put("date", time);
        body.put("record_object_id", getIntent().getStringExtra("record_object_id"));
        body.put("status", status);
        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(CLOCK_HOST)
                .setBody(body)
                .setMethod(VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    Log.v("Clocked", response.toString());
                    try {
                        if (response.getBoolean("status")) {
                            Toast.makeText(this, getString(R.string.clockin_success), Toast.LENGTH_LONG).show();
                            if (status.equals("ON")) {
                                clockedin = true;
                                binding.chronometer.setFormat(getString(R.string.clocked_in_for));
                                binding.chronometer.setBase(SystemClock.elapsedRealtime());
                                binding.chronometer.start();
                            } else {
                                clockedin = false;
                                binding.chronometer.setBase(SystemClock.elapsedRealtime());
                                binding.chronometer.stop();
                                binding.chronometer.setText(R.string.clocked_out);
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

    private void printDifference(Date prev) {
        Calendar calendar = Calendar.getInstance();
        Date curr = calendar.getTime();
        final long[] difference = {curr.getTime() - prev.getTime()};
        binding.chronometer.setOnChronometerTickListener(chronometer -> {
            difference[0] +=1000;
            int h   = (int)(difference[0] / 3600000);
            int m = (int)(difference[0] - h*3600000) / 60000;
            int s= (int)(difference[0] - h*3600000- m*60000)/1000 ;
            String t = (h < 10 ? "0"+h: h)+":"+(m < 10 ? "0"+m: m)+":"+ (s < 10 ? "0"+s: s);
            binding.chronometer.setText(getString(R.string.clocked_in_for, t));
        });
        binding.chronometer.setBase(difference[0]);
        binding.chronometer.setText(R.string.timer_format);
        binding.chronometer.start();
    }

    private void welcomeMessage() {
        String username = getIntent().getStringExtra("username");
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
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
}
