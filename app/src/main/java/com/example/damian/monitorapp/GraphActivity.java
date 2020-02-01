package com.example.damian.monitorapp;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.example.damian.monitorapp.Utils.HourAxisValueFormatter;
import com.example.damian.monitorapp.models.nosql.STATUSDO;
import com.example.damian.monitorapp.requester.DatabaseAccess;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import net.steamcrafted.materialiconlib.MaterialIconView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import butterknife.ButterKnife;
import butterknife.OnClick;


public class GraphActivity extends AppCompatActivity {

    private static final String TAG = "GraphActivity";
    private LineChart lineChart;
    private List<STATUSDO> allStatuses;
    private MaterialIconView refreshButton;
    private Spinner dateSpinner;
    private Button refreshButtonWrapper;
    final DatabaseAccess databaseAccess = DatabaseAccess.getInstance(null);

    private final String todayString = new SimpleDateFormat("dd.MM.yyyy").format(new Date());
    private final String yesterdayString = new SimpleDateFormat("dd.MM.yyyy").format(new Date(System.currentTimeMillis()-24*60*60*1000));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        Log.i(TAG, "onCreate: Start Graph activity");

        refreshButton = (MaterialIconView) findViewById(R.id.refreshGraphButton);
        dateSpinner = (Spinner) findViewById(R.id.dateSpinner);
        lineChart = (LineChart) findViewById(R.id.lineChartId);



        String[] days_array= getResources().getStringArray(R.array.days_array);
        days_array[0] = days_array[0].concat(" - "+ todayString); //Today
        days_array[1] = days_array[1].concat(" - "+ yesterdayString); //Yesterday

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, days_array);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
        dateSpinner.setAdapter(spinnerArrayAdapter);

        Description description = new Description();
        description.setText("Wykres czasu");
        lineChart.setDescription(description);

        Legend legend = lineChart.getLegend();
        legend.setEnabled(false);

        ButterKnife.bind(this);
        lineChart.invalidate(); // refresh
    }

    @OnClick({R.id.refreshGraphButton})
    public void refreshGraph(){

        String selectedItem = dateSpinner.getSelectedItem().toString();

        Runnable getStatusesTask;

        if(selectedItem.contains("Today")){
            Log.i(TAG, "refreshGraph: Today");
            getStatusesTask = new Runnable() {
                @Override
                public void run() {
                    allStatuses = databaseAccess.getStatusFromToday();
            }};
        }else if(selectedItem.contains("Yesterday")){
            Log.i(TAG, "refreshGraph: Yesterday");
            getStatusesTask = new Runnable() {
                @Override
                public void run() {
                    allStatuses = databaseAccess.getStatusFromYesterday();
                }};
        }else{
            Log.i(TAG, "refreshGraph: Selected 7 days");
            getStatusesTask = new Runnable() {
                @Override
                public void run() {
                    allStatuses = databaseAccess.getStatusFromToday();
                }};
        }

        Toast.makeText(this,"Refreshing",Toast.LENGTH_SHORT).show();

        Thread statusThread = new Thread(getStatusesTask);
        statusThread.start();

        try {
            statusThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<MyData> dataObjects = new ArrayList<>();

        int count = 1;
        for (STATUSDO reply : allStatuses) {
            dataObjects.add(new MyData(Integer.parseInt(reply.getUnixTime()),count));
            count++;
        }


        final List<Entry> entries = new ArrayList<Entry>();
        for (MyData data : dataObjects) {
            // turn your data into Entry objects
            entries.add(new Entry(data.getValueX(), data.getValueY()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Label"); // add entries to dataset
        dataSet.setColor(Color.parseColor("#03adfc"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setCircleColor(Color.parseColor("#228ea3"));
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLUE);

        LineData lineData = new LineData(dataSet);

        lineChart.setDrawBorders(true);
        lineChart.setBorderColor(Color.BLACK);

        lineChart.setGridBackgroundColor(Color.parseColor("#bbbfbf"));

        lineChart.setDrawGridBackground(true);

        lineChart.setData(lineData);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        xAxis.setAvoidFirstLastClipping(true);

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.set(Calendar.HOUR_OF_DAY, -1); // -1 (Poland)
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long unixTimeStamp = c.getTimeInMillis() / 1000;


        HourAxisValueFormatter hourAxisValueFormatter = new HourAxisValueFormatter(unixTimeStamp);
        xAxis.setValueFormatter(hourAxisValueFormatter);

        lineChart.invalidate();
    }

    class MyData{
        public MyData(int x, int y) {
            this.valueX = x;
            this.valueY = y;
        }

        int valueX;
        int valueY;

        public int getValueX() {
            return valueX;
        }

        public int getValueY() {
            return valueY;
        }

    }
}
