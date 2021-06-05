package com.hyperion.skywall.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.benny.openlauncher.BuildConfig;
import com.hyperion.skywall.activity.BlockedActivity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class WindowChangeDetectingService extends AccessibilityService {

    private static final String TAG = WindowChangeDetectingService.class.getSimpleName();

    private WhitelistService whitelistService;
    public static final String ACTIVITY_NAME = "activityName";

    private static final Set<String> allowedAndroidActivities = new HashSet<>(
            Arrays.asList("com.android.internal.app.ResolverActivity",
                    "com.android.internal.app.ChooserActivity",
                    "com.android.settings.Settings$WifiSettings2Activity",
                    "com.android.settings.Settings$ConnectedDeviceDashboardActivity",
                    "com.android.quickstep.RecentsActivity",
                    "com.android.settings.password.ConfirmDeviceCredentialActivity"));

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        whitelistService = WhitelistService.getInstance(getApplicationContext());

        //Configure these here for compatibility with API 13 and below.
        AccessibilityServiceInfo config = new AccessibilityServiceInfo();
        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;

        setServiceInfo(config);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null && event.getClassName() != null) {

                String packageName = event.getPackageName().toString();
                String className = event.getClassName().toString();
                ComponentName componentName = new ComponentName(packageName, className);
                ActivityInfo activityInfo = tryGetActivity(componentName);

                boolean isActivity = activityInfo != null;
                if (isActivity) {
                    Log.i("CurrentActivity", componentName.flattenToShortString());
                    if (!allowedAndroidActivities.contains(className)) {
                        if (!whitelistService.refreshAndCheckWhitelisted(packageName)) {
                            Log.i(TAG, "Found non-whitelisted foreground activity: " + className);

                            Intent intent = new Intent(Intent.ACTION_MAIN);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            String canonicalName = BlockedActivity.class.getCanonicalName();
                            intent.setClassName(BuildConfig.APPLICATION_ID, canonicalName);
                            intent.putExtra(ACTIVITY_NAME, className);
                            getApplicationContext().startActivity(intent);
                        }
                    }
                }
            }
        }
    }

    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public void onInterrupt() {}
}