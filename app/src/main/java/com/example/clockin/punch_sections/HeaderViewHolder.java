package com.example.clockin.punch_sections;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clockin.R;

public class HeaderViewHolder extends RecyclerView.ViewHolder{
    final TextView date;

    HeaderViewHolder(@NonNull View view) {
        super(view);

        date = view.findViewById(R.id.date);
    }
}
