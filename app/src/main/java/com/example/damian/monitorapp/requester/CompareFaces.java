package com.example.damian.monitorapp.requester;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.model.CompareFacesMatch;
import com.amazonaws.services.rekognition.model.CompareFacesRequest;
import com.amazonaws.services.rekognition.model.CompareFacesResult;
import com.amazonaws.services.rekognition.model.ComparedFace;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.util.IOUtils;

public class CompareFaces {
    private static final String ACCESS_KEY = "AKIATV7HZTTOYZCKXJMK";
    private static final String SECRET_KEY = "w8HFJnDoscJprRUeHaQBv2dB";

    public static void main(String[] args) throws Exception{
        Float similarityThreshold = 70F;
        String sourceImage = "/home/damian/Projects/inzynierka/inzynierka-android/app/Resources/Source/User1/sourceImage.jpg";
        String targetImage = "/home/damian/Projects/inzynierka/inzynierka-android/app/Resources/Target/User1/targetImage2.jpg";
        ByteBuffer sourceImageBytes=null;
        ByteBuffer targetImageBytes=null;

        AWSCredentials credentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
        AmazonRekognition rekognitionClient = new AmazonRekognitionClient(credentials);

        rekognitionClient.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));

        //        AmazonRekognition rekognitionClient = new AmazonRekognitionClient(new BasicAWSCredentials("AKIATV7HZTTOYZCKXJMK",
//                "QbDIbxFC/cy+PbU+w8HFJnDoscJprRUeHaQBv2dB"));
        //AmazonRekognition rekognitionClient = new AmazonRekognitionClient(new DefaultAWSCredentialsProviderChain());



        //Load source and target images and create input parameters
        try (InputStream inputStream = new FileInputStream(new File(sourceImage))) {
            sourceImageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
        }
        catch(Exception e)
        {
            System.out.println("Failed to load source image " + sourceImage);
            System.exit(1);
        }
        try (InputStream inputStream = new FileInputStream(new File(targetImage))) {
            targetImageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
        }
        catch(Exception e)
        {
            System.out.println("Failed to load target images: " + targetImage);
            System.exit(1);
        }

        Image source=new Image()
                .withBytes(sourceImageBytes);
        Image target=new Image()
                .withBytes(targetImageBytes);

        CompareFacesRequest request = new CompareFacesRequest()
                .withSourceImage(source)
                .withTargetImage(target)
                .withSimilarityThreshold(similarityThreshold);

        // Call operation
        //CompareFacesResult compareFacesResult= rekognitionClient.compareFaces(request);


        // Display results
        //List<CompareFacesMatch> faceDetails = compareFacesResult.getFaceMatches();
//        for (CompareFacesMatch match: faceDetails){
//            ComparedFace face= match.getFace();
//            BoundingBox position = face.getBoundingBox();
//            System.out.println("Face at " + position.getLeft().toString()
//                    + " " + position.getTop()
//                    + " matches with " + match.getSimilarity().toString()
//                    + "% confidence.");
//
//        }
        //List<ComparedFace> uncompared = compareFacesResult.getUnmatchedFaces();



//        System.out.println("There was " + uncompared.size()
//                + " face(s) that did not match");
    }
}