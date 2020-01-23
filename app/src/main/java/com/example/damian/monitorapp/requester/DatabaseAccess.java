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
import com.example.damian.monitorapp.AWSChangable.UILApplication;
import com.example.damian.monitorapp.Utils.CognitoSettings;
import com.example.damian.monitorapp.Utils.Constants;
import com.example.damian.monitorapp.models.UserDO;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatabaseAccess {

    private Context context;
    private CognitoCachingCredentialsProvider credentialsProvider;
    private CognitoSettings cognitoSettings;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private Table dbTable;
    private static final String TABLE_NAME = "User_Check";

    private static DatabaseAccess instance;

    private DatabaseAccess(Context context) {
        this.context = context;

        cognitoSettings = CognitoSettings.getInstance();
        credentialsProvider = UILApplication.cognitoCachingCredentialsProvider;

        amazonDynamoDBClient = new AmazonDynamoDBClient(UILApplication.cognitoCachingCredentialsProvider);
        amazonDynamoDBClient.setRegion(Region.getRegion(Constants.COGNITO_REGION));

        //dbTable = Table.loadTable(amazonDynamoDBClient, TABLE_NAME);
    }

    public static synchronized DatabaseAccess getInstance(Context context){
        if (instance == null){
            instance = new DatabaseAccess(context);
        }
        return instance;
    }

    public void createUserCheck(Document userCheckDocument){
        if(!userCheckDocument.containsKey("userId")){
            userCheckDocument.put("userId", credentialsProvider.getCachedIdentityId());
        }
        if(!userCheckDocument.containsKey(""))
        return;

/*
        PutItemOperationConfig putItemOperationConfig = new PutItemOperationConfig();
        putItemOperationConfig.withReturnValues(ReturnValue.ALL_OLD);
*/
    //    dbTable.putItem(userCheckDocument/*, putItemOperationConfig*/);
    }


    public void readUserCheck() {

        List<Document> returnedItem = dbTable.query(new Primitive(cognitoSettings.getUserPool().getCurrentUser().getUserId())).getAllResults();
        System.out.println("-----------------");
        System.out.println(returnedItem.get(1).toString());
        System.out.println(returnedItem.size());
        System.out.println("-----------------");
        System.out.println("-----------------");
    }
}
