package com.ihwapp.android;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.ihwapp.android.model.Curriculum;

public class LaunchActivity extends IHWActivity {
    private boolean shouldFinish = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Curriculum.yearSetManually()) Curriculum.updateCurrentYear();
    }

    protected void onStart() {
        super.onStart();
        if (shouldFinish) {
            finish();
            return;
        }
        shouldFinish = true;
        Intent i;
        if (Curriculum.isFirstRun()) {
            i = new Intent(this, FirstRunActivity.class);
            Log.d("iHW", "First Run: Going to campus selection screen");
            i.putExtra("skipToCourses", false);
        } else if (Curriculum.shouldPromptForCourses()) {
            i = new Intent(this, FirstRunActivity.class);
            Log.d("iHW", "Skipping to course selection");
            i.putExtra("skipToCourses", true);
        } else {
            Curriculum.reloadCurrentCurriculum();
            i = new Intent(this, ScheduleActivity.class);
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        this.startActivity(i);
    }
}
