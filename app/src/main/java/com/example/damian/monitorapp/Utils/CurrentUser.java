package com.example.damian.monitorapp.Utils;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;

public class CurrentUser {

    private static CurrentUser ourInstance = null;

    private CognitoUserPool cognitoUserPool;

    private CurrentUser() {}

    public static CurrentUser getInstance() {
        if(ourInstance == null) {
            synchronized (CurrentUser.class) {
                ourInstance = new CurrentUser();
            }
        }
        return ourInstance;
    }
    public CognitoUserPool getCognitoUserPool() {
        return cognitoUserPool;
    }

    public void setCognitoUserPool(CognitoUserPool cognitoUserPool) {
        this.cognitoUserPool = cognitoUserPool;
    }
}
