package com.example.clockin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.clockin.databinding.ManagementMenuBinding;
import com.example.clockin.punch_sections.ManagementPunches;

public class ManagementMenu extends AppCompatActivity implements View.OnClickListener{
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ManagementMenuBinding binding = ManagementMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.action_bar_buttonless, null);
        actionBar.setCustomView(v);
        binding.menubutton1.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), ManagementPunches.class);
            intent.putExtra("ACCOUNT", getIntent().getStringExtra("ACCOUNT"));
            startActivity(intent);
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case (R.id.menubutton1):
                Intent intent = new Intent(getApplicationContext(), ManagementPunches.class);
                intent.putExtra("ACCOUNT", getIntent().getStringExtra("ACCOUNT"));
                Log.v("Punches", "Starting management punches");
                // startActivity(intent);
                // todo: view records for all employees, organized by date and with edit function on side
                break;
            case (R.id.menubutton2):
                // todo: add user
                break;
            case (R.id.menubutton3):
                //  todo: user management (list of all users + ability to edit details)
                break;
            case (R.id.menubutton4):
                // todo: correct user records
            case (R.id.menubutton5):
                // todo: config
            case (R.id.menubutton6):
                // todo: logout
                break;
        }
    }
}
