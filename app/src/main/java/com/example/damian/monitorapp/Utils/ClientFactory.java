package com.example.damian.monitorapp.Utils;

import android.support.v7.app.AppCompatActivity;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;

public class ClientFactory extends AppCompatActivity{
    private CognitoCachingCredentialsProvider credentialsProvider;

    public AmazonRekognition createClient() {
        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setConnectionTimeout(30000);
        clientConfig.setProtocol(Protocol.HTTPS);

        return new AmazonRekognitionClient(initAndGetCredentialsProvider());
    }

    public CognitoCachingCredentialsProvider initAndGetCredentialsProvider() {
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "eu-west-2:e6e456d7-f824-4910-8705-e914330e9663", // Identity pool ID
                Regions.EU_WEST_2 // Region
        );
        return credentialsProvider;
    }
}
