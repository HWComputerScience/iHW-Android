package com.ihwapp.android;

import android.app.Activity;
import android.os.Bundle;

import com.ihwapp.android.model.Curriculum;

public class IHWActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Curriculum.ctx = this.getApplicationContext();
    }
}
