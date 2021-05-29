package com.hyperion.skywall.activity;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.FrameLayout;

import com.benny.openlauncher.R;
import com.hyperion.skywall.fragment.PendingFragment;
import com.hyperion.skywall.fragment.WhitelistFragment;
import com.hyperion.skywall.service.WhitelistService;

public class SkyWallActivity extends AppCompatActivity {

    private FragmentManager fragmentManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_skywall);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle(R.string.skywall);

        TabLayout tabLayout = findViewById(R.id.activity_skywall_tablayout);

        TabLayout.Tab firstTab = tabLayout.newTab();
        firstTab.setText("Whitelist");
        tabLayout.addTab(firstTab);
        TabLayout.Tab secondTab = tabLayout.newTab();
        secondTab.setText("Pending Changes");
        tabLayout.addTab(secondTab);

        WhitelistService.getInstance(this);
        WhitelistFragment whitelistFragment = new WhitelistFragment();
        PendingFragment pendingFragment = new PendingFragment();
        fragmentManager = getSupportFragmentManager();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Fragment fragment = null;

                switch (tab.getPosition()) {
                    case 0:
                        fragment = whitelistFragment;
                        break;
                    case 1:
                        fragment = pendingFragment;
                        break;
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
    }

    private void doTransition(Fragment fragment) {
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.activity_skywall_framelayout, fragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();
    }
}
