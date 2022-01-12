package com.example.clockin;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clockin.databinding.InfoEditBinding;

public class EditPage extends AppCompatActivity {
    private int NAME = 0;
    private int PHONE = 1;
    private int EMAIL = 2;
    private int WAGE = 3;
    private InfoEditBinding binding;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = InfoEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.textView.setText(getIntent().getStringExtra("data"));
        binding.button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (getIntent().getIntExtra("Purpose", 0)) {
                    case 0:
                }
            }
        });
    }

}
