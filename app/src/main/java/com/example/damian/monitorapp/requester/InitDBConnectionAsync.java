package com.example.damian.monitorapp.requester;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;


public class InitDBConnectionAsync extends AsyncTask<String, Integer, InitDBConnectionAsync> {

    private Context context;
    private DatabaseAccess databaseAccess;
    private AmazonDynamoDBClient dynamoDBClient;

    public InitDBConnectionAsync(Context context, AmazonDynamoDBClient dynamoDBClient) {
        this.context = context;
        this.dynamoDBClient = dynamoDBClient;
    }

    @Override
    protected InitDBConnectionAsync doInBackground(String... strings) {
        databaseAccess = DatabaseAccess.getInstance(context);

        return null;
    }
    @Override
    protected void onPostExecute(InitDBConnectionAsync initDBConnectionAsync) {
        super.onPostExecute(initDBConnectionAsync);
        Toast.makeText(context.getApplicationContext(), "Connection Database: " + (!databaseAccess.toString().isEmpty()), Toast.LENGTH_LONG).show();
        //context = null;
    }

}
