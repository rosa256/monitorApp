package com.example.damian.monitorapp;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
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

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.example.damian.monitorapp.AWSChangable.utils.AppHelper;
import com.example.damian.monitorapp.Utils.ClientAWSFactory;
import com.example.damian.monitorapp.Utils.CognitoSettings;
import com.example.damian.monitorapp.Utils.Constants;
import com.example.damian.monitorapp.Utils.FileManager;
import com.example.damian.monitorapp.fragments.CameraPreviewFragment;
import com.example.damian.monitorapp.requester.DatabaseAccess;
import com.example.damian.monitorapp.requester.RekognitionRequester;
import com.michaldrabik.tapbarmenulib.TapBarMenu;

import net.steamcrafted.materialiconlib.MaterialDrawableBuilder;
import net.steamcrafted.materialiconlib.MaterialIconView;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraPreviewFragment.OnFragmentInteractionListener{

    private static final String TAG = "MainActivity";
    private TextView usernameEditText;
    private FileManager fileManager;
    private Toolbar toolbar;

    private CognitoCachingCredentialsProvider credentialsProvider;
    private AmazonRekognitionClient rekognitionClient;
    private AmazonDynamoDBClient dynamoDBClient;
    private CognitoSettings cognitoSettings;
    private Button pictureDelayButton;

    static final List<Integer> DELAY_DURATIONS = Arrays.asList(60,60);
    static final int DEFAULT_DELAY = 5;
    int pictureDelay = DEFAULT_DELAY;
    static final String DELAY_PREFERENCES_KEY = "delay";
    private static final String SERVICE_STATE_KEY = "service_state";
    // assign ID when we start awsconfiguration timed picture, used in makeDecrementTimerFunction callback. If the ID changes, the countdown will stop.

    private TextView statusTextField;
    private FloatingActionButton sendPhotoAwsButton;
    private MaterialIconView runServiceButton;
    private MaterialIconView stopServiceButton;

    private MaterialIconView appStatusIcon;

    private TimeLevelReceiver timeLevelReceiver;

    private DatabaseAccess databaseAccess;

    @Bind(R.id.tapBarMenu)
    TapBarMenu tapBarMenu;
    private String awsServiceOption = Constants.AWS_COMPARE_FACES;

    Intent serviceIntent;
    private CameraPreviewFragment cameraPreviewFragment;
    private BusyIndicator busyIndicator;
    private IntentFilter mIntentFilter;

    public MainActivity() { }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: Invoked");
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        cognitoSettings = CognitoSettings.getInstance();
        cognitoSettings.initContext(MainActivity.this);

        ClientAWSFactory clientAWSFactory = new ClientAWSFactory();

        rekognitionClient = (AmazonRekognitionClient) clientAWSFactory.createRekognitionClient(getApplicationContext());
        dynamoDBClient = (AmazonDynamoDBClient) clientAWSFactory.createDynamoDBClient(getApplicationContext());

        fileManager = FileManager.getInstance();
        fileManager.initFileManager(this.getResources());

        usernameEditText = findViewById(R.id.usernameEditText);
        usernameEditText.setText(AppHelper.getPool().getCurrentUser().getUserId());

        appStatusIcon = (MaterialIconView) findViewById(R.id.appStatus);

        sendPhotoAwsButton = (FloatingActionButton) findViewById(R.id.fab_send_photo_aws);
        runServiceButton = (MaterialIconView) findViewById(R.id.runServiceButton);
        stopServiceButton = (MaterialIconView) findViewById(R.id.stopServiceButton);

        pictureDelayButton = (Button) findViewById(R.id.button_delay_photo);
        statusTextField = (TextView) findViewById(R.id.statusTextField);

        //boolean b =IdentityManager.getDefaultIdentityManager().areCredentialsExpired();
        DatabaseAccess.getInstance(this);
        timeLevelReceiver = new MainActivity.TimeLevelReceiver();

        Toast.makeText(getApplicationContext(), "Service State: " + readServiceStatePreference(), Toast.LENGTH_SHORT ).show();

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("com.example.damian.monitorApp.GET_TIME");
        getApplicationContext().registerReceiver(timeLevelReceiver, mIntentFilter);

        //TODO: Ewentualnie sprawdzic czy istnieje zdjęcie źródłowe
        cameraPreviewFragment = (CameraPreviewFragment) getSupportFragmentManager().findFragmentById(R.id.cameraPreviewFragment);

        //TODO--end

        this.readDelayPreference();
        ButterKnife.bind(this);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        busyIndicator = new BusyIndicator(cameraPreviewFragment);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("firstTimeRun", false)) {
            enableAutoStart();

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("firstTimeRun", true);
            editor.commit();
        }

    }

    @OnClick(R.id.fab_send_photo_aws)
    public void onSendPhotoToAWS() {
        sendPhotoAwsButton.setEnabled(false);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new RekognitionRequester().doAwsService(rekognitionClient, fileManager.getCurrentTakenPhotoFile(), awsServiceOption, MainActivity.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private class TimeLevelReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.example.damian.monitorApp.GET_TIME")) {
                String timeToShow = intent.getStringExtra("LEVEL_TIME");
                updateTimerMessage(timeToShow);
            }
        }
    }

    @OnClick(R.id.runServiceButton)
    public void runService(){
        if(!fileManager.checkIfExistsSourcePhotoFile()){
            Intent sourceImageIntent = new Intent(MainActivity.this, SourcePhotoActivity.class);
            startActivity(sourceImageIntent);
            return;
        }
        runServiceButton.setEnabled(false);
        stopServiceButton.setEnabled(true);

        ConnectivityManager connectivityManager =
                (ConnectivityManager)MainActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isInternetConnection = connectivityManager.getActiveNetworkInfo() != null &&
                connectivityManager.getActiveNetworkInfo().isConnected();
        if(!isInternetConnection){
            Log.i(TAG, "runService(): No Internet Connection");
            Toast.makeText(MainActivity.this, "No internet connection!", Toast.LENGTH_LONG).show();
            return;
        }

        serviceIntent = new Intent(this, CameraService.class);
        serviceIntent.setPackage("com.example.damian.monitorapp");
        serviceIntent.setAction(CameraService.ACTION_START);

        //VisualChanges - BEGIN
        appStatusIcon.setIcon(MaterialDrawableBuilder.IconValue.EYE);
        appStatusIcon.setColor(Color.rgb(104, 182, 0)); //GREEN
        //VisualChanges - END

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        busyIndicator = new BusyIndicator(cameraPreviewFragment);
        busyIndicator.dimBackground();

    }

    @OnClick(R.id.stopServiceButton)
    public void stopMyService(){
        stopServiceButton.setEnabled(false);
        runServiceButton.setEnabled(true);
        serviceIntent = new Intent(this, CameraService.class);
        serviceIntent.setPackage("com.example.damian.monitorapp");
        serviceIntent.setAction(CameraService.ACTION_STOP);
        statusTextField.setText("Time Left");

        if(serviceIntent != null) {
            //VisualChanges
            appStatusIcon.setIcon(MaterialDrawableBuilder.IconValue.EYE_OFF);
            appStatusIcon.setColor(Color.rgb(170, 34, 34)); //RED
            //VisualChanges

            startService(serviceIntent);

            busyIndicator.unDimBackgorund();
            sendPhotoAwsButton.setEnabled(true);
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

    @OnClick(R.id.chart_icon_id)
    public void onChartButtonClick(){
        Intent intent = new Intent(this, GraphActivity.class);
        startActivity(intent);
    }


    void updateTimerMessage(final String timeToDisplay) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String messageFormat = getString(R.string.timerCountdownMessageFormat);
                statusTextField.setText(String.format(messageFormat, timeToDisplay));
            }
        });
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


    boolean readServiceStatePreference() {
        // reads picture delay from preferences, updates this.pictureDelay and delay button text
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        return prefs.getBoolean(SERVICE_STATE_KEY, false);
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
        final Intent[] intent = new Intent[1];
        switch (item.getItemId()) {
            case R.id.registerItem:
                Toast.makeText(this, "Register", Toast.LENGTH_SHORT).show();
                intent[0] = new Intent(this, RegisterActivity.class);
                startActivity(intent[0]);
                return true;

            case R.id.loginItem:
                Toast.makeText(this, "Login", Toast.LENGTH_SHORT).show();
                intent[0] = new Intent(this, LoginActivity.class);
                startActivity(intent[0]);
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
                cognitoSettings.getUserPool().getCurrentUser().globalSignOutInBackground(new GenericHandler() {
                    @Override
                    public void onSuccess() {
                        cognitoSettings.getUserPool().getCurrentUser().signOut();
                        cognitoSettings.getCredentialsProvider().clear();
                        cognitoSettings.getCredentialsProvider().clearCredentials();
                        Toast.makeText(MainActivity.this, "Success Logout", Toast.LENGTH_SHORT).show();
                        intent[0] = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent[0]);
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        Toast.makeText(MainActivity.this,"Cannot log out.", Toast.LENGTH_SHORT).show();
                    }
                });

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
        if(readServiceStatePreference()) {
            resumeUIState();
        }
        registerReceiver(timeLevelReceiver, mIntentFilter);

        //check Source Photo
        if (cameraPreviewFragment != null) {
            if(fileManager.checkIfExistsSourcePhotoFile()){
                Bitmap myBitmap = BitmapFactory.decodeFile(fileManager.getSourcePhotoFile().getAbsolutePath());
                cameraPreviewFragment.getImageViewSource().setImageBitmap(myBitmap);
            }else{
                cameraPreviewFragment.getImageViewSource().setImageBitmap(null);
            }
        }

        Log.i(TAG, "onResume: Invoked");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(timeLevelReceiver != null) {
            unregisterReceiver(timeLevelReceiver);
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
    }


    void resumeUIState(){
        //VisualChanges - BEGIN
        appStatusIcon.setIcon(MaterialDrawableBuilder.IconValue.EYE);
        appStatusIcon.setColor(Color.rgb(104, 182, 0)); //GREEN
        //VisualChanges - END
        busyIndicator.dimBackground();
    }

    private void enableAutoStart() {
        if (Build.BRAND.equalsIgnoreCase("xiaomi")) {
            new MaterialDialog.Builder(MainActivity.this).title("Enable AutoStart")
                    .content("Please allow monitorApp to always run in the background \n" +
                            "Find monitorApp and Deselect \" Manage automatically \" \n" +
                            "Else our services can't be accessed.")
                    .theme(Theme.LIGHT)
                    .positiveText("ALLOW")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("com.miui.securitycenter",
                                    "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                            startActivity(intent);
                        }
                    })
                    .show();

        }else if (Build.BRAND.equalsIgnoreCase("huawei")) {
            new MaterialDialog.Builder(MainActivity.this).title("Allow Background work.")
                    .content(
                            "Find monitorApp and Deselect \"Manage automatically\" \n\n" +
                            "Else our services can't be accessed.")
                    .theme(Theme.LIGHT)
                    .positiveText("ALLOW")
                    .positiveColor(Color.GRAY)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("com.huawei.systemmanager",
                                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"));
                            startActivity(intent);
                        }
                    })
                    .show();
        } else if (Build.BRAND.equalsIgnoreCase("Letv")) {
            new MaterialDialog.Builder(MainActivity.this).title("Enable AutoStart")
                    .content(
                            "Please allow monitorApp to always run in the background \n" +
                            "Find monitorApp and Deselect \" Manage automatically \" \n" +
                            "Else our services can't be accessed.")
                    .theme(Theme.LIGHT)
                    .positiveText("ALLOW")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("com.letv.android.letvsafe",
                                    "com.letv.android.letvsafe.AutobootManageActivity"));
                            startActivity(intent);
                        }
                    })
                    .show();
        } else if (Build.BRAND.equalsIgnoreCase("Honor")) {
            new MaterialDialog.Builder(MainActivity.this).title("Enable AutoStart")
                    .content(
                            "Please allow monitorApp to always run in the background \n" +
                            "Find monitorApp and Deselect \" Manage automatically \" \n" +
                            "Else our services can't be accessed.")
                    .theme(Theme.LIGHT)
                    .positiveText("ALLOW")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("com.huawei.systemmanager",
                                    "com.huawei.systemmanager.optimize.process.ProtectActivity"));
                            startActivity(intent);
                        }
                    })
                    .show();
        } else if (Build.MANUFACTURER.equalsIgnoreCase("oppo")) {
            new MaterialDialog.Builder(MainActivity.this).title("Enable AutoStart")
                    .content(
                            "Please allow monitorApp to always run in the background \n" +
                            "Find monitorApp and Deselect \" Manage automatically \" \n" +
                            "Else our services can't be accessed.")
                    .theme(Theme.LIGHT)
                    .positiveText("ALLOW")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            try {
                                Intent intent = new Intent();
                                intent.setClassName("com.coloros.safecenter",
                                        "com.coloros.safecenter.permission.startup.StartupAppListActivity");
                                startActivity(intent);
                            } catch (Exception e) {
                                try {
                                    Intent intent = new Intent();
                                    intent.setClassName("com.oppo.safe",
                                            "com.oppo.safe.permission.startup.StartupAppListActivity");
                                    startActivity(intent);
                                } catch (Exception ex) {
                                    try {
                                        Intent intent = new Intent();
                                        intent.setClassName("com.coloros.safecenter",
                                                "com.coloros.safecenter.startupapp.StartupAppListActivity");
                                        startActivity(intent);
                                    } catch (Exception exx) {
                                    }
                                }
                            }
                        }
                    })
                    .show();
        } else if (Build.MANUFACTURER.contains("vivo")) {
            new MaterialDialog.Builder(MainActivity.this).title("Enable AutoStart")
                    .content(
                            "Please allow monitorApp to always run in the background.Our app runs in background else our services can't be accesed.")
                    .theme(Theme.LIGHT)
                    .positiveText("ALLOW")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            try {
                                Intent intent = new Intent();
                                intent.setComponent(new ComponentName("com.iqoo.secure",
                                        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"));
                                startActivity(intent);
                            } catch (Exception e) {
                                try {
                                    Intent intent = new Intent();
                                    intent.setComponent(new ComponentName("com.vivo.permissionmanager",
                                            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
                                    startActivity(intent);
                                } catch (Exception ex) {
                                    try {
                                        Intent intent = new Intent();
                                        intent.setClassName("com.iqoo.secure",
                                                "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager");
                                        startActivity(intent);
                                    } catch (Exception exx) {
                                        ex.printStackTrace();
                                    }
                                }
                            }
                        }
                    })
                    .show();
        }
    }
}

