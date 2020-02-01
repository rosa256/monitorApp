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
import com.leavjenn.smoothdaterangepicker.date.SmoothDateRangePickerFragment;

import net.steamcrafted.materialiconlib.MaterialIconView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private SmoothDateRangePickerFragment smoothDateRangePickerFragment;
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

        smoothDateRangePickerFragment = SmoothDateRangePickerFragment.newInstance(onDateRangeSetListenerCallback);

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
        }else if(selectedItem.contains("Yesterday")) {
            Log.i(TAG, "refreshGraph: Yesterday");
            getStatusesTask = new Runnable() {
                @Override
                public void run() {
                    allStatuses = databaseAccess.getStatusFromYesterday();
                }
            };
        }else if(selectedItem.contains("Custom Date")){
            Toast.makeText(this,"Select your custom Date",Toast.LENGTH_SHORT).show();
            return;
        }else{
            Log.i(TAG, "refreshGraph: Selected 7 days");
            getStatusesTask = new Runnable() {
                @Override
                public void run() {
                    allStatuses = databaseAccess.getStatusFromLastWeek();
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

    @OnClick(R.id.calendarIcon)
    public void selectDateRange(){

        smoothDateRangePickerFragment.setThemeDark(true);
        smoothDateRangePickerFragment.setMinDate(null);
        smoothDateRangePickerFragment.setMaxDate(Calendar.getInstance());
        smoothDateRangePickerFragment.show(getFragmentManager(), "smoothDateRangePicker");
    }


    private SmoothDateRangePickerFragment.OnDateRangeSetListener onDateRangeSetListenerCallback =
            new SmoothDateRangePickerFragment.OnDateRangeSetListener() {
        @Override
        public void onDateRangeSet(SmoothDateRangePickerFragment view,
        int yearStart, int monthStart,
        int dayStart, int yearEnd,
        int monthEnd, int dayEnd) {
            //Month is stared from 0 to 11....

            Map<Integer, String> values = new HashMap<>();
            values.put(monthStart + 1,String.valueOf(monthStart + 1)); values.put(monthEnd + 1,String.valueOf(monthEnd + 1));
            values.put(dayStart, String.valueOf(dayStart)); values.put(dayEnd, String.valueOf(dayEnd));
            for (Map.Entry<Integer, String> value: values.entrySet()){
                if(value.getKey() < 10)
                    values.put(value.getKey(), "0" + (value.getKey())) ;
            }
            String[] days_array= getResources().getStringArray(R.array.days_array);

            days_array[0] = days_array[0].concat(" - "+ todayString); //Today
            days_array[1] = days_array[1].concat(" - "+ yesterdayString); //Yesterday

            String customDate = "";
            String customDayStart = values.get(dayStart) + "." + values.get(monthStart + 1) + "." + yearStart;
            String customDayEnd = values.get(dayEnd) + "." + values.get(monthEnd + 1) + "." + yearEnd;
            customDate = customDayStart + " - " + customDayEnd;

                            System.out.println(customDayStart);
                System.out.println(customDayEnd);
                System.out.println(new Date().toString());

            //---------------------------- Date Validation -------------------
//            try {
//                System.out.println(customDayStart);
//                System.out.println(customDayEnd);
//                Date customDateStart = new SimpleDateFormat("dd.MM.yyyy").parse(customDayStart);
//                Date customDateEnd = new SimpleDateFormat("dd.MM.yyyy").parse(customDayEnd);
//                System.out.println(customDateStart.toString());
//                System.out.println(customDateEnd.toString());
//                System.out.println(new Date().toString());
//
//                if( customDateStart.after(new Date()) || customDateEnd.after(new Date())){
//                    Toast.makeText(GraphActivity.this, "Wrong Date Range", Toast.LENGTH_SHORT).show();
//                    return;
//                }else{
//                    System.out.println("INNNNNYYYYYY");
//                }
//
//            } catch (ParseException e) {
//                e.printStackTrace();
//            }


            days_array[days_array.length - 1] = customDate;

            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(GraphActivity.this, android.R.layout.simple_spinner_item, days_array);
            spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
            dateSpinner.setAdapter(spinnerArrayAdapter);
            dateSpinner.setSelection(days_array.length - 1);

        }
    };

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
