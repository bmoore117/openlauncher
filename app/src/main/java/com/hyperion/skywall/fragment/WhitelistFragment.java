package com.hyperion.skywall.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.Toast;

import com.benny.openlauncher.BuildConfig;
import com.benny.openlauncher.R;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.App;
import com.hyperion.skywall.fragment.view.DisplayApp;
import com.hyperion.skywall.service.WhitelistService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WhitelistFragment extends Fragment {

    private static final String TAG = WhitelistFragment.class.getSimpleName();

    private final WhitelistService whitelistService;
    private List<DisplayApp> nonWhitelistedApps;

    public WhitelistFragment() {
        whitelistService = WhitelistService.getInstance(getContext());
        nonWhitelistedApps = new ArrayList<>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        List<App> apps = Setup.appLoader().getAllApps(getContext(), false);
        Set<String> whiteListedApps = whitelistService.getCurrentWhitelistedApps();
        nonWhitelistedApps = apps.stream()
                .filter(app -> !whiteListedApps.contains(app.getClassName())
                        && !BuildConfig.APPLICATION_ID.equals(app.getPackageName()))
                .map(app -> new DisplayApp(app.getLabel(), app.getClassName(), app.getIcon(), null))
                .collect(Collectors.toList());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_whitelist, container, false);

        Spinner delayList = view.findViewById(R.id.fragment_whitelist_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.delay_values, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        delayList.setAdapter(adapter);
        delayList.setSelection(whitelistService.getDelayDisplayValuePosition());

        Button apply = view.findViewById(R.id.fragment_whitelist_apply_button);
        apply.setOnClickListener(button -> {
            String spinnerValue = delayList.getSelectedItem().toString();
            long millisValue = WhitelistService.valueInMilliSeconds(spinnerValue);
            if (millisValue >= whitelistService.getCurrentDelayMillis()) {
                whitelistService.increaseDelay(millisValue);
                Toast.makeText(getContext(), R.string.delay_set, Toast.LENGTH_SHORT).show();
            } else {
                whitelistService.queueDelayReduction(millisValue);
                Toast.makeText(getContext(), R.string.change_queued, Toast.LENGTH_SHORT).show();
            }
        });

        GridView gridView = view.findViewById(R.id.fragment_whitelist_grid);
        gridView.setAdapter(new WhitelistFragmentAdapter(getContext(), nonWhitelistedApps));

        Button whitelist = view.findViewById(R.id.fragment_whitelist_button);
        whitelist.setOnClickListener(button -> {
            for (DisplayApp displayApp : nonWhitelistedApps) {
                if (displayApp.isSelected()) {
                    whitelistService.queueApp(displayApp.getActivityName());
                }
            }
        });

        return view;
    }

}
