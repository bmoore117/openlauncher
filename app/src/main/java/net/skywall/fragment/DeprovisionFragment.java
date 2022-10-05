package net.skywall.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

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
        CompletableFuture.runAsync(() -> {
            try {
                List<Phone> fetched = skywallService.fetchUserPhones();
                phones.clear();
                phones.addAll(fetched);
                if (fragmentAdapter != null) {
                    fragmentAdapter.notifyDataSetChanged();
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error fetching user phones", e);
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentAdapter = new DeprovisionFragmentAdapter(getContext(), phones, whitelistService, skywallService);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_deprovision, container, false);

        GridView gridView = view.findViewById(R.id.fragment_deprovision_grid);
        gridView.setAdapter(fragmentAdapter);

        return view;
    }
}

