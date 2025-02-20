package net.skywall.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.App;

import net.skywall.fragment.view.DisplayApp;
import net.skywall.openlauncher.R;
import net.skywall.service.SkywallService;
import net.skywall.service.WhitelistService;
import net.skywall.utils.LicenseUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WhitelistFragment extends Fragment {

    private static final String TAG = WhitelistFragment.class.getSimpleName();

    private final WhitelistService whitelistService;
    private final SkywallService skywallService;
    private List<DisplayApp> nonWhitelistedApps;

    public WhitelistFragment() {
        whitelistService = WhitelistService.getInstance(getContext());
        skywallService = SkywallService.getInstance(getContext());
        nonWhitelistedApps = new ArrayList<>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        List<App> apps = Setup.appLoader().getAllApps(false);
        Set<String> whiteListedApps = whitelistService.getCurrentWhitelistedApps();
        nonWhitelistedApps = apps.stream()
                .filter(app -> !whiteListedApps.contains(app.getPackageName())
                        && !WhitelistService.isPredefinedAllowedApp(app.getPackageName(), null)
                        && !whitelistService.isAppPending(app.getPackageName()))
                .map(app -> new DisplayApp(app.getLabel(), app.getPackageName(), app.getIcon(), null))
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
                whitelistService.setDelay(millisValue);
                // making toast with getContext() will use the custom theme, breaks in android 13. Use system theme from application
                Toast.makeText(getContext().getApplicationContext(), R.string.delay_set, Toast.LENGTH_SHORT).show();
            } else {
                whitelistService.queueDelayReduction(millisValue);
                Toast.makeText(getContext().getApplicationContext(), R.string.change_queued, Toast.LENGTH_SHORT).show();
            }
        });

        GridView gridView = view.findViewById(R.id.fragment_whitelist_grid);
        WhitelistFragmentAdapter fragmentAdapter = new WhitelistFragmentAdapter(getContext(), nonWhitelistedApps);
        gridView.setAdapter(fragmentAdapter);

        EditText search = view.findViewById(R.id.fragment_whitelist_search);
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

        Button whitelist = view.findViewById(R.id.fragment_whitelist_button);
        whitelist.setOnClickListener(button -> LicenseUtils.performOrShowMessage(() -> {
            boolean queued = false;
            Iterator<DisplayApp> it = nonWhitelistedApps.iterator();
            while (it.hasNext()) {
                DisplayApp displayApp = it.next();
                if (displayApp.isSelected()) {
                    whitelistService.queueApp(displayApp.getPackageName());
                    queued = true;
                    it.remove();
                }
            }
            if (queued) {
                fragmentAdapter.notifyDataSetChanged();
                if (whitelistService.getCurrentDelayMillis() > 0) {
                    Toast.makeText(getContext().getApplicationContext(), R.string.changes_queued, Toast.LENGTH_SHORT).show();
                }
            }
        }, skywallService::isLicensed, getContext()));

        return view;
    }

}
