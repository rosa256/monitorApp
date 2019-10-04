package com.example.damian.monitorapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.widget.ImageView;
import android.widget.Toast;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;

import com.amazonaws.services.rekognition.model.AgeRange;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.util.IOUtils;
import com.amazonaws.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class AbouteMe extends AppCompatActivity {

    public static final String TAG = "AbouteMe";
    private static final int REQUEST_WRITE_STORAGE_REQUEST_CODE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private AmazonRekognition rekognitionClient;
    private CognitoCachingCredentialsProvider credentialsProvider;
    public static int count = 0;

    private static final String IMAGE_DIRECTORY = "/YourDirectName";
    private Context mContext;
    //private CircleImageView circleImageView;  // imageview
    private int GALLERY = 1, CAMERA = 2;
    int TAKE_PHOTO_CODE = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, ":onCreate");
        setContentView(R.layout.activity_aboute_me);
        requestAppPermissions();
        //rekognitionClient = createClient();
        System.out.println("udalo sie");
    }

    private boolean hasReadPermissions() {
        return (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    private boolean hasWritePermissions() {
        return (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    public void doAction(View view) {


        final String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/picFolder/";
        File newdir = new File(dir);

        if(!newdir.exists()) {
            newdir.mkdir();
            System.out.println("STWORZONO ************************");
        }else{
            System.out.println("NIE STWORZONO ------------------------");
        }

        count++;
        String file = dir+count+".jpg";
        File newfile = new File(file);




        try {
            newfile.createNewFile();
        }
        catch (IOException e)
        { }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
        }
    }


    //    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == TAKE_PHOTO_CODE && resultCode == RESULT_OK) {
//            final Uri imageUri = data.getData();
//            InputStream imageStream = null;
//            try {
//                imageStream = getContentResolver().openInputStream(imageUri);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//            final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
//            String encodedImage = encodeImage(selectedImage);
//            System.out.println("aAAAAAAAAAAAAAAaaaaaaaaaaaaaaa");
//            System.out.println(encodedImage);
//        }
//    }


    private void requestAppPermissions() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        if (hasReadPermissions() && hasWritePermissions()) {
            return;
        }

        ActivityCompat.requestPermissions(this,
                new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_WRITE_STORAGE_REQUEST_CODE); // your request code
    }



    private String encodeImage(Bitmap bm)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG,100,baos);
        byte[] b = baos.toByteArray();
        String encImage = Base64.encodeToString(b, Base64.DEFAULT);

        return encImage;
    }

    public void choosePhotoFromGallary() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY);
    }

    private void takePhotoFromCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA);
    }




        //
//        ByteBuffer sourceImageBytes = null;
//
//        try (InputStream inputStream = new FileInputStream(new File(sourceImage))) {
//            sourceImageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
//        } catch (Exception e) {
//            System.out.println("Failed to load source image " + sourceImage);
//            System.exit(1);
//        }
        //Log.i("AWS", "rekognitionClient:" + rekognitionClient.toString());


    public CognitoCachingCredentialsProvider initAndGetCredentialsProvider() {
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "eu-west-2:e6e456d7-f824-4910-8705-e914330e9663", // Identity pool ID
                Regions.EU_WEST_2 // Region
        );
        if (StringUtils.isBlank(credentialsProvider.getIdentityId())) {
            Toast.makeText(this, "ID: " + credentialsProvider.getIdentityId(),
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Pusto",
                    Toast.LENGTH_LONG).show();
        }
        return credentialsProvider;
    }

    public AmazonRekognition createClient() {
        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setConnectionTimeout(30000);
        clientConfig.setProtocol(Protocol.HTTPS);
        return new AmazonRekognitionClient(initAndGetCredentialsProvider());
    }
}
//        DetectFacesRequest request = new DetectFacesRequest()
//                .withImage(new Image().withBytes(sourceImageBytes))
//                .withAttributes();
//
//        try {
//            DetectFacesResult result = rekognitionClient.detectFaces(request);
//            List<FaceDetail> faceDetails = result.getFaceDetails();
//
//            for (FaceDetail face: faceDetails) {
//                if (request.getAttributes().contains("ALL")) {
//                    AgeRange ageRange = face.getAgeRange();
//                    System.out.println("The detected face is estimated to be between "
//                            + ageRange.getLow().toString() + " and " + ageRange.getHigh().toString()
//                            + " years old.");
//                    System.out.println("Here's the complete set of attributes:");
//                } else { // non-default attributes have null values.
//                    System.out.println("Here's the default set of attributes:");
//                }

//                ObjectMapper objectMapper = new ObjectMapper();
//                System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(face));
//            }
//        } catch (AmazonServiceException e){
//
//        }
//
//    }


        /*ByteBuffer sourceImageBytes=null;
        String sourceImage = "/home/damian/Projects/inzynierka/inzynierka-android/app/Resources/Source/User1/sourceImage.jpg";
        try (InputStream inputStream = new FileInputStream(new File(sourceImage))) {
            sourceImageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
        }
        catch(Exception e)
        {
            System.out.println("Failed to load source image " + sourceImage);
            System.exit(1);
        }
        Log.i("AWS","rekognitionClient:" + rekognitionClient.toString());
        DetectFacesRequest request = new DetectFacesRequest()
                .withImage(new Image().withBytes(sourceImageBytes))
                .withAttributes();
*/
//        try {
//            DetectFacesResult result = rekognitionClient.detectFaces(request);
//            List<FaceDetail> faceDetails = result.getFaceDetails();
//
//            for (FaceDetail face: faceDetails) {
//                if (request.getAttributes().contains("ALL")) {
//                    AgeRange ageRange = face.getAgeRange();
//                    System.out.println("The detected face is estimated to be between "
//                            + ageRange.getLow().toString() + " and " + ageRange.getHigh().toString()
//                            + " years old.");
//                    System.out.println("Here's the complete set of attributes:");
//                } else { // non-default attributes have null values.
//                    System.out.println("Here's the default set of attributes:");
//                }
//
////                ObjectMapper objectMapper = new ObjectMapper();
////                System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(face));
//            }
//}
//        } catch (AmazonServiceException e){
//
//        }

