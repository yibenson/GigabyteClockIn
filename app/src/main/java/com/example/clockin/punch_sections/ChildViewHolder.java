package com.example.clockin.punch_sections;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.clockin.R;

import org.json.JSONException;

class ChildViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    // for individual records
    private final TextView inTime;
    private final TextView username;
    // private final TextView outDate;
    private final TextView outTime;
    private final TextView totalTime;
    ImageView photo;
    private final ImageButton editButton;


    private OnEditClickListener clickListener;

    public ChildViewHolder(View itemView, OnEditClickListener clickListener) {
        super(itemView);
        inTime = itemView.findViewById(R.id.start_time);
        outTime = itemView.findViewById(R.id.end_time);
        totalTime = itemView.findViewById(R.id.total_time);
        photo = itemView.findViewById(R.id.photo);
        editButton = itemView.findViewById(R.id.edit_button);
        username = itemView.findViewById(R.id.username);
        this.clickListener = clickListener;
        editButton.setOnClickListener(this);
    }

    public void fillValues(Child child, Context context) throws JSONException {
        inTime.setText(context.getString(R.string.in_string, child.getInTime()));
        outTime.setText(context.getString(R.string.out_string, child.getOutTime()));
        totalTime.setText(context.getString(R.string.hours, child.getTotalTime()));
        username.setText(child.getName());
    }

    @Override
    public void onClick(View view) {
        clickListener.onEditClick(getAbsoluteAdapterPosition());
    }


    public interface OnEditClickListener {
        void onEditClick(int position);
    }
}
