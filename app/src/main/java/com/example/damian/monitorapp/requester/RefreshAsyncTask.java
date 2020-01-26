package com.example.damian.monitorapp.requester;

import android.os.AsyncTask;
import android.util.Log;

import com.example.damian.monitorapp.Utils.CognitoSettings;

public class RefreshAsyncTask extends AsyncTask<Integer, Void, Integer> {

    private String TAG = "RefreshAsyncTask";
    private CognitoSettings cognitoSettings;
    @Override
    protected Integer doInBackground(Integer... integers) {
        Log.i(TAG, "in asynctask doInBackground()");
        cognitoSettings = CognitoSettings.getInstance();
        System.out.println("BEFORE REFRESH: "+cognitoSettings.getCredentialsProvider().getToken());
        cognitoSettings.getCredentialsProvider().refresh();
        return integers[0];
    }

    @Override
    protected void onPostExecute(Integer action) {
        Log.i(TAG, "in asynctask onPostExecute()");
        Log.i(TAG, "onPostExecute(): Refresed credentails.");
    }
}
