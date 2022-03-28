package com.example.clockin;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.clockin.databinding.UserRegistrationWindowBinding;
import com.example.clockin.volley.VolleyDataRequester;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;

public class UserRegistrationWindow extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    String HOST = "https://52.139.218.209:443/user/user_reigster"; // typo courtesy of backend guy
    private UserRegistrationWindowBinding binding;

    private DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private String photo;
    private String landmarks;
    private int sex; // 0 = male, 1 = female

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = UserRegistrationWindowBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        if (getIntent().hasExtra("PHOTO")) {
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
        if (data.hasExtra("PHOTO")) {
            binding.username.getEditText().setText(data.getExtras().getString("USERNAME"));
            binding.phone.getEditText().setText(data.getExtras().getString("PHONE"));
            binding.email.getEditText().setText(data.getExtras().getString("MAIL"));
            binding.wage.getEditText().setText(data.getExtras().getString("WAGE"));
            binding.manager.setChecked((boolean) data.getExtras().get("MANAGER"));
            binding.birthday.getEditText().setText(data.getExtras().getString("BIRTHDAY"));

            // assign priv variables
            sex = data.getExtras().getInt("SEX");
            binding.sex.setSelection(sex);
            photo = data.getStringExtra("PHOTO");
            landmarks = data.getStringExtra("LANDMARK");
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
        body.put("account", getIntent().getStringExtra("ACCOUNT"));
        body.put("name", binding.username.getEditText().getText().toString());
        body.put("phone", binding.phone.getEditText().getText().toString());
        body.put("mail", binding.email.getEditText().getText().toString());
        body.put("wage", binding.wage.getEditText().getText().toString());
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
        intent.putExtra("PURPOSE", "IDENTIFY");
        intent.putExtra("ACCOUNT", getIntent().getStringExtra("ACCOUNT"));
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
        if (binding.birthday.getEditText().getText().toString().equals("")) {
            return true;
        }
        return false;
    }

    public void openFaceClockIn() {
        if (emptyForm()) { return; }
        Intent intent = new Intent(this, FaceClockIn.class);
        intent.putExtra("PURPOSE", "REGISTER");
        intent.putExtra("ACCOUNT", getIntent().getStringExtra("ACCOUNT"));
        intent.putExtra("USERNAME", binding.username.getEditText().getText().toString());
        intent.putExtra("PHONE", binding.phone.getEditText().getText().toString());
        intent.putExtra("MAIL", binding.email.getEditText().getText().toString());
        intent.putExtra("WAGE", binding.wage.getEditText().getText().toString());
        intent.putExtra("MANAGER", binding.manager.isChecked());
        intent.putExtra("BIRTHDAY", binding.birthday.getEditText().getText().toString());
        intent.putExtra("SEX", sex);
        startActivity(intent);
    }

    private void openDateDialog() {
        // date picker dialog
        DatePickerDialog picker = new DatePickerDialog(UserRegistrationWindow.this,
                (datePicker, i, i1, i2) -> {
                    LocalDate localDate = LocalDate.of(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                    binding.birthday.getEditText().setText(localDate.format(DAY_FORMAT));
                }, LocalDateTime.now().getYear(), LocalDateTime.now().getMonthValue(), LocalDateTime.now().getDayOfMonth());
        picker.getDatePicker().setSpinnersShown(true);
        picker.getDatePicker().setCalendarViewShown(false);
        picker.show();
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
