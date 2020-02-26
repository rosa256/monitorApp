package com.example.damian.monitorapp.requester;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.CompareFacesMatch;
import com.amazonaws.services.rekognition.model.CompareFacesRequest;
import com.amazonaws.services.rekognition.model.CompareFacesResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.InvalidParameterException;
import com.example.damian.monitorapp.utils.Constants;

import java.util.List;

public class CompareFacesAsync extends AsyncTask<String, Void, Void> {
    private static final String TAG = "CompareFacesAsync";

    private AmazonRekognitionClient amazonRekognitionClient;
    private Context context;
    private Image sourceImage;
    private Image targetImage;
    private String confidenceStr ="0";
    private Float confidence = 0F;
    private Float actuallSimilarity = 0F;
    boolean comparisonFailed;


    public CompareFacesAsync(AmazonRekognitionClient rekognitionClient, Image sourceImage, Image targetImage, Context context) {
        super();
        this.amazonRekognitionClient = rekognitionClient;
        this.sourceImage = sourceImage;
        this.targetImage = targetImage;
        this.context = context;
    }

    @Override
    protected Void doInBackground(String... strings) {
        Log.i(TAG,": Invoke do In background");

        CompareFacesRequest request = new CompareFacesRequest()
                .withSourceImage(sourceImage)
                .withTargetImage(targetImage)
                .withSimilarityThreshold(Constants.SIMILARITY_THRESHOLD);

        try{
            CompareFacesResult response = amazonRekognitionClient.compareFaces(request);
            List<CompareFacesMatch> faceDetails = response.getFaceMatches();
            for (CompareFacesMatch details : faceDetails) {
                Log.d(TAG, "Face matches with " + details.getSimilarity().toString() + "% confidence.");
                actuallSimilarity  = details.getSimilarity();
                if (confidence < actuallSimilarity){
                    confidence = actuallSimilarity;
                }
            }
            confidenceStr = confidence.toString();
            Log.d(TAG, "There was " + response.getUnmatchedFaces().size() + " faces which didn't match.");
        }catch (InvalidParameterException e){
            comparisonFailed = true;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Toast.makeText(context.getApplicationContext(), "Comparison: " + confidence, Toast.LENGTH_SHORT).show();
        final DatabaseAccess databaseAccess = DatabaseAccess.getInstance(null);
        if(comparisonFailed){
            databaseAccess.createStatus("0");
        }else{
            databaseAccess.createStatus(confidenceStr);
        }
    }
}
