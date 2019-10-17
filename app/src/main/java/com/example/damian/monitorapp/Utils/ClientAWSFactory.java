package com.example.damian.monitorapp.Utils;

import android.support.v7.app.AppCompatActivity;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.BasicAWSCredentials;
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








}
