package com.hyperion.skywall.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.benny.openlauncher.R;
import com.benny.openlauncher.activity.ColorActivity;
import com.hyperion.skywall.fragment.LoginFragment;
import com.hyperion.skywall.fragment.MainFragment;
import com.hyperion.skywall.service.AuthService;

public class SkyWallActivity extends ColorActivity {

    private static FragmentManager fragmentManager;
    private static MainFragment mainFragment;
    private static LoginFragment loginFragment;

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
        loginFragment = new LoginFragment();
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

    public static LoginFragment getLoginFragment() {
        return loginFragment;
    }

    @Override
    public void onDestroy() {
        fragmentManager = null;
        mainFragment = null;
        loginFragment = null;
        super.onDestroy();
    }
}
