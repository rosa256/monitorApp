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
import com.example.damian.monitorapp.Utils.HourAxisValueFormatter;
import com.example.damian.monitorapp.Utils.HourLinearXAxisValueFormatter;
import com.example.damian.monitorapp.Utils.MarkerGraph;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;


public class LinearGraphFragment extends Fragment {
    public static final String TAG = "LinearGraphFragment";
    private LineChart lineChart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.linear_graph_fragment, container,false);

        lineChart = (LineChart) view.findViewById(R.id.linear_chart_id);
        lineChart.setNoDataTextColor(R.color.colorGreyLight);
        lineChart.setNoDataText("Refresh to get data.");
        Description description = new Description();
        description.setText("Wykres czasu");
        lineChart.setDescription(description);

        Legend legend = lineChart.getLegend();
        legend.setEnabled(false);
        lineChart.setNoDataText("Refresh to get data.");
        lineChart.setNoDataTextColor(R.color.colorGreyLight);
        lineChart.setDrawBorders(true);
        lineChart.setBorderColor(Color.BLACK);

        lineChart.setGridBackgroundColor(Color.parseColor("#bbbfbf"));

        lineChart.setDrawGridBackground(true);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAvoidFirstLastClipping(false);

        return view;
    }

    public void refreshDataGraph(List<Entry> entries, long referenceTimeStamp){


        LineDataSet dataSet = new LineDataSet(entries, "Label"); // add entries to dataset
        dataSet.setColor(Color.parseColor("#03adfc"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleColor(Color.parseColor("#228ea3"));
        dataSet.setValueTextSize(0f);
        dataSet.setValueTextColor(Color.BLUE);
        dataSet.setHighLightColor(Color.GRAY);
        dataSet.setHighlightLineWidth(2f);

        LineData lineData = new LineData(dataSet);

        lineChart.setData(lineData);

        YAxis yAxisLeft = lineChart.getAxisLeft();
        lineChart.getAxisRight().setEnabled(false);

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.set(Calendar.HOUR_OF_DAY, -1); // -1 (Poland)
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long unixTimeStamp = c.getTimeInMillis() / 1000;


        HourLinearXAxisValueFormatter X_hourAxisValueFormatter = new HourLinearXAxisValueFormatter(referenceTimeStamp);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(X_hourAxisValueFormatter);

        HourAxisValueFormatter Y_hourAxisValueFormatter = new HourAxisValueFormatter(unixTimeStamp);
        yAxisLeft.setValueFormatter(Y_hourAxisValueFormatter);

        MarkerGraph myMarkerView= new MarkerGraph(getContext(), R.layout.marker_graph, referenceTimeStamp);
        lineChart.setMarker(myMarkerView);


        lineChart.invalidate();
    }
}
