package com.ihwapp.android;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

/**
 * Created by ethan on 9/22/14.
 */
public class iHWDashClockExtension extends DashClockExtension {
    private static final String TAG = "iHW-DashClockExtension";

    public static final String PREF_NAME = "pref_name";

    protected void onUpdateData(int reason) {
        // Get preference value
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        // Timer time =  TODO: read time from the active countDownTimer in PeriodView or DayFragment and format it

        // Publish the extension data update.
        publishUpdate(new ExtensionData()
                .visible(true)
                .icon(R.drawable.ic_launcher)
                .status("Hello")
                .expandedTitle("Period starts in: " )
                .expandedBody("This is a test extension")
                .contentDescription("This shows the time remaining until your next class."));
    }
}
