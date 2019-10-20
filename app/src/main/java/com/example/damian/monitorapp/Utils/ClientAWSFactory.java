package com.example.damian.monitorapp.Utils;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;

public class ClientAWSFactory extends AppCompatActivity{
    private AWSCredentials AWSCredentials;
    private CognitoCachingCredentialsProvider cognitoCachingCredentialsProvider;
    private Context context;
    private CognitoSettings cognitoSettings;

    public AmazonRekognition createRekognitionClient(Context context) {
        this.context = context;
        /*ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setConnectionTimeout(30000);
        clientConfig.setProtocol(Protocol.HTTPS);*/
        cognitoSettings = CognitoSettings.getInstance();
        cognitoSettings.initContext(context.getApplicationContext());
        System.out.println("XXXXXXXXXXXX");
        System.out.println(cognitoSettings.getUserPool().getCurrentUser().getUserId());
        cognitoCachingCredentialsProvider = cognitoSettings.getCredentialsProvider();
        System.out.println(cognitoCachingCredentialsProvider.getCachedIdentityId());

        System.out.println("XXXXXXXXXXXX");
        return new AmazonRekognitionClient(cognitoCachingCredentialsProvider);
    }

    public AWSCredentials initAndGetCredentialsProvider() {
        AWSCredentials = new BasicAWSCredentials(Constants.ACCESS_KEY,Constants.SECRET_KEY);
        return AWSCredentials;
    }
}
