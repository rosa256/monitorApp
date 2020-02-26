package com.example.damian.monitorapp.activities;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.damian.monitorapp.R;
import com.example.damian.monitorapp.adapters.SectionsPageAdapter;
import com.example.damian.monitorapp.fragments.BarGraphFragment;
import com.example.damian.monitorapp.fragments.LinearGraphFragment;
import com.example.damian.monitorapp.models.MyDataGraph;
import com.example.damian.monitorapp.models.nosql.STATUSDO;
import com.example.damian.monitorapp.requester.DatabaseAccess;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
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

import butterknife.ButterKnife;
import butterknife.OnClick;

public class GraphTabActivity extends AppCompatActivity {

    private static final String TAG = "GraphTabActivity";
    private SectionsPageAdapter mSectionAdapter;

    private ViewPager mViewPager;

    private List<STATUSDO> allStatuses;
    private Spinner dateSpinner;
    private SmoothDateRangePickerFragment smoothDateRangePickerFragment;
    final DatabaseAccess databaseAccess = DatabaseAccess.getInstance(null);
    private Date customDateStart;
    private Date customDateEnd;
    private TextView usingTimeTV;
    private TextView offTimeTV;
    private TextView summaryTimeTV;
    private RelativeLayout layoutToDim;
    private static final int TWO_MINUTES =  2 * 60 ;

    private final String todayString = new SimpleDateFormat("dd.MM.yyyy").format(new Date());
    private final String yesterdayString = new SimpleDateFormat("dd.MM.yyyy").format(new Date(System.currentTimeMillis()-24*60*60*1000));

    private Map<String, List<BarEntry>> dateCacheBarGrpah;
    private Map<String, List<Entry>> dateCacheLinearGrpah;
    private Map<String, Long> dataCachedReferenceTimeStamp;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph_tab);
        Log.d(TAG, "onCreate: Starting.");


        dateSpinner = (Spinner) findViewById(R.id.dateSpinner);
        usingTimeTV = (TextView) findViewById(R.id.TimeTVvalue);
        offTimeTV = (TextView) findViewById(R.id.deviceOffTimeTVvalue);
        summaryTimeTV = (TextView) findViewById(R.id.summaryTimeTVvalue);
        layoutToDim = (RelativeLayout) findViewById(R.id.dim_graph_layout);

        dateCacheLinearGrpah = new HashMap<>();
        dateCacheBarGrpah = new HashMap<>();
        dataCachedReferenceTimeStamp = new HashMap<>();

        String[] days_array= getResources().getStringArray(R.array.days_array);
        days_array[0] = days_array[0].concat(" - "+ todayString); //Today
        days_array[1] = days_array[1].concat(" - "+ yesterdayString); //Yesterday

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, days_array);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view
        dateSpinner.setAdapter(spinnerArrayAdapter);

        smoothDateRangePickerFragment = SmoothDateRangePickerFragment.newInstance(onDateRangeSetListenerCallback);

        ButterKnife.bind(this);

        mViewPager = (ViewPager) findViewById(R.id.view_pager_id);
        setupViewPager(mViewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    private void setupViewPager(ViewPager viewPager){
        mSectionAdapter = new SectionsPageAdapter(getSupportFragmentManager());
        mSectionAdapter.addFragment(new LinearGraphFragment(), "Linear Graph");
        mSectionAdapter.addFragment(new BarGraphFragment(), "Bar Graph");
        viewPager.setAdapter(mSectionAdapter);
    }


    @OnClick({R.id.refreshGraphButton})
    public void refreshGraph(){
        LinearGraphFragment linearFragment = (LinearGraphFragment) mSectionAdapter.getItem(0);
        BarGraphFragment barGraph = (BarGraphFragment) mSectionAdapter.getItem(1);

        final List<BarEntry> entriesBar = new ArrayList<BarEntry>();
        final List<Entry> entriesLinear = new ArrayList<Entry>();
        String selectedItem = dateSpinner.getSelectedItem().toString();

        if(dateCacheBarGrpah.containsKey(selectedItem) && dateCacheBarGrpah.containsKey(selectedItem) && dataCachedReferenceTimeStamp.containsKey(selectedItem)){
            long cachedRefStamp = dataCachedReferenceTimeStamp.get(selectedItem);
            linearFragment.refreshDataGraph(dateCacheLinearGrpah.get(selectedItem), cachedRefStamp);
            barGraph.refreshDataGraph(dateCacheBarGrpah.get(selectedItem), cachedRefStamp);
            Log.e(TAG, "refreshGraph(): Getting Data From Cache");
            return;
        }

        Toast.makeText(GraphTabActivity.this,"Refreshing",Toast.LENGTH_SHORT).show();
        layoutToDim.setVisibility(View.VISIBLE);


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
            layoutToDim.setVisibility(View.INVISIBLE);
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

        Thread statusThread = new Thread(getStatusesTask);
        statusThread.start();

        try {
            statusThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<MyDataGraph> dataObjects = new ArrayList<>();

        //TODO: Zrobic Logikę związaną ze sprawdzaniem Veryfied = 0!

        if(allStatuses.isEmpty()){
            Toast.makeText(GraphTabActivity.this, "No data in selected range.",Toast.LENGTH_SHORT).show();
            layoutToDim.setVisibility(View.INVISIBLE);
            return;
        }

        long referenceTimeStamp = Long.parseLong(allStatuses.get(0).getUnixTime()); // Frist Unix_time of the day is reference Time.
        String beforeDate = null;
        String actuallDate = null;
        try {
            beforeDate = new SimpleDateFormat("yyyy-MM-dd").format(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(allStatuses.get(0).getFullDate()));
            actuallDate= new SimpleDateFormat("yyyy-MM-dd").format(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(allStatuses.get(0).getFullDate()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String offTime = "";
        long usingTime = 0L;
        long summaryTime = 0L;
        int time = 0;
        Long new_X = 0L;
        float bar_count = 0f;
        int bar_Y_dayValue = 0;

        Long previous_stamp = 0L;
        for (int i = 0; i < allStatuses.size(); i++){
            if (allStatuses.get(i).getVerified()) {
                new_X = (Long.parseLong(allStatuses.get(i).getUnixTime()) - referenceTimeStamp);
                if (new_X >= previous_stamp + TWO_MINUTES) {
                    dataObjects.add(new MyDataGraph((new_X - 60L), time));
                    usingTime += (new_X - previous_stamp);
                }

                try {
                    actuallDate = new SimpleDateFormat("yyyy-MM-dd").format(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(allStatuses.get(i).getFullDate()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if (i != 0) {
                    time = time + 60;
                    bar_Y_dayValue += 60;// 1 minute
                }

                if(!beforeDate.equals(actuallDate) ){
                    Log.e(TAG, "refreshGraph: actuall: " + actuallDate);
                    Log.e(TAG, "refreshGraph: before: " + beforeDate);

                    entriesBar.add(new BarEntry(bar_count, bar_Y_dayValue));
                    bar_count = bar_count + 1f;
                    beforeDate = actuallDate;
                    bar_Y_dayValue = 0;
                }

                dataObjects.add(new MyDataGraph(new_X, time));
                previous_stamp = new_X;
            }
        }

        //For last Day OR When is only One Day
        entriesBar.add(new BarEntry(bar_count, bar_Y_dayValue));

        summaryTime = usingTime + time;
        offTime = String.valueOf(time);
        offTimeTV.setText(getDurationString(time));
        usingTimeTV.setText((getDurationString((int) usingTime)));
        summaryTimeTV.setText(getDurationString((int) summaryTime));

        for (MyDataGraph data : dataObjects) {
            entriesLinear.add(new Entry(data.getValueX(), data.getValueY()));
        }

        linearFragment.refreshDataGraph(entriesLinear, referenceTimeStamp);
        barGraph.refreshDataGraph(entriesBar, referenceTimeStamp);

        if(!selectedItem.equals(selectedItem)) {
            if (!dateCacheLinearGrpah.containsKey(selectedItem)) {
                dateCacheLinearGrpah.put(selectedItem, entriesLinear);
                dataCachedReferenceTimeStamp.put(selectedItem, referenceTimeStamp);
                Log.i(TAG, "refreshGraph(): Putting Linear Graph cached data - on key: " + selectedItem);
            }
            if (!dateCacheBarGrpah.containsKey(selectedItem)) {
                dateCacheBarGrpah.put(selectedItem, entriesBar);
                Log.i(TAG, "refreshGraph(): Putting Bar Graph cached data - on key: " + selectedItem);
            }
        }

        layoutToDim.setVisibility(View.INVISIBLE);
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

                ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(GraphTabActivity.this, android.R.layout.simple_spinner_item, days_array);
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



    private String getDurationString(int seconds) {

        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        return twoDigitString(hours) + " : " + twoDigitString(minutes) + " : " + twoDigitString(seconds);
    }

    private String twoDigitString(int number) {

        if (number == 0) {
            return "00";
        }

        if (number / 10 == 0) {
            return "0" + number;
        }

        return String.valueOf(number);
    }
}