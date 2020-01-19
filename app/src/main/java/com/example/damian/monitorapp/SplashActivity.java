package com.example.damian.monitorapp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.amazonaws.mobile.auth.core.DefaultSignInResultHandler;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.IdentityProvider;
import com.amazonaws.mobile.auth.core.StartupAuthErrorDetails;
import com.amazonaws.mobile.auth.core.StartupAuthResult;
import com.amazonaws.mobile.auth.core.StartupAuthResultHandler;
import com.amazonaws.mobile.auth.core.signin.AuthException;
import com.example.damian.monitorapp.AWSChangable.UILApplication;
import com.example.damian.monitorapp.AWSChangable.activity.SignInActivity;

import java.lang.ref.WeakReference;

public class SplashActivity extends Activity implements StartupAuthResultHandler {

    private static final String TAG = "SplashActivity";

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
    }
}
