package com.benny.openlauncher.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import net.skywall.openlauncher.R;
import com.benny.openlauncher.model.App;
import com.benny.openlauncher.util.AppManager;
import com.benny.openlauncher.util.AppSettings;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class HideAppsFragment extends Fragment {
    private static final String TAG = "RequestActivity";
    private static final boolean DEBUG = true;

    private final ArrayList<String> _listActivitiesHidden = new ArrayList<>();
    private final ArrayList<App> _listActivitiesAll = new ArrayList<>();
    private final AsyncWorkerList _taskList = new AsyncWorkerList();
    private ViewSwitcher _switcherLoad;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.view_hide_apps, container, false);
        _switcherLoad = rootView.findViewById(R.id.viewSwitcherLoadingMain);

        FloatingActionButton fab = rootView.findViewById(R.id.fab_rq);
        fab.setOnClickListener(view -> confirmSelection());

        if (_taskList.getStatus() == AsyncTask.Status.PENDING) {
            // task has not started yet
            _taskList.execute();
        }

        if (_taskList.getStatus() == AsyncTask.Status.FINISHED) {
            // task is done and onPostExecute has been called
            new AsyncWorkerList().execute();
        }

        return rootView;
    }

    public class AsyncWorkerList extends AsyncTask<String, Integer, String> {

        private AsyncWorkerList() {
        }

        @Override
        protected void onPreExecute() {
            List<String> hiddenList = AppSettings.get().getHiddenAppsList();
            _listActivitiesHidden.addAll(hiddenList);

            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... arg0) {
            try {
                // compare to installed apps
                prepareData();
                return null;
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            populateView();
            // switch from loading screen to the main view
            _switcherLoad.showNext();

            super.onPostExecute(result);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (DEBUG) Log.v(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(savedInstanceState);
    }

    private void confirmSelection() {
        Thread actionSend_Thread = new Thread(() -> {
            // update hidden apps
            AppSettings.get().setHiddenAppsList(_listActivitiesHidden);
            getActivity().finish();
        });

        if (!actionSend_Thread.isAlive()) {
            // prevents thread from being executed more than once
            actionSend_Thread.start();
        }
    }

    private void prepareData() {
        List<App> apps = AppManager.getInstance(getContext()).getNonFilteredApps();
        _listActivitiesAll.addAll(apps);
    }

    private void populateView() {
        ListView _grid = getActivity().findViewById(R.id.app_grid);

        assert _grid != null;
        _grid.setFastScrollEnabled(true);
        _grid.setFastScrollAlwaysVisible(false);

        HideAppsAdapter _appInfoAdapter = new HideAppsAdapter(getActivity(), _listActivitiesAll);

        _grid.setAdapter(_appInfoAdapter);
        _grid.setOnItemClickListener((AdapterView, view, position, row) -> {
            App appInfo = (App) AdapterView.getItemAtPosition(position);
            CheckBox checker = view.findViewById(R.id.checkbox);
            ViewSwitcher icon = view.findViewById(R.id.viewSwitcherChecked);

            checker.toggle();
            if (checker.isChecked()) {
                _listActivitiesHidden.add(appInfo.getComponentName());
                if (DEBUG) Log.v(TAG, "Selected App: " + appInfo.getLabel());
                if (icon.getDisplayedChild() == 0) {
                    icon.showNext();
                }
            } else {
                _listActivitiesHidden.remove(appInfo.getComponentName());
                if (DEBUG) Log.v(TAG, "Deselected App: " + appInfo.getLabel());
                if (icon.getDisplayedChild() == 1) {
                    icon.showPrevious();
                }
            }
        });
    }

    private class HideAppsAdapter extends ArrayAdapter<App> {
        private HideAppsAdapter(Context context, ArrayList<App> adapterArrayList) {
            super(context, R.layout.item_hide_apps, adapterArrayList);
        }

        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_hide_apps, parent, false);
                holder = new ViewHolder();
                holder._apkIcon = convertView.findViewById(R.id.appIcon);
                holder._apkName = convertView.findViewById(R.id.appName);
                holder._apkPackage = convertView.findViewById(R.id.appPackage);
                holder._checker = convertView.findViewById(R.id.checkbox);
                holder._switcherChecked = convertView.findViewById(R.id.viewSwitcherChecked);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            App appInfo = getItem(position);

            holder._apkPackage.setText(appInfo.getClassName());
            holder._apkName.setText(appInfo.getLabel());
            holder._apkIcon.setImageDrawable(appInfo.getIcon());

            holder._switcherChecked.setInAnimation(null);
            holder._switcherChecked.setOutAnimation(null);
            holder._checker.setChecked(_listActivitiesHidden.contains(appInfo.getComponentName()));
            if (_listActivitiesHidden.contains(appInfo.getComponentName())) {
                if (holder._switcherChecked.getDisplayedChild() == 0) {
                    holder._switcherChecked.showNext();
                }
            } else {
                if (holder._switcherChecked.getDisplayedChild() == 1) {
                    holder._switcherChecked.showPrevious();
                }
            }
            return convertView;
        }
    }

    private static class ViewHolder {
        TextView _apkName;
        TextView _apkPackage;
        ImageView _apkIcon;
        CheckBox _checker;
        ViewSwitcher _switcherChecked;
    }
}
