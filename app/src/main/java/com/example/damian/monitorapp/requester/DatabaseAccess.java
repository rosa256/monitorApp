package com.example.damian.monitorapp.requester;

import android.content.Context;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.document.PutItemOperationConfig;
import com.amazonaws.mobileconnectors.dynamodbv2.document.Table;
import com.amazonaws.mobileconnectors.dynamodbv2.document.datatype.Document;
import com.amazonaws.mobileconnectors.dynamodbv2.document.datatype.DynamoDBEntry;
import com.amazonaws.mobileconnectors.dynamodbv2.document.datatype.Primitive;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Region;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.example.damian.monitorapp.Utils.CognitoSettings;
import com.example.damian.monitorapp.Utils.Constants;
import com.example.damian.monitorapp.models.UserDO;

import java.util.HashSet;
import java.util.Set;

public class DatabaseAccess {

    private Context context;
    private CognitoCachingCredentialsProvider credentialsProvider;
    private CognitoSettings cognitoSettings;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private Table dbTable;
    private static final String TABLE_NAME = "User_Check";


    private static DatabaseAccess instance;
    private DynamoDBMapper dynamoDBMapper;

    private DatabaseAccess(Context context) {
        this.context = context;

        cognitoSettings = CognitoSettings.getInstance();
        credentialsProvider = cognitoSettings.getCredentialsProvider();
        amazonDynamoDBClient = new AmazonDynamoDBClient(credentialsProvider);
        amazonDynamoDBClient.setRegion(Region.getRegion(Constants.COGNITO_REGION));
//        this.dynamoDBMapper = DynamoDBMapper.builder()
//                .dynamoDBClient(dynamoDBClient)
//                .build();

        dbTable = Table.loadTable(amazonDynamoDBClient, TABLE_NAME);
    }

    public static synchronized DatabaseAccess getInstance(Context context){
        if (instance == null){
            instance = new DatabaseAccess(context);
        }
        return instance;
    }

    public void createUserCheck(/*Document userDocument*/){

        Document userDocument = new Document();
        userDocument.put("userId","test2");
        userDocument.put("confidence","100");
        userDocument.put("date","15-05-2005");
        userDocument.put("hour","17:23");

//        Set<String> mySet = new HashSet<>();
//
//        mySet.add()

        DynamoDBEntry item1 = new Primitive("primiteve1");

        PutItemOperationConfig putItemOperationConfig = new PutItemOperationConfig();
        putItemOperationConfig.withReturnValues(ReturnValue.ALL_OLD);

        Document result = dbTable.putItem(userDocument, putItemOperationConfig);

       // Document retrievedDoc = dbTable.getItem(new Primitive("userId"));
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                dynamoDBMapper.save(userDO);
//            }
//        }).start();
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
