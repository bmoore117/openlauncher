package net.skywall.fragment;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import net.skywall.openlauncher.BuildConfig;
import net.skywall.openlauncher.R;
import com.google.android.material.tabs.TabLayout;
import net.skywall.activity.SkyWallActivity;
import net.skywall.service.SkywallService;
import net.skywall.service.WhitelistService;
import net.skywall.service.WindowChangeDetectingService;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainFragment extends Fragment {

    private FragmentManager fragmentManager;
    private SkywallService skywallService;
    private static final AtomicBoolean startupChecksPassed = new AtomicBoolean(false);
    private WhitelistService whitelistService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        skywallService = SkywallService.getInstance(getContext());
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
        TabLayout.Tab fourthTab = tabLayout.newTab();
        fourthTab.setText(R.string.deprovision);
        tabLayout.addTab(fourthTab);


        whitelistService = WhitelistService.getInstance(getContext());
        SkywallService.getInstance(getContext());
        WhitelistFragment whitelistFragment = new WhitelistFragment();
        PendingFragment pendingFragment = new PendingFragment();
        RemoveFragment removeFragment = new RemoveFragment();
        DeprovisionFragment deprovisionFragment = new DeprovisionFragment();
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
                        break;
                    case 3:
                        fragment = deprovisionFragment;
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

    @Override
    public void onResume() {
        super.onResume();
        Context context = getContext();
        if (!startupChecksPassed.get()) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                if (skywallService.isLicensed()) {
                    // here we check for the proper permissions and throw up screens
                    // this is all run on a delay as the OS does not seem to start accessibility services
                    // until a second or two after boot. Of note, without being the default home app
                    // the accessibility service will not light up and run, at least on the emulator
                    if (!isHomeApp(context)) {
                        showDialog(getString(R.string.default_home_app_prompt), getString(R.string.default_home_app_title), () -> {
                            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                            startupChecksPassed.set(false);
                        });
                    } else if (!isDeviceAdmin(context)) {
                        showDialog(getString(R.string.device_admin_prompt), getString(R.string.device_admin_prompt_title), () -> {
                            startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
                            startupChecksPassed.set(false);
                        });
                    } else if (!isAccessibilityServiceEnabled(context)) {
                        String prompt;
                        if (whitelistService.getCurrentDelayMillis() > 0) {
                            whitelistService.setDelay(0); // should immediately set delay 0
                            prompt = getString(R.string.accessibility_has_reset);
                        } else {
                            prompt = getString(R.string.accessibility_prompt);
                        }
                        showDialog(prompt, getString(R.string.accessibility_prompt_title), () -> {
                            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                            startupChecksPassed.set(false);
                        });
                    } else {
                        startupChecksPassed.set(true);
                    }
                }
            }, 1500);
        }
    }

    private void showDialog(String message, String title, Runnable positiveAction) {
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel())
                .setPositiveButton("OK",
                        (dialog, id) -> positiveAction.run()).create();
        alertDialog.show();
    }

    private boolean isHomeApp(Context context) {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        final ResolveInfo res = context.getPackageManager().resolveActivity(intent, 0);
        return res != null && res.activityInfo != null && context.getPackageName()
                .equals(res.activityInfo.packageName);
    }

    private boolean isDeviceAdmin(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        List<ComponentName> activeAdmins = dpm.getActiveAdmins();
        if (activeAdmins != null) {
            return activeAdmins.stream().anyMatch(componentName -> componentName.getPackageName().equals(BuildConfig.APPLICATION_ID));
        }
        return false;
    }

    public boolean isAccessibilityServiceEnabled(Context context) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
            if (enabledServiceInfo.packageName.equals(context.getPackageName()) && enabledServiceInfo.name.equals(WindowChangeDetectingService.class.getName())) {
                return true;
            }
        }

        return false;
    }

    private void doTransition(Fragment fragment) {
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.fragment_main_framelayout, fragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.logout, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_logout_logout_btn) {
            skywallService.logout();
            SkyWallActivity.doTransition(SkyWallActivity.getLoginFragment());
            return true;
        } else if (item.getItemId() == R.id.menu_logout_info_btn) {
            SkyWallActivity.doTransition(SkyWallActivity.getInfoFragment());
        }
        return super.onOptionsItemSelected(item); // important line
    }
}
