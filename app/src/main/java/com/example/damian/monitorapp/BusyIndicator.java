package com.example.damian.monitorapp;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

public class BusyIndicator {
    private Activity activity;
    private PopupWindow fadePopup;
    private Fragment fragmentToDim;
    private RelativeLayout relativeLayout;
    private View viewFromFragment;
    public BusyIndicator(Activity activity) {
        this.activity = activity;
    }

    public BusyIndicator(Fragment fragmentToDim) {
        this.fragmentToDim = fragmentToDim;
        this.viewFromFragment = fragmentToDim.getView();
    }

    public void dimBackground() {
        if(null != activity) {
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            final View layout = inflater.inflate(R.layout.dim_layout,
                    (ViewGroup) activity.findViewById(R.id.dim_popup));

            fadePopup = new PopupWindow(layout,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    false);
            fadePopup.showAtLocation(layout, Gravity.NO_GRAVITY, 0, 0);
        }

        if(null != fragmentToDim){
            relativeLayout = viewFromFragment.findViewById(R.id.dim_fragment_layout);
            fragmentToDim.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    relativeLayout.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    public void unDimBackgorund() {
        if(null != activity) {
            fadePopup.dismiss();
        }

        if(null != fragmentToDim){
            relativeLayout = viewFromFragment.findViewById(R.id.dim_fragment_layout);
            fragmentToDim.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    relativeLayout.setVisibility(View.INVISIBLE);
                }
            });
        }
    }
}
