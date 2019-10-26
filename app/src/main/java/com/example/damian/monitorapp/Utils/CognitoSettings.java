package com.example.damian.monitorapp.Utils;

import android.content.Context;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.regions.Regions;

import java.util.HashMap;
import java.util.Map;

//CognitoCachingCredentialsProvider

public class CognitoSettings {

    private String userPoolId = Constants.USER_POOL_ID;
    private String clientId = Constants.APP_CLIENT_ID;
    private String clientSecret = Constants.APP_CLIENT_SECRET;
    private String cognitoRegion = Constants.COGNITO_REGION;
    private CognitoCachingCredentialsProvider cognitoCachingCredentialsProvider;

    private Context context;

    private static CognitoSettings instance;

    private CognitoSettings() {}

    public static synchronized CognitoSettings getInstance() {
        if (instance == null) {
            instance = new CognitoSettings();
        }
        return(instance);
    }

    public void initContext(Context context) {
        this.context = context;
    }

    public CognitoUserPool getUserPool(){
        return new CognitoUserPool(
                context,
                userPoolId,
                clientId,
                clientSecret,
                Regions.fromName(cognitoRegion)
        );
    }

    public CognitoCachingCredentialsProvider getCredentialsProvider() {
        if(cognitoCachingCredentialsProvider == null) {
            cognitoCachingCredentialsProvider = new CognitoCachingCredentialsProvider(
                    context.getApplicationContext(),
                    "eu-west-2:e6e456d7-f824-4910-8705-e914330e9663",
                    Regions.fromName(cognitoRegion)
            );
            return cognitoCachingCredentialsProvider;
        }else {
            return cognitoCachingCredentialsProvider;
        }
    }

}
