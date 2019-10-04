package com.example.damian.monitorapp.requester;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.util.IOUtils;
import com.example.damian.monitorapp.Utils.ClientAWSFactory;

public class CompareFaces {


    public void main() throws Exception{
        Float similarityThreshold = 70F;
        String sourceImage = "/home/damian/Projects/inzynierka/inzynierka-android/app/Resources/Source/User1/sourceImage.jpg";
        String targetImage = "/home/damian/Projects/inzynierka/inzynierka-android/app/Resources/Target/User1/targetImage2.jpg";
        ByteBuffer sourceImageBytes=null;
        ByteBuffer targetImageBytes=null;


        AmazonRekognition rekognitionClient = new ClientAWSFactory().createClient();

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

  /*      Image target=new Image()
                .withBytes(targetImageBytes);

        Image source=new Image()
                .withBytes(sourceImageBytes);
  */      /*CompareFacesRequest request = new CompareFacesRequest()
                .withSourceImage(source)
                .withTargetImage(target)
                .withSimilarityThreshold(similarityThreshold);
*/
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