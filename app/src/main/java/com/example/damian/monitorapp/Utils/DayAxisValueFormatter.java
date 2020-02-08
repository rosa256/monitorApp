package com.example.damian.monitorapp.Utils;

import android.content.Intent;
import android.util.Log;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DayAxisValueFormatter extends ValueFormatter {

    private long referenceTimestamp; // minimum timestamp in your data set
    private DateFormat mDataFormat;
    private Date mDate;
    private Map<Float, String> formattedDateCache; // Need to do Becouse, GetFormmattedValue is buged. Multiple invokes.
    private String day;
    private String month;


    public DayAxisValueFormatter(long referenceTimestamp) {
        this.referenceTimestamp = referenceTimestamp;

        this.mDataFormat = new SimpleDateFormat("dd.MM", Locale.ENGLISH);
        this.mDate = new Date(referenceTimestamp * 1000);
        this.formattedDateCache = new HashMap<>();
    }

    @Override
    public String getFormattedValue(float value) {
        // convertedTimestamp = originalTimestamp - referenceTimestamp
        String formattedValue;

        if (!formattedDateCache.containsKey(value)) {

            day = (String) new SimpleDateFormat("dd").format(mDate);
            month = (String) new SimpleDateFormat("MM").format(mDate);

            formattedValue = day + "." + month;
            formattedDateCache.put(value, formattedValue);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(mDate);
            calendar.add(Calendar.DATE, 1);
            mDate = calendar.getTime();

        } else {
            formattedValue = formattedDateCache.get(value);
        }

        return formattedValue;
    }


}
