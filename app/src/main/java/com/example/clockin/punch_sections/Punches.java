package com.example.clockin.punch_sections;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.clockin.R;
import com.example.clockin.databinding.PunchesBinding;
import com.example.clockin.volley.VolleyDataRequester;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

public class Punches extends AppCompatActivity implements PunchesAdapter.ItemClickListener {
    private String HOST = "https://52.139.218.209:443/record/cal_working_hours";

    PunchesAdapter adapter;
    private JSONArray punches;
    SwipeRefreshLayout mSwipeRefreshLayout;
    private PunchesBinding binding;

    private int START_DATE = 0;
    private int END_DATE = 1;

    private int AUTOMATIC_DATES = 0;
    private int MANUAL_DATES = 1;

    // holds date formatters to swap between strings
    private DateTimeFormatter BASE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PunchesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mSwipeRefreshLayout = binding.swipeLayout;
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.punch_title));
        setDateWindow();
        binding.startDate.setOnClickListener(view -> openDateDialog(START_DATE));
        binding.endDate.setOnClickListener(view -> openDateDialog(END_DATE));
        binding.button3.setOnClickListener(view -> populateDict(null, MANUAL_DATES));
        punches = new JSONArray();
        Log.v("Punches", "Setting up recycler view");
        setUpRecyclerView();
        Log.v("Punches", "Attempting to populate dict");
        populateDict(LocalDateTime.now(), AUTOMATIC_DATES);
    }

    private void populateDict(LocalDateTime localDateTime, int mode) {
        String ACCOUNT = getIntent().getStringExtra("ACCOUNT");
        String USERNAME = getIntent().getStringExtra("USERNAME");
        HashMap<String, String> body = new HashMap<>();
        body.put("account", ACCOUNT);
        if (mode == AUTOMATIC_DATES) {
            HashMap<String, String> dates = getTimes(localDateTime);
            body.put("starttime", dates.get("prev"));
            body.put("endtime", dates.get("curr"));
        } else if (mode == MANUAL_DATES) {
            LocalDate start = LocalDate.parse(binding.startDate.getText().toString(), DAY_FORMAT);
            LocalDate end = LocalDate.parse(binding.endDate.getText().toString(), DAY_FORMAT);
            if (start.isAfter(end)) {
                Toast.makeText(getApplicationContext(), "Dates are out of order", Toast.LENGTH_LONG).show();
                return;
            } else {
                body.put("starttime", start.format(DAY_FORMAT));
                body.put("endtime", end.format(DAY_FORMAT));
            }
        }

        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(HOST)
                .setBody(body)
                .setMethod( VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    try {
                        Log.v("Punches", response.toString());
                        if (!response.getBoolean("status")) {
                            Toast.makeText(this, "Connection failed. Try again", Toast.LENGTH_LONG).show();
                        } else {
                            // Response object from API looks like {..., "result": { "name1": {..., "detail": JSONArray(JSONArray(clockindate1, clockoutdate1, totaltime1), ...)}}}
                            JSONArray detail = response.getJSONObject("result").getJSONObject(USERNAME).getJSONArray("detail");
                            /*
                            int added_size = detail.length(); // number of records we are inserting
                            for (int i = 0; i < punches.length(); i++) {
                                detail.put(punches.get(i));
                            }
                             */
                            punches = detail;
                            if (punches.length()==0) {
                                Toast.makeText(getApplicationContext(), "No data detected", Toast.LENGTH_LONG).show();
                            }
                            if (mode == AUTOMATIC_DATES) {
                                adapter.updateData(punches);
                                adapter.notifyDataSetChanged();
                            } else {
                                adapter.updateData(punches);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    } catch (JSONException e) {
                        // means no more data, so do nothing
                    }
                }).requestJson();
    }

    private void setDateWindow() {
        String curr = LocalDateTime.now().format(DAY_FORMAT);
        String prev = LocalDateTime.now().minusDays(30).format(DAY_FORMAT);
        binding.startDate.setText(prev);
        binding.endDate.setText(curr);
    }

    private void setUpRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.rvPunches);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PunchesAdapter(this, punches);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);
        // populate dict automatically decrements calendar by 30 days
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            LocalDate time = LocalDate.parse(binding.startDate.getText().toString(), DAY_FORMAT);
            String prev = time.minusDays(30).format(DAY_FORMAT);
            binding.startDate.setText(prev);
            populateDict(null, MANUAL_DATES);
            mSwipeRefreshLayout.setRefreshing(false);
        });
    }

    private void openDateDialog(int mode) {
        LocalDate localDate = LocalDate.now();
        if (mode == START_DATE) {
            localDate = LocalDate.parse(binding.startDate.getText(), DAY_FORMAT);
        } else if (mode == END_DATE) {
            localDate = LocalDate.parse(binding.endDate.getText(), DAY_FORMAT);
        }
        // date picker dialog
        DatePickerDialog picker = new DatePickerDialog(Punches.this,
                (datePicker, i, i1, i2) -> {
                    LocalDate selectedDate = LocalDate.of(datePicker.getYear(), datePicker.getMonth() + 1,
                            datePicker.getDayOfMonth());
                    if (mode == START_DATE) {
                        binding.startDate.setText(selectedDate.format(DAY_FORMAT));
                    } else {
                        binding.endDate.setText(selectedDate.format(DAY_FORMAT));
                    }
                }, localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
        picker.getDatePicker().setSpinnersShown(true);
        picker.getDatePicker().setCalendarViewShown(false);
        picker.show();
    }


    // given calendar, returns dict of calendar's time and string of calendar's time 30 days prior
    private HashMap<String, String> getTimes(LocalDateTime time) {
        String curr = time.format(DAY_FORMAT);
        String prev = time.minusDays(30).format(DAY_FORMAT);
        return new HashMap<String, String>() {{
            put("curr", curr);
            put("prev", prev);
        }};
    }

    @Override
    public void onItemClick(View view, int position) {
        // Toast.makeText(getApplicationContext(), "You clicked on position " + position, Toast.LENGTH_LONG).show();
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