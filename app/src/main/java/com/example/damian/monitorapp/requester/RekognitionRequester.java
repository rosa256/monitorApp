package com.example.damian.monitorapp.requester;


import android.util.Log;

import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.Attribute;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.util.IOUtils;
import com.example.damian.monitorapp.Utils.Constants;
import com.example.damian.monitorapp.Utils.FileManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class RekognitionRequester {

    private static final String TAG = "RekognitionRequester";

    public static void doAwsService(AmazonRekognitionClient rekognitionClient,File currentTakenPhotoFile, String awsServiceOption){
        Log.i(TAG, "doAwsService() - Invoke with awsServiceOption: "+awsServiceOption);
        if(Constants.AWS_DETECT_FACES.equals(awsServiceOption)){
            doAwsFaceDetection(rekognitionClient, currentTakenPhotoFile);
        }else if(Constants.AWS_COMPARE_FACES.equals(awsServiceOption)){
            doAwsCompareFaces(rekognitionClient, currentTakenPhotoFile);
        }
    }

    private static void doAwsFaceDetection(AmazonRekognitionClient rekognitionClient, File currentTakenPhotoFile) {
        Log.i(TAG,"doAwsDetectFaces() - Started");
        ByteBuffer sourceImageBytes = null;

        try (InputStream inputStream = new FileInputStream(currentTakenPhotoFile)) {
            sourceImageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
        } catch(Exception e){
            Log.i(TAG,"Failed to load current taken photo file" + currentTakenPhotoFile.getPath());
            System.exit(1);
        }

        Image source=new Image()
                .withBytes(sourceImageBytes);

        DetectFacesRequest request = new DetectFacesRequest()
                .withImage(source)
                .withAttributes(Attribute.ALL.toString());

        Log.i(TAG,"doAwsFaceDetection() - invoke DetectFacesAsync");
        new DetectFacesAsync(rekognitionClient, request).execute();
        Log.i(TAG,"doAwsDetectFaces() - Finished");
    }

    private static void doAwsCompareFaces(AmazonRekognitionClient rekognitionClient, File currentTakenPhotoFile) {
        Log.i(TAG,"doAwsCompareFaces() - Started");

        ByteBuffer sourceImageBytes = null;
        ByteBuffer targetImageBytes = null;

        //Load source and target images and create input parameters
        try (InputStream inputStream = new FileInputStream(currentTakenPhotoFile)) {
            targetImageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
        }catch(Exception e){
            Log.i(TAG,"Failed to load current taken photo file" + currentTakenPhotoFile.getPath());
            System.exit(1);
        }

        try(InputStream inputStream = new FileInputStream(FileManager.getInstance().getSourceFileImage())){
            sourceImageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
        }catch (Exception e){
            Log.i(TAG,"Failed to load source image file " + currentTakenPhotoFile.getPath());
            System.exit(1);
        }

        Image source = new Image()
                .withBytes(sourceImageBytes);

        Image target = new Image()
                .withBytes(targetImageBytes);


        Log.i(TAG,"doAwsCompareFaces() - invoke CompareFacesAsync");
        new CompareFacesAsync(rekognitionClient, source, target).execute();

        Log.i(TAG,"doAwsCompareFaces() - Finished");
    }

}
