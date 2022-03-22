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

    // holds the curr date endpoint of record window
    private Calendar calendar;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY/MM/dd");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PunchesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mSwipeRefreshLayout = binding.swipeLayout;
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.punch_title));
        calendar = Calendar.getInstance();
        setDateWindow();
        binding.startDate.setOnClickListener(view -> openDateDialog(START_DATE));
        binding.endDate.setOnClickListener(view -> openDateDialog(END_DATE));
        binding.button3.setOnClickListener(view -> populateDict(calendar, MANUAL_DATES));
        punches = new JSONArray();
        Log.v("Punches", "Setting up recycler view");
        setUpRecyclerView();
        Log.v("Punches", "Attempting to populate dict");
        populateDict(calendar, AUTOMATIC_DATES);
    }

    private void populateDict(Calendar calendar, int mode) {
        String company_number = getIntent().getStringExtra("ACCOUNT");
        String username = getIntent().getStringExtra("USERNAME");
        HashMap<String, String> body = new HashMap<>();
        body.put("account", company_number);
        if (mode == AUTOMATIC_DATES) {
            HashMap<String, String> dates = getTimes(calendar);
            body.put("starttime", dates.get("prev"));
            body.put("endtime", dates.get("curr"));
        } else {
            // do same thing for now
            body.put("starttime", binding.startDate.getText().toString());
            body.put("endtime", binding.endDate.getText().toString());
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
                            JSONObject jsonObject = response.getJSONObject("result");
                            for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                                String s = it.next();
                                jsonObject = jsonObject.getJSONObject(s);
                                JSONArray temp = jsonObject.getJSONArray("detail");
                                int added_size = temp.length();
                                // want earliest punches to go to front of list, latest (most recent) at end
                                for (int i = 0; i < punches.length(); i++) {
                                    temp.put(punches.get(i));
                                }
                                punches = temp;
                                if (punches.length()==0) {
                                    Toast.makeText(getApplicationContext(), "No data detected", Toast.LENGTH_LONG).show();
                                }
                                if (mode == AUTOMATIC_DATES) {
                                    adapter.updateData(punches);
                                    adapter.notifyItemRangeInserted(0, added_size);
                                } else {
                                    adapter.updateData(punches);
                                    adapter.notifyDataSetChanged();
                                }


                            }

                        }
                    } catch (JSONException e) {
                        // means no more data, so do nothing
                    }
                }).requestJson();
    }

    private void setDateWindow() {
        HashMap<String, String> dates = getTimes(calendar);
        String curr = simpleDateFormat.format(Calendar.getInstance().getTime());
        binding.startDate.setText(dates.get("prev"));
        binding.endDate.setText(curr);
    }


    // given calendar, returns dict of calendar's time and string of calendar's time 30 days prior
    private HashMap<String, String> getTimes(Calendar cldr) {
        String curr = simpleDateFormat.format(cldr.getTime());
        cldr.add(Calendar.DAY_OF_YEAR, -30);
        String prev = simpleDateFormat.format(cldr.getTime());
        cldr.add(Calendar.DAY_OF_YEAR, 30);
        return new HashMap<String, String>() {{
            put("curr", curr);
            put("prev", prev);
        }};
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
            calendar.add(Calendar.DAY_OF_YEAR, -30);
            setDateWindow();
            populateDict(calendar, AUTOMATIC_DATES);
            mSwipeRefreshLayout.setRefreshing(false);
        });
    }

    private void openDateDialog(int mode) {
        final Calendar cldr = Calendar.getInstance();
        int day = cldr.get(Calendar.DAY_OF_MONTH);
        int month = cldr.get(Calendar.MONTH);
        int year = cldr.get(Calendar.YEAR);
        // date picker dialog
        DatePickerDialog picker = new DatePickerDialog(Punches.this,
                (datePicker, i, i1, i2) -> {
                    String date = i + "/" + (i1+1) + "/" + i2;
                    Log.v("Punches", date);
                    if (mode == START_DATE) {
                        binding.startDate.setText(date);
                    } else {
                        binding.endDate.setText(date);
                    }
                }, year, month, day);
        picker.setButton(DialogInterface.BUTTON_NEUTRAL, "Name", (dialog, which) -> {
            // do nothing
        });
        picker.getDatePicker().setSpinnersShown(true);
        picker.getDatePicker().setCalendarViewShown(false);
        picker.show();
    }

    @Override
    public void onItemClick(View view, int position) {
        // TODO: Allow editing of punches
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