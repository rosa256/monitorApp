package com.example.damian.monitorapp.activities;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.example.damian.monitorapp.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class CustomForgotPasswordActivity extends AppCompatActivity {
    private EditText passwordEditText;
    private EditText passwordConfirmEditText;
    private EditText codeEditText;
    private TextView message;

    private boolean passwordCorrect = false;
    private boolean passwordConfirmCorrect = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_forgot_password);

        passwordEditText = (EditText) findViewById(R.id.forgot_password_EditText);
        passwordConfirmEditText = (EditText) findViewById(R.id.forgot_password_confirmEditText);
        codeEditText = (EditText) findViewById(R.id.code_EditText);
        message = (TextView) findViewById(R.id.forgot_password_informationTextView);
        passwordEditText.addTextChangedListener(new MyTextWatcher(passwordEditText));
        passwordConfirmEditText.addTextChangedListener(new MyTextWatcher(passwordConfirmEditText));

        Bundle extras = getIntent().getExtras();
        if (extras !=null) {
            if (extras.containsKey("destination")) {
                String dest = extras.getString("destination");
                String delMed = extras.getString("deliveryMedium");
                String textToDisplay = "Code to set a new password was sent to " + dest + " via "+delMed;
                message.setText(textToDisplay);
            }
        }
        ButterKnife.bind(this);
    }

    @OnClick(R.id.resetPasswordButton)
    public void resetPassword(){
        String newPassword = passwordEditText.getText().toString();

        if(passwordCorrect && passwordConfirmCorrect) {
            String verCode = codeEditText.getText().toString();

            if (verCode.length() == 6) {
                exit(newPassword, verCode);
            }else{
                codeEditText.setError("Incorrect code!\n*Must be 6 long\n*Must be number");
            }
        }
    }

    private void exit(String newPass, String code) {
        Intent intent = new Intent();
        if(newPass == null || code == null) {
            newPass = "";
            code = "";
        }
        intent.putExtra("password", newPass);
        intent.putExtra("verification_code", code);
        setResult(RESULT_OK, intent);
        finish();
    }

    private class MyTextWatcher implements TextWatcher {
        private View view;
        private MyTextWatcher(View view) {
            this.view = view;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }
        @Override
        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.forgot_password_EditText:
                    validatePassword(passwordEditText);
                    validatePasswordConfirmation(passwordConfirmEditText);
                    break;
                case R.id.forgot_password_confirmEditText:
                    validatePasswordConfirmation(passwordConfirmEditText);
                    break;
            }
        }
    }
    private void validatePassword(EditText passwordGiven) {
        String dataToValid = passwordGiven.getText().toString();
        String patternPassword = "^(?=.{6,})(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+*!=]).*$";
        Pattern pattern = Pattern.compile(patternPassword);
        Matcher matcher = pattern.matcher(dataToValid);
        boolean isPasswordValid = matcher.matches();

        if(!isPasswordValid) {
            passwordGiven.setError(" Incorrect password!\n*Min. 6 long\n*Lower and Upper character\n*Special character");
        }else{
            passwordGiven.setError(null);
            passwordCorrect = true;
        }

    }

    private void validatePasswordConfirmation(EditText passwordConfirmationGiven) {
        String dataToValid = passwordConfirmationGiven.getText().toString();

        boolean isPasswordConfirmationValid = dataToValid.equals(passwordEditText.getText().toString());

        if(!isPasswordConfirmationValid) {
            passwordConfirmationGiven.setError("Does not match password!");
        }else{
            passwordConfirmationGiven.setError(null);
            passwordConfirmCorrect = true;
        }
    }


}
