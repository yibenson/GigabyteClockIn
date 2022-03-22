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
                /**
                if (getIntent().getExtras().getBoolean("MANAGER")) {
                    Intent employees_intent = new Intent(this, Employees.class);
                    employees_intent.putExtra("ACCOUNT", getIntent().getStringExtra("ACCOUNT"));
                    employees_intent.putExtra("USERNAME", getIntent().getStringExtra("USERNAME"));
                    Log.v("Response", employees_intent.toString());
                    startActivity(employees_intent);
                } else {
                    Intent profile_intent = new Intent(this, UserProfile.class);
                    profile_intent.putExtra("company_number", getIntent().getStringExtra("company_number"));
                    profile_intent.putExtra("username", getIntent().getStringExtra("username"));
                    profile_intent.putExtra("manager", getIntent().getBooleanExtra("manager", false));
                    startActivity(profile_intent);
                }
                 */
                break;
            case R.id.logout:
                finish();
        }
    }





    // returns string representation of curr month and prev month
    private String[] getMonths() {
        Calendar calendar = Calendar.getInstance();
        // add 1 to result of calendar.get because months are counted 0-11
        String curr_month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
        String curr = calendar.get(Calendar.YEAR) + "/" + curr_month +
                "/" + calendar.get(Calendar.DAY_OF_MONTH);
        calendar.add(Calendar.DAY_OF_YEAR, -3);
        String prev_month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
        String prev = calendar.get(Calendar.YEAR) + "/" + prev_month +
                "/" + calendar.get(Calendar.DAY_OF_MONTH);
        calendar.add(Calendar.DAY_OF_YEAR, 3);
        return new String[]{curr, prev};
    }



    private void startTimer() {
        String[] months = getMonths();
        String curr_month = months[0];
        String prev_month = months[1];
        HashMap<String, String> body = new HashMap<String, String>(){{
            put("account", getIntent().getStringExtra("ACCOUNT"));
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
                            // binding.chronometer.setText(getString(R.string.error_connecting));
                            Log.v("Homepage", "Error connecting to server");
                        } else {
                            // iterate through records to find most recent record with matching username
                            JSONArray jsonArray = response.getJSONArray("result");
                            earliest_date = simpleDateFormat.parse("1900/01/01 00:00:00");
                            String username = getIntent().getStringExtra("USERNAME");
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
                                // do nothing
                                // clockedin = false;
                                // binding.chronometer.setText(R.string.clocked_out);

                            } else {
                                clockedin = true;
                                Date date = simpleDateFormat.parse(jsonObject[0].getString("date"));
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(date);
                                String time = calendar.get(Calendar.HOUR_OF_DAY) +
                                        " : " + calendar.get(Calendar.MINUTE);
                                binding.clockinTime.setText(time);
                                binding.clockinButton.setBackground(getResources().getDrawable(R.drawable.active_button));


                                // this will print how long user has been clocked in on chronometer
                                // date is null if no punch records exist yet (new users)
                                // binding.clockinTime
                                // printDifference(date);
                            }
                        }
                    } catch (JSONException | ParseException | NullPointerException e) {
                        binding.chronometer.setText(R.string.clocked_out);
                    }
                }).requestJson();
    }


    private void showAlertDialog(String status) {
        AlertDialog alertDialog = new AlertDialog.Builder(Homepage.this, R.style.AlertDialogTheme).create();
        Calendar calendar = Calendar.getInstance();
        String time = " : " + calendar.get(Calendar.HOUR_OF_DAY) +
                ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND);
        alertDialog.setMessage(time);

        alertDialog.setMessage(getString(R.string.clockin_confirm) + time);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes),
                (dialog, which) -> {
                    clock(status);
                    dialog.dismiss();
                    /**
                    Intent intent = new Intent(this, FaceClockIn.class);
                    intent.putExtra("ACCOUNT", getIntent().getStringExtra("ACCOUNT"));
                    intent.putExtra("PURPOSE", "IDENTIFY");
                    startActivity(intent);
                    finish();
                     */
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
        body.put("account", getIntent().getStringExtra("ACCOUNT"));
        body.put("user_object_id", getIntent().getStringExtra("USER_OBJECT_ID"));
        body.put("date", time);
        body.put("record_object_id", getIntent().getStringExtra("RECORD_OBJECT_ID"));
        body.put("status", status);
        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(CLOCK_HOST)
                .setBody(body)
                .setMethod(VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    Log.v("Clocked", response.toString());
                    try {
                        if (response.getBoolean("status")) {
                            if (status.equals("ON")) {
                                Toast.makeText(this, getString(R.string.clockin_success), Toast.LENGTH_LONG).show();
                                clockedin = true;
                                /**
                                binding.chronometer.setFormat(getString(R.string.clocked_in_for));
                                binding.chronometer.setBase(SystemClock.elapsedRealtime());
                                binding.chronometer.start();
                                 */
                                binding.clockinButton.setBackground(getResources().getDrawable(R.drawable.active_button));
                                int hour24hours = calendar.get(Calendar.HOUR_OF_DAY);
                                int minutes = calendar.get(Calendar.MINUTE);
                                binding.clockinTime.setText(hour24hours +  ":" +  minutes);
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
        String username = getIntent().getStringExtra("USERNAME");
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
