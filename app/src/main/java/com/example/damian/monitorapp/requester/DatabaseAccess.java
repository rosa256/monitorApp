package com.example.damian.monitorapp.requester;

import android.content.Context;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.document.Table;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Region;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.example.damian.monitorapp.Utils.CognitoSettings;
import com.example.damian.monitorapp.Utils.Constants;
import com.example.damian.monitorapp.models.UserDO;

public class DatabaseAccess {

    private CognitoCachingCredentialsProvider credentialsProvider;
    private CognitoSettings cognitoSettings;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private Table dbTable;
    private static final String TABLE_NAME = "User_Check";


    private static DatabaseAccess instance;
    private DynamoDBMapper dynamoDBMapper;

    private DatabaseAccess(Context context, AmazonDynamoDBClient dynamoDBClient) {

        cognitoSettings = CognitoSettings.getInstance();
        credentialsProvider = cognitoSettings.getCredentialsProvider();
        amazonDynamoDBClient = new AmazonDynamoDBClient(credentialsProvider);
        amazonDynamoDBClient.setRegion(Region.getRegion(Constants.COGNITO_REGION));
        this.dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .build();

        dbTable = Table.loadTable(amazonDynamoDBClient, TABLE_NAME);
    }

    public static synchronized DatabaseAccess getInstance(Context context, AmazonDynamoDBClient dynamoDBClient){
        if (instance == null){
            instance = new DatabaseAccess(context, dynamoDBClient);
        }
        return instance;
    }

    public void createUserCheck(final UserDO userDO){
        new Thread(new Runnable() {
            @Override
            public void run() {
                dynamoDBMapper.save(userDO);
            }
        }).start();
    }
    public UserDO readUserCheck(final UserDO userDO){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //Logic
            }
        }).start();
        return null;
    }
}
