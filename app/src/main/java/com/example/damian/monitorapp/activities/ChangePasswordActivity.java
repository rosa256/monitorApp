package com.example.damian.monitorapp.activities;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import com.example.damian.monitorapp.R;
import com.example.damian.monitorapp.utils.AppHelper;
import com.example.damian.monitorapp.utils.BusyIndicator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText currentpasswordEditText;
    private EditText passwordNewEditText;
    private EditText passwordConfirmEditText;

    private boolean passwordCorrect = false;
    private boolean passwordConfirmCorrect = false;
    private boolean currentpasswordCorrect = false;

    BusyIndicator busyIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        currentpasswordEditText = (EditText) findViewById(R.id.change_password_current_EditText);
        passwordNewEditText = (EditText) findViewById(R.id.change_password_new_EditText);
        passwordConfirmEditText = (EditText) findViewById(R.id.change_password_confirm_EditText);

        currentpasswordEditText.addTextChangedListener(new MyTextWatcher(currentpasswordEditText));
        passwordNewEditText.addTextChangedListener(new MyTextWatcher(passwordNewEditText));
        passwordConfirmEditText.addTextChangedListener(new MyTextWatcher(passwordConfirmEditText));

        busyIndicator = new BusyIndicator(this);
        ButterKnife.bind(this);
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
                case R.id.change_password_new_EditText:
                    validatePassword(passwordNewEditText);
                    validatePasswordConfirmation(passwordConfirmEditText);
                    break;
                case R.id.change_password_confirm_EditText:
                    validatePasswordConfirmation(passwordConfirmEditText);
                    break;
                case R.id.change_password_current_EditText:
                    validateCurrPasswordConfirmation(currentpasswordEditText);
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

    @OnClick(R.id.changePasswordButton)
    public void changedPassword(){

        if(passwordCorrect && passwordConfirmCorrect && currentpasswordCorrect) {
            String newPassword = passwordNewEditText.getText().toString();
            String currentPassword = currentpasswordEditText.getText().toString();

            if(newPassword.equals(currentPassword)){
                new MaterialDialog.Builder(ChangePasswordActivity.this).title("Warning!")
                        .content("Current password cannot be the same as New Password!")
                        .theme(Theme.LIGHT)
                        .positiveColor(Color.GRAY)
                        .positiveText("ok")
                        .show();
                busyIndicator.unDimBackgorund();
                return;
            }

            busyIndicator.dimBackground();
            AppHelper.getPool().getUser(AppHelper.getCurrUser()).changePasswordInBackground(currentPassword, newPassword, callback);
        }
    }

    GenericHandler callback = new GenericHandler() {
        @Override
        public void onSuccess() {
            busyIndicator.unDimBackgorund();
            new MaterialDialog.Builder(ChangePasswordActivity.this).title("Success")
                    .content("Password successfully changed!")
                    .theme(Theme.LIGHT)
                    .positiveColor(Color.GRAY)
                    .positiveText("ok")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            finish();
                            onBackPressed();
                        }
                    })
                    .show();
            clearInput();

            return;
        }

        @Override
        public void onFailure(Exception exception) {
            busyIndicator.unDimBackgorund();
            new MaterialDialog.Builder(ChangePasswordActivity.this).title("Failed")
                    .content("Wrong credentials. Try again.")
                    .theme(Theme.LIGHT)
                    .positiveColor(Color.GRAY)
                    .positiveText("ok")
                    .show();
            return;
        }
    };

    private void clearInput() {
        currentpasswordEditText.setText("");
        currentpasswordEditText.setError(null);
        passwordNewEditText.setText("");
        passwordNewEditText.setError(null);
        passwordConfirmEditText.setText("");
        passwordConfirmEditText.setError(null);
    }

    private void validatePasswordConfirmation(EditText passwordConfirmationGiven) {
        String dataToValid = passwordConfirmationGiven.getText().toString();

        boolean isPasswordConfirmationValid = dataToValid.equals(passwordNewEditText.getText().toString());

        if(!isPasswordConfirmationValid) {
            passwordConfirmationGiven.setError("Does not match password!");
        }else{
            passwordConfirmationGiven.setError(null);
            passwordConfirmCorrect = true;
        }
    }

    private void validateCurrPasswordConfirmation(EditText passwordConfirmationGiven) {
        String dataToValid = passwordConfirmationGiven.getText().toString();

        boolean isPasswordConfirmationValid = dataToValid.equals(currentpasswordEditText.getText().toString());

        if(!isPasswordConfirmationValid) {
            passwordConfirmationGiven.setError("Does not match password!");
        }else{
            passwordConfirmationGiven.setError(null);
            currentpasswordCorrect = true;
        }
    }

}
