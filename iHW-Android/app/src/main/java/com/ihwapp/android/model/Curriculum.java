package com.ihwapp.android.model;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.ihwapp.android.Constants;
import com.ihwapp.android.NotificationService;
import com.ihwapp.android.UpdateService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class Curriculum {
    public static Context ctx;
    private static Curriculum currentCurriculum;
    /**
     * ****************************BEGIN INSTANCE STUFF*******************************
     */

    private final int campus;
    private final int year; //2012 for the 2012-2013 school year, for example
    private HashSet<Course> courses;
    private JSONObject normalDayTemplate;
    private JSONObject normalMondayTemplate;
    private SortedMap<Date, JSONObject> specialDayTemplates;
    private SortedMap<Date, JSONObject> dayCaptions;
    private int passingPeriodLength;
    private SortedMap<Date, JSONObject> loadedWeeks; //contains week JSON by first day
    private SortedMap<Date, Day> loadedDays; //will contain days from loadedEndDates[0] to loadedEndDates[1], inclusive
    private SortedMap<Date, Integer> dayNumbers;
    private Date[] semesterEndDates; //3 values: the first day of the first semester and the last days of both semesters
    private Date[] trimesterEndDates; //4 values: the first day of the first trimester and the last days of all trimesters
    private int loadingProgress;
    private Time dayStartTime;
    private boolean currentlyCaching = false;
    private HashSet<ModelLoadingListener> mlls;

    private Curriculum(int campus, int year, Date startingDate) {
        this.campus = campus;
        this.year = year;
        loadingProgress = -1;
        mlls = new HashSet<ModelLoadingListener>(5);
        if (startingDate.compareTo(new Date(7, 1, year)) < 0) startingDate = new Date(7, 1, year);
        else if (startingDate.compareTo(new Date(7, 1, year + 1)) >= 0)
            startingDate = new Date(7, 1, year + 1).dateByAdding(-1);
        loadEverything(startingDate);
    }

    public static Curriculum getCurrentCurriculum() {
        return getCurriculum(getCurrentCampus(), getCurrentYear());
    }

    /********************************END STATIC STUFF***********************************/

    public static Curriculum reloadCurrentCurriculum() {
        currentCurriculum = null;
        return getCurrentCurriculum();
    }

    /*
     * Returns the curriculum specified by campus and year. If that curriculum is not ready yet,
     * attempts to load it immediately. If it cannot be loaded immediately, returns null.
     */
    private static Curriculum getCurriculum(int campus, int year) {
        if (currentCurriculum == null || currentCurriculum.getCampus() != campus || currentCurriculum.getYear() != year) {
            //Log.d("iHW", "Recreating curriculum object");
            currentCurriculum = new Curriculum(campus, year, new Date());
        }
        //Log.d("iHW", "Returning existing curriculum object");
        return currentCurriculum;
    }

    public static int getCurrentYear() {
        SharedPreferences prefs = ctx.getSharedPreferences("iHW", Context.MODE_PRIVATE);
        return prefs.getInt("year", 0);
    }

    public static void setCurrentYear(int year) {
        Date d = new Date();
        d.add(Date.MONTH, -6);
        ctx.getSharedPreferences("iHW", Context.MODE_PRIVATE).edit()
                .putInt("year", year)
                .putInt("manualYear", d.get(Date.YEAR)).commit();
    }

    public static int getCurrentCampus() {
        SharedPreferences prefs = ctx.getSharedPreferences("iHW", Context.MODE_PRIVATE);
        return prefs.getInt("campus", 0);
    }

    public static void setCurrentCampus(int campus) {
        ctx.getSharedPreferences("iHW", Context.MODE_PRIVATE).edit().putInt("campus", campus).commit();
    }

    public static void updateCurrentYear() {
        Date d = new Date();
        d.add(Date.MONTH, -6);
        SharedPreferences p = ctx.getSharedPreferences("iHW", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = p.edit();
        if (p.getInt("year", 0) != d.get(Date.YEAR)) {
            ed.putInt("campus", 0);
        }
        ed.putInt("year", d.get(Date.YEAR));
        ed.putInt("manualYear", 0);
        ed.commit();
    }

    public static boolean yearSetManually() {
        Date d = new Date();
        d.add(Date.MONTH, -6);
        SharedPreferences prefs = ctx.getSharedPreferences("iHW", Context.MODE_PRIVATE);
        return (prefs.getInt("manualYear", 0) == d.get(Date.YEAR));
    }

    public static boolean getNotificationsEnabled() {
        SharedPreferences prefs = ctx.getSharedPreferences("iHW", Context.MODE_PRIVATE);
        return prefs.getBoolean("allNotifications", false);
    }

    public static void setNotificationsEnabled(boolean enabled) {
        ctx.getSharedPreferences("iHW", Context.MODE_PRIVATE).edit().putBoolean("allNotifications", enabled).commit();
    }

    public static boolean isFirstRun() {
        return (getCurrentYear() == 0 || getCurrentCampus() == 0);
    }

    public static boolean shouldPromptForCourses() {
        try {
            String campusChar = getCampusChar(getCurrentCampus());
            SharedPreferences prefs = ctx.getSharedPreferences(getCurrentYear() + campusChar, Context.MODE_PRIVATE);
            String yearJSON = prefs.getString("yearJSON", "");
            return yearJSON.equals("") || new JSONObject(yearJSON).getJSONArray("courses").length() == 0;
        } catch (JSONException ignored) {
        }
        return false;
    }

    private static String getCampusChar(int campus) {
        String campusChar = null;
        if (campus == Constants.CAMPUS_MIDDLE) campusChar = "m";
        else if (campus == Constants.CAMPUS_UPPER) campusChar = "u";
        return campusChar;
    }

    private static int getWeekNumber(int year, Date d) {
        Date firstDate = new Date(7, 1, year).dateOfNextSunday();
        if (d.compareTo(firstDate) < 0 && d.compareTo(new Date(7, 1, year)) >= 0) return 0;
        else if (d.compareTo(new Date(7, 1, year + 1)) < 0)
            return (firstDate.getDaysUntil(d) / 7) + 1;
        else return -1;
    }

    private static Date getWeekStart(int year, Date d) {
        Date weekStart = d.dateOfPreviousSunday();
        Date july1 = new Date(7, 1, year);
        if (weekStart.compareTo(july1) < 0) weekStart = july1;
        return weekStart;
    }

    private static String generateBlankYearJSON(int campus, int year) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("year", year);
            obj.put("campus", campus);
            obj.put("courses", new JSONArray());
            return obj.toString(4);
        } catch (JSONException e) {
            return null;
        }
    }

    private static String generateBlankWeekJSON(Date startingDate) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("startingDate", startingDate.toString());
            obj.put("notes", new JSONObject());
            return obj.toString(4);
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Returns true when two classes scheduled for terms a and b can coexist regardless
     * of whether their periods conflict or not.
     */
    private static boolean termsCompatible(int a, int b) {
        if (a == b) return false;
        if (a == Constants.TERM_FULL_YEAR || b == Constants.TERM_FULL_YEAR) return false;
        if (a == Constants.TERM_FIRST_SEMESTER) {
            if (b == Constants.TERM_FIRST_TRIMESTER || b == Constants.TERM_SECOND_TRIMESTER)
                return false;
        } else if (a == Constants.TERM_SECOND_SEMESTER) {
            if (b == Constants.TERM_SECOND_TRIMESTER || b == Constants.TERM_THIRD_TRIMESTER)
                return false;
        }
        if (b == Constants.TERM_FIRST_SEMESTER) {
            if (a == Constants.TERM_FIRST_TRIMESTER || a == Constants.TERM_SECOND_TRIMESTER)
                return false;
        } else if (b == Constants.TERM_SECOND_SEMESTER) {
            if (a == Constants.TERM_SECOND_TRIMESTER || a == Constants.TERM_THIRD_TRIMESTER)
                return false;
        }
        return true;
    }

    public int getCampus() {
        return campus;
    }

    public int getYear() {
        return year;
    }

    public int getPassingPeriodLength() {
        return passingPeriodLength;
    }

    public Time getDayStartTime() {
        return dayStartTime;
    }

    /**
     * ******************************BEGIN LOADING STUFF*************************************
     */

    public void addModelLoadingListener(ModelLoadingListener ofll) {
        mlls.add(ofll);
    }

    public void removeModelLoadingListener(ModelLoadingListener ofll) {
        mlls.remove(ofll);
    }

    private void loadEverything(final Date startingDate) {
        if (loadingProgress >= 0) {
            //Log.d("iHW", "Tried to load everything but loading has already started.");
            return;
        }
        String campusChar = getCampusChar(this.campus);
        SharedPreferences prefs = ctx.getSharedPreferences(year + campusChar, Context.MODE_PRIVATE);
        final String scheduleJSON = prefs.getString("scheduleJSON", "");

        final AsyncTask<Void, Integer, Void> phase2 = new AsyncTask<Void, Integer, Void>() {
            protected Void doInBackground(Void... params) {
                Log.d("iHW", "starting phase 2");
                boolean success = loadThisWeekAndDay(startingDate);
                if (!success) {
                    //Log.e("iHW", "ERROR loading first week and day");
                    return null;
                }
                this.publishProgress(4);
                cacheNeededWeeksDays(startingDate);
                this.publishProgress(5);
                constructNotifications();
                AlarmManager mgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(ctx, UpdateService.class);
                intent.setData(Uri.parse("ihwupdate:0"));
                PendingIntent pi = PendingIntent.getService(ctx, 0, intent, 0);
                if (getNotificationsEnabled()) {
                    mgr.setInexactRepeating(AlarmManager.RTC, new Date().getTimeInMillis() + 24 * 3600 * 1000, 7 * 24 * 3600 * 1000, pi);
                } else {
                    mgr.cancel(pi);
                }
                Log.d("iHW", "finished phase 2");
                return null;
            }

            protected void onProgressUpdate(Integer... values) {
                loadingProgress++;
                for (ModelLoadingListener mll : mlls) {
                    mll.onProgressUpdate(loadingProgress);
                    if (values[0] == 4) mll.onFinishedLoading(Curriculum.this);
                }
            }
        };

        final AsyncTask<Void, Integer, Void> phase1a = new AsyncTask<Void, Integer, Void>() {

            protected Void doInBackground(Void... params) {
                this.publishProgress(0);
                //Log.d("iHW", "starting phase 1a");
                boolean success = downloadParseScheduleJSON(scheduleJSON.equals(""));
                if (!success) {
                    //Log.e("iHW", "FATAL ERROR downloading schedule JSON");
                    this.publishProgress(-1);
                    return null;
                }
                this.publishProgress(1);
                success = loadDayNumbers();
                if (!success) {
                    //Log.e("iHW", "ERROR loading day numbers");
                    this.publishProgress(-1);
                    return null;
                }
                this.publishProgress(2);
                //Log.d("iHW", "finished phase 1a");
                return null;
            }

            protected void onProgressUpdate(Integer... values) {
                if (values[0] == -1) {
                    for (ModelLoadingListener mll : mlls) {
                        mll.onLoadFailed(Curriculum.this);
                    }
                }
                loadingProgress++;
                for (ModelLoadingListener mll : mlls) {
                    mll.onProgressUpdate(loadingProgress);
                }
                if (values[0] == 2 && loadingProgress == 3) {
                    phase2.execute();
                }
            }
        };

        final AsyncTask<Void, Integer, Void> phase1b = new AsyncTask<Void, Integer, Void>() {
            protected Void doInBackground(Void... params) {
                //Log.d("iHW", "starting phase 1b");
                boolean success = loadCourses();
                if (!success) {
                    //Log.e("iHW", "ERROR loading courses");
                    return null;
                }
                this.publishProgress(3);
                //Log.d("iHW", "finished phase 1b");
                return null;
            }

            protected void onProgressUpdate(Integer... values) {
                loadingProgress++;
                for (ModelLoadingListener mll : mlls) {
                    mll.onProgressUpdate(loadingProgress);
                }
                if (loadingProgress == 3) {
                    phase2.execute();
                }
            }
        };

        phase1a.execute();
        phase1b.execute();
    }

    public Date getFirstLoadedDate() {
        if (loadedDays == null || loadedDays.size() == 0) return null;
        return loadedDays.firstKey();
    }

    public Date getLastLoadedDate() {
        if (loadedDays == null || loadedDays.size() == 0) return null;
        return loadedDays.lastKey();
    }

    public boolean isLoaded(Date d) {
        return loadedDays != null && loadedDays.containsKey(d);
    }

    public boolean isLoaded() {
        return loadingProgress >= 4;
    }

    private boolean downloadParseScheduleJSON(boolean important) {
        //Log.d("iHW", "Starting schedule JSON download (if able)");
        ConnectivityManager connMgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) { //no Internet connection
            if (important) return false;
            parseScheduleJSON();
            return true;
        } else { //Internet is available; should update
            DownloadTask downloadTask = new DownloadTask();
            if (important) {
                //Log.d("iHW", "downloading on MAIN THREAD");
                String result = downloadTask.doInBackground();
                downloadTask.onPostExecute(result);
                return (!result.equals(""));
            } else {
                //Log.d("iHW", "downloading in BACKGROUND");
                parseScheduleJSON();
                downloadTask.execute();
                return true;
            }
        }
    }

    private boolean parseScheduleJSON() {
        try {
            //Log.d("iHW", "Starting to parse schedule JSON");
            String campusChar = getCampusChar(this.campus);
            SharedPreferences prefs = ctx.getSharedPreferences(year + campusChar, Context.MODE_PRIVATE);
            String scheduleJSON = prefs.getString("scheduleJSON", "");
            if (scheduleJSON.equals("")) return false;
            JSONObject scheduleObj = new JSONObject(scheduleJSON);

            //load semester/trimester end dates, etc.
            JSONArray semestersArr = scheduleObj.getJSONArray("semesterEndDates");
            semesterEndDates = new Date[semestersArr.length()];
            for (int i = 0; i < semestersArr.length(); i++) {
                semesterEndDates[i] = new Date(semestersArr.getString(i));
            }
            JSONArray trimestersArr = scheduleObj.getJSONArray("trimesterEndDates");
            trimesterEndDates = new Date[trimestersArr.length()];
            for (int i = 0; i < trimestersArr.length(); i++) {
                trimesterEndDates[i] = new Date(trimestersArr.getString(i));
            }
            normalDayTemplate = scheduleObj.getJSONObject("normalDay");
            normalMondayTemplate = scheduleObj.getJSONObject("normalMonday");
            passingPeriodLength = scheduleObj.getInt("passingPeriodLength");
            String timeStr = scheduleObj.optString("dayStartTime");
            if (timeStr != null && !timeStr.equals("")) dayStartTime = new Time(timeStr);
            else dayStartTime = new Time(8, 0);

            JSONObject specialDaysObj = scheduleObj.getJSONObject("specialDays");
            SortedMap<Date, JSONObject> sdts = Collections.synchronizedSortedMap(new TreeMap<Date, JSONObject>());
            Iterator<?> iter = specialDaysObj.keys();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                Date d = new Date(key);
                sdts.put(d, specialDaysObj.getJSONObject(key));
            }
            specialDayTemplates = sdts;

            JSONObject dayCaptionsObj = scheduleObj.optJSONObject("dayCaptions");
            SortedMap<Date, JSONObject> dcs = Collections.synchronizedSortedMap(new TreeMap<Date, JSONObject>());
            if (dayCaptionsObj != null) {
                Iterator<?> iter2 = dayCaptionsObj.keys();
                while (iter2.hasNext()) {
                    String key = (String) iter2.next();
                    Date d = new Date(key);
                    dcs.put(d, dayCaptionsObj.getJSONObject(key));
                }
            }
            dayCaptions = dcs;
            //Log.d("iHW", "finished parsing schedule JSON");
            return true;
        } catch (JSONException ignored) {
        }
        return false;
    }

    private boolean loadCourses() {
        //Log.d("iHW", "starting to load courses");
        HashSet<Course> coursesSet;
        try {
            String campusChar = getCampusChar(campus);
            SharedPreferences prefs = ctx.getSharedPreferences(year + campusChar, Context.MODE_PRIVATE);
            String yearJSON = prefs.getString("yearJSON", "");
            if (yearJSON.equals("")) yearJSON = generateBlankYearJSON(campus, year);

            JSONObject yearObj = new JSONObject(yearJSON);
            JSONArray coursesArr = yearObj.getJSONArray("courses");
            coursesSet = new HashSet<Course>(coursesArr.length());
            for (int i = 0; i < coursesArr.length(); i++) {
                coursesSet.add(new Course(coursesArr.getJSONObject(i)));
            }
            courses = coursesSet;
            //Log.d("iHW", "finished loading courses");
            return true;
        } catch (JSONException ignored) {
        }
        return false;
    }

    private boolean loadDayNumbers() {
        //Log.d("iHW", "starting to load day numbers");
        if (specialDayTemplates == null || semesterEndDates == null) return false;
        SortedMap<Date, Integer> dayNums;
        try {
            dayNums = Collections.synchronizedSortedMap(new TreeMap<Date, Integer>());
            Date d = semesterEndDates[0];
            int dayNum = 1;
            while (d.compareTo(semesterEndDates[2]) <= 0) {
                if (specialDayTemplates.containsKey(d)) {
                    if (specialDayTemplates.get(d).getString("type").equals("normal")) {
                        int thisNum = specialDayTemplates.get(d).optInt("dayNumber");
                        if (thisNum != 0) dayNum = thisNum + 1;
                        //This special day has a number; continue the count from this day's daynum
                        dayNums.put(d, thisNum);
                    } else {
                        //This special day doesn't have a day number; set this daynum to 0 and don't increment
                        dayNums.put(d, 0);
                    }
                } else {
                    //This is a normal day; continue the count
                    if (!d.isWeekend()) {
                        dayNums.put(d, dayNum);
                        dayNum++;
                    }
                }
                if (dayNum > campus) dayNum -= campus;
                d = d.dateByAdding(1);
            }
            dayNumbers = dayNums;
            //Log.d("iHW", "finished loading day numbers");
            return true;
        } catch (JSONException ignored) {
        }
        return false;
    }

    private boolean loadThisWeekAndDay(Date date) {
        boolean success = loadWeek(date);
        if (!success) {
            //Log.e("iHW", "ERROR loading week");
            return false;
        }
        success = loadDay(date);
        if (!success) {
            //Log.e("iHW", "ERROR loading day");
            return false;
        }
        return true;
    }

    private void cacheNeededWeeksDays(final Date currentDate) {
        if (currentlyCaching) return;
        currentlyCaching = true;
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                //Log.d("iHW", "starting to cache needed weeks and days");
                Date currentWeekStart = getWeekStart(year, currentDate);
                ArrayList<Date> weeksNeeded = new ArrayList<Date>(3);
                for (int i = -7; i <= 7; i += 7) weeksNeeded.add(currentWeekStart.dateByAdding(i));
                loadedWeeks.keySet().retainAll(weeksNeeded);
                for (Date d : weeksNeeded)
                    if (isInBounds(d) && !loadedWeeks.containsKey(d)) {
                        loadWeek(d);
                    }
                ArrayList<Date> daysNeeded = new ArrayList<Date>(7);
                for (int i = -3; i <= 3; i++) daysNeeded.add(currentDate.dateByAdding(i));
                loadedDays.keySet().retainAll(daysNeeded);
                for (Date d : daysNeeded)
                    if (isInBounds(d) && !loadedDays.containsKey(d)) {
                        loadDay(d);
                    }
                //Log.d("iHW", "finished caching weeks and days");
                currentlyCaching = false;
                return null;
            }
        }.execute();
    }

    private boolean loadWeek(Date date) {
        try {
            //Log.d("iHW", "starting to load week containing " + date);
            int weekNumber = getWeekNumber(year, date);
            Date weekStart = getWeekStart(year, date);
            if (loadedWeeks != null && loadedWeeks.containsKey(new Date(weekStart.toString())))
                return true;
            if (weekNumber == -1) return false;
            SharedPreferences prefs = ctx.getSharedPreferences(year + getCampusChar(campus), Context.MODE_PRIVATE);
            String weekJSON = prefs.getString("week" + weekNumber, "");
            if (weekJSON.equals("")) {
                weekJSON = generateBlankWeekJSON(weekStart);
            }
            JSONObject weekJSONObj;
            weekJSONObj = new JSONObject(weekJSON);
            if (loadedWeeks == null)
                loadedWeeks = Collections.synchronizedSortedMap(new TreeMap<Date, JSONObject>());
            loadedWeeks.put(new Date(weekStart.toString()), weekJSONObj);
            //Log.d("iHW", "finished loading week");
            return true;
        } catch (JSONException ignored) {
        }
        return false;
    }

    private boolean loadDay(Date d) {
        if (!isInBounds(d)) return false;
        try {
            //Log.d("iHW", "starting to load day: " + d);
            //Date weekStart = getWeekStart(year, d);
            if (loadedDays == null)
                loadedDays = Collections.synchronizedSortedMap(new TreeMap<Date, Day>());
            //if (!loadedWeeks.containsKey(weekStart)) return false;
            JSONObject template;
            if (specialDayTemplates.containsKey(d)) {
                template = specialDayTemplates.get(d);
            } else if (d.compareTo(semesterEndDates[0]) < 0 || d.compareTo(semesterEndDates[2]) > 0) {
                Day day = new Holiday(d, "Summer");
                JSONObject captionObj = dayCaptions.get(d);
                if (captionObj != null && day.getCaption() == null) {
                    day.setCaption(captionObj.optString("text"));
                    day.setCaptionLink(captionObj.optString("link"));
                }
                loadedDays.put(d, day);
                return true;
            } else if (d.isWeekend()) {
                Day day = new Holiday(d, "");
                JSONObject captionObj = dayCaptions.get(d);
                if (captionObj != null && day.getCaption() == null) {
                    day.setCaption(captionObj.optString("text"));
                    day.setCaptionLink(captionObj.optString("link"));
                }
                loadedDays.put(d, day);
                return true;
            } else if (d.isMonday()) {
                JSONArray namesArr = normalMondayTemplate.names();
                String[] names = new String[namesArr.length()];
                for (int i = 0; i < names.length; i++) names[i] = (String) namesArr.get(i);
                template = new JSONObject(normalMondayTemplate, names);
                template.put("date", d.toString());
                template.put("dayNumber", dayNumbers.get(d));
            } else {
                JSONArray namesArr = normalDayTemplate.names();
                String[] names = new String[namesArr.length()];
                for (int i = 0; i < names.length; i++) names[i] = (String) namesArr.get(i);
                template = new JSONObject(normalDayTemplate, names);
                template.put("date", d.toString());
                template.put("dayNumber", dayNumbers.get(d));
            }
            String type = template.getString("type");
            Day day = null;
            if (type.equals("normal")) {
                day = new NormalDay(template);
                ((NormalDay) day).fillPeriods(this);
            } else if (type.equals("test")) day = new TestDay(template);
            else if (type.equals("holiday")) day = new Holiday(template);
            else return false;

            JSONObject captionObj = dayCaptions.get(d);
            if (captionObj != null && day.getCaption() == null) {
                day.setCaption(captionObj.optString("text"));
                day.setCaptionLink(captionObj.optString("link"));
            }

            loadedDays.put(d, day);
            //Log.d("iHW", "finished loading day");
            return true;
        } catch (JSONException ignored) {
        }
        return false;
    }

    public Day getDay(Date d) {
        if (!isInBounds(d)) {
            Log.d("iHW", "Date out of bounds: " + d);
            return null;
        }
        //Log.d("iHW", "getting " + d.toString());
        //Log.d("iHW", "weeks loaded: " + loadedWeeks.keySet().toString());
        if (!isLoaded(d)) {
            boolean success = true;
            if (loadedWeeks == null || !loadedWeeks.containsKey(getWeekStart(year, d)))
                success = loadWeek(d);
            if (!success) Log.e("iHW", "ERROR loading week");
            if (loadedDays == null || !loadedDays.containsKey(d)) success = loadDay(d);
            if (!success) Log.e("iHW", "ERROR loading day");
        }
        if (!isLoaded(d)) return null;
        else {
            return loadedDays.get(d);
            //cacheNeededWeeksDays(d);
        }
    }

    public void clearUnnededItems(Date d) {
        Date currentWeekStart = getWeekStart(year, d);
        ArrayList<Date> weeksNeeded = new ArrayList<Date>(3);
        for (int i = -7; i <= 7; i += 7) weeksNeeded.add(currentWeekStart.dateByAdding(i));
        if (loadedWeeks != null) loadedWeeks.keySet().retainAll(weeksNeeded);
        ArrayList<Date> daysNeeded = new ArrayList<Date>(7);
        for (int i = -3; i <= 3; i++) daysNeeded.add(d.dateByAdding(i));
        if (loadedDays != null) loadedDays.keySet().retainAll(daysNeeded);
    }

    private boolean isInBounds(Date d) {
        return (d != null && d.compareTo(new Date(7, 1, year)) >= 0 && d.compareTo(new Date(7, 1, year + 1)) < 0);
    }

    /**
     * ***********************BEGIN COURSES STUFF**************************
     */

    public List<String> getAllCourseNames() {
        List<String> list = new ArrayList<String>();
        for (Course c : courses) list.add(c.getName());
        return list;
    }

    /**
     * Attempts to add the specified course to the curriculum.
     * Returns true if it was added, and false if scheduling conflicts prevented it from being added.
     */
    public boolean addCourse(Course c) {
        for (Course check : courses) {
            if (!termsCompatible(c.getTerm(), check.getTerm())) {
                if (c.getPeriod() == check.getPeriod()) {
                    for (int i = 1; i <= campus; i++) {
                        if (c.getMeetingOn(i) != Constants.MEETING_X_DAY &&
                                check.getMeetingOn(i) != Constants.MEETING_X_DAY) return false;
                    }
                } else if (Math.abs(c.getPeriod() - check.getPeriod()) == 1) {
                    Course later, earlier;
                    if (c.getPeriod() > check.getPeriod()) {
                        later = c;
                        earlier = check;
                    } else {
                        later = check;
                        earlier = c;
                    }
                    for (int i = 1; i <= campus; i++) {
                        if (earlier.getMeetingOn(i) == Constants.MEETING_DOUBLE_AFTER &&
                                later.getMeetingOn(i) != Constants.MEETING_X_DAY) return false;
                        if (later.getMeetingOn(i) == Constants.MEETING_DOUBLE_BEFORE &&
                                earlier.getMeetingOn(i) != Constants.MEETING_X_DAY) return false;
                    }
                } else if (Math.abs(c.getPeriod() - check.getPeriod()) == 2) {
                    Course later, earlier;
                    if (c.getPeriod() > check.getPeriod()) {
                        later = c;
                        earlier = check;
                    } else {
                        later = check;
                        earlier = c;
                    }
                    for (int i = 1; i <= campus; i++) {
                        if (earlier.getMeetingOn(i) == Constants.MEETING_DOUBLE_AFTER &&
                                later.getMeetingOn(i) == Constants.MEETING_DOUBLE_BEFORE)
                            return false;
                    }
                }
            }
        }
        courses.add(c);
        //this.rebuildSpecialDays();
        loadedDays.clear();
        return true;
    }

    /***************************END LOADING STUFF****************************/

    public void removeCourse(Course c) {
        courses.remove(c);
        //rebuildSpecialDays();
        loadedDays.clear();
    }

    public void removeAllCourses() {
        courses.clear();
        //rebuildSpecialDays();
        loadedDays.clear();
    }

    public boolean replaceCourse(String oldName, Course c) {
        Course oldCourse = this.getCourse(oldName);
        this.removeCourse(oldCourse);
        if (this.addCourse(c)) {
            return true;
        } else {
            this.addCourse(oldCourse);
            return false;
        }

    }

    public List<Integer> termsFromDate(Date d) {
        List<Integer> list = new ArrayList<Integer>(3);
        if (d.compareTo(semesterEndDates[0]) >= 0) {
            if (d.compareTo(semesterEndDates[1]) <= 0) {
                list.add(Constants.TERM_FULL_YEAR);
                list.add(Constants.TERM_FIRST_SEMESTER);
            } else if (d.compareTo(semesterEndDates[2]) <= 0) {
                list.add(Constants.TERM_FULL_YEAR);
                list.add(Constants.TERM_SECOND_SEMESTER);
            }
        }
        if (d.compareTo(trimesterEndDates[0]) >= 0) {
            if (d.compareTo(trimesterEndDates[1]) <= 0) list.add(Constants.TERM_FIRST_TRIMESTER);
            else if (d.compareTo(trimesterEndDates[2]) <= 0)
                list.add(Constants.TERM_SECOND_TRIMESTER);
            else if (d.compareTo(trimesterEndDates[3]) <= 0)
                list.add(Constants.TERM_THIRD_TRIMESTER);
        }
        return list;
    }

    public Course getCourse(Date d, int period) {
        if (d.compareTo(semesterEndDates[0]) < 0 || d.compareTo(semesterEndDates[2]) > 0)
            return null;
        int dayNum = dayNumbers.get(d);
        List<Integer> terms = termsFromDate(d);
        if (dayNum == 0) {
            Course maxMeetings = null;
            int max = 1;
            for (Course c : courses) {
                boolean termFound = false;
                for (int term : terms) {
                    if (term == c.getTerm()) {
                        termFound = true;
                        break;
                    }
                }
                if (!termFound) continue;
                if (c.getPeriod() == period && c.getTotalMeetings() > max) {
                    maxMeetings = c;
                    max = c.getTotalMeetings();
                }
            }
            return maxMeetings;
        }
        for (Course c : courses) {
            boolean termFound = false;
            for (int term : terms) {
                if (term == c.getTerm()) {
                    termFound = true;
                    break;
                }
            }
            if (!termFound) continue;
            if (c.getPeriod() == period) {
                if (dayNum == 0) return c;
                if (c.getMeetingOn(dayNum) != Constants.MEETING_X_DAY) return c;
            } else if (period == c.getPeriod() - 1) {
                if (c.getMeetingOn(dayNum) == Constants.MEETING_DOUBLE_BEFORE) return c;
            } else if (period == c.getPeriod() + 1) {
                if (c.getMeetingOn(dayNum) == Constants.MEETING_DOUBLE_AFTER) return c;
            }
        }
        return null;
    }

    public Course[] getCourseList(Date d) {
        if (semesterEndDates == null ||
                semesterEndDates[0] == null || d.compareTo(semesterEndDates[0]) < 0 ||
                semesterEndDates[2] == null || d.compareTo(semesterEndDates[2]) > 0) return null;
        int dayNum = dayNumbers.get(d);
        List<Integer> terms = termsFromDate(d);
        Course[] courseList = new Course[campus + 4]; //campus+3 is numPeriods, add 1 to keep 0 index empty
        int[] maxMeetings = new int[campus + 4];
        for (int i = 0; i < maxMeetings.length; i++) maxMeetings[i] = 1;
        for (Course c : courses) {
            if (!terms.contains(c.getTerm())) continue;
            if (dayNum == 0) {
                int meetings = c.getTotalMeetings();
                if (meetings > maxMeetings[c.getPeriod()]) {
                    courseList[c.getPeriod()] = c;
                    maxMeetings[c.getPeriod()] = meetings;
                }
            } else if (c.getMeetingOn(dayNum) != Constants.MEETING_X_DAY) {
                courseList[c.getPeriod()] = c;
                if (c.getMeetingOn(dayNum) == Constants.MEETING_DOUBLE_AFTER)
                    courseList[c.getPeriod() + 1] = c;
                if (c.getMeetingOn(dayNum) == Constants.MEETING_DOUBLE_BEFORE)
                    courseList[c.getPeriod() - 1] = c;
            }
        }
        return courseList;
    }

    public Course getCourse(String name) {
        for (Course c : courses) if (c.getName().equals(name)) return c;
        return null;
    }

    public void constructNotifications() {
        Log.d("iHW", "Constructing Notifications");
        boolean create = Curriculum.getNotificationsEnabled();
        AlarmManager mgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Date startDate = new Date();
        startDate.set(Date.HOUR_OF_DAY, 0);
        startDate.set(Date.MINUTE, 0);
        startDate.set(Date.SECOND, 0);
        startDate.set(Date.MILLISECOND, 0);
        Date endDate = startDate.dateByAdding(8);
        boolean isToday = true;
        for (Date d = startDate; d.compareTo(endDate) < 0; d = d.dateByAdding(1)) {
            Log.d("iHW", "Checking date: " + d.toString() + " (" + d.getTimeInMillis() + ")");
            Day day = this.getDay(d);
            if (!(day instanceof NormalDay)) {
                Log.d("iHW", "Day isn't a normal day: " + day);
                isToday = false;
                continue;
            }
            for (int i = 0; i < day.getPeriods().size(); i++) {
                Period thisPeriod = day.getPeriods().get(i);
                if (thisPeriod.isFreePeriod() &&
                        (!isToday || thisPeriod.getEndTime().secondsUntil(new Time()) < 0)) {
                    if (i < day.getPeriods().size() - 1 &&
                            !day.getPeriods().get(i + 1).isFreePeriod()) {
                        Period next = day.getPeriods().get(i + 1);
                        Intent intent = new Intent(ctx, NotificationService.class);
                        intent.putExtra("date", d.toString());
                        intent.putExtra("period", i);
                        intent.putExtra("notificationTitle", "Class in " + this.getPassingPeriodLength() + " minutes");
                        intent.putExtra("notificationText", next.getName() + " starts in " + this.getPassingPeriodLength() + " minutes");
                        intent.setData(Uri.parse("ihwnotification:" + d.getYear() + "-" + d.getMonth() + "-" + d.getDay() + "." + i));
                        PendingIntent pi = PendingIntent.getService(ctx, 0, intent, 0);
                        if (create) {
                            long time = thisPeriod.getEndTime().timeMillisWithDate(d);
                            Log.d("iHW", "Creating alarm for " + next.getName() + " on " + d.toString() + " at " + thisPeriod.getEndTime() + " (" + time + ")");
                            mgr.set(AlarmManager.RTC_WAKEUP, time, pi);
                        } else {
                            Log.d("iHW", "Canceling alarm for " + next.getName() + " on " + d.toString() + " at " + thisPeriod.getEndTime());
                            mgr.cancel(pi);
                        }
                    }
                }
            }
            isToday = false;
        }
    }

    /**
     * *******************************BEGIN NOTES STUFF*************************************
     */

    public ArrayList<Note> getNotes(Date d, int period) {
        Date weekStart = getWeekStart(year, d);
        boolean success = true;
        if (loadedWeeks == null || !loadedWeeks.containsKey(weekStart)) success = loadWeek(d);
        if (!success) {
            //Log.e("iHW", "ERROR loading week");
            return null;
        } else {
            try {
                String key = d.toString() + "." + period;
                JSONObject weekJSON = loadedWeeks.get(weekStart);
                if (weekJSON.getJSONObject("notes").has(key)) {
                    JSONArray notesArr = weekJSON.getJSONObject("notes").getJSONArray(key);
                    ArrayList<Note> notes = new ArrayList<Note>(notesArr.length());
                    for (int i = 0; i < notesArr.length(); i++) {
                        JSONObject noteObj = notesArr.getJSONObject(i);
                        notes.add(new Note(noteObj));
                    }
                    return notes;
                } else {
                    return new ArrayList<Note>(6);
                }
            } catch (JSONException e) {
                //Log.e("iHW", "JSONException!", e);
            }
            return null;
        }
    }

    public void setNotes(Date d, int period, ArrayList<Note> notes) {
        Date weekStart = getWeekStart(year, d);
        if (!isLoaded(d)) {
            //boolean success = true;
            if (!loadedWeeks.containsKey(weekStart)) /*success =*/ loadWeek(d);
            //if (!success) Log.e("iHW", "ERROR loading week");
            if (!loadedDays.containsKey(d)) /*success =*/ loadDay(d);
            //if (!success) Log.e("iHW", "ERROR loading day");
        }
        if (isLoaded(d)) {
            try {
                String key = d.toString() + "." + period;
                JSONObject weekJSON = loadedWeeks.get(weekStart);
                JSONArray notesArr = new JSONArray();
                for (Note note : notes) notesArr.put(note.saveNote());
                weekJSON.getJSONObject("notes").put(key, notesArr);
            } catch (JSONException ignored) {
            }
        }
    }

    /**********************************END COURSES STUFF**************************************/

    /**
     * ******************************BEGIN SAVING STUFF************************************
     */

    public void saveWeek(Date d) {
        Date weekStart = getWeekStart(year, d);
        JSONObject weekObj = loadedWeeks.get(weekStart);
        int weekNumber = getWeekNumber(year, d);
        SharedPreferences prefs = ctx.getSharedPreferences(year + getCampusChar(campus), Context.MODE_PRIVATE);
        String weekJSON = weekObj.toString();
        prefs.edit().putString("week" + weekNumber, weekJSON).apply();
    }

    public void saveCourses() {
        String campusChar = getCampusChar(campus);
        SharedPreferences prefs = ctx.getSharedPreferences(year + campusChar, Context.MODE_PRIVATE);
        try {
            JSONObject yearObj = new JSONObject();
            yearObj.put("year", year);
            yearObj.put("campus", campus);
            JSONArray coursesArr = new JSONArray();
            for (Course c : courses) coursesArr.put(c.saveCourse());
            yearObj.put("courses", coursesArr);
            String yearJSON = yearObj.toString();
            prefs.edit().putString("yearJSON", yearJSON).apply();

        } catch (JSONException ignored) {
        }
    }

    /**********************************END NOTES STUFF***************************************/

    public interface ModelLoadingListener {
        public void onProgressUpdate(int progress);

        public void onFinishedLoading(Curriculum c);

        public void onLoadFailed(Curriculum c);
    }

    private class DownloadTask extends AsyncTask<Void, Void, String> {
        public String doInBackground(Void... params) {
            //Log.d("iHW", "internet is available - will download schedule JSON");
            HttpURLConnection urlConnection = null;
            String result = null;
            String campusChar = getCampusChar(campus);
            String urlStr = "http://www.ihwapp.com/curriculum/" + year + campusChar + ".hws";
            try {
                URL url = new URL(urlStr);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                InputStreamReader ir = new InputStreamReader(in);
                StringBuilder sb = new StringBuilder();
                BufferedReader br = new BufferedReader(ir);
                String line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    line = br.readLine();
                }
                result = sb.toString();
            } catch (Exception e) {
                return ""; //false if no previous version is available, true if there is one
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }
            return result;
        }

        public void onPostExecute(String result) {
            if (result != null && !result.equals("")) {
                String campusChar = getCampusChar(campus);
                //Log.d("iHW", "downloaded schedule JSON successfully");
                SharedPreferences prefs = ctx.getSharedPreferences(year + campusChar, Context.MODE_PRIVATE);
                prefs.edit().putString("scheduleJSON", result).commit();
                /*boolean success =*/
                parseScheduleJSON();
                //if (!success) Log.e("iHW", "ERROR parsing schedule JSON");
            }
        }
    }

    /**********************************END SAVING STUFF**************************************/
}
