package com.example.damian.monitorapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler;
import com.amazonaws.services.cognitoidentityprovider.model.SignUpResult;
import com.example.damian.monitorapp.Utils.CognitoSettings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RegisterActivity extends AppCompatActivity {

    CognitoSettings cognitoSettings;
    // Create a CognitoUserAttributes object and add user attributes
    CognitoUserAttributes userAttributes;
    EditText usernameGiven;
    EditText passwordGiven;
    EditText confirmPasswordGiven;
    EditText emailGiven;
    @Bind(R.id.registerButton) Button buttonBtn;

    static final String TAG = "RegisterActivity";
    private boolean wasFocusedUsername = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);

        usernameGiven = findViewById(R.id.usernameEditText);
        //usernameGiven.setText("maniek256");
        passwordGiven = findViewById(R.id.passowrdTextView);
        passwordGiven.setText("ABCabc!@#");
        confirmPasswordGiven= findViewById(R.id.passowrdConfirmTextView);
        confirmPasswordGiven.setText("ABCabc!@#");
        emailGiven = findViewById(R.id.emailEditText);
        emailGiven.setText("d.rosinski256@gmail.com");
        /* Create a CognitoUserPool instance */
        cognitoSettings = CognitoSettings.getInstance();
        cognitoSettings.initContext(RegisterActivity.this);

        usernameGiven.addTextChangedListener(new MyTextWatcher(usernameGiven));
        usernameGiven.setOnFocusChangeListener(new MyFocusListener(usernameGiven));
    }

    SignUpHandler signupCallback = new SignUpHandler() {
        @Override
        public void onSuccess(CognitoUser user, SignUpResult signUpResult) {
            Toast.makeText(RegisterActivity.this,"Pomyślna Rejestracja", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "sing up success...is confirmed:" + signUpResult.getUserConfirmed());

            // Check if this user (cognitoUser) needs to be confirmed
            if(!signUpResult.getUserConfirmed()){
                Log.i(TAG, "sing up success...not confirmed, verification code sent to:"
                        + signUpResult.getCodeDeliveryDetails().getDestination());

                Intent intent = new Intent(RegisterActivity.this, RegisterConfirmation.class);
                intent.putExtra("username", usernameGiven.getText().toString());
                startActivity(intent);

                // This user must be confirmed and a confirmation code was sent to the user
                // cognitoUserCodeDeliveryDetails will indicate where the confirmation code was sent
                // Get the confirmation code from user
            }
            else {
                Log.i(TAG, "sing up success...confirmed:" + signUpResult.getUserConfirmed());
                Toast.makeText(RegisterActivity.this,"Nie pomyślna rejestracja.", Toast.LENGTH_SHORT).show();
                // The user has already been confirmed
            }
        }
        @Override
        public void onFailure(Exception exception) {
            Log.i(TAG, "sing up failure:" + exception.getLocalizedMessage());
            // Sign-up failed, check exception for the cause
        }
    };

    @OnClick(R.id.registerButton)
    public void OnRegisterClcik(){
        Toast.makeText(RegisterActivity.this,"Registration invoke",Toast.LENGTH_SHORT).show();

        userAttributes = new CognitoUserAttributes();
        userAttributes.addAttribute("email",emailGiven.getText().toString());

        cognitoSettings.getUserPool().signUpInBackground(
                usernameGiven.getText().toString(),
                passwordGiven.getText().toString(),
                userAttributes,
                null,
                signupCallback
        );
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

                    break;
                case R.id.passowrdTextView:

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
                    validateEmail(emailGiven);
                    break;
                case R.id.passowrdTextView:
                    validatePassword(passwordGiven);
                    break;
            }
        }


    }

    private void validateUsername(EditText usernameGiven) {
        String dataToValid = usernameGiven.getText().toString();
        Pattern patternUsername = Pattern.compile("^(?=.{8,})(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+*!=]).*$");
        Matcher matcher = patternUsername.matcher(dataToValid);
        Boolean isValidUsername = matcher.matches();

        if (!isValidUsername)
            usernameGiven.setError("Wrong username!");
        else
            usernameGiven.setError(null);
/*
            (/^
            (?=.{6,})                //should be 6 characters or more
            (?=.*[a-z])             //should contain at least one lower case
            (?=.*[A-Z])             //should contain at least one upper case
            (?=.*[@#$%^&+*!=])      //should contain at least 1 special characters
            .*$/)
*/

    }

    private void validatePassword(EditText passwordGiven) {
        String dataToValid = usernameGiven.getText().toString();

    }

    private void validateEmail(EditText emailGiven) {
        String dataToValid = usernameGiven.getText().toString();

    }
}


