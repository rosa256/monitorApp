package com.example.damian.monitorapp.utils;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HourLinearXAxisValueFormatter extends ValueFormatter {

    private long referenceTimestamp; // minimum timestamp in your data set
    private DateFormat mDataFormat;
    private Date mDate;
    private long ONE_HOUR = 3600L;

    public HourLinearXAxisValueFormatter(long referenceTimestamp) {
        this.referenceTimestamp = referenceTimestamp - ONE_HOUR;
        this.mDataFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
        this.mDate = new Date();
    }
    
    @Override
    public String getFormattedValue(float value) {
        // convertedTimestamp = originalTimestamp - referenceTimestamp
        long convertedTimestamp = (long) value;

        // Retrieve original timestamp
        long originalTimestamp = referenceTimestamp + convertedTimestamp;

        // Convert timestamp to hour:minute
        return getHour(originalTimestamp);
    }

    private String getHour(long timestamp){
        try{
            mDate.setTime(timestamp*1000);
            return mDataFormat.format(mDate);
        }
        catch(Exception ex){
            return "xx";
        }
    }
}
