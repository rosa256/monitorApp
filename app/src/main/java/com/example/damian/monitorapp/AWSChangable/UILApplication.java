package com.example.damian.monitorapp.AWSChangable;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.multidex.MultiDexApplication;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.example.damian.monitorapp.AWSChangable.utils.AppHelper;
import com.example.damian.monitorapp.AWSChangable.utils.AuthUIConfiguration;
import com.example.damian.monitorapp.AWSChangable.utils.CustomCognitoUserPoolsSignInProvider;

public class UILApplication extends MultiDexApplication {
    private static boolean activityVisible;

    private static final String LOG_TAG = UILApplication.class.getSimpleName();
    public static AWSConfiguration awsConfiguration;
    public static AuthUIConfiguration sAuthUIConfiguration =
            new AuthUIConfiguration.Builder()
                    .userPools(true)
                    //.logoResId(R.drawable.default_sign_in_logo)
                    .build();

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @SuppressWarnings("unused")

    @Override
    public void onCreate() {
        super.onCreate();

        initializeApplication();
    }

    private void initializeApplication() {
        // Initialize application

        AppHelper.init(getApplicationContext());
        awsConfiguration = new AWSConfiguration(this);


        if (IdentityManager.getDefaultIdentityManager() == null) {
            CognitoCachingCredentialsProvider credentials = new CognitoCachingCredentialsProvider(
                    getApplicationContext(),
                    AppHelper.IDENTITY_POOL_ID,
                    AppHelper.cognitoRegion);
            final IdentityManager identityManager = new IdentityManager(getApplicationContext(), awsConfiguration);
            IdentityManager.setDefaultIdentityManager(identityManager);
        }


        // Add UserPools as an SignIn Provider.
        IdentityManager.getDefaultIdentityManager().addSignInProvider(CustomCognitoUserPoolsSignInProvider.class);

    }

}