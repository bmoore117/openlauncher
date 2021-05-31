package com.hyperion.skywall.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.benny.openlauncher.R;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.App;
import com.hyperion.skywall.service.WhitelistService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RemoveFragment extends Fragment {

    private static final String TAG = RemoveFragment.class.getSimpleName();

    private final WhitelistService whitelistService;
    private final List<DisplayApp> whitelistedApps;

    public RemoveFragment() {
        whitelistService = WhitelistService.getInstance(getContext());
        whitelistedApps = new ArrayList<>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Set<String> currentWhitelistedApps = whitelistService.getCurrentWhitelistedApps();
        Map<String, App> apps = Setup.appLoader().getAllApps(getContext(), false).stream().collect(Collectors.toMap(App::getClassName, Function.identity()));
        whitelistedApps.clear();
        for (String appActivityName : currentWhitelistedApps) {
            App app = apps.get(appActivityName);
            whitelistedApps.add(new DisplayApp(app.getLabel(), appActivityName, app.getIcon(), null));
        }
        if (whitelistedApps.isEmpty()) {
            whitelistedApps.add(new DisplayApp(getString(R.string.no_whitelisted_apps), null, null, null));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_remove, container, false);

        GridView gridView = view.findViewById(R.id.fragment_remove_grid);
        gridView.setAdapter(new PendingFragmentAdapter(getContext(), whitelistedApps, whitelistService));

        return view;
    }
}
