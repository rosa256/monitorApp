package com.example.damian.monitorapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.mobile.auth.core.DefaultSignInResultHandler;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.IdentityProvider;
import com.amazonaws.mobile.auth.core.StartupAuthErrorDetails;
import com.amazonaws.mobile.auth.core.StartupAuthResult;
import com.amazonaws.mobile.auth.core.StartupAuthResultHandler;
import com.amazonaws.mobile.auth.core.signin.AuthException;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.example.damian.monitorapp.AWSChangable.UILApplication;
import com.example.damian.monitorapp.AWSChangable.activity.SignInActivity;
import com.example.damian.monitorapp.AWSChangable.utils.AppHelper;
import com.example.damian.monitorapp.Utils.CustomPrivileges;

import java.lang.ref.WeakReference;

public class SplashActivity extends Activity implements StartupAuthResultHandler {

    private static final String TAG = "SplashActivity";
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        final IdentityManager identityManager = IdentityManager.getDefaultIdentityManager();
        identityManager.resumeSession(this,this);
    }

    @Override
    protected void onPause() {
        finish();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onComplete(StartupAuthResult authResults) {
        final IdentityManager identityManager = authResults.getIdentityManager();

        if(authResults.isUserSignedIn()){
            Log.i(TAG, "onComplete(): User is Signed In.");
            final IdentityProvider identityProvider = identityManager.getCurrentIdentityProvider();
            IdentityManager.setDefaultIdentityManager(authResults.getIdentityManager());
            getDetails();
        }else{
            Log.i(TAG, "onComplete(): User is not Signed In.");

            final StartupAuthErrorDetails errors = authResults.getErrorDetails();

            if (errors.didErrorOccurRefreshingProvider()) {
                final AuthException providerAuthException = errors.getProviderRefreshException();
                Log.w(TAG, String.format(
                        "Credentials for Previously signed-in provider %s could not be refreshed.",
                        providerAuthException.getProvider().getDisplayName()), providerAuthException);
            }

            doSignIn(identityManager);
            return;
        }
    }

    private void doSignIn(IdentityManager identityManager) {
        final WeakReference<SplashActivity> self = new WeakReference<SplashActivity>(this);

        identityManager.login(this, new DefaultSignInResultHandler() {

            @Override
            public void onSuccess(Activity activity, IdentityProvider identityProvider) {
                // User has signed in
                Log.e("NotError", "User signed in");
                Activity callingActivity = self.get();

            }

            @Override
            public boolean onCancel(Activity activity) {
                // This
                return false;
            }
        });

        SignInActivity.startSignInActivity(this, UILApplication.sAuthUIConfiguration);

        CustomPrivileges.setUpPrivileges(this);


    }

    GetDetailsHandler getDetailsHandler = new GetDetailsHandler() {
        @Override
        public void onSuccess(CognitoUserDetails cognitoUserDetails) {
            // The user detail are in cognitoUserDetails
            AppHelper.setUserDetails(cognitoUserDetails);
            String displayName="";
            if(AppHelper.getItemCount()>0) {
                displayName = String.format("%s",AppHelper.getItemForDisplay(0).getDataText());

            }else{
                displayName=username;
            }
           // Toast.makeText(SplashActivity.this, String.format(getString(R.string.sign_in_message),
            //        displayName), Toast.LENGTH_LONG).show();
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        }
        @Override
        public void onFailure(Exception exception) {
            //closeWaitDialog();
            // Fetch user details failed, check exception for the cause
            Log.e(TAG,"Failed to fetch user details "+ exception.getMessage());
        }
    };

    private void getDetails() {
        //showWaitDialog("Signing in...");
        username = AppHelper.getPool().getCurrentUser().getUserId();
        if(username!=null) {
            AppHelper.getPool().getUser(username).getDetailsInBackground(getDetailsHandler);
        }else{
            Toast.makeText(this,"Unable to fetch user details", Toast.LENGTH_LONG).show();
        }
    }
}
