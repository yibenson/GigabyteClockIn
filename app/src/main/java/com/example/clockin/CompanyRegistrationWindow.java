package com.example.clockin;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.clockin.volley.VolleyDataRequester;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class CompanyRegistrationWindow extends AppCompatActivity {
    private String host = "https://52.139.218.209:443";
    private String account_register = "/account/company_register";

    EditText company_username;
    EditText password;
    EditText confirm_password;
    EditText company_name;
    EditText email;
    TextView banner;
    Button button;


    private View.OnClickListener RegisterClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            registerButtonClicked();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.company_registration_window);
        company_username = findViewById(R.id.company_username);
        password = findViewById(R.id.password);
        confirm_password = findViewById(R.id.confirm_password);
        company_name = findViewById(R.id.company_name);
        email = findViewById(R.id.email);
        button = findViewById(R.id.register_button);
        button.setOnClickListener(RegisterClickListener);

    }

    private boolean isEmpty(EditText editText) {
        CharSequence txt = editText.getText().toString();
        return TextUtils.isEmpty(txt);
    }

    private boolean equals(EditText editText1, EditText editText2) {
        CharSequence txt1 = editText1.getText().toString();
        CharSequence txt2 = editText2.getText().toString();
        return TextUtils.equals(txt1, txt2);
    }

    private void registerButtonClicked() {
        Log.v("Test", "Attempting button click");
        if (isEmpty(company_username)) {
            email.setError("Please enter company username");
        } else if (isEmpty(password)) {
            password.setError("Please enter your password");
        } else if (isEmpty(confirm_password)) {
            Log.v("Test", "Attempting button click");
            confirm_password.setError("Confirm password does not match");
        } else if (isEmpty(company_name)) {
            company_name.setError("Please enter company name");
        } else if (isEmpty(email)) {
            password.setError("Please enter your email");

        }
        Log.v("Test", "We are here");
        HashMap<String, String> body = new HashMap<>();
        body.put("account", company_username.getText().toString());
        body.put("password", password.getText().toString());
        body.put("mail", email.getText().toString());
        body.put("workspace", company_name.getText().toString());
        body.put("third_party", "");
        Log.v("Response", body.toString());
        VolleyDataRequester.withSelfCertifiedHttps(this)
                .setUrl(host + account_register)
                .setBody(body)
                .setMethod(VolleyDataRequester.Method.POST)
                .setJsonResponseListener(new VolleyDataRequester.JsonResponseListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.v("Response", response.toString());
                        try {
                            int error_code = response.getInt("error_msg");
                            switch (error_code) {
                                case 610:
                                    Toast.makeText(getApplicationContext(), "Account already registered", Toast.LENGTH_LONG).show();
                                    break;
                                case 612:
                                    Toast.makeText(getApplicationContext(), "Password length invalid", Toast.LENGTH_LONG).show();
                                    break;
                            }
                        } catch (JSONException e) {
                            // no error code
                            Toast.makeText(getApplicationContext(), "Account registration successful", Toast.LENGTH_LONG).show();
                        }
                    }
                }).requestJson();
    }



    private void runText(String msg) {
        final String str = msg;
        banner.setText(str);

    }
}