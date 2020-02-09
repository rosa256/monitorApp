package com.example.damian.monitorapp;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler;
import com.amazonaws.services.cognitoidentityprovider.model.SignUpResult;
import com.example.damian.monitorapp.Utils.AppHelper;
import com.example.damian.monitorapp.Utils.CognitoSettings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RegisterActivity extends AppCompatActivity {

    private CognitoSettings cognitoSettings;
    // Create awsconfiguration CognitoUserAttributes object and add user attributes
    private CognitoUserAttributes userAttributes;
    private EditText usernameGiven;
    private EditText passwordGiven;
    private EditText confirmPasswordGiven;
    private EditText emailGiven;
    @Bind(R.id.registerButton) Button buttonBtn;

    static final String TAG = "RegisterActivity";
    private boolean wasFocusedUsername = false;
    private boolean wasFocusedEmail = false;
    private boolean wasFocusedPassword = false;

    private boolean usernameCorrect = false;
    private boolean emailCorrect = false;
    private boolean passwordCorrect = false;
    private boolean passwordConfirmCorrect = false;
    private BusyIndicator busyIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);

        usernameGiven = findViewById(R.id.usernameEditText);
        passwordGiven = findViewById(R.id.passowrdTextView);
        confirmPasswordGiven= findViewById(R.id.passowrdConfirmTextView);
        emailGiven = findViewById(R.id.emailEditText);
        /* Creating awsconfiguration CognitoUserPool instance */
        cognitoSettings = CognitoSettings.getInstance();
        cognitoSettings.initContext(RegisterActivity.this);

        usernameGiven.addTextChangedListener(new MyTextWatcher(usernameGiven));
        usernameGiven.setOnFocusChangeListener(new MyFocusListener(usernameGiven));
        emailGiven.addTextChangedListener(new MyTextWatcher(emailGiven));
        emailGiven.setOnFocusChangeListener(new MyFocusListener(emailGiven));
        passwordGiven.addTextChangedListener(new MyTextWatcher(passwordGiven));
        passwordGiven.setOnFocusChangeListener(new MyFocusListener(passwordGiven));
        confirmPasswordGiven.addTextChangedListener(new MyTextWatcher(confirmPasswordGiven));

        busyIndicator = new BusyIndicator(this);
    }

    SignUpHandler signupCallback = new SignUpHandler() {
        @Override
        public void onSuccess(CognitoUser user, SignUpResult signUpResult) {
            Log.i(TAG, "Sing up success...is confirmed:" + signUpResult.getUserConfirmed());

            if(!signUpResult.getUserConfirmed()){
                Log.i(TAG, "Sing up success...not confirmed, verification code sent to:"
                        + signUpResult.getCodeDeliveryDetails().getDestination());

                Intent intent = new Intent(RegisterActivity.this, RegisterConfirmation.class);
                intent.putExtra("username", usernameGiven.getText().toString());
                intent.putExtra("destination", signUpResult.getCodeDeliveryDetails().getDestination());
                intent.putExtra("deliveryMed", signUpResult.getCodeDeliveryDetails().getDeliveryMedium());
                intent.putExtra("attribute", signUpResult.getCodeDeliveryDetails().getAttributeName());
                busyIndicator.unDimBackgorund();
                startActivity(intent);

                // This user must be confirmed and awsconfiguration confirmation code was sent to the user
                // cognitoUserCodeDeliveryDetails will indicate where the confirmation code was sent
                // Get the confirmation code from user
            }
            else {
                Log.i(TAG, "sing up success...confirmed:" + signUpResult.getUserConfirmed());
                Toast.makeText(RegisterActivity.this,"Użytkownik jest już potwierdzony.", Toast.LENGTH_SHORT).show();
                // The user has already been confirmed
            }
            busyIndicator.unDimBackgorund();
        }

        @Override
        public void onFailure(Exception exception) {
            Log.i(TAG, "Sing up failure:" + exception.getLocalizedMessage());
            new MaterialDialog.Builder(RegisterActivity.this).title("Register Problem")
                    .content("Cannot process registration.\n" +
                            "Please try again later.")
                    .theme(Theme.LIGHT)
                    .positiveColor(Color.GRAY)
                    .positiveText("ok")
                    .show();
            busyIndicator.unDimBackgorund();
        }
    };

    @OnClick(R.id.registerButton)
    public void OnRegisterClcik(){
        busyIndicator.dimBackground();

        if(usernameCorrect && emailCorrect && passwordCorrect && passwordConfirmCorrect) {
            userAttributes = new CognitoUserAttributes();
            userAttributes.addAttribute("email", emailGiven.getText().toString());


            AppHelper.getPool().signUpInBackground(
                    usernameGiven.getText().toString(),
                    passwordGiven.getText().toString(),
                    userAttributes,
                    null,
                    signupCallback
            );
        }else{
            validateUsername(usernameGiven);
            validateEmail(emailGiven);
            validatePassword(passwordGiven);
            validatePasswordConfirmation(confirmPasswordGiven);
            busyIndicator.unDimBackgorund();
        }
    }


    private class MyFocusListener implements View.OnFocusChangeListener{
        private View view;

        private MyFocusListener(View view) {
            this.view = view;
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            switch (view.getId()) {
                case R.id.usernameEditText:
                    if(!usernameGiven.getText().toString().isEmpty()) {
                        wasFocusedUsername = true;
                        validateUsername(usernameGiven);
                    }else{
                        usernameGiven.setError(null);
                        wasFocusedUsername = false;
                    }
                break;
                case R.id.emailEditText:
                    if(!emailGiven.getText().toString().isEmpty()) {
                        wasFocusedEmail = true;
                        validateEmail(emailGiven);
                    }else{
                        emailGiven.setError(null);
                        wasFocusedEmail = false;
                    }
                break;
                case R.id.passowrdTextView:
                    if(!passwordGiven.getText().toString().isEmpty()) {
                        wasFocusedPassword = true;
                        validatePassword(passwordGiven);
                    }else{
                        passwordGiven.setError(null);
                        wasFocusedPassword = false;
                    }
                break;
                case R.id.passowrdConfirmTextView:
                    if(!confirmPasswordGiven.getText().toString().isEmpty()) {
                        validatePasswordConfirmation(confirmPasswordGiven);
                    }
                    break;
            }
        }
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
                case R.id.usernameEditText:
                    if(wasFocusedUsername) {
                        validateUsername(usernameGiven);
                    }
                    break;
                case R.id.emailEditText:
                    if(wasFocusedEmail) {
                        validateEmail(emailGiven);
                    }
                    break;
                case R.id.passowrdTextView:
                    if(wasFocusedPassword) {
                        validatePassword(passwordGiven);
                        validatePasswordConfirmation(confirmPasswordGiven);
                    }
                    break;
                case R.id.passowrdConfirmTextView:
                    validatePasswordConfirmation(confirmPasswordGiven);
                    break;
            }
        }
    }

    private void validateUsername(EditText usernameGiven) {
        String dataToValid = usernameGiven.getText().toString();
        String patternUsername = "^(?=.{6,})(?=.*[a-z])(?=.*[A-Z]).*$";
        Pattern pattern = Pattern.compile(patternUsername);
        Matcher matcher = pattern.matcher(dataToValid);
        boolean isValidUsername = matcher.matches();

        if (!isValidUsername) {
            usernameGiven.setError("Incorrect username!\n*Min 6 long\n*Lower and Upper character");
        }else{
            usernameGiven.setError(null);
            usernameCorrect = true;
        }

/*          (/^
            (?=.{6,})               //should be 6 characters or more
            (?=.*[awsconfiguration-z])             //should contain at least one lower case
            (?=.*[A-Z])             //should contain at least one upper case
            (?=.*[@#$%^&+*!=])      //should contain at least 1 special characters
            .*$/)
*/
    }

    private void validateEmail(EditText emailGiven) {
        String dataToValid = emailGiven.getText().toString();
        boolean isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(dataToValid).matches();

        if(!isEmailValid) {
            emailGiven.setError("Incorrect email!");
        }else{
            emailGiven.setError(null);
            emailCorrect = true;
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

        boolean isPasswordConfirmationValid = dataToValid.equals(passwordGiven.getText().toString());

        if(!isPasswordConfirmationValid) {
            passwordConfirmationGiven.setError("Does not match password!");
        }else{
            passwordConfirmationGiven.setError(null);
            passwordConfirmCorrect = true;
        }
    }

    @Override
    public void onBackPressed() {
        Intent loginIntent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }
}


