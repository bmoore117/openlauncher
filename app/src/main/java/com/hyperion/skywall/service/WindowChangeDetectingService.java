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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WindowChangeDetectingService extends AccessibilityService {

    private static final String TAG = WindowChangeDetectingService.class.getSimpleName();

    private WhitelistService whitelistService;
    public static final String ACTIVITY_NAME = "activityName";

    private static final Set<String> allowedAndroidSystemActivities = new HashSet<>(
            Arrays.asList("com.android.internal.app.ResolverActivity",
                    "com.android.internal.app.ChooserActivity",
                    "com.android.permissioncontroller.permission.ui.GrantPermissionsActivity",
                    "com.android.quickstep.RecentsActivity",
                    "com.android.settings.password.ConfirmDeviceCredentialActivity",
                    "com.android.settings.Settings$WifiSettings2Activity",
                    "com.android.settings.Settings$ConnectedDeviceDashboardActivity",
                    "com.android.settings.Settings$ManageExternalStorageActivity",
                    "com.android.settings.SubSettings",
                    "com.android.systemui.pip.phone.PipMenuActivity",
                    "com.google.android.gms.auth.api.credentials.ui.CredentialPickerActivity",
                    "com.google.android.gms.wallet.activity.GenericDelegatorInternalActivity"));

    private static final Set<String> blockedActivities = new HashSet<>(
            Collections.singletonList("com.facebook.browser.lite.BrowserLiteActivity"));

    @Override
    protected void onServiceConnected() {
        try  {
            super.onServiceConnected();

            whitelistService = WhitelistService.getInstance(getApplicationContext());

            //Configure these here for compatibility with API 13 and below.
            AccessibilityServiceInfo config = new AccessibilityServiceInfo();
            config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
            config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;

            setServiceInfo(config);
        } catch (RuntimeException e) {
            Log.e(TAG, "Error starting " + TAG, e);
            throw e;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (event.getPackageName() != null && event.getClassName() != null) {
                    String packageName = event.getPackageName().toString();
                    String className = event.getClassName().toString();
                    ComponentName componentName = new ComponentName(packageName, className);
                    ActivityInfo activityInfo = tryGetActivity(componentName);

                    boolean isActivity = activityInfo != null;
                    if (isActivity) {
                        Log.i(TAG, "CurrentActivity: " + componentName.flattenToShortString());
                        if (allowedAndroidSystemActivities.contains(className)) {
                            return;
                        }

                        if (blockedActivities.contains(className)) {
                            blockActivity(className);
                        }

                        if (!whitelistService.refreshAndCheckWhitelisted(packageName)) {
                            Log.i(TAG, "Found non-whitelisted foreground activity: " + className);
                            blockActivity(className);
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Error while checking screen contents", e);
            throw e;
        }
    }

    private void blockActivity(String className) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String canonicalName = BlockedActivity.class.getCanonicalName();
        intent.setClassName(BuildConfig.APPLICATION_ID, canonicalName);
        intent.putExtra(ACTIVITY_NAME, className);
        getApplicationContext().startActivity(intent);
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

    @Override
    public void onLowMemory() {
        Log.w(TAG, "Bumped for low memory");
    }

    @Override
    public void onTrimMemory(int level) {
        Log.w(TAG, "Bumped to trim memory, level: " + level);
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "Service destroyed");
    }
}