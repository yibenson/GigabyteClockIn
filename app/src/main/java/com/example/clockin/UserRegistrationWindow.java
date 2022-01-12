package com.example.clockin;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.clockin.databinding.UserRegistrationWindowBinding;
import com.example.clockin.volley.VolleyDataRequester;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class UserRegistrationWindow extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    String HOST = "https://52.139.218.209:443/user/user_reigster"; // typo courtesy of backend guy
    private UserRegistrationWindowBinding binding;

    private String photo;
    private String landmarks;
    private int sex; // 0 = male, 1 = female

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = UserRegistrationWindowBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getIntent().hasExtra("photo")) {
            binding.addPhoto.setText("Photo added!");
        }

        // Creating sex spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.性別, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.sex.setAdapter(adapter);
        binding.sex.setOnItemSelectedListener(this);

        // set onclick listeners
        binding.birthday.setOnClickListener(view -> openDateDialog());
        binding.addPhoto.setOnClickListener(view -> openFaceClockIn());
        binding.register.setOnClickListener(view -> sendInfo());

        // fill data if returning to window
        Intent data = getIntent();
        if (data.hasExtra("photo")) {
            binding.username.setText(data.getExtras().getString("username"));
            binding.phone.setText(data.getExtras().getString("phone"));
            binding.email.setText(data.getExtras().getString("mail"));
            binding.wage.setText(data.getExtras().getString("wage"));
            binding.manager.setChecked((boolean) data.getExtras().get("manager"));
            binding.birthday.setText(data.getExtras().getString("birthday"));

            // assign priv variables
            sex = data.getExtras().getInt("sex");
            binding.sex.setSelection(sex);
            photo = data.getStringExtra("photo");
            landmarks = data.getStringExtra("landmark");
        }
    }

    private void sendInfo() {
        if (emptyForm()) {
            return;
        }
        if ((photo == null) || landmarks == null) {
            binding.addPhoto.setError("Please add a photo");
            return;
        }
        HashMap<String, String> body = new HashMap<>();
        body.put("account", getIntent().getStringExtra("company_number"));
        body.put("name", binding.username.getText().toString());
        body.put("phone", binding.phone.getText().toString());
        body.put("mail", binding.email.getText().toString());
        body.put("wage", binding.wage.getText().toString());
        body.put("manager", Boolean.toString(binding.manager.isChecked()));
        body.put("landmark", landmarks);
        body.put("face", photo);
        if (sex == 0) {
            body.put("sex", "male");
        } else {
            body.put("sex", "female");
        }
        body.put("birthday", binding.birthday.toString());

        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(HOST)
                .setBody(body)
                .setMethod( VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    Toast.makeText(getApplicationContext(), response.toString(), Toast.LENGTH_LONG).show();
                })
                .requestJson();
        // Success --> return to idle clock in
        Intent intent = new Intent(this, FaceClockIn.class);
        intent.putExtra("Purpose", "Identify");
        intent.putExtra("company_number", getIntent().getStringExtra("company_number"));
        startActivity(intent);
        finish();
    }

    private boolean emptyForm() {
        for (int i = 0; i < binding.getRoot().getChildCount(); i++) {
            View v = binding.getRoot().getChildAt(i);
            if (v instanceof EditText) {
                String text = ((EditText) v).getText().toString();
                if (TextUtils.isEmpty(text)) {
                    ((EditText) v).setError("Item cannot be empty");
                    return true;
                }
            }
        }
        if (binding.birthday.getText().toString().equals("")) {
            return true;
        }
        return false;
    }

    public void openFaceClockIn() {
        if (emptyForm()) { return; }
        Intent intent = new Intent(this, FaceClockIn.class);
        intent.putExtra("Purpose", "Register");
        intent.putExtra("company_number", getIntent().getStringExtra("company_number"));
        intent.putExtra("username", binding.username.getText().toString());
        intent.putExtra("phone", binding.phone.getText().toString());
        intent.putExtra("mail", binding.email.getText().toString());
        intent.putExtra("wage", binding.wage.getText().toString());
        intent.putExtra("manager", binding.manager.isChecked());
        intent.putExtra("birthday", binding.birthday.getText().toString());
        intent.putExtra("sex", sex);
        startActivity(intent);
    }

    private void openDateDialog() {
        final Calendar cldr = Calendar.getInstance();
        int day = cldr.get(Calendar.DAY_OF_MONTH);
        int month = cldr.get(Calendar.MONTH);
        int year = cldr.get(Calendar.YEAR);
        // date picker dialog
        DatePickerDialog picker = new DatePickerDialog(UserRegistrationWindow.this,
                (datePicker, i, i1, i2) -> {
                    String date = i + "." + i1 + "." + i2;
                    binding.birthday.setText(date);
                }, year, month, day);
        picker.getDatePicker().setSpinnersShown(true);
        picker.getDatePicker().setCalendarViewShown(false);
        picker.show();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        sex = i;
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        // do nothing?
    }
}
