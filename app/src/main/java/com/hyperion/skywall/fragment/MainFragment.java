package com.hyperion.skywall.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.benny.openlauncher.R;
import com.google.android.material.tabs.TabLayout;
import com.hyperion.skywall.service.WhitelistService;

public class MainFragment extends Fragment {

    private FragmentManager fragmentManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        TabLayout tabLayout = view.findViewById(R.id.fragment_main_tablayout);

        TabLayout.Tab firstTab = tabLayout.newTab();
        firstTab.setText(R.string.add);
        tabLayout.addTab(firstTab);
        TabLayout.Tab secondTab = tabLayout.newTab();
        secondTab.setText(R.string.remove);
        tabLayout.addTab(secondTab);
        TabLayout.Tab thirdTab = tabLayout.newTab();
        thirdTab.setText(R.string.pending);
        tabLayout.addTab(thirdTab);

        WhitelistService.getInstance(getContext());
        WhitelistFragment whitelistFragment = new WhitelistFragment();
        PendingFragment pendingFragment = new PendingFragment();
        RemoveFragment removeFragment = new RemoveFragment();
        fragmentManager = getChildFragmentManager();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Fragment fragment = null;

                switch (tab.getPosition()) {
                    case 0:
                        fragment = whitelistFragment;
                        break;
                    case 1:
                        fragment = removeFragment;
                        break;
                    case 2:
                        fragment = pendingFragment;
                }
                doTransition(fragment);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        doTransition(whitelistFragment);

        return view;
    }

    private void doTransition(Fragment fragment) {
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.fragment_main_framelayout, fragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();
    }
}
