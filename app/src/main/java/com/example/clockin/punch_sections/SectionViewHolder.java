package com.example.clockin.punch_sections;


import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clockin.R;

public class SectionViewHolder extends RecyclerView.ViewHolder {

    TextView date;

    public SectionViewHolder(View itemView) {
        super(itemView);
        date = (TextView) itemView.findViewById(R.id.date);
    }

    public void setDate(String date) {
        this.date.setText(date);
    }
}
