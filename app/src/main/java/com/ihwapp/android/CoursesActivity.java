package com.ihwapp.android;

import android.app.ListActivity;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ihwapp.android.model.Curriculum;

import java.util.Arrays;

public abstract class CoursesActivity extends ListActivity implements ListAdapter {
    private String[] courseNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Curriculum.ctx = this.getApplicationContext();
        setContentView(R.layout.activity_courses);
    }

    protected void onStart() {
        super.onStart();
        // TODO if (!Curriculum.loadCurrentCurriculum(this)) finish();
        reloadData();
        this.getListView().setDivider(this.getResources().getDrawable(android.R.drawable.divider_horizontal_bright));
        this.getListView().setMultiChoiceModeListener(new ListSelectionListener());
        this.getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Curriculum.reloadCurrentCurriculum();
    }

    private void reloadData() {
        //Get course names and copy them into a local array
        Object[] courseObjs = Curriculum.getCurrentCurriculum().getAllCourseNames().toArray();
        courseNames = Arrays.copyOf(courseObjs, courseObjs.length, String[].class);
        this.setListAdapter(this);
    }

    @Override
    public abstract boolean onCreateOptionsMenu(Menu menu);
    //Leave this one abstract to control the menu items individually in subclasses

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            //Show a new "Edit Course" activity
            Intent i = new Intent(this, EditCourseActivity.class);
            startActivity(i);
            return true;
        } else if (item.getItemId() == R.id.action_done) {
            //Go back to the main schedule view
            Intent i = new Intent(this, ScheduleActivity.class);
            i.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            this.finish();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            //Up button (top-left arrow by icon) pressed -- close and go back
            this.finish();
            return true;
        }
        return false;
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        //Open this course in a new edit course activity
        //NOTE: Course names must be unique for this to work, and I'm not
        ///sure they're enforced that way...could be a problem later on...
        Intent i = new Intent(this, EditCourseActivity.class);
        i.putExtra("courseName", courseNames[position]);
        startActivity(i);
    }

    @Override
    public int getCount() {
        return courseNames.length;
    }

    @Override
    public Object getItem(int position) {
        return courseNames[position];
    }

    @Override
    public long getItemId(int position) {
        //return courseNames[position].hashCode();
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = this.getLayoutInflater().inflate(R.layout.list_item_course, null);
        }
        ((TextView) convertView.findViewById(R.id.text_course_name)).setText(courseNames[position]);
        convertView.setBackgroundResource(R.drawable.list_item_selector);
        return convertView;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return getCount() == 0;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    //Implements the contextual action bar (long-press to select) functionality
    private class ListSelectionListener implements AbsListView.MultiChoiceModeListener {

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            int i = 0;
            View child = getListView().getChildAt(0);
            while (child != null) {
                child.setBackgroundColor(Color.TRANSPARENT);
                i++;
                child = getListView().getChildAt(i);
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getMenuInflater().inflate(R.menu.cab_courses, menu);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_edit) {
                Intent i = new Intent(CoursesActivity.this, EditCourseActivity.class);
                i.putExtra("courseName", courseNames[(int) getListView().getCheckedItemIds()[0]]);
                startActivity(i);
            } else if (item.getItemId() == R.id.action_delete) {
                //Delete all selected courses
                long[] checked = getListView().getCheckedItemIds();
                for (long id : checked) {
                    Curriculum.getCurrentCurriculum().removeCourse(Curriculum.getCurrentCurriculum().getCourse(courseNames[(int) id]));
                }
                Curriculum.getCurrentCurriculum().saveCourses();
                reloadData();
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position,
                                              long id, boolean checked) {
            //If more than one item is selected, don't show the edit button
            if (getListView().getCheckedItemCount() == 1)
                mode.getMenu().findItem(R.id.action_edit).setVisible(true);
            else mode.getMenu().findItem(R.id.action_edit).setVisible(false);
        }
    }
}

