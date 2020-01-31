package com.example.damian.monitorapp;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.damian.monitorapp.Utils.HourAxisValueFormatter;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class GraphActivity extends AppCompatActivity {

    LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        lineChart = (LineChart) findViewById(R.id.lineChartId);

        List<MyData> dataObjects = new ArrayList<>();
        MyData data1 = new MyData(0,0);
        MyData data2 = new MyData(12600,2);
        MyData data3 = new MyData(25200,3);
        MyData data4 = new MyData(61200,5);
        MyData data5 = new MyData(72400,6);
        MyData data6 = new MyData(84800,8);
        MyData data7 = new MyData(86400,10);

        //TODO: IMPORTANT! Values added to dataObject have to be in ascending order!!!!
        dataObjects.add(data1);
        dataObjects.add(data2);
        dataObjects.add(data3);
        dataObjects.add(data4);
        dataObjects.add(data5);
        dataObjects.add(data6);
        dataObjects.add(data7);


        List<Entry> entries = new ArrayList<Entry>();
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

        Description description = new Description();
        description.setText("Wykres czasu");
        lineChart.setDescription(description);

        //lineChart.setVisibleXRangeMaximum(86400F);

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


        Legend legend = lineChart.getLegend();
        legend.setEnabled(false);

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
}
