package net.skywall.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import net.skywall.openlauncher.R;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.App;

import net.skywall.utils.Pair;
import net.skywall.fragment.view.DisplayApp;
import net.skywall.service.WhitelistService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PendingFragment extends Fragment {

    private static final String TAG = PendingFragment.class.getSimpleName();

    private final WhitelistService whitelistService;
    private final List<DisplayApp> pendingApps;

    public static final DateFormat simpleDateFormat = SimpleDateFormat.getTimeInstance();

    public PendingFragment() {
        whitelistService = WhitelistService.getInstance(getContext());
        pendingApps = new ArrayList<>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Map<String, Long> pendingChanges = whitelistService.getPendingApps();

        // cannot simply stream to map because of stupid google quicksearchbox, listing itself twice
        Map<String, App> apps = new HashMap<>();
        List<App> appList = Setup.appLoader().getAllApps(getContext(), false);
        for (App app : appList) {
            apps.put(app.getPackageName(), app);
        }

        pendingApps.clear();
        for (String appPackageName : pendingChanges.keySet()) {
            App app = apps.get(appPackageName);
            if (app == null) {
                // here the user has uninstalled an app, but it hasn't been removed from the list yet
                whitelistService.cancelPendingChange(appPackageName);
                continue;
            }
            pendingApps.add(new DisplayApp(app.getLabel(), appPackageName, app.getIcon(), new Date(pendingChanges.get(appPackageName))));
        }
        if (pendingApps.isEmpty()) {
            pendingApps.add(new DisplayApp(getString(R.string.no_pending_apps), null, null, null));
        }
        pendingApps.sort(Comparator.comparing(DisplayApp::getName));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pending, container, false);

        TextView pendingDelay = view.findViewById(R.id.fragment_pending_delay);
        TextView pendingDelayDate = view.findViewById(R.id.fragment_pending_delay_date);
        Button cancel = view.findViewById(R.id.fragment_pending_delay_cancel);

        Optional<Pair<Long, Long>> pendingDelayPair = whitelistService.getPendingDelay();
        if (pendingDelayPair.isPresent()) {
            Pair<Long, Long> delayVal = pendingDelayPair.get();
            String text = pendingDelay.getText() + ": " + whitelistService.getDisplayValue(delayVal.second);
            pendingDelay.setText(text);
            pendingDelayDate.setText(simpleDateFormat.format(new Date(delayVal.first)));
            cancel.setOnClickListener(v -> {
                whitelistService.cancelPendingChange(WhitelistService.DELAY_KEY);
                pendingDelay.setText(R.string.no_pending_delay);
                pendingDelayDate.setVisibility(View.INVISIBLE);
                cancel.setVisibility(View.INVISIBLE);
            });
        } else {
            pendingDelay.setText(R.string.no_pending_delay);
            pendingDelayDate.setVisibility(View.INVISIBLE);
            cancel.setVisibility(View.INVISIBLE);
        }


        GridView gridView = view.findViewById(R.id.fragment_pending_grid);
        gridView.setAdapter(new PendingFragmentAdapter(getContext(), pendingApps, whitelistService));

        return view;
    }
}
