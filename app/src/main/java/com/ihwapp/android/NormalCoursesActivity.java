package com.ihwapp.android;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

public class NormalCoursesActivity extends CoursesActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_course);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle("Edit Courses");
        toolbar.inflateMenu(R.menu.courses);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.courses, menu);
		return true;
	}
}
