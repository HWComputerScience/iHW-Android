package com.ihwapp.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import com.ihwapp.android.model.Curriculum;

public class GuidedCoursesActivity extends CoursesActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Add Your Courses");
        toolbar.inflateMenu(R.menu.courses);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.guided_courses, menu);
        return true;
    }

    public void onBackPressed() {
        if (coursesFragment.getCount() > 0) {
            new AlertDialog.Builder(this).setMessage("Are you sure you want to go back? You will " +
                    "lose the courses you have added.")
                    .setNegativeButton("Go Back", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Curriculum.getCurrentCurriculum().removeAllCourses();
                            Curriculum.getCurrentCurriculum().saveCourses();
                            GuidedCoursesActivity.this.finish();
                        }
                    }).setPositiveButton("Keep Editing", null).show();
        } else {
            super.onBackPressed();
        }
    }
}
