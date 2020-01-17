package com.example.damian.monitorapp;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

public class BusyIndicator {
    private Activity activity;
    private PopupWindow fadePopup;

    public BusyIndicator(Activity activity) {
        this.activity = activity;
    }

    public void dimBackground() {

        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final View layout = inflater.inflate(R.layout.dim_layout,
                (ViewGroup) activity.findViewById(R.id.dim_popup));

        fadePopup = new PopupWindow(layout,
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                false);
        fadePopup.showAtLocation(layout, Gravity.NO_GRAVITY, 0, 0);
    }

    public void unDimBackgorund() {
        fadePopup.dismiss();
    }
}
