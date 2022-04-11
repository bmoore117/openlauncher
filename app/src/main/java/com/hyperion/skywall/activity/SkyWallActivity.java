package com.hyperion.skywall.activity;

import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.benny.openlauncher.R;
import com.hyperion.skywall.fragment.LoginFragment;
import com.hyperion.skywall.fragment.MainFragment;
import com.hyperion.skywall.fragment.PendingFragment;
import com.hyperion.skywall.fragment.RemoveFragment;
import com.hyperion.skywall.fragment.WhitelistFragment;
import com.hyperion.skywall.service.AuthService;
import com.hyperion.skywall.service.WhitelistService;

public class SkyWallActivity extends AppCompatActivity {

    private static FragmentManager fragmentManager;
    private static MainFragment mainFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_skywall);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle(R.string.skywall);

        AuthService authService = AuthService.getInstance(this);
        mainFragment = new MainFragment();
        LoginFragment loginFragment = new LoginFragment();
        fragmentManager = getSupportFragmentManager();

        if (authService.isLicensed()) {
            SkyWallActivity.doTransition(mainFragment);
        } else {
            SkyWallActivity.doTransition(loginFragment);
        }
    }

    public static void doTransition(Fragment fragment) {
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.activity_skywall_framelayout, fragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();
    }

    public static MainFragment getMainFragment() {
        return mainFragment;
    }
}
