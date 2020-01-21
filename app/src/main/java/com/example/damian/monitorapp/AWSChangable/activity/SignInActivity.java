package com.example.damian.monitorapp.AWSChangable.activity;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.amazonaws.mobile.auth.core.IdentityProvider;
import com.amazonaws.mobile.auth.core.SignInResultHandler;
import com.amazonaws.mobile.auth.core.signin.SignInManager;
import com.amazonaws.mobile.auth.core.signin.SignInProviderResultHandler;
import com.example.damian.monitorapp.AWSChangable.utils.AuthUIConfiguration;
import com.example.damian.monitorapp.MainActivity;
import com.example.damian.monitorapp.R;
import com.example.damian.monitorapp.SplashActivity;

import java.util.HashMap;
import java.util.UUID;

public class SignInActivity extends AppCompatActivity {
    private static final String LOG_TAG = SignInActivity.class.getSimpleName();
    private SignInManager signInManager;
    public static HashMap<String, AuthUIConfiguration> configurationStore = new HashMap();

    public SignInActivity() {
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.signInManager = SignInManager.getInstance();
        if(this.signInManager == null) {
            Log.e(LOG_TAG, "Invoke SignInActivity.startSignInActivity() method to create the SignInManager.");
        } else {
            this.signInManager.setProviderResultsHandler(this, new SignInActivity.SignInProviderResultHandlerImpl());
            this.setContentView(R.layout.activity_sign_in);
        }
    }

    protected void onResume() {
        super.onResume();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        this.signInManager.handleRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.signInManager.handleActivityResult(requestCode, resultCode, data);
    }

    public void onBackPressed() {
        if(this.signInManager.getResultHandler().onCancel(this)) {
            super.onBackPressed();
            SignInManager.dispose();
        }

    }

    public static void startSignInActivity(Context context, AuthUIConfiguration config) {
        try {
            String exception = UUID.randomUUID().toString();
            HashMap intent = configurationStore;
            synchronized(configurationStore) {
                configurationStore.put(exception, config);
            }

            Intent intent1 = new Intent(context, SignInActivity.class);
            intent1.putExtra("com.amazonaws.mobile.auth.ui.configurationkey", exception);
            intent1.putExtra("signInBackgroundColor", config.getSignInBackgroundColor(-12303292));

            context.startActivity(intent1);
        } catch (Exception var6) {
            Log.e(LOG_TAG, "Cannot start the SignInActivity. Check the context and the configuration object passed in.", var6);
        }

    }

    public static void startSignInActivity(Context context) {
        try {
            Intent exception = new Intent(context, SignInActivity.class);
            context.startActivity(exception);
        } catch (Exception var2) {
            Log.e(LOG_TAG, "Cannot start the SignInActivity. Check the context and the configuration object passed in.", var2);
        }

    }

    private class SignInProviderResultHandlerImpl implements SignInProviderResultHandler {
        private SignInProviderResultHandlerImpl() {

        }

        public void onSuccess(IdentityProvider provider) {
            Log.i(LOG_TAG, String.format(SignInActivity.this.getString(R.string.sign_in_succeeded_message_format), new Object[]{provider.getDisplayName()}));
            SignInManager.dispose();
            SignInResultHandler signInResultsHandler = SignInActivity.this.signInManager.getResultHandler();
            signInResultsHandler.onSuccess(SignInActivity.this, provider);
            startActivity(new Intent(SignInActivity.this, MainActivity.class));
            SignInActivity.this.finish();

        }

        public void onCancel(IdentityProvider provider) {
            Log.i(LOG_TAG, String.format(SignInActivity.this.getString(R.string.sign_in_canceled_message_format), new Object[]{provider.getDisplayName()}));
            SignInActivity.this.signInManager.getResultHandler().onIntermediateProviderCancel(SignInActivity.this, provider);
        }

        public void onError(IdentityProvider provider, Exception ex) {
            Log.e(LOG_TAG, String.format("Sign-in with %s caused an error.", new Object[]{provider.getDisplayName()}), ex);
            SignInActivity.this.signInManager.getResultHandler().onIntermediateProviderError(SignInActivity.this, provider, ex);
        }
    }
}

