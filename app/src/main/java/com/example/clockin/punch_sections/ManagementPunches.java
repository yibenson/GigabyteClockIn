package com.example.clockin.punch_sections;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.clockin.R;
import com.example.clockin.databinding.ManagementPunchesBinding;
import com.example.clockin.volley.VolleyDataRequester;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class ManagementPunches extends AppCompatActivity {
    private String CAL_WORKING_HOURS = "https://52.139.218.209:443/record/cal_working_hours";
    private String GET_USER_PROFILE = "https://52.139.218.209:443/user/get_user_profile";


    AdapterSectionRecycler adapter;
    private List<SectionHeader> sectionHeaderList;
    private JSONObject faces;
    private JSONObject dates;
    SwipeRefreshLayout mSwipeRefreshLayout;
    private ManagementPunchesBinding binding;

    private final int START_DATE = 0;
    private int END_DATE = 1;

    private final int AUTOMATIC_DATES = 0;
    private final int MANUAL_DATES = 1;

    // holds date formatters to swap between strings
    private DateTimeFormatter BASE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /* Current possible bugs:
    1. Since getFaces() and populateDict() are requested asynchronously, the server request in populateDict might connect/get data back before
    the server request in getFaces(). This is very unlikely to happen but still possible and would likely crash the app


     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ManagementPunchesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mSwipeRefreshLayout = binding.swipeLayout;
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.all_punches_title));
        setDateWindow();
        binding.startDate.setOnClickListener(view -> openDateDialog(START_DATE));
        binding.endDate.setOnClickListener(view -> openDateDialog(END_DATE));
        binding.button3.setOnClickListener(view -> populateDict(null, MANUAL_DATES));
        faces = new JSONObject();
        dates = new JSONObject();
        sectionHeaderList = new ArrayList<>();

        getFaces();
        setUpRecyclerView();
        populateDict(LocalDateTime.now(), AUTOMATIC_DATES);
    }

    private void populateDict(LocalDateTime localDateTime, int mode) {
        String ACCOUNT = getIntent().getStringExtra("ACCOUNT");
        HashMap<String, String> body = new HashMap<>();
        body.put("account", ACCOUNT);
        if (mode == AUTOMATIC_DATES) {
            HashMap<String, String> dates = getTimes(localDateTime);
            body.put("starttime", dates.get("prev"));
            body.put("endtime", dates.get("curr"));
        } else if (mode == MANUAL_DATES) {
            LocalDateTime start = LocalDateTime.parse(binding.startDate.getText().toString(), DAY_FORMAT);
            LocalDateTime end = LocalDateTime.parse(binding.endDate.getText().toString(), DAY_FORMAT);
            if (start.isAfter(end)) {
                Toast.makeText(getApplicationContext(), "Dates are out of order", Toast.LENGTH_LONG).show();
                return;
            } else {
                body.put("starttime", binding.startDate.getText().toString());
                body.put("endtime", binding.endDate.getText().toString());
                dates = new JSONObject();
                sectionHeaderList = new ArrayList<>();
            }
        }
        Log.e("Punches", body.toString());

        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(CAL_WORKING_HOURS)
                .setBody(body)
                .setMethod( VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    try {
                        Log.e("Punches", response.toString());
                        if (!response.getBoolean("status")) {
                            Toast.makeText(this, "Connection failed. Try again", Toast.LENGTH_LONG).show();
                        } else {
                            // This section creates JSONObject of dates as keys and a JSONArray containing punch records for that date as values
                            // Each JSONArray is itself an array of JSONArrays, each inner array representing a different punch record: {timein, timeout, total time, name}
                            // Response object from API looks like {..., "result": { "name1": {..., "detail": JSONArray(JSONArray(clockindate1, clockoutdate1, totaltime1), ...)}}}
                            JSONObject result = response.getJSONObject("result");
                            int index;
                            if (sectionHeaderList.size() == 0) {
                                index = 0;
                            } else {
                                index = sectionHeaderList.size();
                            }
                            for (Iterator<String> it = result.keys(); it.hasNext();) {
                                String name = it.next();
                                JSONObject jsonObject = result.getJSONObject(name);
                                JSONArray detail = jsonObject.getJSONArray("detail"); // this json array contains json arrays of each clockin/out entry
                                for (int i = 0; i < detail.length(); i++) {
                                    JSONArray punch_times = detail.getJSONArray(i); // contains one clockin/out entry for one user
                                    LocalDateTime date = LocalDateTime.parse(punch_times.getString(0), BASE_FORMAT); // gets clockin time for this entry
                                    String date_string = date.format(DAY_FORMAT); // gets date/year of string
                                    if (!dates.has(date_string)) {
                                        dates.put(date_string, index);
                                        sectionHeaderList.add(new SectionHeader(new ArrayList<>(), date_string, 0));
                                        index++;
                                    }
                                    punch_times.put(name); // adds name of entry to the json array (VERY IMPORTANT)
                                     // puts the entry into the punches table
                                    int sectionIndex = dates.getInt(date_string);
                                    sectionHeaderList.get(sectionIndex).childList.add(new Child(punch_times));
                                }
                            }
                            Log.e("Punches", dates.toString());
                            if (dates.length()==0) {
                                Toast.makeText(getApplicationContext(), "No data detected", Toast.LENGTH_LONG).show();
                            }
                            if (mode == AUTOMATIC_DATES) { // if we load dates via refresh or upon first startup, insert into page
                                sectionHeaderList = sortDates(sectionHeaderList);
                                adapter.notifyDataChanged(sectionHeaderList);
                            } else { // otherwise, replace entire recyclerview with requested date range
                                sectionHeaderList = sortDates(sectionHeaderList);
                                adapter.notifyDataChanged(sectionHeaderList);
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

    /* Requests and stores the facial images of every employee. Destroyed when app exits */
    private void getFaces() {
        String ACCOUNT = getIntent().getStringExtra("ACCOUNT");
        HashMap<String, String> body = new HashMap<>();
        body.put("account", ACCOUNT);

        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(GET_USER_PROFILE)
                .setBody(body)
                .setMethod( VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    try {
                        if (!response.getBoolean("status")) {
                            Toast.makeText(this, "Connection failed. Try again", Toast.LENGTH_LONG).show();
                        } else {
                            JSONArray jsonArray = response.getJSONArray("result");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String name = jsonObject.getString("name");
                                if (!faces.has(name)) {
                                    faces.put(name, jsonObject.getString("face"));
                                }
                            }
                        }
                    } catch (JSONException e) {
                        // means no more data, so do nothing
                    }
                }).requestJson();
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

    private void setUpRecyclerView() {
        RecyclerView recyclerView = binding.rvPunches;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdapterSectionRecycler(this, sectionHeaderList, faces);
        recyclerView.setAdapter(adapter);
        //DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),DividerItemDecoration.VERTICAL);
        // recyclerView.addItemDecoration(dividerItemDecoration);
        // populate dict automatically decrements calendar by 30 days
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            LocalDateTime time = LocalDate.parse(binding.startDate.getText().toString(), DAY_FORMAT).atStartOfDay();
            String prev = time.minusDays(30).format(DAY_FORMAT);
            binding.startDate.setText(prev);
            populateDict(null, MANUAL_DATES);
            mSwipeRefreshLayout.setRefreshing(false);
        });
    }

    private void openDateDialog(int mode) {
        final Calendar cldr = Calendar.getInstance();
        int day = cldr.get(Calendar.DAY_OF_MONTH);
        int month = cldr.get(Calendar.MONTH);
        int year = cldr.get(Calendar.YEAR);
        // date picker dialog
        DatePickerDialog picker = new DatePickerDialog(this,
                (datePicker, i, i1, i2) -> {
                    LocalDate localDate = LocalDate.of(datePicker.getYear(), datePicker.getMonth(),
                            datePicker.getDayOfMonth());
                    if (mode == START_DATE) {
                        binding.startDate.setText(localDate.format(DAY_FORMAT));
                    } else {
                        binding.endDate.setText(localDate.format(DAY_FORMAT));
                    }
                }, year, month, day);
        picker.setButton(DialogInterface.BUTTON_NEUTRAL, "Name", (dialog, which) -> {
            // do nothing
        });
        picker.getDatePicker().setSpinnersShown(true);
        picker.getDatePicker().setCalendarViewShown(false);
        picker.show();
    }

    private List<SectionHeader> sortDates(List<SectionHeader> list) {
        List<String> dateStrings = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            dateStrings.add(list.get(i).getSectionText());
        }

        if (dateStrings.size() > 1) {
            Collections.sort(dateStrings, new Comparator<String>() {
                @Override
                public int compare(String s, String t1) {
                    LocalDateTime s1 = LocalDate.parse(s, DAY_FORMAT).atStartOfDay();
                    LocalDateTime s2 = LocalDate.parse(t1, DAY_FORMAT).atStartOfDay();
                    return s1.compareTo(s2);
                }
            });
        }

        HashMap<String, Integer> hashMap = new HashMap<>();
        for (int i = 0; i < dateStrings.size(); i++) {
            hashMap.put(dateStrings.get(i), i);
        }

        for (int i = 0; i < list.size(); i++) {
            SectionHeader sectionHeader = list.get(i);
            sectionHeader.index = hashMap.get(sectionHeader.getSectionText());
        }
        return list;
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