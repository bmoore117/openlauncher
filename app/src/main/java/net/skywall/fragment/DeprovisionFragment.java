package net.skywall.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.fragment.app.Fragment;

import net.skywall.fragment.view.Phone;
import net.skywall.openlauncher.R;
import net.skywall.service.SkywallService;
import net.skywall.service.WhitelistService;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DeprovisionFragment extends Fragment {

    private static final String TAG = DeprovisionFragment.class.getSimpleName();

    private final WhitelistService whitelistService;
    private final SkywallService skywallService;
    private final List<Phone> phones;
    private DeprovisionFragmentAdapter fragmentAdapter;

    public DeprovisionFragment() {
        whitelistService = WhitelistService.getInstance(getContext());
        skywallService = SkywallService.getInstance(getContext());
        phones = new ArrayList<>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_deprovision, container, false);

        GridView gridView = view.findViewById(R.id.fragment_deprovision_grid);

        ProgressBar progressBar = view.findViewById(R.id.fragment_deprovision_progress_bar);
        LinearLayout progressBarPlate = view.findViewById(R.id.fragment_deprovision_progress_bar_plate);

        progressBarPlate.setVisibility(View.VISIBLE);
        progressBarPlate.setZ(1000.0f);
        progressBar.setIndeterminate(true);

        final Context context = getContext();
        final Handler handler = new Handler(Looper.getMainLooper());
        CompletableFuture.runAsync(() -> {
            try {
                List<Phone> fetched = skywallService.fetchUserPhones();
                phones.clear();
                phones.addAll(fetched);
                handler.post(() -> {
                    fragmentAdapter = new DeprovisionFragmentAdapter(context, phones, whitelistService, skywallService);
                    gridView.setAdapter(fragmentAdapter);
                    progressBarPlate.setVisibility(View.GONE);
                    fragmentAdapter.notifyDataSetChanged();
                });
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error fetching user phones", e);
            }
        });

        return view;
    }
}

