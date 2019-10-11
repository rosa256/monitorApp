package com.example.damian.monitorapp.Utils;

import android.support.v7.app.AppCompatActivity;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;

public class ClientAWSFactory extends AppCompatActivity{
    private CognitoCachingCredentialsProvider credentialsProvider;
    private BasicAWSCredentials basicAWSCredentials;


    public AmazonRekognition createRekognitionClient() {
        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setConnectionTimeout(30000);
        clientConfig.setProtocol(Protocol.HTTPS);

        return new AmazonRekognitionClient(initAndGetCredentialsProvider());
    }

    public BasicAWSCredentials initAndGetCredentialsProvider() {
        basicAWSCredentials = new BasicAWSCredentials(Constants.ACCESS_KEY,Constants.SECRET_KEY);
        return basicAWSCredentials;
    }

//    public CognitoCachingCredentialsProvider initAndGetCredentialsProvider() {
//        credentialsProvider = new CognitoCachingCredentialsProvider(
//                getApplicationContext(),
//                Constants.IDENTITY_POOL, // Identity pool ID
//                Regions.EU_WEST_2 // Region
//        );
//        return credentialsProvider;
//    }
}
