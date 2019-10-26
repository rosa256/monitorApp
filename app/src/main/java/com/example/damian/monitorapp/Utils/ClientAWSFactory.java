package com.example.damian.monitorapp.Utils;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.example.damian.monitorapp.requester.RefreshAsyncTask;

import java.util.HashMap;
import java.util.Map;

public class ClientAWSFactory extends AppCompatActivity{
    private AWSCredentials AWSCredentials;
    private CognitoCachingCredentialsProvider credentialsProvider;
    private Context context;
    private CognitoSettings cognitoSettings;
    private String TAG ="ClientAWSFactory";

    public AmazonRekognition createRekognitionClient(Context context) {
        this.context = context;
        cognitoSettings = CognitoSettings.getInstance();
        cognitoSettings.initContext(context.getApplicationContext());

        /*Identity pool credentials provider*/
        Log.i(TAG, "getting Identity Pool credentials provider");
        credentialsProvider = cognitoSettings.getCredentialsProvider();

        /*get user - User Pool*/
        Log.i(TAG, "getting user Pool user");
        CognitoUser currentUser = cognitoSettings.getUserPool().getCurrentUser();

        /*get token for logged in user - user pool*/
        Log.i(TAG, "calling getSessionInBackground....");
        currentUser.getSessionInBackground(new AuthenticationHandler() {
            @Override
            public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {

                if (userSession.isValid()) {
                    Log.i(TAG, "user session valid, getting token...");
                    // Get id token from CognitoUserSession.
                    String idToken = userSession.getIdToken().getJWTToken();

                    if (idToken.length() > 0) {
                        // Set up as a credentials provider.
                        Log.i(TAG, "got id token - setting credentials using token");
                        Map<String, String> logins = new HashMap<>();
                        System.out.println("Size:"+credentialsProvider.getLogins().size());
                        //logins.put("cognito-idp.eu-west-1.amazonaws.com/eu-west-1_2n6uKeWCd", idToken);
                        //credentialsProvider.setLogins(logins);
                        System.out.println("Size:"+credentialsProvider.getLogins().size());

                        Log.i(TAG, "using credentials for the logged in user");

                        /*refresh provider off main thread*/
                        Log.i(TAG, "refreshing credentials provider in asynctask..");
                        //new RefreshAsyncTask().execute(1);

                    } else {
                        Log.i(TAG, "no token...");
                    }
                } else {
                    Log.i(TAG, "user session not valid - using identity pool credentials - guest user");
                }
                //performAction(action);
            }

            @Override
            public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
                Log.i(TAG, " Not logged in! using identity pool credentials for guest user");
                //performAction(action);
            }
            @Override
            public void getMFACode(MultiFactorAuthenticationContinuation continuation) { }
            @Override
            public void authenticationChallenge(ChallengeContinuation continuation) { }
            @Override
            public void onFailure(Exception exception) {
                Log.i(TAG, "error getting session: " + exception.getLocalizedMessage());
//                proceed using guest user credentials
                //performAction(action);
                }
        });

        return new AmazonRekognitionClient(credentialsProvider);
    }
}

