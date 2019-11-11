package com.example.damian.monitorapp;

import android.content.Intent;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.v7.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.example.damian.monitorapp.Utils.ClientAWSFactory;
import com.example.damian.monitorapp.Utils.CognitoSettings;
import com.example.damian.monitorapp.Utils.Constants;
import com.example.damian.monitorapp.Utils.CustomPrivileges;
import com.example.damian.monitorapp.Utils.FileManager;
import com.example.damian.monitorapp.fragments.ActionMenu;
import com.example.damian.monitorapp.fragments.CameraPreviewFragment;
import com.example.damian.monitorapp.models.UserDO;
import com.example.damian.monitorapp.requester.DatabaseAccess;
import com.example.damian.monitorapp.requester.InitDBConnectionAsync;
import com.example.damian.monitorapp.requester.RekognitionRequester;
import com.michaldrabik.tapbarmenulib.TapBarMenu;

import net.steamcrafted.materialiconlib.MaterialDrawableBuilder;
import net.steamcrafted.materialiconlib.MaterialIconView;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements CameraPreviewFragment.OnFragmentInteractionListener {

    private static final String TAG = "MainActivity";
    private TextView usernameEditText;
    private FileManager fileManager;
    private Toolbar toolbar;

    private CognitoCachingCredentialsProvider credentialsProvider;
    private RekognitionRequester rekognitionRequester;
    private AmazonRekognitionClient rekognitionClient;
    private AmazonDynamoDBClient dynamoDBClient;
    private CognitoSettings cognitoSettings;
    private Button pictureDelayButton;

    static final List<Integer> DELAY_DURATIONS = Arrays.asList(0, 5, 15, 30);
    static final int DEFAULT_DELAY = 5;
    int pictureDelay = DEFAULT_DELAY;
    static final String DELAY_PREFERENCES_KEY = "delay";
    Handler handler = new Handler();
    // assign ID when we start a timed picture, used in makeDecrementTimerFunction callback. If the ID changes, the countdown will stop.
    int currentPictureID = 0;
    int pictureTimer = 0;
    private TextView statusTextField;

    private MaterialIconView playButton;
    private MaterialIconView appStatusIcon;
    private Toolbar mToolbar;
    private boolean onOff = false;

    ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();

    private DatabaseAccess databaseAccess;

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
        dynamoDBClient = (AmazonDynamoDBClient) clientAWSFactory.createDynamoDBClient(getApplicationContext());

        CustomPrivileges.setUpPrivileges(this);

        fileManager = FileManager.getInstance();
        fileManager.initFileManager(this.getResources());

        usernameEditText = findViewById(R.id.usernameEditText);
        usernameEditText.setText(cognitoSettings.getUserPool().getCurrentUser().getUserId());

        playButton = (MaterialIconView) findViewById(R.id.runAppButton);
        appStatusIcon = (MaterialIconView) findViewById(R.id.appStatus);

        pictureDelayButton = (Button) findViewById(R.id.button_delay_photo);
        statusTextField = (TextView) findViewById(R.id.statusTextField);

        //Init DB in thread - Network Connection.
        //TODO: Sprawdzanie czy jest zainicjalizowana Baza.
        InitDBConnectionAsync initDBConnectionAsync = new InitDBConnectionAsync(getApplicationContext(), dynamoDBClient, databaseAccess);
        initDBConnectionAsync.execute();


        //TODO:TO trzeba poprawić. To jest to samo co linijke wyzej.
        //DatabaseAccess.getInstance(getApplicationContext(), dynamoDBClient);
        //TODO: Ewentualnie sprawdzic czy istnieje zdjęcie źródłowe
        CameraPreviewFragment fr = (CameraPreviewFragment) getSupportFragmentManager().findFragmentById(R.id.cameraPreviewFragment);
        if (fr != null) {
            Bitmap myBitmap = BitmapFactory.decodeFile(fileManager.getSourcePhotoFile().getAbsolutePath());
            fr.getImageViewSource().setImageBitmap(myBitmap);
        }
        //TODO--end

        this.readDelayPreference();

        ButterKnife.bind(this);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }


    @OnClick(R.id.fab_send_photo_aws)
    public void onSendPhotoToAWS() {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new RekognitionRequester().doAwsService(rekognitionClient, fileManager.getCurrentTakenPhotoFile(), awsServiceOption, getApplicationContext());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    @OnClick(R.id.runAppButton)
    public void runApp() {
        if (!onOff) { //ON
            onOff = true;
            appStatusIcon.setIcon(MaterialDrawableBuilder.IconValue.EYE);
            appStatusIcon.setColor(Color.rgb(104, 182, 0)); //GREEN
            executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(periodicTask, 0, pictureDelay + 3, TimeUnit.SECONDS);

        } else { //OFF
            onOff = false;
            appStatusIcon.setIcon(MaterialDrawableBuilder.IconValue.EYE_OFF);
            appStatusIcon.setColor(Color.rgb(170, 34, 34)); //RED
            executor.shutdown();
        }
    }

    @OnClick(R.id.tapBarMenu)
    public void onMenuButtonClick() {
        tapBarMenu.toggle();
    }

    void updateDelayButton() {
        if (pictureDelay == 0) {
            pictureDelayButton.setText(getString(R.string.delayButtonLabelNone));
        } else {
            String labelFormat = getString(R.string.delayButtonLabelSecondsFormat);
            pictureDelayButton.setText(String.format(labelFormat, this.pictureDelay));
        }
    }

    public void decrementTimer(final int pictureID) {
        if (pictureID != this.currentPictureID) {
            return;
        }
        boolean takePicture = (pictureTimer == 1);
        --pictureTimer;
        if (takePicture) {
            savePictureNow();
            //playTimerBeep();
        } else if (pictureTimer > 0) {

            updateTimerMessage();

            handler.postDelayed(makeDecrementTimerFunction(pictureID), 1000);
            //if (pictureTimer<3) playTimerBeep();
        }
    }

    void updateTimerMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String messageFormat = getString(R.string.timerCountdownMessageFormat);
                statusTextField.setText(String.format(messageFormat, pictureTimer));
            }
        });
    }

    @OnClick(R.id.fab_delay_photo)
    public void savePicture() {
        if (this.pictureDelay == 0) {
            savePictureNow();
        } else {

            savePictureAfterDelay(this.pictureDelay);
        }
    }

    void savePictureAfterDelay(int delay) {

        pictureTimer = delay;
        updateTimerMessage();
        currentPictureID++;
        handler.postDelayed(makeDecrementTimerFunction(currentPictureID), 1000);
    }

    Runnable makeDecrementTimerFunction(final int pictureID) {
        return new Runnable() {
            public void run() {
                decrementTimer(pictureID);
            }
        };
    }

    public void savePictureNow() {
        ActionMenu cameraPreviewFragment = (ActionMenu) getSupportFragmentManager().findFragmentById(R.id.actionMenuFragment);
        //pictureURIs = new ArrayList<Uri>();
        statusTextField.setText("Taking picture...");

        UserDO userDO = new UserDO();
        userDO.setUserId("maniek2567");
        userDO.setConfidence("80");
        userDO.setDate("11-11-2011");
        userDO.setHour("17:28");
        databaseAccess.createUserCheck(userDO);
        cameraPreviewFragment.onTakePhoneButtonClicked();

        //arManager.getCamera().autoFocus(this);
    }


    @OnClick(R.id.button_delay_photo)
    public void cycleDelay() {
        int index = DELAY_DURATIONS.indexOf(this.pictureDelay);
        if (index < 0) {
            this.pictureDelay = DEFAULT_DELAY;
        } else {
            this.pictureDelay = DELAY_DURATIONS.get((index + 1) % DELAY_DURATIONS.size());
        }
        writeDelayPreference();
        updateDelayButton();

        executor.shutdown();
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(periodicTask, 0, pictureDelay + 3, TimeUnit.SECONDS);

    }

    void writeDelayPreference() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(DELAY_PREFERENCES_KEY, this.pictureDelay);
        editor.commit();
    }

    void readDelayPreference() {
        // reads picture delay from preferences, updates this.pictureDelay and delay button text
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int delay = prefs.getInt(DELAY_PREFERENCES_KEY, -1);
        if (!DELAY_DURATIONS.contains(delay)) {
            delay = DEFAULT_DELAY;
        }
        this.pictureDelay = delay;
        updateDelayButton();
    }

    public void aboutMe(View view) {
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
                Toast.makeText(this, "Register", Toast.LENGTH_SHORT).show();
                intent = new Intent(this, RegisterActivity.class);
                startActivity(intent);
                return true;

            case R.id.loginItem:
                Toast.makeText(this, "Login", Toast.LENGTH_SHORT).show();
                intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                return true;

            case R.id.detectFaces:
                Toast.makeText(this, "Detect Faces Set", Toast.LENGTH_SHORT).show();
                awsServiceOption = Constants.AWS_DETECT_FACES;
                return true;

            case R.id.compareFaces:
                Toast.makeText(this, "Compare Faces Set", Toast.LENGTH_SHORT).show();
                awsServiceOption = Constants.AWS_COMPARE_FACES;
                return true;
            case R.id.logoutItem:
                Toast.makeText(this, "Logout", Toast.LENGTH_SHORT).show();

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


    public class DoComparisonThread extends Thread {
        private static final String TAG = "DoComparisonThread";
        volatile boolean flag = true;

        @Override
        public void run() {
            Log.i(TAG, "begin comparison thread");
            while (flag) {
                savePicture();
                try {
                    this.sleep(pictureDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    Runnable periodicTask = new Runnable() {
        public void run() {
            // Invoke method(s) to do the work
            savePicture();
        }
    };
}

