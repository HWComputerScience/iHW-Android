package com.ihwapp.android.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Holiday extends Day {
    private String name;

    public Holiday(Date d, String name) {
        super(d);
        this.name = name;
        this.periods = new ArrayList<Period>(0);
    }

    public Holiday(JSONObject obj) {
        super(obj);
        this.periods = new ArrayList<Period>(0);
        try {
            name = obj.getString("name");
        } catch (JSONException ignored) {
        }
    }

    public JSONObject saveDay() {
        JSONObject obj = super.saveDay();
        try {
            obj.put("name", name);
            obj.put("type", "holiday");
        } catch (JSONException ignored) {
        }
        return obj;
    }

    public String getName() {
        return name;
    }

	/*public String getTitle() {
		String weekdayName = getDate().getDisplayName(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.SHORT, Locale.getDefault());
		return weekdayName + ", " + getDate().toString();
	}*/
}
