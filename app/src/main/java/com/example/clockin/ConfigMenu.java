package com.example.clockin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class ConfigMenu extends AppCompatActivity implements View.OnClickListener {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.configmenu);
        findViewById(R.id.Employees).setOnClickListener( this );
        findViewById(R.id.History).setOnClickListener( this );
        findViewById(R.id.UserProfile).setOnClickListener( this );
        findViewById(R.id.ScheduleGenerator).setOnClickListener( this );
        findViewById( R.id.configButton).setOnClickListener( this );
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){

            case R.id.Employees:
                // startActivity(new Intent(getApplicationContext(), Employees.class));
                break;

            case R.id.History:
                // stringRequestPostHttpExample();
                break;

            case R.id.UserProfile:
                startActivity(new Intent(getApplicationContext(), UserRegistrationWindow.class));
                break;

            case R.id.ScheduleGenerator:
                // jsonRequestPostHttpsExample ();
                break;

            case R.id.configButton:
                // jsonArrayRequestGetHttpsExample ();
                break;
        }
    }

}
