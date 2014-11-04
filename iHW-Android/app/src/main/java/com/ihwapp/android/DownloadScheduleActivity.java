package com.ihwapp.android;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ihwapp.android.model.Course;
import com.ihwapp.android.model.Curriculum;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DownloadScheduleActivity extends IHWActivity {
    private final Activity thisActivity = this;
    private int alreadyLoaded = 0;

    private static Course parseCourse(String code, String name, String[] periodTokens) {
        //parse for term
        int term = Constants.TERM_FULL_YEAR;
        if (code.length() >= 6) term = Integer.parseInt(code.substring(5, 6));

        //parse period list
        int numDays = Curriculum.getCurrentCampus();
        int numPeriods = numDays + 3;
        boolean[][] periods = new boolean[numDays][numPeriods + 1];
        int[] periodFrequency = new int[numPeriods + 1];
        int minPeriod = numPeriods + 1;
        int maxPeriod = 0;
        int day = 0;
        for (String token : periodTokens) {
            for (int i = 0; i < token.length(); i++) {
                int period = 0;
                try {
                    period = Integer.parseInt(token.substring(i, i + 1));
                } catch (NumberFormatException ignored) {
                }
                if (period > 0) {
                    periods[day][period] = true;
                    minPeriod = Math.min(minPeriod, period);
                    maxPeriod = Math.max(maxPeriod, period);
                    periodFrequency[period]++;
                }
            }
            day++;
        }
        //determine course period
        int coursePeriod;
        if (minPeriod == maxPeriod) coursePeriod = minPeriod;
        else if (maxPeriod - minPeriod == 2) coursePeriod = maxPeriod - 1;
        else if (maxPeriod - minPeriod == 1 && periodFrequency[maxPeriod] > periodFrequency[minPeriod])
            coursePeriod = maxPeriod;
        else if (maxPeriod - minPeriod == 1 && periodFrequency[maxPeriod] <= periodFrequency[minPeriod])
            coursePeriod = minPeriod;
        else return null;
        //create meetings array
        int[] meetings = new int[numDays];
        for (int i = 0; i < numDays; i++) {
            if (!periods[i][coursePeriod]) meetings[i] = Constants.MEETING_X_DAY;
            else if (coursePeriod - 1 > 0 && periods[i][coursePeriod - 1])
                meetings[i] = Constants.MEETING_DOUBLE_BEFORE;
            else if (coursePeriod + 1 <= numPeriods && periods[i][coursePeriod + 1])
                meetings[i] = Constants.MEETING_DOUBLE_AFTER;
            else meetings[i] = Constants.MEETING_SINGLE_PERIOD;
        }

        return new Course(name, coursePeriod, term, meetings);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        WebView webview = (WebView) this.findViewById(R.id.webView_download);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setSupportZoom(true);
        webview.getSettings().setBuiltInZoomControls(true);
        webview.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.equals("https://www.hw.com/students/Login?returnurl=/students/School-Resources/My-Schedule-Events") ||
                        url.equals("https://www.hw.com/students/School-Resources/My-Schedule-Events") ||
                        url.equals("http://www.hw.com/students/School-Resources/My-Schedule-Events")) {
                    //Log.d("iHW-dl", "Loading URL: " + url);
                    return false;
                }
                //Log.d("iHW-dl", "Preventing you from loading URL: " + url);
                return true;
            }

            public void onPageFinished(WebView view, String url) {
                if (url.equals("https://www.hw.com/students/Login?returnurl=/students/School-Resources/My-Schedule-Events")) {
                    view.setVisibility(View.VISIBLE);
                    changeInfoText("Please log into HW.com below.");
                    ((LinearLayout) findViewById(R.id.layout_notes)).setGravity(Gravity.TOP | Gravity.LEFT);
                } else if (url.equals("http://www.hw.com/students/School-Resources/My-Schedule-Events")) {
                    //Log.d("iHW-dl", "Loaded My Schedule and Events: " + alreadyLoaded);
                    if (alreadyLoaded == 0) {
                        ((LinearLayout) findViewById(R.id.layout_notes)).setGravity(Gravity.CENTER);
                        changeInfoText("Please wait, finding schedule...");
                        view.setVisibility(View.INVISIBLE);
                        view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0));
                        view.loadUrl("javascript:__doPostBack(\"dnn$ctr8420$InteractiveSchedule$lnkStudentScheduleHTML\", \"\");");
                    } else if (alreadyLoaded == 2) {
                        view.loadUrl("javascript:console.log(\"SCHEDULE_URL=\"+document.getElementById(\"dnn_ctr8420_InteractiveSchedule_txtWindowPopupUrl\").value)");
                    }
                    alreadyLoaded++;
                }/* else {
                    new AlertDialog.Builder(DownloadScheduleActivity.this)
						.setTitle("Schedule Unavailable")
						.setMessage("Your schedule is not currently available on HW.com. You can still enter your courses manually, though:")
						.setPositiveButton("Add Courses Manually", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Intent i = new Intent(DownloadScheduleActivity.this, GuidedCoursesActivity.class);
							i.putExtra("firstRun", true);
							startActivity(i);
						}
					}).setCancelable(false).create().show();
				}*/
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                //Log.e("iHW-dl", errorCode + ": " + description);
                showScheduleUnavailableError();
            }
        });
        webview.setWebChromeClient(new WebChromeClient() {
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                if (consoleMessage.message().startsWith("SCHEDULE_URL=")) {
                    String url = consoleMessage.message().substring(13);
                    if (url.length() != 0) {
                        new DownloadParseScheduleTask().execute(url);
                    } else {
                        //Log.e("iHW-dl", "Failed to get Schedule URL: " + consoleMessage.message());
                        showScheduleUnavailableError();
                    }
                }
                return false;
            }
        });
        android.webkit.CookieManager.getInstance().removeAllCookie();
        webview.loadUrl("https://www.hw.com/students/Login?returnurl=/students/School-Resources/My-Schedule-Events");
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && !this.getIntent()
                .getBooleanExtra("firstRun", false)) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void changeInfoText(String s) {
        TextView tv = (TextView) findViewById(R.id.empty);
        tv.setText(s);
    }

    public void showScheduleUnavailableError() {
        new AlertDialog.Builder(DownloadScheduleActivity.this, R.style.PopupTheme)
                .setTitle("Schedule Unavailable")
                .setMessage("Your schedule is not currently available on HW.com. You can still enter your courses manually, though:")
                .setPositiveButton("Add Courses Manually", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(DownloadScheduleActivity.this, GuidedCoursesActivity.class);
                        i.putExtra("firstRun", true);
                        startActivity(i);
                        DownloadScheduleActivity.this.finish();
                    }
                }).setNegativeButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent(thisActivity, LaunchActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        }).setCancelable(false).create().show();
    }

    private class DownloadParseScheduleTask extends AsyncTask<String, Void, Document> {
        protected void onPreExecute() {
            changeInfoText("Schedule found. Downloading...");
        }

        protected Document doInBackground(String... url) {
            //Log.d("iHW-dl", "Downloading/parsing HTML");
            Document doc = null;
            try {
                doc = Jsoup.connect(url[0]).get();
            } catch (Exception e) {
                //Log.e("iHW-dl", e.getClass().getName() + " Downloading/parsing HTML");
                e.printStackTrace();
            }
            return doc;
        }

        protected void onPostExecute(Document result) {
            if (result == null) {
                showScheduleUnavailableError();
                return;
            }
            Elements divs = result.getElementsByTag("div");
            String lastCode = null;
            String lastName = null;
            String lastPeriodList = null;
            boolean shouldShowWarning = false;
            Curriculum.getCurrentCurriculum().removeAllCourses();

            for (Element div : divs) {
                if (div.attr("id").equals("nameStudentName1-0")) {
                    //Log.d("iHW", "Name: " + div.getElementsByTag("span").first().text());
                } else if (div.attr("id").equals("sectCode1")) {
                    lastCode = div.getElementsByTag("span").first().text();
                    if (lastCode.length() <= 4) shouldShowWarning = true;
                    //Log.d("iHW", "Course code: " + lastCode);
                } else if (div.attr("id").equals("sectTitle1")) {
                    lastName = div.getElementsByTag("span").first().text();
                    //Log.d("iHW", "Course name: " + lastName);
                } else if (div.attr("id").equals("sectPeriodList1")) {
                    lastPeriodList = div.getElementsByTag("span").first().text();
                    //Log.d("iHW", "Course meets: " + lastPeriodList);
                    String[] tokens = lastPeriodList.split("\\.");
                    //Log.d("iHW", "Number of tokens: " + tokens.length);
                    if (tokens.length != Curriculum.getCurrentCampus()) {
                        new AlertDialog.Builder(DownloadScheduleActivity.this, R.style.PopupTheme)
                                .setTitle("Wrong Campus!")
                                .setMessage("You chose the wrong campus during the setup. Please start again.")
                                .setPositiveButton("Back", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent i = new Intent(thisActivity, LaunchActivity.class);
                                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(i);
                                    }
                                }).setCancelable(false).create().show();
                        return;
                    }
                    Course c = parseCourse(lastCode, lastName, tokens);
                    if (c != null) Curriculum.getCurrentCurriculum().addCourse(c);
                    lastCode = null;
                    lastName = null;
                    lastPeriodList = null;
                } else if (div.attr("id").equals("Subreport8")) {
                    break;
                }
            }
            Curriculum.getCurrentCurriculum().saveCourses();
			/*changeInfoText("Schedule downloaded.");
			ProgressBar pb = ((ProgressBar)findViewById(R.id.progressBar1));
			pb.setVisibility(View.INVISIBLE);*/

            if (shouldShowWarning) {
                new AlertDialog.Builder(DownloadScheduleActivity.this, R.style.PopupTheme)
                        .setTitle("Full Schedule Unavailable")
                        .setMessage("The full schedule is not yet available, so you will need to edit the " +
                                "courses that are not full-year and set the right semester/trimester.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent i = new Intent(thisActivity, LaunchActivity.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                            }
                        }).setCancelable(false).create().show();
            } else {
                Intent i = new Intent(thisActivity, LaunchActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }

        }
    }

}
