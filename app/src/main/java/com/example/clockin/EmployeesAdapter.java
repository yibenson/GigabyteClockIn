package com.example.clockin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EmployeesAdapter extends RecyclerView.Adapter<EmployeesAdapter.ViewHolder> {
    private HashMap<String, JSONObject> mData;
    private ArrayList<String> users;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private int red;
    private int green;


    // data is passed into the constructor
    EmployeesAdapter(Context context, HashMap<String, JSONObject> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        red = context.getColor(R.color.red);
        green = context.getColor(R.color.green);
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.user_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        try {
            users = new ArrayList<>();
            users.addAll(mData.keySet());
            // retrieve from JSONObject
            JSONObject employee = mData.get(users.get(position));
            holder.username.setText(employee.getString("user"));
            holder.clockedIn = employee.getString("status").equals("ON");
            holder.color();
            String base64 = employee.getString("image");
            Log.v("Employees", base64);
            String base = base64.substring(0, base64.length() - 209);
            byte[] decodedString = Base64.decode(base, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            holder.photo.setImageBitmap(decodedByte);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView username;
        TextView status;
        ImageView photo;
        boolean clockedIn;
        private View itemView;

        ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            username = itemView.findViewById(R.id.userrow_username);
            status = itemView.findViewById(R.id.userrow_status);
            photo = itemView.findViewById(R.id.photo);
            itemView.setOnClickListener(this);
        }

        public void color() {
            if (clockedIn) {
                status.setText("STATUS: ON");
                itemView.setBackgroundColor(green);
            } else {
                status.setText("STATUS: OFF");
                itemView.setBackgroundColor(red);
            }
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

     // convenience method for getting data at click position
     String getItem(int id) {
        return users.get(id);
     }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}