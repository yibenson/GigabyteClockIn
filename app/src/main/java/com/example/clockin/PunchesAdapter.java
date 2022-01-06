package com.example.clockin;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class PunchesAdapter extends RecyclerView.Adapter<PunchesAdapter.ViewHolder> {

    private JSONArray mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    PunchesAdapter(Context context, JSONArray data) {
        this.mInflater = LayoutInflater.from(context);
        if (data.length() == 0) {
            JSONArray nullArray = new JSONArray();
            try {
                nullArray.put(0, "No data detected");
                nullArray.put(1, "No data detected");
                nullArray.put(2, "No data detected");
                data.put(0, nullArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        this.mData = data;
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
        JSONArray punch = null;
        try {
            punch = (JSONArray) mData.get(position);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        StringBuilder stringBuilder = new StringBuilder();
        try {
            stringBuilder.append("上班： ").append(punch.get(0)).append(" || ")
                    .append("下班： ").append(punch.get(1)).append(" || ")
                    .append("Hours worked: ").append(punch.get(2));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        holder.myTextView.setText(stringBuilder);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.length();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView myTextView;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.tvAnimalName);
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