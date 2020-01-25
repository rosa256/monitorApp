package com.example.damian.monitorapp.requester;

import android.content.Context;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.AgeRange;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.example.damian.monitorapp.BusyIndicator;
import com.example.damian.monitorapp.fragments.CameraPreviewFragment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

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

        try {
            result = amazonRekognitionClient.detectFaces(request);
            List<FaceDetail> faceDetails = result.getFaceDetails();

            for (FaceDetail face: faceDetails) {
                if (request.getAttributes().contains("ALL")) {
                    AgeRange ageRange = face.getAgeRange();
                    gender = face.getGender().toString();
                    Log.i(TAG,"The detected face is estimated to be between "
                            + ageRange.getLow().toString() + " and " + ageRange.getHigh().toString()
                            + " years old.");

                } else { // non-default attributes have null values.
                    Log.i(TAG,"Default set of attributes:");
                }

                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    Log.i(TAG,"Complete set of attributes:");
                    Log.i(TAG, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(face));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    Log.e("monitorApp","exception",e);
                }
            }
        } catch (AmazonServiceException e){
            this.exception = e;
            Log.e("monitorApp","exception",e);
        }
        return result;
    }

    @Override
    protected void onPostExecute(DetectFacesResult detectFacesResult) {
        super.onPostExecute(detectFacesResult);
        Toast.makeText(context.getApplicationContext(), "Gender: " + gender, Toast.LENGTH_LONG).show();
        //busyIndicator.unDimBackgorund();
        //sendPhotoAwsButton.setEnabled(true);
        context = null;
    }
}
