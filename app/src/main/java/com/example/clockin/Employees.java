package com.example.clockin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clockin.volley.VolleyDataRequester;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Objects;

public class Employees extends AppCompatActivity implements EmployeesAdapter.ItemClickListener {

    EmployeesAdapter adapter;
    private HashMap<String, JSONObject> employees;
    private String HOST = "https://52.139.218.209:443/record/get_user_record";
    private String earliest_date = "1900/01/01 00:00:00";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.employees);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // data to populate the RecyclerView with
        populateDict();
    }

    private void populateDict() {
        Calendar calendar = Calendar.getInstance();
        String curr_month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
        String curr_day = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH) + 1);
        String curr = calendar.get(Calendar.YEAR) + "/" + curr_month +
                "/" + curr_day;
        calendar.add(Calendar.DAY_OF_YEAR, -30);
        String prev_month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
        String prev = calendar.get(Calendar.YEAR) + "/" + prev_month +
                "/" + calendar.get(Calendar.DAY_OF_MONTH);
        calendar.add(Calendar.DAY_OF_YEAR, 30);

        String company_number = getIntent().getStringExtra("company_number");
        HashMap<String, String> body = new HashMap<>();
        body.put("account", company_number);
        body.put("starttime", prev);
        body.put("endtime", curr);

        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(HOST)
                .setBody(body)
                .setMethod(VolleyDataRequester.Method.POST)
                .setJsonResponseListener(response -> {
                    largeLog("Response", response.toString());
                    try {
                        if (!response.getBoolean("status")) {
                            Toast.makeText(this, "Connection failed. Try again", Toast.LENGTH_LONG).show();
                        } else {
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                            Date earliest = simpleDateFormat.parse(earliest_date);
                            JSONArray jsonArray = response.getJSONArray("result");
                            employees = new HashMap<>();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject currentObject = jsonArray.getJSONObject(i);
                                String username = currentObject.getString("user");
                                // insert employee object into hashmap if it doesn't exist yet
                                if (!employees.containsKey(username)) {
                                    employees.put(currentObject.getString("user"), currentObject);
                                    earliest = simpleDateFormat.parse(currentObject.getString("date"));
                                } else {
                                    // if employee object exists in hashmap, insert the most recent employee object
                                    Date date = simpleDateFormat.parse(currentObject.getString("date"));
                                    if (earliest.compareTo(date) < 0) {
                                        employees.put(currentObject.getString("user"), currentObject);
                                        earliest = date;
                                    }
                                }
                            }
                            setUpRecyclerView();

                        }
                    } catch (JSONException | ParseException e){
                        e.printStackTrace();
                    }
                }).requestJson();

    }


    private void setUpRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.rvEmployees);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmployeesAdapter(this, employees);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);
    }

    public static void largeLog(String tag, String content) {
        if (content.length() > 4000) {
            Log.d(tag, content.substring(0, 4000));
            largeLog(tag, content.substring(4000));
        } else {
            Log.d(tag, content);
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        String username = adapter.getItem(position);
        Intent intent = new Intent(this, UserProfile.class);
        intent.putExtra("username", username);
        intent.putExtra("company_number", getIntent().getStringExtra("company_number"));
        startActivity(intent);
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