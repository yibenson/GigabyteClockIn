package com.example.clockin;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clockin.databinding.PunchRowBinding;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PunchesAdapter extends RecyclerView.Adapter<PunchesAdapter.ViewHolder> {
    private JSONArray mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    PunchesAdapter(Context context, JSONArray data) {
        this.mInflater = LayoutInflater.from(context);
        if (data.length() == 0) {
            mData = new JSONArray();
        } else {
            this.mData = data;
        }
        Log.v("Punches", mData.toString());
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.punch_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        try {
            JSONArray punch = (JSONArray) mData.get(position);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            SimpleDateFormat date = new SimpleDateFormat("yyyy/MM/dd");
            SimpleDateFormat hour = new SimpleDateFormat("HH:mm");
            Date clockin = simpleDateFormat.parse(punch.getString(0));
            Date clockout = simpleDateFormat.parse(punch.getString(1));
            String total = punch.getString(2);

            holder.inDate.setText(date.format(clockin));
            holder.inTime.setText(hour.format(clockin));
            holder.outDate.setText(date.format(clockout));
            holder.outTime.setText(hour.format(clockout));
            holder.totalTime.setText(total);
        } catch (ParseException | JSONException e) {
            e.printStackTrace();
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.length();
    }

    public void updateData(JSONArray data) {
        this.mData = data;
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView inDate;
        public TextView inTime;
        public TextView outDate;
        public TextView outTime;
        public TextView totalTime;

        ViewHolder(View itemView) {
            super(itemView);
            inDate = itemView.findViewById(R.id.in_date);
            inTime = itemView.findViewById(R.id.in_time);
            outDate = itemView.findViewById(R.id.out_date);
            outTime = itemView.findViewById(R.id.out_time);
            totalTime = itemView.findViewById(R.id.total_time);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

/**
    // convenience method for getting data at click position
    String getItem(int id) {
        return mData.get(id);
    }*/

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}