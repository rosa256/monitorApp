package com.example.damian.monitorapp.requester;

import android.content.Context;
import android.util.Log;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.dynamodbv2.document.Table;
import com.amazonaws.mobileconnectors.dynamodbv2.document.datatype.Document;
import com.amazonaws.mobileconnectors.dynamodbv2.document.datatype.Primitive;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.example.damian.monitorapp.AWSChangable.UILApplication;
import com.example.damian.monitorapp.AWSChangable.utils.AppHelper;
import com.example.damian.monitorapp.Utils.CognitoSettings;
import com.example.damian.monitorapp.models.nosql.STATUSDO;
import com.example.damian.monitorapp.models.nosql.USERDO;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class DatabaseAccess {

    private static final String TAG = "DatabaseAccess";
    private Context context;
    private CognitoCachingCredentialsProvider credentialsProvider;
    private CognitoSettings cognitoSettings;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private Table dbTable;
    private static final String TABLE_NAME = "User_Check";

    private static DatabaseAccess instance;
    // Declare a DynamoDBMapper object
    DynamoDBMapper dynamoDBMapper;
    AmazonDynamoDBClient dynamoDBClient;
    private DatabaseAccess(Context context) {
        this.context = context;

        cognitoSettings = CognitoSettings.getInstance();
        credentialsProvider = UILApplication.cognitoCachingCredentialsProvider;

        // AWSMobileClient enables AWS user credentials to access your table
        AWSMobileClient.getInstance().initialize(context).execute();

        AWSCredentialsProvider credentialsProvider = AWSMobileClient.getInstance().getCredentialsProvider();
        AWSConfiguration configuration = AWSMobileClient.getInstance().getConfiguration();


        // Add code to instantiate a AmazonDynamoDBClient
        dynamoDBClient = new AmazonDynamoDBClient(credentialsProvider);

        this.dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .awsConfiguration(configuration)
                .build();

        //amazonDynamoDBClient = new AmazonDynamoDBClient(UILApplication.cognitoCachingCredentialsProvider);
        //amazonDynamoDBClient.setRegion(Region.getRegion(Constants.COGNITO_REGION));

        //dbTable = Table.loadTable(amazonDynamoDBClient, TABLE_NAME);
    }

    public static synchronized DatabaseAccess getInstance(Context context){
        if (instance == null){
            instance = new DatabaseAccess(context);
        }
        return instance;
    }

    public void createUser(final String userId, final String username, final String email){
        Runnable runnable = new Runnable() {
            public void run() {
                final USERDO userItem = new USERDO();

                userItem.setUserId(userId);
                userItem.setUsername(username);
                userItem.setEmail(email);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        dynamoDBMapper.save(userItem);
                    }
                }).start();
            }
        };

        Thread createUserThread = new Thread(runnable);
        createUserThread.start();
    }

    public void createStatus(final String confidence){
        Log.i(TAG, "onPostExecute: Saving Status to DB");


        Runnable runnable = new Runnable() {
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        STATUSDO statusItem = createStatusToSave(confidence);

                        Log.i(TAG, "createStatus: " + statusItem.toString());

                        dynamoDBMapper.save(statusItem);
                    }
                }).start();
            }
        };

        Thread createStatusThread = new Thread(runnable);
        createStatusThread.start();
    }

    private STATUSDO createStatusToSave(String confidence){
        final STATUSDO statusItem = new STATUSDO();

        Calendar now = Calendar.getInstance();

        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH) + 1; // Note: zero based!
        int day = now.get(Calendar.DAY_OF_MONTH);
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        int second = now.get(Calendar.SECOND);

        String date = String.format("%d-%02d-%02d", year, month, day);
        String detailHour = String.format("%02d:%02d:%02d", hour, minute, second);
        String full_date =  String.format("%d-%02d-%02d %02d:%02d:%02d", year, month, day, hour, minute, second);

        int offset = now.getTimeZone().getRawOffset() / 1000;
        int unix_time_utc = (int) (now.getTimeInMillis() / 1000);
        String unixTime = String.valueOf(unix_time_utc + offset);

        statusItem.setUserId(AppHelper.getPool().getCurrentUser().getUserId());
        statusItem.setDate(date);
        statusItem.setHour(detailHour);
        statusItem.setConfidence(confidence);
        statusItem.setFullDate(full_date);
        statusItem.setUnixTime(unixTime);

        if(Double.parseDouble(confidence) > 75D ) {
            statusItem.setVerified(true);
        }else{
            statusItem.setVerified(false);
        }

        return statusItem;
    }


    public List<STATUSDO> getStatusFromToday(){

        Log.i(TAG, "getStatusFromToday: Find replies for thread Message = 'DynamoDB Thread 2' posted within a period.");
        //long startDateMilli = (new Date()).getTime() - (14L * 24L * 60L * 60L * 1000L); // Two
        //long endDateMilli = (new Date()).getTime() - (7L * 24L * 60L * 60L * 1000L); // One
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        //String startDate = dateFormatter.format(startDateMilli);
        //String endDate = dateFormatter.format(endDateMilli);

        Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
        eav.put(":val1", new AttributeValue().withS(AppHelper.getPool().getCurrentUser().getUserId()));
        //eav.put(":val2", new AttributeValue().withS(startDate));
        //eav.put(":val3", new AttributeValue().withS(endDate));
        STATUSDO hasKey = new STATUSDO();
        hasKey.setUserId(AppHelper.getPool().getCurrentUser().getUserId());

        DynamoDBQueryExpression<STATUSDO> queryExpression = new DynamoDBQueryExpression<STATUSDO>()
                .withHashKeyValues(hasKey);
        //.withExpressionAttributeValues(eav);

        List<STATUSDO> allStatuses = dynamoDBMapper.query(STATUSDO.class, queryExpression);
        for (STATUSDO reply : allStatuses) {
            System.out.format("Id=%s, FullDate=%s, UnixTime=%s , Verified=%s \n", reply.getUserId(),
                    reply.getFullDate(), reply.getUnixTime(), reply.getVerified());
        }

        return allStatuses;
    }

    public List<STATUSDO> getStatusFromYesterday(){

        Log.i(TAG, "getStatusFromYesterday: Invoke method.");
        // today
        Calendar date = new GregorianCalendar();

        // reset hour, minutes, seconds and millis
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        String endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date.getTime()); //Yesterday Start Midnight

        // next day
        date.add(Calendar.DAY_OF_MONTH, -1);

        String startDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date.getTime()); //Yesterday End Midnight

        ArrayList<AttributeValue> atributes = new ArrayList<>();
        atributes.add(new AttributeValue().withS(startDate));
        atributes.add(new AttributeValue().withS(endDate));

        STATUSDO hasKey = new STATUSDO();
        hasKey.setUserId(AppHelper.getPool().getCurrentUser().getUserId());

        Condition condition = new Condition();
        condition.setComparisonOperator(ComparisonOperator.BETWEEN);
        condition.setAttributeValueList(atributes);

        DynamoDBQueryExpression<STATUSDO> queryExpression = new DynamoDBQueryExpression<STATUSDO>()
                .withHashKeyValues(hasKey)
                .withRangeKeyCondition("full_date", condition);

        List<STATUSDO> allStatuses = dynamoDBMapper.query(STATUSDO.class, queryExpression);
        for (STATUSDO reply : allStatuses) {
            System.out.format("Id=%s, FullDate=%s, UnixTime=%s , Verified=%s \n", reply.getUserId(),
                    reply.getFullDate(), reply.getUnixTime(), reply.getVerified());
        }

        return allStatuses;
    }


    public void createUserCheck(Document userCheckDocument){
        if(!userCheckDocument.containsKey("userId")){
            userCheckDocument.put("userId", credentialsProvider.getCachedIdentityId());
        }
        if(!userCheckDocument.containsKey(""))
        return;
    }


    public void readUserCheck() {

        List<Document> returnedItem = dbTable.query(new Primitive(cognitoSettings.getUserPool().getCurrentUser().getUserId())).getAllResults();
        System.out.println(returnedItem.get(1).toString());
        System.out.println(returnedItem.size());

    }

    //*************************************************************************
}
