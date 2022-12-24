package net.skywall.activity;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import net.skywall.fragment.InfoFragment;
import net.skywall.openlauncher.R;
import com.benny.openlauncher.activity.ColorActivity;
import net.skywall.fragment.LoginFragment;
import net.skywall.fragment.MainFragment;
import net.skywall.service.SkywallService;

public class SkyWallActivity extends ColorActivity {

    private static FragmentManager fragmentManager;
    private static MainFragment mainFragment;
    private static LoginFragment loginFragment;
    private static InfoFragment infoFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_skywall);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle(R.string.skywall);

        SkywallService skywallService = SkywallService.getInstance(this);
        mainFragment = new MainFragment();
        loginFragment = new LoginFragment();
        infoFragment = new InfoFragment();
        fragmentManager = getSupportFragmentManager();

        if (!skywallService.isLicensed() && skywallService.getUsername() == null) {
            SkyWallActivity.doTransition(loginFragment);
        } else {
            SkyWallActivity.doTransition(mainFragment);
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

    public static InfoFragment getInfoFragment() {
        return infoFragment;
    }

    @Override
    public void onDestroy() {
        fragmentManager = null;
        mainFragment = null;
        loginFragment = null;
        infoFragment = null;
        super.onDestroy();
    }
}
