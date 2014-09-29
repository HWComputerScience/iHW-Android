package com.ihwapp.android;

import android.content.Intent;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

/**
 * Created by ethan on 9/22/14.
 */
public class iHWDashClockExtension extends DashClockExtension {
    public static final String PREF_NAME = "pref_name";
    private static final String TAG = "iHW-DashClockExtension";

    @Override
    protected void onInitialize(boolean isReconnect) {
        setUpdateWhenScreenOn(true);
    }

    protected void onUpdateData(int reason) {
        long secsUntil = PeriodView.staticSecsUntil;
        String secs = "" + secsUntil % 60;
        if (secsUntil % 60 < 10) secs = "0" + secs;

        // Publish the extension data update.
        publishUpdate(new ExtensionData()
                .visible(true)
                .icon(R.drawable.notification_small)
                .status(secsUntil / 60 + ":" + secs)
                .expandedTitle("iHW")
                .expandedBody("Next period starts in " + secsUntil / 60 + ":" + secs)
                .contentDescription("This shows the time remaining until your next class.")
                .clickIntent(new Intent(this, LaunchActivity.class)));

        // Hide extension if there's no data.
        if (secsUntil == 0) publishUpdate(null);
    }
}
