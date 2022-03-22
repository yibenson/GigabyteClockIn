package com.example.clockin.punch_sections;

import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.clockin.R;

import org.json.JSONException;

class ChildViewHolder extends RecyclerView.ViewHolder {
    // for individual records

    private final TextView inTime;
    private final TextView username;
    // private final TextView outDate;
    private final TextView outTime;
    private final TextView totalTime;
    ImageView photo;
    private final ImageButton editButton;


    public ChildViewHolder(View itemView) {
        super(itemView);
        inTime = itemView.findViewById(R.id.start_time);
        outTime = itemView.findViewById(R.id.end_time);
        totalTime = itemView.findViewById(R.id.total_time);
        photo = itemView.findViewById(R.id.photo);
        editButton = itemView.findViewById(R.id.edit_button);
        username = itemView.findViewById(R.id.username);
    }

    public void fillValues(Child child) throws JSONException {
        inTime.setText(child.getInTime());
        outTime.setText(child.getOutTime());
        totalTime.setText(child.getTotalTime());
        username.setText(child.getName());
    }



}
