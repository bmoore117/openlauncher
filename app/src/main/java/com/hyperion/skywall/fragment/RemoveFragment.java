package com.hyperion.skywall.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridView;

import androidx.fragment.app.Fragment;

import com.benny.openlauncher.R;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.App;
import com.hyperion.skywall.fragment.view.DisplayApp;
import com.hyperion.skywall.service.WhitelistService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RemoveFragment extends Fragment {

    private static final String TAG = RemoveFragment.class.getSimpleName();

    private WhitelistService whitelistService;
    private final List<DisplayApp> whitelistedApps;

    public RemoveFragment() {
        whitelistService = WhitelistService.getInstance(getContext());
        whitelistedApps = new ArrayList<>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        whitelistService = WhitelistService.getInstance(getContext());
        Set<String> currentWhitelistedApps = whitelistService.getCurrentWhitelistedApps();

        // cannot simply stream to map because of stupid google quicksearchbox, listing itself twice
        Map<String, App> apps = new HashMap<>();
        List<App> appList = Setup.appLoader().getAllApps(getContext(), false);
        for (App app : appList) {
            apps.put(app.getPackageName(), app);
        }

        whitelistedApps.clear();
        for (String appPackageName : currentWhitelistedApps) {
            App app = apps.get(appPackageName);
            whitelistedApps.add(new DisplayApp(app.getLabel(), appPackageName, app.getIcon(), null));
        }
        if (whitelistedApps.isEmpty()) {
            whitelistedApps.add(new DisplayApp(getString(R.string.no_whitelisted_apps), null, null, null));
        }
        whitelistedApps.sort(Comparator.comparing(DisplayApp::getName));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_remove, container, false);

        RemoveFragmentAdapter fragmentAdapter = new RemoveFragmentAdapter(getContext(), whitelistedApps, whitelistService);

        GridView gridView = view.findViewById(R.id.fragment_remove_grid);
        gridView.setAdapter(fragmentAdapter);

        EditText search = view.findViewById(R.id.fragment_remove_search);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                fragmentAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });



        return view;
    }
}
