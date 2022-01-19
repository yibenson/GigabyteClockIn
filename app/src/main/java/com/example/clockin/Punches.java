package com.example.clockin;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.clockin.databinding.PunchesBinding;
import com.example.clockin.volley.VolleyDataRequester;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;

public class Punches extends AppCompatActivity implements PunchesAdapter.ItemClickListener {
    private String HOST = "https://52.139.218.209:443/record/cal_working_hours";

    PunchesAdapter adapter;
    private JSONArray punches;
    SwipeRefreshLayout mSwipeRefreshLayout;
    private PunchesBinding binding;

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
        calendar = Calendar.getInstance();
        setDateWindow();
        punches = new JSONArray();
        setUpRecyclerView();
        populateDict(calendar);
    }

    private void populateDict(Calendar calendar) {
        HashMap<String, String> dates = getTimes(calendar);
        String company_number = getIntent().getStringExtra("company_number");
        String username = getIntent().getStringExtra("username");
        HashMap<String, String> body = new HashMap<>();
        body.put("account", company_number);
        body.put("starttime", dates.get("prev"));
        body.put("endtime", dates.get("curr"));

        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(HOST)
                .setBody(body)
                .setMethod( VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    try {
                        Log.v("Response", response.toString());
                        if (!response.getBoolean("status")) {
                            Toast.makeText(this, "Connection failed. Try again", Toast.LENGTH_LONG).show();
                        } else {
                            JSONObject jsonObject = response.getJSONObject("result");
                            jsonObject = jsonObject.getJSONObject(username);
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
                            adapter.updateData(punches);
                            adapter.notifyItemRangeInserted(0, added_size);
                        }
                    } catch (JSONException e) {
                        // means no more data, so do nothing
                    }
                }).requestJson();
    }

    private void setDateWindow() {
        HashMap<String, String> dates = getTimes(calendar);
        String curr = simpleDateFormat.format(Calendar.getInstance().getTime());
        binding.dateWindow.setText(getString(R.string.date_window, dates.get("prev"), curr));
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
            populateDict(calendar);
            mSwipeRefreshLayout.setRefreshing(false);
        });
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