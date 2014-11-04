package com.ihwapp.android.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public abstract class Day {
    protected Date date;
    protected ArrayList<Period> periods;
    protected String caption;
    protected String captionLink;

    public Day(Date d) {
        this.date = d;
    }

    public Day(JSONObject obj) {
        try {
            date = new Date(obj.getString("date"));
            caption = obj.optString("caption");
            captionLink = obj.optString("captionLink");
            if (caption.equals("")) caption = null;
            if (captionLink.equals("")) captionLink = null;
        } catch (JSONException ignored) {
        }
    }

    public com.ihwapp.android.model.Date getDate() {
        return date;
    }

    public ArrayList<Period> getPeriods() {
        return periods;
    }

    //Not really ever used...
    public JSONObject saveDay() {
        try {
            JSONObject toReturn = new JSONObject();
            toReturn.put("date", date);
            if (caption != null) toReturn.put("caption", caption);
            if (captionLink != null) toReturn.put("captionLink", captionLink);
            return toReturn;
        } catch (JSONException e) {
            return null;
        }
    }

    public String getTitle() {
        return getDate().toString();
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String c) {
        caption = c;
    }

    public String getCaptionLink() {
        return captionLink;
    }

    public void setCaptionLink(String l) {
        if (l.equals("")) captionLink = null;
        else captionLink = l;
    }
}
