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

import com.example.clockin.volley.VolleyDataRequester;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class UserRegistrationWindow extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    String HOST = "https://52.139.218.209:443/user/user_reigster"; // typo courtesy of backend guy
    Button add_photo_button;
    Button register_button;

    private EditText username;
    private EditText phone;
    private EditText mail;
    private EditText wage;
    private TextView birthday;
    private DatePickerDialog picker;
    private CheckBox manager;
    private ArrayList<EditText> texts;
    private String photo;
    private String landmarks;
    private String company_number;
    private Spinner sexSpinner;
    private Integer sex;
    private String date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_registration_window);
        ConstraintLayout rootLayout = findViewById(R.id.user_register_layout);
        username = findViewById(R.id.user_username);
        phone = findViewById(R.id.user_phone);
        mail = findViewById(R.id.user_mail);
        wage = findViewById(R.id.user_wage);
        manager = findViewById(R.id.user_is_manager);
        birthday = findViewById(R.id.user_birthday);
        add_photo_button = findViewById(R.id.user_add_photo_button);
        if (getIntent().hasExtra("photo")) {
            add_photo_button.setText("Photo added!");
        }
        register_button = findViewById(R.id.user_register_button);

        // add EditTexts to list
        texts = new ArrayList<>();
        for(int i = 0; i < rootLayout.getChildCount(); i++) {
            if(rootLayout.getChildAt(i) instanceof EditText) {
                texts.add( (EditText) rootLayout.getChildAt(i));
            }
        }

        // set spinner
        sexSpinner = findViewById(R.id.sex);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.性別, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        sexSpinner.setAdapter(adapter);
        sexSpinner.setOnItemSelectedListener(this);

        // set onclick listeners
        birthday.setOnClickListener(view -> openDateDialog());
        add_photo_button.setOnClickListener(view -> openFaceClockIn());
        register_button.setOnClickListener(view -> sendInfo());

        // fill data if returning to window
        Intent data = getIntent();
        company_number = data.getExtras().get("company_number").toString();
        if (data.hasExtra("photo")) {
            username.setText(data.getExtras().get("username").toString());
            phone.setText(data.getExtras().get("phone").toString());
            mail.setText(data.getExtras().get("mail").toString());
            wage.setText(data.getExtras().get("wage").toString());
            manager.setChecked((boolean) data.getExtras().get("manager"));
            photo = data.getExtras().get("photo").toString();
            landmarks = (String) data.getExtras().get("landmark");
            date = data.getExtras().get("birthday").toString();
            birthday.setText(date);
            sexSpinner.setSelection((Integer) data.getExtras().get("sex"));
        }
    }

    private void sendInfo() {
        if (emptyForm()) {
            return;
        }
        if ((photo == null) || landmarks == null) {
            register_button.setError("Please add a photo");
            return;
        }
        HashMap<String, String> body = new HashMap<>();
        body.put("account", company_number);
        body.put("name", username.getText().toString());
        body.put("phone", phone.getText().toString());
        body.put("mail", mail.getText().toString());
        body.put("wage", wage.getText().toString());
        body.put("manager", Boolean.toString(manager.isChecked()));
        body.put("landmark", landmarks);
        body.put("face", photo);
        if (sex == 0) {
            body.put("sex", "male");
        } else {
            body.put("sex", "female");
        }
        body.put("birthday", date);

        VolleyDataRequester.withSelfCertifiedHttps(getApplicationContext())
                .setUrl(HOST)
                .setBody(body)
                .setMethod( VolleyDataRequester.Method.POST )
                .setJsonResponseListener(response -> {
                    Log.v("Response", response.toString());
                    Toast.makeText(getApplicationContext(), response.toString(), Toast.LENGTH_LONG).show();
                })
                .requestJson();
        Intent intent = new Intent(this, FaceClockIn.class);
        intent.putExtra("Purpose", "Identify");
        startActivity(intent);
    }

    private boolean emptyForm() {
        for (EditText e : texts) {
            String text = e.getText().toString();
            if (TextUtils.isEmpty(text)) {
                e.setError("Item cannot be empty");
                return true;
            }
        }
        if ((sex==null) || birthday.getText().toString().equals("")) {
            return true;
        }
        return false;
    }

    public void openFaceClockIn() {
        if (emptyForm()) {
            return;
        }
        Intent intent = new Intent(this, FaceClockIn.class);
        intent.putExtra("Purpose", "Register");
        intent.putExtra("company_number", company_number);
        intent.putExtra("username", username.getText().toString());
        intent.putExtra("phone", phone.getText().toString());
        intent.putExtra("mail", mail.getText().toString());
        intent.putExtra("wage", wage.getText().toString());
        intent.putExtra("manager", manager.isChecked());
        intent.putExtra("birthday", date);
        Log.v("Response", date);
        intent.putExtra("sex", sex);
        startActivity(intent);
    }

    private void openDateDialog() {
        final Calendar cldr = Calendar.getInstance();
        int day = cldr.get(Calendar.DAY_OF_MONTH);
        int month = cldr.get(Calendar.MONTH);
        int year = cldr.get(Calendar.YEAR);
        // date picker dialog
        picker = new DatePickerDialog(UserRegistrationWindow.this,
                (datePicker, i, i1, i2) -> {
                    date = i + "." + i1 + "." + i2;
                    birthday.setText(date);
                }, year, month, day);
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
