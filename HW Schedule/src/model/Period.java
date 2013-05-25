package model;

import java.util.*;
import org.json.*;

public class Period {
	private String name;
	private Time startTime;
	private Time endTime;
	private List<Note> notes;
	private Date d;
	private int periodNum;
	
	public Period(String name, Date d, Time start, Time end, int periodNum) {
		this.name = name;
		this.startTime = start;
		this.endTime = end;
		this.d=d;
		this.periodNum=periodNum;
		notes = new LinkedList<Note>();
	}
	
	public Period(JSONObject obj) {
		this.name = obj.getString("name");
		this.startTime = new Time(obj.getString("startTime"));
		this.endTime = new Time(obj.getString("endTime"));
		this.d = new Date(obj.getString("date"));
		this.periodNum = obj.getInt("periodNum");
	}
	
	public String getName() { return name; }
	public Time getStartTime() { return startTime; }
	public Time getEndTime() { return endTime; }
	public List<Note> getNotes() { return new ArrayList<Note>(notes); }
	public Date getDate() { return d; }
	public void setDate(Date d) { this.d = d; }
	public int getNum() { return periodNum; }
	public void setNum(int periodNum) { this.periodNum = periodNum; }
	
	public void addNote(Note note) {
		notes.add(note);
	}
	
	public void removeNote(Note note) {
		notes.remove(note);
	}
	
	public JSONObject savePeriod() {
		JSONObject obj = new JSONObject();
		obj.put("name", name);
		obj.put("startTime", startTime);
		obj.put("endTime", endTime);
		obj.put("date", d);
		obj.put("periodNum", periodNum);
		return obj;
	}
	
	public static Period newFreePeriod(Date d, Time start, Time end, int periodNum) {
		return new Period("Free", d, start, end, periodNum);
	}
}
