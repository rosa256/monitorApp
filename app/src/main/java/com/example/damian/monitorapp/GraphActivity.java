package com.example.damian.monitorapp;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import butterknife.ButterKnife;
import butterknife.OnClick;


public class GraphActivity extends AppCompatActivity {

    private static final String TAG = "GraphActivity";
    private LineChart lineChart;
    private List<STATUSDO> allStatuses;
    private MaterialIconView refreshButton;
    private Button refreshButtonWrapper;
    final DatabaseAccess databaseAccess = DatabaseAccess.getInstance(null);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        refreshButton = (MaterialIconView) findViewById(R.id.refreshGraphButton);

        lineChart = (LineChart) findViewById(R.id.lineChartId);

        Log.i(TAG, "onPostExecute: Start saving status to DB");

        Description description = new Description();
        description.setText("Wykres czasu");
        lineChart.setDescription(description);

        //lineChart.setVisibleXRangeMaximum(86400F);

        Legend legend = lineChart.getLegend();
        legend.setEnabled(false);

        ButterKnife.bind(this);
        lineChart.invalidate(); // refresh

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

    @OnClick({R.id.refreshGraphButton})
    public void refreshGraph(){
        Toast.makeText(this,"Refreshing",Toast.LENGTH_SHORT).show();

        Runnable getStatusesTask = new Runnable() {
            @Override
            public void run() {
                allStatuses = databaseAccess.getStatusFromToday();
            }};

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
}
