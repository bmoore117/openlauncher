package com.hyperion.skywall.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.Spinner;

import com.benny.openlauncher.R;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.App;
import com.hyperion.skywall.service.WhitelistService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WhitelistFragment extends Fragment {

    private static final String TAG = WhitelistFragment.class.getSimpleName();

    private final WhitelistService whitelistService;
    private List<App> nonWhitelistedApps;

    public WhitelistFragment() {
        whitelistService = WhitelistService.getInstance(getContext());
        nonWhitelistedApps = new ArrayList<>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        List<App> apps = Setup.appLoader().getAllApps(getContext(), false);
        Set<String> whiteListedApps = whitelistService.getCurrentActiveApps();
        nonWhitelistedApps = apps.stream().filter(app -> !whiteListedApps.contains(app.getPackageName())).collect(Collectors.toList());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_whitelist, container, false);

        Spinner delayList = view.findViewById(R.id.fragment_whitelist_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.delay_values, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        delayList.setAdapter(adapter);

        Button apply = view.findViewById(R.id.fragment_whitelist_apply_button);
        apply.setOnClickListener(button -> {
            String spinnerValue = delayList.getSelectedItem().toString();
            long millisValue = valueInMilliSeconds(spinnerValue);
            if (millisValue >= whitelistService.getCurrentDelayMillis()) {
                whitelistService.increaseDelay(millisValue);
            } else {
                whitelistService.queueDelayReduction(millisValue);
            }
        });

        GridView gridView = view.findViewById(R.id.fragment_whitelist_grid);
        gridView.setAdapter(new WhitelistFragmentAdapter(getContext(), nonWhitelistedApps));

        Button whitelist = view.findViewById(R.id.fragment_whitelist_button);
        whitelist.setOnClickListener(button -> {
            for (int i = 0; i < gridView.getChildCount(); i++) {
                View child = gridView.getChildAt(i);
                CheckBox checkBox = child.findViewById(R.id.item_whitelist_checkbox);
                if (checkBox.isChecked()) {
                    whitelistService.queueApp(((App) adapter.getItem(i)).getPackageName());
                }
            }
        });

        return view;
    }

    public static long valueInMilliSeconds(String delay) {
        String[] parts = delay.split(" ");
        if ("0".equals(parts[0])) {
            return 0;
        } else if (parts[1].contains("minute")) {
            return Integer.parseInt(parts[0])*60*1000;
        } else {
            return Integer.parseInt(parts[0])*60*60*1000;
        }
    }

}
