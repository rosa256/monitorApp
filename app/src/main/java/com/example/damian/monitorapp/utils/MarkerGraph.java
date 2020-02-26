package com.example.damian.monitorapp.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.example.damian.monitorapp.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SuppressLint("ViewConstructor")
public class MarkerGraph extends MarkerView {

    private TextView tvContent;
    private long referenceTimestamp;  // minimum timestamp in your data set
    private DateFormat mDataFormat;
    private Date mDate;

    public MarkerGraph (Context context, int layoutResource, long referenceTimestamp) {
        super(context, layoutResource);
        // this markerview only displays a textview
        tvContent = (TextView) findViewById(R.id.tvContent);
        this.referenceTimestamp = referenceTimestamp;
        this.mDataFormat = new SimpleDateFormat("dd.MM HH:mm:ss", Locale.ENGLISH);
        this.mDate = new Date();
    }

    // callbacks everytime the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        long currentTimestamp = (int)e.getX() + referenceTimestamp;
        String timeDate = getTimedate(currentTimestamp);
        String spentTimeInSecounds = Utils.formatNumber(e.getY(), 0, true);
        Integer spentTimeInt = Integer.parseInt(spentTimeInSecounds.replace(".", ""));
        int hours = spentTimeInt / 3600;
        int minutes = (spentTimeInt % 3600) / 60;
        int seconds = spentTimeInt % 60;
        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        String fullInfo =  timeDate + " - " + timeString;
        tvContent.setText(fullInfo);

        super.refreshContent(e, highlight);

    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2), -getHeight());
    }

    private String getTimedate(long timestamp){

        try{
            mDate.setTime(timestamp*1000);
            return mDataFormat.format(mDate);
        }
        catch(Exception ex){
            return "xx";
        }
    }
}