package com.example.damian.monitorapp.utils;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;

public class ClientAWSFactory extends AppCompatActivity{
    private String TAG ="ClientAWSFactory";

    public AmazonRekognition createRekognitionClient() {

        Log.i(TAG, "Getting Identity Pool credentials provider");
        return new AmazonRekognitionClient(AppHelper.getCognitoCachingCredentialsProvider());
    }

    public AmazonDynamoDBClient createDynamoDBClient() {
        return new AmazonDynamoDBClient(AppHelper.getCognitoCachingCredentialsProvider());
    }

}

