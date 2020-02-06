package com.example.damian.monitorapp;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.damian.monitorapp.Utils.HourAxisValueFormatter;
import com.example.damian.monitorapp.Utils.MarkerGraph;
import com.example.damian.monitorapp.models.nosql.STATUSDO;
import com.example.damian.monitorapp.requester.DatabaseAccess;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
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
    private Date customDateStart;
    private Date customDateEnd;
    private TextView usingTimeTV;
    private TextView offTimeTV;
    private TextView summaryTimeTV;
    private static final int TWO_MINUTES =  2 * 60 ;

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
        usingTimeTV = (TextView) findViewById(R.id.TimeTVvalue);
        offTimeTV = (TextView) findViewById(R.id.deviceOffTimeTVvalue);
        summaryTimeTV = (TextView) findViewById(R.id.summaryTimeTVvalue);


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
            Log.i(TAG, "refreshGraph(): Today");
            getStatusesTask = new Runnable() {
                @Override
                public void run() {
                    allStatuses = databaseAccess.getStatusFromToday();
            }};
        }else if(selectedItem.contains("Yesterday")) {
            Log.i(TAG, "refreshGraph(): Yesterday");
            getStatusesTask = new Runnable() {
                @Override
                public void run() {
                    allStatuses = databaseAccess.getStatusFromYesterday();
                }
            };
        }else if(selectedItem.contains("Custom Date")){
            Log.i(TAG, "refreshGraph(): Custom Date");
            Toast.makeText(this,"Select your custom Date",Toast.LENGTH_SHORT).show();
            return;
        }else if(selectedItem.contains("Last 7 Days")){
            Log.i(TAG, "refreshGraph(): Selected 7 days");
            getStatusesTask = new Runnable() {
                @Override
                public void run() {
                    allStatuses = databaseAccess.getStatusFromLastWeek();
                }};
        }else{
            Log.i(TAG, "refreshGraph(): Selected Range Date");
            if(customDateEnd != null && customDateStart != null) {
                getStatusesTask = new Runnable() {
                    @Override
                    public void run() {
                        allStatuses = databaseAccess.getStatusFromCustomDate(customDateStart, customDateEnd);
                    }
                };
            }else{
                Log.i(TAG, "refreshGraph(): Selected Range Date wrong date: "+ customDateStart);
                Log.i(TAG, "refreshGraph(): customDateStart: "+ customDateStart);
                Log.i(TAG, "refreshGraph(): customDateEnd: "+ customDateEnd);
                Toast.makeText(this,"Wrong Date Range",Toast.LENGTH_SHORT).show();
                return;
            }
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

        //TODO: Zrobic Logikę związaną ze sprawdzaniem Veryfied = 0!
        //TODO: Aktualny Unic timeStamp jest o godzinę do przodu. Należy cofnąć o godzinę unix time.
        long referenceTimeStamp = Long.parseLong(allStatuses.get(0).getUnixTime()); // Frist Unix_time of the day is reference Time.
        String offTime = "";
        long usingTime = 0L;
        long summaryTime = 0L;
        int time = 0;

        Long previous_stamp = 0L;
        for (int i = 0; i < allStatuses.size(); i++){
            if (allStatuses.get(i).getVerified()) {
                Long new_X = (Long.parseLong(allStatuses.get(i).getUnixTime()) - referenceTimeStamp);
                if (new_X >= previous_stamp + TWO_MINUTES) {
                    dataObjects.add(new MyData((new_X - 60L), time));
                    usingTime += (new_X - previous_stamp);
                }

                if (i != 0) {
                    time = time + 60; // 1 minute
                }
                dataObjects.add(new MyData(new_X, time));
                previous_stamp = new_X;
            }
        }
        summaryTime = usingTime + time;
        offTime = String.valueOf(time);
        offTimeTV.setText(offTime + " sec");
        usingTimeTV.setText((usingTime) + " sec");
        summaryTimeTV.setText((summaryTime) + " sec");

        final List<Entry> entries = new ArrayList<Entry>();
        for (MyData data : dataObjects) {
            entries.add(new Entry(data.getValueX(), data.getValueY()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Label"); // add entries to dataset
        dataSet.setColor(Color.parseColor("#03adfc"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleColor(Color.parseColor("#228ea3"));
        dataSet.setValueTextSize(0f);
        dataSet.setValueTextColor(Color.BLUE);

        LineData lineData = new LineData(dataSet);

        lineChart.setDrawBorders(true);
        lineChart.setBorderColor(Color.BLACK);

        lineChart.setGridBackgroundColor(Color.parseColor("#bbbfbf"));

        lineChart.setDrawGridBackground(true);

        lineChart.setData(lineData);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAvoidFirstLastClipping(false);

        YAxis yAxisLeft = lineChart.getAxisLeft();
        lineChart.getAxisRight().setEnabled(false);

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.set(Calendar.HOUR_OF_DAY, -1); // -1 (Poland)
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long unixTimeStamp = c.getTimeInMillis() / 1000;


        HourAxisValueFormatter X_hourAxisValueFormatter = new HourAxisValueFormatter(referenceTimeStamp);
        xAxis.setValueFormatter(X_hourAxisValueFormatter);

        HourAxisValueFormatter Y_hourAxisValueFormatter = new HourAxisValueFormatter(unixTimeStamp);
        yAxisLeft.setValueFormatter(Y_hourAxisValueFormatter);

        MarkerGraph myMarkerView= new MarkerGraph(getApplicationContext(), R.layout.marker_graph, referenceTimeStamp);
        lineChart.setMarker(myMarkerView);

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

            days_array[days_array.length - 1] = customDate;

            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(GraphActivity.this, android.R.layout.simple_spinner_item, days_array);
            spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
            dateSpinner.setAdapter(spinnerArrayAdapter);
            dateSpinner.setSelection(days_array.length - 1);

            String customDayEndPlusOne = values.get(dayEnd) + "." + values.get(monthEnd + 1) + "." + yearEnd;
            try {
                customDateStart = new SimpleDateFormat("dd.MM.yyyy").parse(customDayStart);
                customDateEnd = new SimpleDateFormat("dd.MM.yyyy").parse(customDayEndPlusOne);
                customDateEnd.setHours(23);
                customDateEnd.setMinutes(59);
                customDateEnd.setSeconds(59);
            } catch (ParseException e) {
                e.printStackTrace();
            }

        }
    };

    class MyData{
        public MyData(long x, int y) {
            this.valueX = x;
            this.valueY = y;
        }

        long valueX;
        int valueY;

        public long getValueX() {
            return valueX;
        }

        public int getValueY() {
            return valueY;
        }

    }
}
