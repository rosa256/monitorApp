package com.example.damian.monitorapp.requester;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.CompareFacesMatch;
import com.amazonaws.services.rekognition.model.CompareFacesRequest;
import com.amazonaws.services.rekognition.model.CompareFacesResult;
import com.amazonaws.services.rekognition.model.ComparedFace;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.InvalidParameterException;
import com.example.damian.monitorapp.Utils.Constants;
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
    private String confidence ="0";
    boolean sendFalse;

    private Exception exception;

    public CompareFacesAsync(AmazonRekognitionClient rekognitionClient, Image source, Image target, Context context) {
        super();
        this.amazonRekognitionClient = rekognitionClient;
        this.source = source;
        this.target = target;
        this.context = context;

        objectMapper = new ObjectMapper();
    }


    @Override
    protected Void doInBackground(String... strings) {
        Log.i(TAG,": Invoke do In background");

        CompareFacesRequest request = new CompareFacesRequest()
                .withSourceImage(source)
                .withTargetImage(target)
                .withSimilarityThreshold(Constants.SIMILARITY_THRESHOLD);

        // Call operation
        // When in target or source photo there is no faces, then its return InvalidparametersExcpetion.
        CompareFacesResult compareFacesResult;
        sendFalse = false;
        try{
            compareFacesResult = amazonRekognitionClient.compareFaces(request);

            List<CompareFacesMatch> faceDetails = compareFacesResult.getFaceMatches();
            for (CompareFacesMatch match : faceDetails) {
                ComparedFace face = match.getFace();
                BoundingBox position = face.getBoundingBox();
                System.out.println("Face at " + position.getLeft().toString()
                        + " " + position.getTop()
                        + " matches with " + match.getSimilarity().toString()
                        + "% confidence.");
                confidence = match.getSimilarity().toString();
                try {
                    Log.i(TAG, "Complete set of attributes:");
                    Log.i(TAG, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(face));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    Log.e("monitorApp", "exception", e);
                }
            }

            List<ComparedFace> uncompared = compareFacesResult.getUnmatchedFaces();

            Log.i(TAG, "There was " + uncompared.size()
                    + " face(s) that did not match");

        }catch (InvalidParameterException e){
            sendFalse = true;
        }


        //Display results
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Toast.makeText(context.getApplicationContext(), "Comparison: " + confidence, Toast.LENGTH_LONG).show();

        final DatabaseAccess databaseAccess = DatabaseAccess.getInstance(null);
        Log.i(TAG, "onPostExecute: Start saving status to DB");
        if(sendFalse){
            databaseAccess.createStatus("0");
        }else{
            databaseAccess.createStatus(confidence);
        }
        Log.i(TAG, "onPostExecute: End saving status to DB");
    }
}
