package com.example.clockin.punch_sections;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.clockin.R;

import org.json.JSONArray;
import org.json.JSONException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Child {
    private String inDate;
    private String inTime;
    private String outTime;
    private String totalTime;
    private String name;
    private DateTimeFormatter BASE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public Child(JSONArray entry) {
        try {
            LocalDateTime clockin = LocalDateTime.parse(entry.getString(0), BASE_FORMAT);
            LocalDateTime clockout = LocalDateTime.parse(entry.getString(1), BASE_FORMAT);
            String total = entry.getString(2);
            inDate = clockin.format(DAY_FORMAT);
            inTime = clockin.format(HOUR_FORMAT);
            outTime = clockout.format(HOUR_FORMAT);
            totalTime = total;
            name = entry.getString(3);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getInDate() {
        return inDate;
    }

    public String getInTime() {
        return inTime;
    }

    public String getOutTime() {
        return outTime;
    }

    public String getTotalTime() {
        return totalTime;
    }

    public String getName() {
        return name;
    }

}
