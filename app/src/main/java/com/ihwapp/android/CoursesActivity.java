package com.ihwapp.android;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
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
import java.util.List;

public abstract class CoursesActivity extends AppCompatActivity implements Curriculum.ModelLoadingListener {
    protected CoursesFragment coursesFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Curriculum.ctx = this.getApplicationContext();
        setContentView(R.layout.activity_courses);

        coursesFragment = new CoursesFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.courses_container,
                coursesFragment).commit();
    }

    @Override
    public abstract boolean onCreateOptionsMenu(Menu menu);
    //Leave this one abstract to control the menu items individually in subclasses

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            //Show a new "Edit Course" activity
            Intent i = new Intent(CoursesActivity.this, EditCourseActivity.class);
            startActivity(i);
            return true;
        } else if (item.getItemId() == R.id.action_done) {
            // Go back to the main schedule view
            Intent i = new Intent(CoursesActivity.this, ScheduleActivity.class);
            i.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            CoursesActivity.this.finish();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            //Up button (top-left arrow by icon) pressed -- close and go back
            CoursesActivity.this.finish();
            return true;
        }
        return false;
    }


    protected void onStart() {
        super.onStart();
        coursesFragment.setListViewStuff();
        if (!Curriculum.getCurrentCurriculum().isLoaded()) {
            Curriculum.getCurrentCurriculum().addModelLoadingListener(this);
        } else {
            coursesFragment.reloadData();
        }
    }

    @Override
    public void onProgressUpdate(int progress) {}

    @Override
    public void onFinishedLoading(Curriculum c) {
        c.removeModelLoadingListener(this);
        coursesFragment.reloadData();
    }

    @Override
    public void onLoadFailed(Curriculum c) {
        c.removeModelLoadingListener(this);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Curriculum.reloadCurrentCurriculum();
    }


    public static class CoursesFragment extends ListFragment implements ListAdapter {
        private String[] courseNames;

        private void reloadData() {
            //Get course names and copy them into a local array
            List<String> courseList = Curriculum.getCurrentCurriculum().getAllCourseNames();
            if (courseList == null) return;
            Object[] courseObjs = courseList.toArray();
            courseNames = Arrays.copyOf(courseObjs, courseObjs.length, String[].class);
            Arrays.sort(courseNames);
            this.setListAdapter(this);
            Log.d("iHW", "loaded data");
        }


        public void onListItemClick(ListView l, View v, int position, long id) {
            //Open this course in a new edit course activity
            //NOTE: Course names must be unique for this to work, and I'm not
            ///sure they're enforced that way...could be a problem later on...
            Intent i = new Intent(this.getActivity(), EditCourseActivity.class);
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
                LayoutInflater inflater = (LayoutInflater) this.getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_item_course, null);
            }
            ((TextView) convertView.findViewById(R.id.text_course_name)).setText(courseNames[position]);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                convertView.setBackgroundResource(R.drawable.list_item_selector);
            return convertView;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean hasStableIds() {
            return false;
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

        public void setListViewStuff() {
            this.getListView().setDivider(this.getResources().getDrawable(android.R.drawable.divider_horizontal_bright));
            this.getListView().setMultiChoiceModeListener(new CoursesFragment.ListSelectionListener());
            this.getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        }

        //Implements the contextual action bar (long-press to select) functionality
        private class ListSelectionListener implements AbsListView.MultiChoiceModeListener {

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                if(getActivity() instanceof NormalCoursesActivity)
                {
                    ((NormalCoursesActivity)getActivity()).getSupportActionBar().show();
                }
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
                if(getActivity() instanceof NormalCoursesActivity)
                {
                    ((NormalCoursesActivity)getActivity()).getSupportActionBar().hide();
                }
                getActivity().getMenuInflater().inflate(R.menu.cab_courses, menu);
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == R.id.action_edit) {
                    Intent i = new Intent(CoursesFragment.this.getActivity(), EditCourseActivity.class);
                    i.putExtra("courseName", courseNames[(int) getListView().getCheckedItemIds()[0]]);
                    startActivity(i);
                } else if (item.getItemId() == R.id.action_delete) {
                    //Delete all selected courses
                    long[] checked = getListView().getCheckedItemIds();
                    for (long id : checked) {
                        Curriculum.getCurrentCurriculum().removeCourse(Curriculum.
                                getCurrentCurriculum().getCourse(courseNames[(int) id]));
                    }
                    Curriculum.getCurrentCurriculum().saveCourses();
                    reloadData();
                    mode.finish();
                    return true;
                }
                return false;
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                                  boolean checked) {
                //If more than one item is selected, don't show the edit button
                if (getListView().getCheckedItemCount() == 1)
                    mode.getMenu().findItem(R.id.action_edit).setVisible(true);
                else mode.getMenu().findItem(R.id.action_edit).setVisible(false);
            }
        }
    }
}
