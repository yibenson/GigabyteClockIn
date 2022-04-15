package com.example.clockin.punch_sections;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.clockin.R;
import com.example.clockin.UserRegistrationWindow;
import com.example.clockin.databinding.ManagementPunchesBinding;
import com.example.clockin.volley.VolleyDataRequester;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class ManagementPunches extends AppCompatActivity implements ChildViewHolder.OnEditClickListener {
    private final String CAL_WORKING_HOURS = "https://52.139.218.209:443/record/cal_working_hours";
    private final String EDIT_USER_RECORD = "https://52.139.218.209:443/record/edit_user_record";
    private final String GET_USER_PROFILE = "https://52.139.218.209:443/user/get_user_profile";


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
    private DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

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

        dates = new JSONObject();
        sectionHeaderList = new ArrayList<>();

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
                            int index = 0;
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
                                        sectionHeaderList.add(new SectionHeader(new ArrayList<>(), date_string, index));
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
        adapter = new AdapterSectionRecycler(this, sectionHeaderList, faces, this);
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
        LocalDate localDate = LocalDate.now();
        if (mode == START_DATE) {
            localDate = LocalDate.parse(binding.startDate.getText(), DAY_FORMAT);
        } else if (mode == END_DATE) {
            localDate = LocalDate.parse(binding.endDate.getText(), DAY_FORMAT);
        }
        // date picker dialog
        DatePickerDialog picker = new DatePickerDialog(this,
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

        Log.e("Date", dateStrings.toString());

        for (int i = 0; i < list.size(); i++) {
            SectionHeader sectionHeader = list.get(i);
            sectionHeader.index = hashMap.get(sectionHeader.getSectionText());
        }

        Collections.sort(list, new Comparator<SectionHeader>() {
            @Override
            public int compare(SectionHeader sectionHeader, SectionHeader t1) {
                return sectionHeader.index < t1.index? -1 : 0;
            }
        });

        return list;
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // implementation of click listener that we pass to AdapterSectionRecycler

    @Override
    public void onEditClick(int position) {
        int counter = -1;
        for (SectionHeader s: sectionHeaderList) {
            counter++;
            if (counter == position) { return; }
            for (Child c: s.childList) {
                counter++;
                if (counter == position) {
                    showAlertDialog(c);
                    return;
                }
            }
        }
    }

    private void showAlertDialog(Child child) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        final View customLayout = getLayoutInflater().inflate(R.layout.edit_view_dialog, null);
        builder.setView(customLayout);
        ImageView imageView = customLayout.findViewById(R.id.photo);
        TextView username = customLayout.findViewById(R.id.username);
        TextView start = customLayout.findViewById(R.id.start_time);
        TextView end = customLayout.findViewById(R.id.end_time);
        TextView total = customLayout.findViewById(R.id.total_time);
        try {
            byte[] decodedString = Base64.decode(faces.getString(child.getName()).replace("\\n", "\n"), Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imageView.setImageBitmap(decodedByte);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        username.setText(child.getName());
        start.setText(getString(R.string.in_string, child.getInTime()));
        end.setText(getString(R.string.out_string, child.getOutTime()));
        total.setText(getString(R.string.hours, child.getTotalTime()));
        builder.setPositiveButton("Edit", (dialog, which) -> {
            Log.e("Edit", "Edit punch pressed");
            showEditDialog(child);
                });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
            dialog.dismiss();
        });
        builder.create().show();
    }

    private void showEditDialog(Child child) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        final View customLayout = getLayoutInflater().inflate(R.layout.edit_edit_dialog, null);
        builder.setView(customLayout);
        TextView start_time = customLayout.findViewById(R.id.start_time);
        TextView end_time = customLayout.findViewById(R.id.end_time);
        TextView start_date = customLayout.findViewById(R.id.start_date);
        TextView end_date = customLayout.findViewById(R.id.end_date);
        start_time.setText(child.getInTime());
        end_time.setText(child.getOutTime());
        start_date.setText(child.getInDate());
        end_date.setText(child.getOutDate());

        String start_string = start_date.getText().toString() + " " + start_time.getText().toString();
        String end_string = end_date.getText().toString() + " " + end_time.getText().toString();
        LocalDateTime start = LocalDateTime.parse(start_string, BASE_FORMAT);
        LocalDateTime end = LocalDateTime.parse(end_string, BASE_FORMAT);

        start_date.setOnClickListener(v -> {
            int day = start.getDayOfMonth();
            int month = start.getMonthValue();
            int year = start.getYear();
            // date picker dialog
            DatePickerDialog picker = new DatePickerDialog(ManagementPunches.this,
                    (datePicker, i, i1, i2) -> {
                        LocalDate localDate = LocalDate.of(i, i1 + 1, i2);
                        start_date.setText(localDate.format(DAY_FORMAT));
                    }, year, month, day);
            picker.getDatePicker().setSpinnersShown(true);
            picker.getDatePicker().setCalendarViewShown(false);
            /* TODO: idk why but the buttons are invisible (you can still press them) - i think the color blends into the white background,
            but trying to change the color of the buttons crashes the page with a null object reference
             */
            //picker.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.main));
            // picker.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.main));
            picker.show();
        });

        end_date.setOnClickListener(v -> {
            int day = end.getDayOfMonth();
            int month = end.getMonthValue();
            int year = end.getYear();
            // date picker dialog
            DatePickerDialog picker = new DatePickerDialog(ManagementPunches.this,
                    (datePicker, i, i1, i2) -> {
                        LocalDate localDate = LocalDate.of(i, i1 + 1, i2);
                        end_date.setText(localDate.format(DAY_FORMAT));
                    }, year, month, day);
            picker.getDatePicker().setSpinnersShown(true);
            picker.getDatePicker().setCalendarViewShown(false);
            // picker.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.main));
            // picker.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.main));
            picker.show();
        });

        start_time.setOnClickListener(v -> {
            int mHour = start.getHour();
            int mMinute = start.getMinute();
            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, hourOfDay, minute) -> {
                        LocalTime localTime = LocalTime.of(hourOfDay, minute);
                        start_time.setText(localTime.format(HOUR_FORMAT));
                    }, mHour, mMinute, false);
            // timePickerDialog.getButton(TimePickerDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.main));
            // timePickerDialog.getButton(TimePickerDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.main));
            timePickerDialog.show();
        });

        end_time.setOnClickListener(v -> {
            int mHour = end.getHour();
            int mMinute = end.getMinute();
            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, hourOfDay, minute) -> {
                        LocalTime localTime = LocalTime.of(hourOfDay, minute);
                        end_time.setText(localTime.format(HOUR_FORMAT));
                    }, mHour, mMinute, false);
            // timePickerDialog.getButton(TimePickerDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.main));
            // timePickerDialog.getButton(TimePickerDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.main));
            timePickerDialog.show();
        });

        builder.setPositiveButton(getString(R.string.yes), (dialog, which) -> {
            String start_string_final = start_date.getText().toString() + " " + start_time.getText().toString();
            String end_string_final = end_date.getText().toString() + " " + end_time.getText().toString();
            LocalDateTime start_final = LocalDateTime.parse(start_string_final, BASE_FORMAT);
            LocalDateTime end_final = LocalDateTime.parse(end_string_final, BASE_FORMAT);
            editRequest(child, start_final, end_final);
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
            dialog.dismiss();
        });
        builder.create().show();
    }

    private void editRequest(Child child, LocalDateTime start, LocalDateTime end) {
        String ACCOUNT = getIntent().getStringExtra("ACCOUNT");
        HashMap<String, String> mapBody = new HashMap<>();
        mapBody.put("account", ACCOUNT);
        mapBody.put("user", child.getName());
        mapBody.put("status", "ON");
        LocalDateTime origin_time_ON = LocalDateTime.parse(child.getInDate() + " " + child.getInTime(), BASE_FORMAT);
        mapBody.put("origin_time", origin_time_ON.format(BASE_FORMAT));
        mapBody.put("edit_time", start.format(BASE_FORMAT));

        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(EDIT_USER_RECORD)
                .setBody(mapBody)
                .setMethod( VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    try {
                        Log.v("Response", response.toString());
                        if (response.get("status").toString().equals("false")) {
                            runToast(getString(R.string.error_connecting));
                        } else {
                            runToast(getString(R.string.editing_success));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }).requestJson();

        mapBody.put("status", "OFF");
        LocalDateTime origin_time_OFF = LocalDateTime.parse(child.getOutDate() + " " + child.getOutTime(), BASE_FORMAT);
        mapBody.put("origin_time", origin_time_OFF.format(BASE_FORMAT));
        mapBody.put("edit_time", end.format(BASE_FORMAT));

        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(EDIT_USER_RECORD)
                .setBody(mapBody)
                .setMethod( VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    try {
                        Log.v("Response", response.toString());
                        if (response.get("status").toString().equals("false")) {
                            runToast(getString(R.string.error_connecting));
                        } else {
                            runToast(getString(R.string.editing_success));
                            // you can comment this out - implementation of what happens after edit can be discussed
                            populateDict(null, MANUAL_DATES);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }).requestJson();
    }

    private void runToast(String msg) {
        final String str = msg;
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show());
    }




}