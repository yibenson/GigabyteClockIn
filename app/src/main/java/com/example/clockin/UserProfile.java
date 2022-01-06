package com.example.clockin;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.clockin.volley.VolleyDataRequester;

import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.charts.ValueLineChart;
import org.eazegraph.lib.models.BarModel;
import org.eazegraph.lib.models.PieModel;
import org.eazegraph.lib.models.ValueLinePoint;
import org.eazegraph.lib.models.ValueLineSeries;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Objects;

public class UserProfile extends AppCompatActivity {
    private String HOST = "https://52.139.218.209:443/user/get_user_profile";
    private TextView username;
    private TextView email;
    private TextView phone;
    private TextView wage;
    private TextView view_punches;
    private ImageView photo;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userprofile);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("User Profile");
        getSupportActionBar().setSubtitle("Account Information");

        // TextViews
        username = findViewById(R.id.profile_username);
        email = findViewById(R.id.profile_email);
        phone = findViewById(R.id.profile_phone);
        wage = findViewById(R.id.profile_wage);
        photo = findViewById(R.id.profile_photo);
        view_punches = findViewById(R.id.view_punches);

        // populate textviews/photo with user's info
        fillInfo();

        view_punches.setOnClickListener(view -> {
            Intent intent = new Intent(this, Punches.class);
            intent.putExtras(getIntent());
            startActivity(intent);
        });

        // bar chart
        BarChart mBarChart = (BarChart) findViewById(R.id.profile_barchart);
        mBarChart.addBar(new BarModel(2.3f, 0xFF123456));
        mBarChart.addBar(new BarModel(2.f,  0xFF343456));
        mBarChart.addBar(new BarModel(3.3f, 0xFF563456));
        mBarChart.addBar(new BarModel(1.1f, 0xFF873F56));
        mBarChart.addBar(new BarModel(2.7f, 0xFF56B7F1));
        mBarChart.addBar(new BarModel(2.f,  0xFF343456));
        mBarChart.addBar(new BarModel(0.4f, 0xFF1FF4AC));
        mBarChart.addBar(new BarModel(4.f,  0xFF1BA4E6));

        mBarChart.startAnimation();

        // value line chart
        ValueLineChart mCubicValueLineChart = (ValueLineChart) findViewById(R.id.cubiclinechart);

        ValueLineSeries series = new ValueLineSeries();
        series.setColor(0xFF56B7F1);

        series.addPoint(new ValueLinePoint("Jan", 2.4f));
        series.addPoint(new ValueLinePoint("Feb", 3.4f));
        series.addPoint(new ValueLinePoint("Mar", .4f));
        series.addPoint(new ValueLinePoint("Apr", 1.2f));
        series.addPoint(new ValueLinePoint("Mai", 2.6f));
        series.addPoint(new ValueLinePoint("Jun", 1.0f));
        series.addPoint(new ValueLinePoint("Jul", 3.5f));
        series.addPoint(new ValueLinePoint("Aug", 2.4f));
        series.addPoint(new ValueLinePoint("Sep", 2.4f));
        series.addPoint(new ValueLinePoint("Oct", 3.4f));
        series.addPoint(new ValueLinePoint("Nov", .4f));
        series.addPoint(new ValueLinePoint("Dec", 1.3f));

        mCubicValueLineChart.addSeries(series);
        mCubicValueLineChart.startAnimation();

        PieChart mPieChart = (PieChart) findViewById(R.id.piechart);

        mPieChart.addPieSlice(new PieModel("Freetime", 15, Color.parseColor("#FE6DA8")));
        mPieChart.addPieSlice(new PieModel("Sleep", 25, Color.parseColor("#56B7F1")));
        mPieChart.addPieSlice(new PieModel("Work", 35, Color.parseColor("#CDA67F")));
        mPieChart.addPieSlice(new PieModel("Eating", 9, Color.parseColor("#FED70E")));

        mPieChart.startAnimation();

    }

    @SuppressLint("SetTextI18n")
    public void fillInfo() {
        HashMap<String, String> body = new HashMap<>();
        body.put("account", getIntent().getExtras().getString("company_number"));
        String name = getIntent().getStringExtra("username");
        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(HOST)
                .setBody(body)
                .setMethod(VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    try {
                        if (!response.getBoolean("status")) {
                            Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show();
                        } else {
                            JSONObject jsonObject = new JSONObject();
                            JSONArray jsonArray = response.getJSONArray("result");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                if (jsonArray.getJSONObject(i).getString("name").equals(name)) {
                                    jsonObject = jsonArray.getJSONObject(i);
                                }
                            }
                            username.setText("Name: " + jsonObject.getString("name"));
                            phone.setText("Phone: " + jsonObject.getString("phone"));
                            email.setText("Email: " + jsonObject.getString("mail"));
                            wage.setText("Wage: " + jsonObject.getString("wage"));
                            byte[] decodedString = Base64.decode(jsonObject.getString("face").replace("\\n", "\n"), Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            photo.setImageBitmap(decodedByte);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }).requestJson();
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
