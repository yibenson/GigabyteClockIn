package com.example.clockin.punch_sections;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.clockin.R;
import com.intrusoft.sectionedrecyclerview.SectionRecyclerViewAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class AdapterSectionRecycler extends SectionRecyclerViewAdapter<SectionHeader, Child, SectionViewHolder, ChildViewHolder> {

    Context context;
    JSONObject faces;
    List<SectionHeader> sections;
    private ChildViewHolder.OnEditClickListener onEditClickListener;

    public AdapterSectionRecycler(Context context, List<SectionHeader> sectionHeaderItemList, JSONObject faces,
                                  ChildViewHolder.OnEditClickListener onEditClickListener) {
        super(context, sectionHeaderItemList);
        this.sections = sectionHeaderItemList;
        this.context = context;
        this.faces = faces;
        this.onEditClickListener = onEditClickListener;
    }

    @Override
    public SectionViewHolder onCreateSectionViewHolder(ViewGroup sectionViewGroup, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.section_header, sectionViewGroup, false);
        return new SectionViewHolder(view);
    }

    @Override
    public ChildViewHolder onCreateChildViewHolder(ViewGroup childViewGroup, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.punch_manage_row, childViewGroup, false);
        return new ChildViewHolder(view, onEditClickListener);
    }

    @Override
    public void onBindSectionViewHolder(SectionViewHolder sectionViewHolder, int sectionPosition, SectionHeader sectionHeader) {
        sectionViewHolder.setDate(sectionHeader.date); // sets section header date textview to correct date
    }

    @Override
    public void onBindChildViewHolder(ChildViewHolder childViewHolder, int sectionPosition, int childPosition, Child child) {
        try {
            childViewHolder.fillValues(child);
            byte[] decodedString = Base64.decode(faces.getString(child.getName()).replace("\\n", "\n"), Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            childViewHolder.photo.setImageBitmap(decodedByte);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
