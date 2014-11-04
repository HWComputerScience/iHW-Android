package com.ihwapp.android;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.ihwapp.android.model.Curriculum;

public class IHWActivity extends ActionBarActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Curriculum.ctx = this.getApplicationContext();
    }
}
