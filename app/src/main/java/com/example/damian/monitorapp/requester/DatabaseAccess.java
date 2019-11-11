package com.example.damian.monitorapp.requester;

import android.content.Context;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.example.damian.monitorapp.Utils.CognitoSettings;
import com.example.damian.monitorapp.Utils.Constants;

public class DatabaseAccess {

    private CognitoCachingCredentialsProvider credentialsProvider;
    private CognitoSettings cognitoSettings;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private Table


    private static DatabaseAccess instance;

    public DatabaseAccess(Context context) {

        cognitoSettings = CognitoSettings.getInstance();
        credentialsProvider = cognitoSettings.getCredentialsProvider();
        amazonDynamoDBClient = new AmazonDynamoDBClient(credentialsProvider);
        amazonDynamoDBClient.setRegion(Region.getRegion(Constants.COGNITO_REGION));
    }

    public static synchronized DatabaseAccess getInstance(Context context){
        if (instance == null){
            instance = new DatabaseAccess(context);
        }
        return instance;
    }
}
