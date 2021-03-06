package com.ihwapp.android;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.widget.DatePicker;
import android.widget.Toast;

import com.ihwapp.android.model.Curriculum;
import com.ihwapp.android.model.Date;

import java.util.Locale;

public class ScheduleActivity extends AppCompatActivity implements Curriculum.ModelLoadingListener {
    private ViewPager pager;
    private DayPagerAdapter adapter;
    private Date currentDate;
    private int[] newDate;
    private int lastIndex;
    private ProgressDialog progressDialog;
    private Menu optionsMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        Curriculum.ctx = this.getApplicationContext();

        setContentView(R.layout.activity_schedule);
        this.setTitle("View Schedule");
        if (savedInstanceState != null) lastIndex = savedInstanceState.getInt("lastIndex");
        else lastIndex = -1;
        Log.d("iHW-lc", "ScheduleActivity onCreate: " + lastIndex);
        if (pager == null) pager = ((ViewPager) this.findViewById(R.id.scheduleViewPager));
        pager.setAdapter(null);
        if (pager.findViewById("pager_title_strip".hashCode()) == null) {
            CustomFontPagerTitleStrip pts = new CustomFontPagerTitleStrip(this);
            pts.setId("pager_title_strip".hashCode());
            pts.setTypeface(Typeface.DEFAULT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
                pts.setBackground(getDrawable(R.drawable.dark_tan));
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
                pts.setBackground(getResources().getDrawable(R.drawable.dark_tan)); // TODO make sure this doesn't crash on ancient devices
            else pts.setBackgroundDrawable(getResources().getDrawable(R.drawable.dark_tan));
            pts.setTextColor(Color.BLACK);
            ViewPager.LayoutParams params = new ViewPager.LayoutParams();
            params.height = LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.TOP;
            pts.setPadding(0, 4, 0, 4);
            pager.addView(pts, 0, params);
        }
        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageSelected(int position) {
                currentDate = new Date(7, 1, Curriculum.getCurrentYear()).dateByAdding(position);
                Log.d("iHW", "Page changed to " + currentDate.toString());
                lastIndex = position;
                if (optionsMenu != null) {
                    optionsMenu.findItem(R.id.action_goto_today).setVisible(!currentDate.equals(new
                            Date()));
                    optionsMenu.findItem(R.id.action_goto_today).setEnabled(!currentDate.equals(new Date()));
                }
                Curriculum.getCurrentCurriculum().clearUnneededItems(currentDate);
            }

            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            public void onPageScrollStateChanged(int arg0) {
            }
        });
        pager.setOffscreenPageLimit(2);
    }

    protected void onStart() {
        super.onStart();
        //Log.d("iHW-lc", "ScheduleActivity onStart: first loaded date " + Curriculum.getCurrentCurriculum().getFirstLoadedDate());
        //Typeface georgia = Typeface.createFromAsset(getAssets(), "fonts/Georgia.ttf");
        if (Curriculum.getCurrentCurriculum().isLoaded()) {
            Log.d("iHW", "Setting adapter: " + lastIndex);
            if (adapter == null) adapter = new DayPagerAdapter(this.getSupportFragmentManager());
            pager.setAdapter(adapter);
            if (lastIndex >= 0) gotoPosition(lastIndex, false);
            else gotoDate(new Date(), false);
        } else {
            Curriculum.getCurrentCurriculum().addModelLoadingListener(this);
        }
        NotificationManager mNotificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Schedule");
        toolbar.inflateMenu(R.menu.schedule);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("iHW-lc", "ScheduleActivity onResume");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d("iHW-lc", "ScheduleActivity onRestoreInstanceState: " + lastIndex);
        //Disable restoring of instance state
    }

    @Override
    public void onProgressUpdate(int progress) {
        if (progress < 4 && progressDialog == null && !this.isFinishing()) {
            progressDialog = new ProgressDialog(this, R.style.PopupTheme);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Loading...");
            progressDialog.show();
        }
    }

    @Override
    public void onFinishedLoading(Curriculum c) {
        Log.d("iHW-lc", "ScheduleActivity onFinishedLoading");
        if (adapter == null) adapter = new DayPagerAdapter(this.getSupportFragmentManager());
        pager.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        if (lastIndex >= 0) gotoPosition(lastIndex, false);
        else gotoDate(new Date(), false);
        if (progressDialog != null) progressDialog.dismiss();
        progressDialog = null;
    }

    public void onLoadFailed(Curriculum c) {
        if (progressDialog != null) progressDialog.dismiss();
        progressDialog = null;
        Curriculum.getCurrentCurriculum().removeModelLoadingListener(this);
        if (this.isFinishing()) return;
        new AlertDialog.Builder(this, R.style.PopupTheme).setMessage("The schedule for the campus " +
                "and year you selected is not available. Check your internet connection and try " +
                "again, or choose a different campus or year.")
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(ScheduleActivity.this, LaunchActivity.class);
                        startActivity(i);
                    }
                })
                .setNeutralButton("Choose Year", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(ScheduleActivity.this, PreferencesActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.putExtra("showYearOptions", true);
                        startActivity(i);
                    }
                })
                .setNegativeButton("Retry", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Curriculum.reloadCurrentCurriculum().addModelLoadingListener(ScheduleActivity.this);
                    }
                }).setCancelable(false).show();
    }

    public void gotoDate(Date d, boolean animated) {
        int position = new Date(7, 1, Curriculum.getCurrentYear()).getDaysUntil(d);
        if (position < 0) Toast.makeText(ScheduleActivity.this, "Please select a previous year " +
                        "(if available) from the \"Options\" menu item to view that date.",
                Toast.LENGTH_LONG).show();
        else if (position > adapter.getCount()) Toast.makeText(ScheduleActivity.this, "Please " +
                "select a future year (if available) from the \"Options\" menu item to view" +
                " that date.", Toast.LENGTH_LONG).show();
        position = Math.max(0, Math.min(adapter.getCount() - 1, position));
        gotoPosition(position, animated);
    }

    public void gotoPosition(int position, boolean animated) {
        currentDate = new Date(7, 1, Curriculum.getCurrentYear()).dateByAdding(position);
        Log.d("iHW", "Going to position " + position + " -- " + currentDate);
        pager.setCurrentItem(position, animated);
        if (optionsMenu != null) {
            optionsMenu.findItem(R.id.action_goto_today).setVisible(!currentDate.equals(new Date()));
            optionsMenu.findItem(R.id.action_goto_today).setEnabled(!currentDate.equals(new Date()));
        }
        lastIndex = pager.getCurrentItem();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.optionsMenu = menu;
        getMenuInflater().inflate(R.menu.schedule, menu);
        if (currentDate != null) {
            optionsMenu.findItem(R.id.action_goto_today).setVisible(!currentDate.equals(new Date()));
            optionsMenu.findItem(R.id.action_goto_today).setEnabled(!currentDate.equals(new Date()));
        }
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) { // AppCompat datepicker broken on Android < 5.1
            optionsMenu.findItem(R.id.action_goto_date).setVisible(false);
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_edit_courses) {
            Intent i = new Intent(this, NormalCoursesActivity.class);
            startActivity(i);
            return true;
        } else if (id == R.id.action_goto_today) {
            //int pos = Math.min(new Date(7,1,Curriculum.getCurrentYear(this)).getDaysUntil(new Date()), adapter.getCount()-1);
            //pager.setCurrentItem(pos);
            gotoDate(new Date(), true);
        } else if (id == R.id.action_goto_date) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {

                DatePickerDialog datePickerDialog = new DatePickerDialog(this, R.style.PopupTheme, null,
                        currentDate.getYear(), currentDate.getMonth() - 1, currentDate.getDay());

                datePickerDialog.getDatePicker().init(currentDate.getYear(), currentDate.getMonth() - 1,
                        currentDate.getDay(), new DatePicker.OnDateChangedListener() {
                            public void onDateChanged(DatePicker view, int year, int monthOfYear,
                                                      int dayOfMonth) {
                                newDate = new int[]{year, monthOfYear, dayOfMonth};
                            }
                        });
                datePickerDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, "Go",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (newDate == null) return;
                                Date d = new Date(newDate[1] + 1, newDate[2], newDate[0]);
                                gotoDate(d, true);
                            }
                        });


                datePickerDialog.show();
            }
        } else if (id == R.id.action_refresh) {
            Curriculum.reloadCurrentCurriculum().addModelLoadingListener(this);
        } else if (id == R.id.action_settings) {
            Intent i = new Intent(this, PreferencesActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("iHW-lc", "ScheduleActivity onSaveInstanceState: " + lastIndex);
        outState.putInt("lastIndex", lastIndex);
    }

    public void onPause() {
        super.onPause();
        Log.d("iHW-lc", "ScheduleActivity onPause");
    }

    public void onStop() {
        this.pager.setAdapter(null);
        Log.d("iHW-lc", "ScheduleActivity onStop");
        pager.setAdapter(null);
        if (progressDialog != null) progressDialog.dismiss();
        progressDialog = null;
        super.onStop();
    }

    public void onDestroy() {
        Log.d("iHW-lc", "ScheduleActivity onDestroy");
        super.onDestroy();
    }

	/*public void onBackPressed() {
		super.onBackPressed();
		/*InputMethodManager ims = ((InputMethodManager)this.getSystemService(INPUT_METHOD_SERVICE));
		Log.d("iHW", ims.isAcceptingText() + " " + ims.isActive());
	}*/

    private class DayPagerAdapter extends FragmentStatePagerAdapter {
        private final int count = new Date(7, 1, Curriculum.getCurrentYear())
                .getDaysUntil(new Date(7, 1, Curriculum.getCurrentYear() + 1));

        public DayPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public String getPageTitle(int position) {
            Date date = new Date(7, 1, Curriculum.getCurrentYear()).dateByAdding(position);
            //Day d = Curriculum.getCurrentCurriculum(ScheduleActivity.this).getDay(date);
            return date.getDayOfWeek(false).toUpperCase(Locale.getDefault());
        }

        @Override
        public Fragment getItem(int position) {
            Date date = new Date(7, 1, Curriculum.getCurrentYear()).dateByAdding(position);
            DayFragment f = new DayFragment();
            Bundle b = new Bundle();
            //Log.d("iHW", "pager: " + pager + " asked for " + date.toString());
            b.putString("date", date.toString());
            f.setArguments(b);
            return f;
        }

        @Override
        public int getCount() {
            return count;
        }
    }
}
