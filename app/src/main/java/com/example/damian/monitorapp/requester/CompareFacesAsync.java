package com.example.damian.monitorapp.requester;

import android.content.Context;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.CompareFacesMatch;
import com.amazonaws.services.rekognition.model.CompareFacesRequest;
import com.amazonaws.services.rekognition.model.CompareFacesResult;
import com.amazonaws.services.rekognition.model.ComparedFace;
import com.amazonaws.services.rekognition.model.Image;
import com.example.damian.monitorapp.BusyIndicator;
import com.example.damian.monitorapp.Utils.Constants;
import com.example.damian.monitorapp.fragments.CameraPreviewFragment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class CompareFacesAsync extends AsyncTask<String, Void, Void> {
    private static final String TAG = "CompareFacesAsync";

    private AmazonRekognitionClient amazonRekognitionClient;
    private ObjectMapper objectMapper;
    private Context context;
    private Image source;
    private Image target;
    private String confidence ="-1";
    private BusyIndicator busyIndicator;
    private FloatingActionButton sendPhotoAwsButton;

    private Exception exception;

    public CompareFacesAsync(AmazonRekognitionClient rekognitionClient, Image source, Image target, Context context, CameraPreviewFragment cameraPreviewFragment, FloatingActionButton sendPhotoAwsButton) {
        super();
        this.amazonRekognitionClient = rekognitionClient;
        this.source = source;
        this.target = target;
        this.context = context;
        this.sendPhotoAwsButton = sendPhotoAwsButton;
        busyIndicator = new BusyIndicator(cameraPreviewFragment);

        objectMapper = new ObjectMapper();
        busyIndicator.dimBackground();
    }


    @Override
    protected Void doInBackground(String... strings) {
        Log.i(TAG,": Invoke do In background");

        CompareFacesRequest request = new CompareFacesRequest()
                .withSourceImage(source)
                .withTargetImage(target)
                .withSimilarityThreshold(Constants.SIMILARITY_THRESHOLD);

        // Call operation
        CompareFacesResult compareFacesResult = amazonRekognitionClient.compareFaces(request);

        //Display results
        List<CompareFacesMatch> faceDetails = compareFacesResult.getFaceMatches();
        for (CompareFacesMatch match: faceDetails){
            ComparedFace face= match.getFace();
            BoundingBox position = face.getBoundingBox();
            System.out.println("Face at " + position.getLeft().toString()
                    + " " + position.getTop()
                    + " matches with " + match.getSimilarity().toString()
                    + "% confidence.");
            confidence = match.getSimilarity().toString();
            try {
                Log.i(TAG,"Complete set of attributes:");
                Log.i(TAG, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(face));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                Log.e("monitorApp","exception",e);
            }
        }

        List<ComparedFace> uncompared = compareFacesResult.getUnmatchedFaces();

        Log.i(TAG,"There was " + uncompared.size()
                + " face(s) that did not match");

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Toast.makeText(context.getApplicationContext(), "Comparison: " + confidence, Toast.LENGTH_LONG).show();
        busyIndicator.unDimBackgorund();
        sendPhotoAwsButton.setEnabled(true);
    }
}
