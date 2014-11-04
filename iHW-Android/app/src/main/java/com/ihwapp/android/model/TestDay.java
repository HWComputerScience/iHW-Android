package com.ihwapp.android.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class TestDay extends Day {

    public TestDay(Date d, ArrayList<Period> tests) {
        super(d);
        this.periods = tests;
    }

    public TestDay(JSONObject obj) {
        super(obj);
        try {
            JSONArray tests = obj.getJSONArray("tests");
            this.periods = new ArrayList<Period>(tests.length());
            for (int i = 0; i < tests.length(); i++) {
                //Create each period and add it
                periods.add(new Period(tests.getJSONObject(i), i));
            }
        } catch (JSONException ignored) {
        }
    }

    public JSONObject saveDay() {
        JSONObject obj = super.saveDay();
        try {
            obj.put("type", "test");
            JSONArray tests = new JSONArray();
            for (Period p : periods) {
                tests.put(p.savePeriod());
            }
            obj.put("tests", tests);
        } catch (JSONException ignored) {
        }
        return obj;
    }

	/*public String getTitle() {
		String weekdayName = getDate().getDisplayName(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.SHORT, Locale.getDefault());
		return weekdayName + ", " + getDate().toString();
	}*/
}
