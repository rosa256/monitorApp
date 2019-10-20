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

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RegisterActivity extends AppCompatActivity {

    CognitoSettings cognitoSettings;
    // Create a CognitoUserAttributes object and add user attributes
    CognitoUserAttributes userAttributes;
    EditText usernameGiven;
    EditText passwordGiven;
    EditText emailGiven;
    @Bind(R.id.registerButton) Button buttonBtn;

    static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);

        usernameGiven = findViewById(R.id.usernameEditText);
        usernameGiven.setText("maniek256");
        passwordGiven = findViewById(R.id.passowrdTextView);
        passwordGiven.setText("ABCabc!@#");
        emailGiven = findViewById(R.id.emailEditText);
        emailGiven.setText("d.rosinski256@gmail.com");
        /* Create a CognitoUserPool instance */
        cognitoSettings = CognitoSettings.getInstance();
        cognitoSettings.initContext(RegisterActivity.this);
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

    private class MyTextWatcher implements TextWatcher {

        private View view;

        private MyTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
        @Override
        public void afterTextChanged(Editable s) { }
    }

}

/*        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.input_name:
                    validateName();
                    break;
                case R.id.input_email:
                    validateEmail();
                    break;
                case R.id.input_password:
                    validatePassword();
                    break;
            }
        }*/

