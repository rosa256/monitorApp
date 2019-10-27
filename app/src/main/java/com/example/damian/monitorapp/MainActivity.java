package com.example.damian.monitorapp;

import android.content.Intent;

import android.net.Uri;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.v7.widget.Toolbar;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.example.damian.monitorapp.Utils.ClientAWSFactory;
import com.example.damian.monitorapp.Utils.CognitoSettings;
import com.example.damian.monitorapp.Utils.Constants;
import com.example.damian.monitorapp.Utils.CustomPrivileges;
import com.example.damian.monitorapp.Utils.FileManager;
import com.example.damian.monitorapp.fragments.CameraPreviewFragment;
import com.example.damian.monitorapp.requester.RekognitionRequester;
import com.michaldrabik.tapbarmenulib.TapBarMenu;

public class MainActivity extends AppCompatActivity implements CameraPreviewFragment.OnFragmentInteractionListener{

    private static final String TAG = "MainActivity";
    private TextView usernameEditText;
    private FileManager fileManager;
    private Toolbar toolbar;

    private CognitoCachingCredentialsProvider credentialsProvider;
    private RekognitionRequester rekognitionRequester;
    private AmazonRekognitionClient rekognitionClient;
    private CognitoSettings cognitoSettings;

    @Bind(R.id.tapBarMenu)
    TapBarMenu tapBarMenu;
    private String awsServiceOption = Constants.AWS_DETECT_FACES;


    public MainActivity() { }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        cognitoSettings = CognitoSettings.getInstance();
        cognitoSettings.initContext(MainActivity.this);

        ClientAWSFactory clientAWSFactory = new ClientAWSFactory();

        rekognitionClient = (AmazonRekognitionClient) clientAWSFactory.createRekognitionClient(getApplicationContext());

        CustomPrivileges.setUpPrivileges(this);

        fileManager = FileManager.getInstance();

        usernameEditText = findViewById(R.id.usernameEditText);
        usernameEditText.setText(cognitoSettings.getUserPool().getCurrentUser().getUserId());

        ButterKnife.bind(this);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }


    @OnClick(R.id.fab_send_photo_aws)
    public void onSendPhotoToAWS() {
        fileManager.initFileManager(this.getResources());

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    new RekognitionRequester().doAwsService(rekognitionClient,fileManager.getCurrentTakenPhotoFile(), awsServiceOption, getApplicationContext());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    @OnClick(R.id.tapBarMenu)
    public void onMenuButtonClick() {
        tapBarMenu.toggle();
    }

    public void aboutMe(View view){
        Intent intent = new Intent(this, AbouteMe.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.registerItem:
                Toast.makeText(this,"Register",Toast.LENGTH_SHORT).show();
                intent = new Intent(this, RegisterActivity.class);
                startActivity(intent);
                return true;

            case R.id.loginItem:
                Toast.makeText(this,"Login",Toast.LENGTH_SHORT).show();
                intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                return true;

            case R.id.detectFaces:
                Toast.makeText(this,"Detect Faces Set",Toast.LENGTH_SHORT).show();
                awsServiceOption = Constants.AWS_DETECT_FACES;
                return true;

            case R.id.compareFaces:
                Toast.makeText(this,"Compare Faces Set",Toast.LENGTH_SHORT).show();
                awsServiceOption = Constants.AWS_COMPARE_FACES;
                return true;
            case R.id.logoutItem:
                Toast.makeText(this,"Logout",Toast.LENGTH_SHORT).show();

                intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
        return true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
    }
}
