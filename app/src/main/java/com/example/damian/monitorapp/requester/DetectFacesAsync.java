package com.example.damian.monitorapp.requester;

import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.AgeRange;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class DetectFacesAsync extends AsyncTask<String, Void, DetectFacesResult> {
    private static final String TAG = "DetectFacesAsync";

    private Exception exception;
    private DetectFacesRequest request;
    private AmazonRekognitionClient amazonRekognitionClient;

    public DetectFacesAsync(AmazonRekognitionClient rekognitionClient, DetectFacesRequest request) {
        super();
        amazonRekognitionClient = rekognitionClient;
        this.request = request;
        return;
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
}
