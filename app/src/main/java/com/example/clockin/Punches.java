package com.example.clockin;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Objects;

public class Punches extends AppCompatActivity implements PunchesAdapter.ItemClickListener {

    PunchesAdapter adapter;
    private JSONArray punches;
    private String HOST = "https://52.139.218.209:443/record/cal_working_hours";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.punches);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // data to populate the RecyclerView with
        Log.v("Response", "Oncreate");
        populateDict();
    }

    private void setUpRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.rvPunches);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PunchesAdapter(this, punches);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    public void onItemClick(View view, int position) {
        // Toast.makeText(this, "You clicked " + adapter.getItem(position) + " on row number " + position, Toast.LENGTH_SHORT).show();
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void populateDict() {
        Log.v("Response", "Populating dict");
        Calendar calendar = Calendar.getInstance();
        String curr_month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
        String curr_day = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)+1);
        String curr = calendar.get(Calendar.YEAR) + "/" + curr_month +
                "/" + curr_day;
        calendar.add(Calendar.DAY_OF_YEAR, -30);
        String prev_month = Integer.toString(calendar.get(Calendar.MONTH) + 1);
        String prev = calendar.get(Calendar.YEAR) + "/" + prev_month +
                "/" + calendar.get(Calendar.DAY_OF_MONTH);
        calendar.add(Calendar.DAY_OF_YEAR, 30);

        String company_number = getIntent().getStringExtra("company_number");
        String username = getIntent().getStringExtra("username");
        HashMap<String, String> body = new HashMap<>();
        body.put("account", company_number);
        body.put("starttime", prev);
        body.put("endtime", curr);
        Log.v("Response", body.toString());

        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(HOST)
                .setBody(body)
                .setMethod( VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    Log.v("Response", response.toString());
                    try {
                        if (!response.getBoolean("status")) {
                            Toast.makeText(this, "Connection failed. Try again", Toast.LENGTH_LONG).show();
                        } else {
                            JSONObject jsonObject = response.getJSONObject("result");
                            jsonObject = jsonObject.getJSONObject(username);
                            punches = (JSONArray) jsonObject.get("detail");
                        }
                    } catch (JSONException e) {
                        punches = new JSONArray();
                    }
                    setUpRecyclerView();
                }).requestJson();

    }
}