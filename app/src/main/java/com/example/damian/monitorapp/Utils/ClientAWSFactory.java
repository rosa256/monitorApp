package com.example.damian.monitorapp.Utils;

import android.support.v7.app.AppCompatActivity;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;

public class ClientAWSFactory extends AppCompatActivity{
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

//    CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
//            getApplicationContext(),
//            "eu-west-2:e6e456d7-f824-4910-8705-e914330e9663", // Identity pool ID
//            Regions.EU_WEST_2 // Region
//    );






}
