package com.example.damian.monitorapp.requester;

import android.content.Context;
import android.util.Log;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.example.damian.monitorapp.utils.AppHelper;
import com.example.damian.monitorapp.models.nosql.STATUSDO;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class DatabaseAccess {

    private static final String TAG = "DatabaseAccess";

    private DynamoDBMapper dynamoDBMapper;
    private AmazonDynamoDBClient dynamoDBClient;
    private static DatabaseAccess instance;
    private DatabaseAccess(Context context) {

        // AWSMobileClient enables AWS user credentials to access your table
        AWSMobileClient.getInstance().initialize(context).execute();

        AWSCredentialsProvider credentialsProvider = AWSMobileClient.getInstance().getCredentialsProvider();
        AWSConfiguration configuration = AWSMobileClient.getInstance().getConfiguration();

        dynamoDBClient = new AmazonDynamoDBClient(credentialsProvider);
        //dynamoDBClient = new AmazonDynamoDBClient(AppHelper.getCognitoCachingCredentialsProvider());

        this.dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .awsConfiguration(configuration)
                .build();
    }

    public static synchronized DatabaseAccess getInstance(Context context){
        if (instance == null){
            instance = new DatabaseAccess(context);
        }
        return instance;
    }

    public void createStatus(final String confidence){
        Log.i(TAG, "onPostExecute: Saving Status to DB");


        Runnable runnable = new Runnable() {
            public void run() {
                STATUSDO statusItem = createStatusToSave(confidence);
                Log.i(TAG, "createStatus: " + statusItem.toString());
                dynamoDBMapper.save(statusItem);
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

        if(Double.parseDouble(confidence) > 70D ) {
            statusItem.setVerified(true);
        }else{
            statusItem.setVerified(false);
        }

        return statusItem;
    }

    public List<STATUSDO> getStatusFromToday(){

        Log.i(TAG, "getStatusFromToday(): Invoke method.");

        Calendar todayMidnightTime = Calendar.getInstance();

        todayMidnightTime.set(Calendar.HOUR_OF_DAY,0);
        todayMidnightTime.set(Calendar.MINUTE,0);
        todayMidnightTime.set(Calendar.SECOND,0);
        todayMidnightTime.set(Calendar.MILLISECOND, 0);

        String todayMidnightString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(todayMidnightTime.getTime());

        STATUSDO hasKey = new STATUSDO();
        hasKey.setUserId(AppHelper.getPool().getCurrentUser().getUserId());

        List<AttributeValue> attributeValue = new ArrayList<>();
        attributeValue.add(new AttributeValue().withS(todayMidnightString));

        Condition todayStatusesCondition = new Condition();
        todayStatusesCondition.withComparisonOperator(ComparisonOperator.GE);
        todayStatusesCondition.withAttributeValueList(attributeValue);

        DynamoDBQueryExpression<STATUSDO> queryExpression = new DynamoDBQueryExpression<STATUSDO>()
                .withHashKeyValues(hasKey)
                .withRangeKeyCondition("full_date", todayStatusesCondition);

        List<STATUSDO> allStatuses = dynamoDBMapper.query(STATUSDO.class, queryExpression);
        for (STATUSDO reply : allStatuses) {
            System.out.format("Id=%s, FullDate=%s, UnixTime=%s , Verified=%s \n", reply.getUserId(),
                    reply.getFullDate(), reply.getUnixTime(), reply.getVerified());
        }

        return allStatuses;
    }

    public List<STATUSDO> getStatusFromYesterday(){

        Log.i(TAG, "getStatusFromYesterday(): Invoke method.");
        // today
        Calendar date = new GregorianCalendar();

        // reset hour, minutes, seconds and millis
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        String endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date.getTime()); //Yesterday End Midnight

        // next day
        date.add(Calendar.DAY_OF_MONTH, -1);

        String startDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date.getTime()); //Yesterday Start Midnight

        ArrayList<AttributeValue> attributes = new ArrayList<>();
        attributes.add(new AttributeValue().withS(startDate));
        attributes.add(new AttributeValue().withS(endDate));

        STATUSDO hasKey = new STATUSDO();
        hasKey.setUserId(AppHelper.getPool().getCurrentUser().getUserId());

        Condition yesterdayStatusesCondition = new Condition();
        yesterdayStatusesCondition.setComparisonOperator(ComparisonOperator.BETWEEN);
        yesterdayStatusesCondition.setAttributeValueList(attributes);

        DynamoDBQueryExpression<STATUSDO> queryExpression = new DynamoDBQueryExpression<STATUSDO>()
                .withHashKeyValues(hasKey)
                .withRangeKeyCondition("full_date", yesterdayStatusesCondition);

        List<STATUSDO> allStatuses = dynamoDBMapper.query(STATUSDO.class, queryExpression);
        for (STATUSDO reply : allStatuses) {
            System.out.format("Id=%s, FullDate=%s, UnixTime=%s , Verified=%s \n", reply.getUserId(),
                    reply.getFullDate(), reply.getUnixTime(), reply.getVerified());
        }

        return allStatuses;
    }

    public List<STATUSDO> getStatusFromLastWeek(){

        Log.i(TAG, "getStatusFromLastWeek(): Invoke method.");
        // today
        Calendar date = new GregorianCalendar();

        String endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date.getTime()); //Right now time

        // reset hour, minutes, seconds and millis
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        // previous 7 days from midnight (example: 10.01.2020 00:00:00(previous 7 days) - 17.01.2020 15:34:22(right now time))
        date.add(Calendar.DAY_OF_MONTH, -7);

        String startDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date.getTime()); //Yesterday Start Midnight

        ArrayList<AttributeValue> attributes = new ArrayList<>();
        attributes.add(new AttributeValue().withS(startDate));
        attributes.add(new AttributeValue().withS(endDate));

        STATUSDO hasKey = new STATUSDO();
        hasKey.setUserId(AppHelper.getPool().getCurrentUser().getUserId());

        Condition lastWeekCondition = new Condition();
        lastWeekCondition.setComparisonOperator(ComparisonOperator.BETWEEN);
        lastWeekCondition.setAttributeValueList(attributes);

        DynamoDBQueryExpression<STATUSDO> queryExpression = new DynamoDBQueryExpression<STATUSDO>()
                .withHashKeyValues(hasKey)
                .withRangeKeyCondition("full_date", lastWeekCondition);

        List<STATUSDO> allStatuses = dynamoDBMapper.query(STATUSDO.class, queryExpression);
        for (STATUSDO status : allStatuses) {
            System.out.format("Id=%s, FullDate=%s, UnixTime=%s , Verified=%s \n", status.getUserId(),
                    status.getFullDate(), status.getUnixTime(), status.getVerified());
        }
        return allStatuses;
    }

    public List<STATUSDO> getStatusFromCustomDate(Date customDateStart, Date customDateEnd){

        Log.i(TAG, "getStatusFromCustomDate(): Invoke method.");

        String dateFormat = "yyyy-MM-dd HH:mm:ss";
        String startDate = new SimpleDateFormat(dateFormat).format(customDateStart);
        String endDate = new SimpleDateFormat(dateFormat).format(customDateEnd);

        Log.i(TAG, "getStatusFromCustomDate(): customDateStart: "+ customDateStart);
        Log.i(TAG, "getStatusFromCustomDate(): customDateEnd: "+ customDateEnd);

        ArrayList<AttributeValue> attributes = new ArrayList<>();
        attributes.add(new AttributeValue().withS(startDate));
        attributes.add(new AttributeValue().withS(endDate));

        STATUSDO statusDO = new STATUSDO();
        statusDO.setUserId(AppHelper.getPool().getCurrentUser().getUserId());

        Condition customDateCondition = new Condition();
        customDateCondition.setComparisonOperator(ComparisonOperator.BETWEEN);
        customDateCondition.setAttributeValueList(attributes);

        DynamoDBQueryExpression<STATUSDO> queryExpression = new DynamoDBQueryExpression<STATUSDO>()
                .withHashKeyValues(statusDO)
                .withRangeKeyCondition("full_date", customDateCondition);

        List<STATUSDO> allStatuses = dynamoDBMapper.query(STATUSDO.class, queryExpression);

        String formatToDisplay = "Id=%s, FullDate=%s, UnixTime=%s , Verified=%s \n";
        for (STATUSDO status : allStatuses) {
            Log.d(TAG, String.format(formatToDisplay,
                    status.getUserId(), status.getFullDate(), status.getUnixTime(), status.getVerified()));
        }
        return allStatuses;
    }
}
