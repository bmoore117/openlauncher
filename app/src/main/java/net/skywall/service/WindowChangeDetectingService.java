package net.skywall.service;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import net.skywall.activity.BlockedActivity;
import net.skywall.openlauncher.BuildConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WindowChangeDetectingService extends AccessibilityService {

    private static final String TAG = WindowChangeDetectingService.class.getSimpleName();
    public static final String SUB_SETTINGS = "com.android.settings/.SubSettings";
    public static final String SUB_SETTINGS_ANDROID_15 = "com.android.settings/.spa.SpaActivity";
    public static final String GOOGLE_ASSISTANT_SAFESEARCH_SETTINGS_ACTIVITY = "com.google.android.googlequicksearchbox/com.google.android.apps.search.googleapp.search.settings.safesearch.SafeSearchSettingActivity";
    public static final String GOOGLE_ASSISTANT_SETTINGS_MENU = "com.google.android.googlequicksearchbox/com.google.android.apps.search.googleapp.settingsui.SettingsActivity";

    private WhitelistService whitelistService;
    public static final String ACTIVITY_NAME = "activityName";
    public static final String APP_NAME = "appName";

    private static final Set<String> blockedActivities = new HashSet<>(
            Arrays.asList("com.facebook.browser.lite.BrowserLiteActivity",
                    "com.instagram.inappbrowser.launcher.BrowserLiteInMainProcessIGActivity",
                    "com.instagram.inappbrowser.fragments.BrowserLiteInMainProcessIGActivity",
                    "com.microsoft.emmx.webview.browser.InAppBrowserActivity"));

    private static final Set<String> PROBABLE_LOGIN_ACTIVITIES = new HashSet<>(
            Arrays.asList("authentication", "auth", "welcome", "login", "signup", "credentialpicker"));

    private String lastActivity;
    // default to production value, fetch actual below
    private String skywallAppName = "SkyWall";

    @Override
    protected void onServiceConnected() {
        try  {
            super.onServiceConnected();
            whitelistService = WhitelistService.getInstance(getApplicationContext());
            skywallAppName = getApplicationInfo().loadLabel(getPackageManager()).toString();
        } catch (RuntimeException e) {
            Log.e(TAG, "Error starting " + TAG, e);
            throw e;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // if delay is non-zero, the accessibility service is active, and we're not the default
        // launcher app, we can get trapped in a loop here where everything is blocked. So only
        // block things if we're the default home app
        if (!isHomeApp()) {
            return;
        }

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
                        String appName = activityInfo.applicationInfo.loadLabel(getPackageManager()).toString();

                        // here we prevent the user from being able to disable the accessibility service
                        // or deactivate device admin for this app, or change the home screen to something else
                        // this enables us to allow the rest of the settings app
                        if (whitelistService.getCurrentDelayMillis() > 0) {
                            String screenTitle = event.getText().isEmpty() || event.getText().get(0) == null ? "" : event.getText().get(0).toString();
                            if (SUB_SETTINGS.equals(componentName.flattenToShortString())) {
                                if (event.getSource() != null) { // sometimes happens
                                    List<AccessibilityNodeInfo> nodes = event.getSource().findAccessibilityNodeInfosByText("Use SkyWall");
                                    if (!nodes.isEmpty() || "device admin apps".equalsIgnoreCase(screenTitle)
                                            || "install unknown apps".equalsIgnoreCase(screenTitle)
                                            || "developer options".equalsIgnoreCase(screenTitle)) {
                                        performGlobalAction(GLOBAL_ACTION_BACK);
                                        return;
                                    } else if ("app info".equalsIgnoreCase(screenTitle)
                                            && (!event.getSource().findAccessibilityNodeInfosByText(skywallAppName).isEmpty()
                                                || !event.getSource().findAccessibilityNodeInfosByText("Firefox").isEmpty()
                                                || !event.getSource().findAccessibilityNodeInfosByText("Firefox Beta").isEmpty()
                                                || !event.getSource().findAccessibilityNodeInfosByText("Firefox Nightly").isEmpty())) {
                                        performGlobalAction(GLOBAL_ACTION_BACK);
                                        return;
                                    }
                                }
                            } else if (SUB_SETTINGS_ANDROID_15.equals(componentName.flattenToShortString())) {
                                if ("install unknown apps".equalsIgnoreCase(screenTitle)) {
                                    performGlobalAction(GLOBAL_ACTION_BACK);
                                    return;
                                }

                                String text = getText(getRootInActiveWindow());
                                if ("app info".equalsIgnoreCase(screenTitle) && (text.startsWith(skywallAppName) || text.startsWith("Firefox"))) {
                                    performGlobalAction(GLOBAL_ACTION_BACK);
                                    return;
                                }
                            } else if (GOOGLE_ASSISTANT_SAFESEARCH_SETTINGS_ACTIVITY.equals(componentName.flattenToShortString())
                                    || GOOGLE_ASSISTANT_SETTINGS_MENU.equals(componentName.flattenToShortString())) {
                                // on the emulator, the google app has a specific recognizable explicit
                                // settings activity. On my pixel 4 xl, it does not, and we sadly
                                // have to block all settings, though you theoretically could still
                                // get to the assistant settings specifically by using voice commands
                                performGlobalAction(GLOBAL_ACTION_BACK);
                            }
                        }

                        if (blockedActivities.contains(className) && whitelistService.getCurrentDelayMillis() > 0) {
                            lastActivity = componentName.flattenToShortString();
                            blockActivity(className, appName);
                            return;
                        }

                        // apps frequently start a chromium tab as a linked/hosted activity which
                        // will run under its own package name in a separate process, and thus fail
                        // the ability to log in so if we're on that new activity, check to see if
                        // the last activity was something to do with sign-in, and allow
                        if (lastActivity != null && PROBABLE_LOGIN_ACTIVITIES.stream()
                                .anyMatch(activityTitleWord -> lastActivity.toLowerCase().contains(activityTitleWord))) {
                            lastActivity = componentName.flattenToShortString();
                            return;
                        }

                        if (!whitelistService.refreshAndCheckWhitelisted(packageName, className)) {
                            Log.i(TAG, "Found non-whitelisted foreground activity: " + className);
                            blockActivity(className, appName);
                        }
                        lastActivity = componentName.flattenToShortString();
                    }
                }
            } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                if (whitelistService.getCurrentDelayMillis() == 0) {
                    return;
                }

                if (event.getPackageName() != null) {
                    if ("com.android.settings".equals(event.getPackageName().toString())
                            && getText(getRootInActiveWindow()).startsWith("Firefox")) {
                        performGlobalAction(GLOBAL_ACTION_BACK);
                        return;
                    }

                    if (event.getPackageName().toString().startsWith("org.mozilla.firefox")
                            && lastActivity.matches("org.mozilla.firefox(.*?)/org.mozilla.fenix.HomeActivity")
                            && event.getSource() != null
                            && !event.getSource().findAccessibilityNodeInfosByText("SkyWall").isEmpty()
                            && !event.getSource().findAccessibilityNodeInfosByText("Run in private browsing").isEmpty()) {
                        performGlobalAction(GLOBAL_ACTION_BACK);
                    }
                }
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Error while checking screen contents", e);
            throw e;
        }
    }

    private String getText(AccessibilityNodeInfo root) {
        if (root == null) {
            return "";
        }
        StringBuilder text = new StringBuilder(root.getText() == null || root.getText().toString().isEmpty() ? "" : root.getText() + "\n");
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo node = root.getChild(i);
            text.append(getText(node));
        }
        return text.toString();
    }

    private boolean isHomeApp() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        final ResolveInfo res = getPackageManager().resolveActivity(intent, 0);
        return res != null && res.activityInfo != null && getPackageName()
                .equals(res.activityInfo.packageName);
    }

    private void blockActivity(String className, String appName) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        String canonicalName = BlockedActivity.class.getCanonicalName();
        intent.setClassName(BuildConfig.APPLICATION_ID, canonicalName);
        intent.putExtra(ACTIVITY_NAME, className);
        intent.putExtra(APP_NAME, appName);
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