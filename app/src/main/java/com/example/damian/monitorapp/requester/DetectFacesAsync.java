package com.example.damian.monitorapp.requester;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;

public class DetectFacesAsync extends AsyncTask<String, Integer, DetectFacesResult> {
    private static final String TAG = "DetectFacesAsync";

    private Context context;
    private Exception exception;
    private DetectFacesRequest request;
    private AmazonRekognitionClient amazonRekognitionClient;
    private String gender ="-1";

    public DetectFacesAsync(AmazonRekognitionClient rekognitionClient, DetectFacesRequest request, Context context) {
        super();
        amazonRekognitionClient = rekognitionClient;
        this.context = context;
        this.request = request;
    }

    @Override
    protected DetectFacesResult doInBackground(String... strings) {
        DetectFacesResult result = null;
        Log.i(TAG,": Invoke do In background");

        return result;
    }

    @Override
    protected void onPostExecute(DetectFacesResult detectFacesResult) {
        super.onPostExecute(detectFacesResult);
        Toast.makeText(context.getApplicationContext(), "Gender: " + gender, Toast.LENGTH_LONG).show();

        context = null;
    }
}
