package com.ihwapp.android;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.ihwapp.android.model.Curriculum;

public class FirstRunActivity extends IHWActivity implements Curriculum.ModelLoadingListener {
    private LinearLayout campusLayout;
    private LinearLayout coursesLayout;
    private ProgressDialog progressDialog;
    private AlertDialog alertDialog;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_firstrun);
        campusLayout = (LinearLayout) this.findViewById(R.id.layout_choose_campus);
        coursesLayout = (LinearLayout) this.findViewById(R.id.layout_get_courses);

        Button msCampus = (Button) this.findViewById(R.id.button_campus_ms);
        msCampus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Curriculum.setCurrentCampus(Constants.CAMPUS_MIDDLE);
                Curriculum.reloadCurrentCurriculum().addModelLoadingListener(FirstRunActivity.this);
                //Log.d("iHW", "Loading curriculum: " + Curriculum.loadCurrentCurriculum(FirstRunActivity.this));
                campusLayout.setVisibility(View.GONE);
                coursesLayout.setVisibility(View.VISIBLE);
                FirstRunActivity.this.getIntent().putExtra("skipToCourses", true);
            }
        });

        Button usCampus = (Button) this.findViewById(R.id.button_campus_us);
        usCampus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Curriculum.setCurrentCampus(Constants.CAMPUS_UPPER);
                Curriculum.reloadCurrentCurriculum().addModelLoadingListener(FirstRunActivity.this);
                //Log.d("iHW", "Loading curriculum: " + Curriculum.loadCurrentCurriculum(FirstRunActivity.this));
                campusLayout.setVisibility(View.GONE);
                coursesLayout.setVisibility(View.VISIBLE);
                FirstRunActivity.this.getIntent().putExtra("skipToCourses", true);
            }
        });

        Button downloadButton = (Button) this.findViewById(R.id.button_download);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(FirstRunActivity.this, DownloadScheduleActivity.class);
                i.putExtra("firstRun", true);
                startActivity(i);
            }
        });

        Button editCoursesButton = (Button) this.findViewById(R.id.button_add_manually);
        editCoursesButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(FirstRunActivity.this, GuidedCoursesActivity.class);
                i.putExtra("firstRun", true);
                startActivity(i);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("iHW", "FirstRunActivity onResume: " + Curriculum.getCurrentYear() + " " + Curriculum.getCurrentCampus());
        if (this.getIntent().getBooleanExtra("skipToCourses", false)) {
            Curriculum.reloadCurrentCurriculum().addModelLoadingListener(FirstRunActivity.this);
            campusLayout.setVisibility(View.GONE);
            coursesLayout.setVisibility(View.VISIBLE);
        }
    }

    public void onBackPressed() {
        if (campusLayout.getVisibility() == View.VISIBLE) {
            super.onBackPressed();
        } else {
            campusLayout.setVisibility(View.VISIBLE);
            coursesLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onProgressUpdate(int progress) {
        if (progress == 0) {
            progressDialog = new ProgressDialog(this, R.style.PopupTheme);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Loading...");
            progressDialog.show();
        }
    }

    @Override
    public void onFinishedLoading(Curriculum c) {
        if (progressDialog != null && !this.isFinishing()) progressDialog.dismiss();
        progressDialog = null;
    }

    @Override
    public void onLoadFailed(Curriculum c) {
        if (progressDialog != null) progressDialog.dismiss();
        progressDialog = null;
        if (this.isFinishing()) return;
        this.alertDialog = new AlertDialog.Builder(this, R.style.PopupTheme).setMessage("The schedule for the campus and year you selected is not available. Check your internet connection and try again, or choose a different campus or year.")
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Curriculum.reloadCurrentCurriculum().addModelLoadingListener(FirstRunActivity.this);
                        alertDialog = null;
                    }
                })
                .setNegativeButton("Choose Year", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(FirstRunActivity.this, PreferencesActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.putExtra("showYearOptions", true);
                        startActivity(i);
                    }
                }).setCancelable(false).create();
        alertDialog.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (progressDialog != null) progressDialog.dismiss();
        progressDialog = null;
        if (alertDialog != null) alertDialog.dismiss();
        alertDialog = null;
    }
}
