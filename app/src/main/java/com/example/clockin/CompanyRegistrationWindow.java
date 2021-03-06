package com.example.clockin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.clockin.databinding.CompanyRegistrationWindowBinding;
import com.example.clockin.volley.VolleyDataRequester;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Objects;

public class CompanyRegistrationWindow extends AppCompatActivity {
    private String host = "https://52.139.218.209:443/account/company_register";
    private CompanyRegistrationWindowBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = CompanyRegistrationWindowBinding.inflate(getLayoutInflater());
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setContentView(binding.getRoot());
        binding.button.setOnClickListener(view -> registerButtonClicked());
    }

    private void registerButtonClicked() {
        if (isEmpty(binding.companyName.getEditText())) {
            binding.companyName.getEditText().setError(getString(R.string.company_name_empty));
        } else if (isEmpty(binding.companyUsername.getEditText())) {
            binding.companyUsername.getEditText().setError(getString(R.string.company_user_empty));
        } else if (isEmpty(binding.password.getEditText())) {
            binding.password.getEditText().setError(getString(R.string.password_empty));
        } else if (!equals(binding.password.getEditText(), binding.confirmPassword.getEditText())) {
            binding.confirmPassword.getEditText().setError(getString(R.string.password_no_match));
        } else if (isEmpty(binding.email.getEditText())) {
            binding.email.getEditText().setError(getString(R.string.company_email_empty));
        } else if (!binding.termsService.isChecked()) {
            binding.termsService.setError(getString(R.string.error_tos));
        }
        HashMap<String, String> body = new HashMap<>();
        body.put("account", binding.companyUsername.getEditText().getText().toString());
        body.put("password", binding.password.getEditText().getText().toString());
        body.put("mail", binding.email.getEditText().getText().toString());
        body.put("workspace", binding.companyName.getEditText().getText().toString());
        body.put("third_party", "");
        VolleyDataRequester.withSelfCertifiedHttps(this)
                .setUrl(host)
                .setBody(body)
                .setMethod(VolleyDataRequester.Method.POST)
                .setJsonResponseListener(response -> {
                    Log.v("Response", response.toString());
                    try {
                        switch (response.getString("error_msg")) {
                            case "ACCOUNT IS REGISTER":
                                binding.companyUsername.setError(getString(R.string.duplicate_account));
                                break;
                            case "ACCOUNT IS INVALID":
                                binding.companyUsername.setError(getString(R.string.invalid_account));
                                break;
                            case "PASSWORD LENGTH IS INVALID":
                                binding.password.setError(getString(R.string.password_empty));
                                break;
                            default:
                                // registration success notification
                                showAlertDialog();
                        }
                    } catch (JSONException e) {
                        // Error occurred
                    }
                }).requestJson();
    }

    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CompanyRegistrationWindow.this);
        builder.setMessage(getString(R.string.registration_success))
                .setPositiveButton("Ok", (dialog, which) -> {finish();});
        builder.create().show();
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

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


}