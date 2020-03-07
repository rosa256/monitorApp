package com.example.damian.monitorapp.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.damian.monitorapp.R;
import com.example.damian.monitorapp.utils.DayAxisValueFormatter;
import com.example.damian.monitorapp.utils.HourAxisValueFormatter;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class BarGraphFragment extends Fragment {

    private BarChart barChart;
    int[] colorsMy = new int[]{Color.GRAY};


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.bar_graph_fragment, container,false);
        barChart = (BarChart) view.findViewById(R.id.bar_chart_id);
        barChart.setNoDataTextColor(R.color.colorGreyLight);
        barChart.setNoDataText("Refresh to get data.");

        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
//        Description description = new Description();
//        description.setText("Y - Summary Time, X - Day");
//        description.setTextSize(12f);
//        barChart.setDescription(description);
        barChart.getDescription().setEnabled(false);

        barChart.setMaxVisibleValueCount(60);

        // scaling can now only be done on x- and y-axis separately
        barChart.setPinchZoom(false);

        barChart.setDrawGridBackground(false);
        barChart.getLegend().setEnabled(false);
        barChart.setTouchEnabled(true);

        //barChart.setOnChartValueSelectedListener(this);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); // only intervals of 1 day
        xAxis.setLabelCount(7);

        barChart.getAxisRight().setEnabled(false);
        return view;
    }

    public void refreshDataGraph(List<BarEntry> entries, long referenceTimeStamp) {

        DayAxisValueFormatter xAxisFormatter = new DayAxisValueFormatter(referenceTimeStamp);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(xAxisFormatter);

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.set(Calendar.HOUR_OF_DAY, -1); // -1 (Poland)
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long unixTimeStamp = c.getTimeInMillis() / 1000;

        YAxis yAxisLeft = barChart.getAxisLeft();
        HourAxisValueFormatter Y_hourAxisValueFormatter = new HourAxisValueFormatter(unixTimeStamp);
        yAxisLeft.setValueFormatter(Y_hourAxisValueFormatter);

        BarDataSet barDataSet = new BarDataSet(entries, "My Bar Set");
        barDataSet.setColors(colorsMy);

        BarData barData = new BarData(barDataSet);
        barData.setValueTextSize(12f);
        barData.setValueFormatter(Y_hourAxisValueFormatter);
        barChart.setData(barData);


        barChart.invalidate();
    }
}
