package com.ihwapp.android;

import android.app.IntentService;
import android.content.Intent;

import com.ihwapp.android.model.Curriculum;

public class UpdateService extends IntentService {
    public UpdateService() {
        super("UpdateService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Curriculum.reloadCurrentCurriculum();
    }

}
