package com.ihwapp.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.ihwapp.android.model.Curriculum;

import java.util.ArrayList;

public class PreferencesActivity extends IHWActivity implements ListAdapter {
    private ListView mainList;
    private ArrayList<String> titles;
    private ArrayList<String> subtitles;
    private int newCampus;
    private int newYear;
    private TextView yearText;
    private CheckBox notificationBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferences);

        titles = new ArrayList<String>(5);
        subtitles = new ArrayList<String>(5);
        titles.add(getString(R.string.pref_notifications));
        subtitles.add(getString(R.string.pref_notifications_desc));
        titles.add(getString(R.string.pref_change));
        subtitles.add(getString(R.string.pref_change_desc));
        titles.add(getString(R.string.pref_redownload));
        subtitles.add(getString(R.string.pref_redownload_desc));
        titles.add(getString(R.string.pref_disclaimer));
        subtitles.add(getString(R.string.pref_disclaimer_desc));
        titles.add(getString(R.string.pref_about));
        subtitles.add("");
        mainList = (ListView) findViewById(R.id.preference_list);
        mainList.setDivider(this.getResources().getDrawable(android.R.drawable.divider_horizontal_bright));
        mainList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) {
                    toggleNotifications(!notificationBox.isChecked());
                } else if (i == 1) {
                    showYearOptions();
                } else if (i == 2) {
                    showRedownloadOptions();
                } else if (i == 3) {
                    showDisclaimer();
                } else if (i == 4) {
                    showAbout();
                }
            }
        });
        mainList.setAdapter(this);
        newCampus = Curriculum.getCurrentCampus();
        newYear = Curriculum.getCurrentYear();


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);

    }

    protected void onStart() {
        super.onStart();
        if (getIntent().getBooleanExtra("showYearOptions", false)) {
            showYearOptions();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public int getCount() {
        return titles.size();
    }

    public Object getItem(int position) {
        return titles.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public int getItemViewType(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = this.getLayoutInflater().inflate(R.layout.list_item_preferences, null);
            TextView titleView = ((TextView) convertView.findViewById(R.id.text_preference_title));
            TextView subtitleView = ((TextView) convertView.findViewById(R.id.text_preference_subtitle));
            titleView.setText(titles.get(position));
            subtitleView.setText(subtitles.get(position));
            if (subtitles.get(position).equals("")) subtitleView.setVisibility(View.GONE);
            if (position == 0) {
                notificationBox = new CheckBox(this);
                notificationBox.setChecked(Curriculum.getNotificationsEnabled());
                notificationBox.setFocusable(false);
                notificationBox.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                notificationBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        toggleNotifications(isChecked);
                    }
                });
                ((ViewGroup) convertView).addView(notificationBox);
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            convertView.setBackgroundResource(R.drawable.list_item_selector);
        return convertView;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public boolean hasStableIds() {
        return true;
    }

    public boolean isEmpty() {
        return false;
    }

    public void registerDataSetObserver(DataSetObserver observer) {
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
    }

    public boolean areAllItemsEnabled() {
        return false;
    }

    public boolean isEnabled(int position) {
        return true;
    }

    public void toggleNotifications(boolean enabled) {
        notificationBox.setChecked(enabled);
        Curriculum.setNotificationsEnabled(enabled);
        Curriculum.reloadCurrentCurriculum();
    }

    public void showYearOptions() {
        LinearLayout view = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_change_year, null);
        if (newCampus == Constants.CAMPUS_MIDDLE)
            ((RadioButton) view.findViewById(R.id.radio_middle)).toggle();
        else ((RadioButton) view.findViewById(R.id.radio_upper)).toggle();
        yearText = ((TextView) view.findViewById(R.id.text_year));
        yearText.setText(getYearStr(newYear));
        AlertDialog d = new AlertDialog.Builder(this, R.style.PopupTheme)
                .setTitle(R.string.pref_change)
                .setView(view)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Curriculum.setCurrentCampus(newCampus);
                        Curriculum.setCurrentYear(newYear);
                        Intent i = new Intent(PreferencesActivity.this, LaunchActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .create();
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface arg0) {
                newCampus = Curriculum.getCurrentCampus();
                newYear = Curriculum.getCurrentYear();
            }
        });
        d.show();
    }

    public void onRadioButtonClicked(View view) {
        if (view.getId() == R.id.radio_middle) newCampus = Constants.CAMPUS_MIDDLE;
        else newCampus = Constants.CAMPUS_UPPER;
        Log.d("iHW", "New campus: " + newCampus);
    }

    public void decrementYear(View view) {
        newYear--;
        yearText.setText(getYearStr(newYear));
    }

    public void incrementYear(View view) {
        newYear++;
        yearText.setText(getYearStr(newYear));
    }

    public void showRedownloadOptions() {
        new AlertDialog.Builder(this, R.style.PopupTheme)
                .setMessage("Are you sure you want to delete your courses and redownload them from HW.com?")
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Redownload", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        redownloadSchedule();
                    }
                })
                .create().show();
    }

    public void redownloadSchedule() {
        Intent i = new Intent(this, DownloadScheduleActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    public void showDisclaimer() {
        Intent i = new Intent(this, WebViewActivity.class);
        i.putExtra("title", R.string.pref_disclaimer);
        i.putExtra("urlstr", "file:///android_asset/disclaimer.html");
        //Log.d("iHW", i.getStringExtra("urlstr"));
        startActivity(i);
    }

    public void showAbout() {
        Intent i = new Intent(this, WebViewActivity.class);
        i.putExtra("title", R.string.pref_about);
        i.putExtra("urlstr", "file:///android_asset/about.html");
        //Log.d("iHW", i.getStringExtra("urlstr"));
        startActivity(i);
    }

    public String getYearStr(int year) {
        return year + " - " + ((year + 1) % 100);
    }
}