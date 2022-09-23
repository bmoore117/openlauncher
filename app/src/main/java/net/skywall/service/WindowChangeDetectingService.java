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

import com.benny.openlauncher.BuildConfig;
import net.skywall.activity.BlockedActivity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WindowChangeDetectingService extends AccessibilityService {

    private static final String TAG = WindowChangeDetectingService.class.getSimpleName();
    public static final String SUB_SETTINGS = "com.android.settings/.SubSettings";
    public static final String DEFAULT_APP_ACTIVITY = "com.google.android.permissioncontroller/com.android.permissioncontroller.role.ui.DefaultAppActivity";

    private WhitelistService whitelistService;
    public static final String ACTIVITY_NAME = "activityName";

    private static final Set<String> allowedActivities = new HashSet<>(
            Arrays.asList("com.android.captiveportallogin.CaptivePortalLoginActivity",
                    "com.android.cellbroadcastreceiver.CellBroadcastAlertDialog",
                    "com.android.certinstaller.CertInstallerMain",
                    "com.android.documentsui.picker.PickActivity",
                    "com.android.internal.app.ResolverActivity",
                    "com.android.internal.app.ChooserActivity",
                    "com.android.packageinstaller.UninstallerActivity",
                    "com.android.permissioncontroller.permission.ui.GrantPermissionsActivity",
                    "com.android.permissioncontroller.permission.ui.ManagePermissionsActivity",
                    "com.android.permissioncontroller.role.ui.RequestRoleActivity",
                    "com.android.quickstep.RecentsActivity",
                    "com.android.systemui.pip.phone.PipMenuActivity",
                    "com.facebook.gdp.LightWeightProxyAuthActivity",
                    "com.facebook.katana.gdp.ProxyAuthDialog",
                    "com.facebook.katana.gdp.WebViewProxyAuth",
                    "com.google.android.apps.inputmethod.latin.preference.SettingsActivity",
                    "com.google.android.gms.appinvite.AppInviteAcceptInvitationActivity",
                    "com.google.android.gms.auth.api.credentials.assistedsignin.ui.AssistedSignInActivity",
                    "com.google.android.gms.auth.api.credentials.ui.CredentialPickerActivity",
                    "com.google.android.gms.auth.api.signin.ui.SignInActivity",
                    "com.google.android.gms.auth.uiflows.addaccount.PreAddAccountActivity",
                    "com.google.android.gms.auth.uiflows.addaccount.WrapperControlledActivity",
                    "com.google.android.gms.auth.uiflows.minutemaid.MinuteMaidActivity",
                    "com.google.android.gms.common.account.AccountPickerActivity",
                    "com.google.android.gms.common.api.GoogleApiActivity",
                    "com.google.android.gms.pay.main.PayActivity",
                    "com.google.android.gms.octarine.ui.OctarineWebviewActivity",
                    "com.google.android.gms.security.recaptcha.RecaptchaActivity",
                    "com.google.android.gms.setupservices.GoogleServicesActivity",
                    "com.google.android.gms.signin.activity.SignInActivity",
                    "com.google.android.gms.update.phone.PopupDialog",
                    "com.google.android.gms.update.SystemUpdateActivity",
                    "com.google.android.gms.wallet.activity.GenericDelegatorInternalActivity",
                    "com.google.android.location.settings.LocationSettingsCheckerActivity"));

    private static final Set<String> blockedActivities = new HashSet<>(
            Arrays.asList("com.facebook.browser.lite.BrowserLiteActivity",
                    "com.microsoft.emmx.webview.browser.InAppBrowserActivity"));

    private static final Set<String> WEWORK_LOGIN_ACTIVITIES = new HashSet<>(
            Arrays.asList("com.wework.ondemand/com.auth0.android.provider.AuthenticationActivity",
                    "com.wework.ondemand/.splash.welcome.WelcomeActivity"));

    private String lastActivity;

    @Override
    protected void onServiceConnected() {
        try  {
            super.onServiceConnected();
            whitelistService = WhitelistService.getInstance(getApplicationContext());
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

                        // here we prevent the user from being able to disable the accessibility service
                        // or deactivate device admin for this app, or change the home screen to something else
                        // this enables us to allow the rest of the settings app
                        if (whitelistService.getCurrentDelayMillis() > 0) {
                            if (SUB_SETTINGS.equals(componentName.flattenToShortString())) {
                                String screenTitle = event.getText().isEmpty() ? "" : event.getText().get(0).toString();
                                List<AccessibilityNodeInfo> nodes = event.getSource().findAccessibilityNodeInfosByText("Use SkyWall");
                                if (!nodes.isEmpty() || "device admin apps".equalsIgnoreCase(screenTitle)
                                        || "install unknown apps".equalsIgnoreCase(screenTitle)) {
                                    performGlobalAction(GLOBAL_ACTION_BACK);
                                }
                            } else if (DEFAULT_APP_ACTIVITY.equals(componentName.flattenToShortString())) {
                                String screenTitle = event.getText().isEmpty() ? "" : event.getText().get(0).toString();
                                if ("default home app".equalsIgnoreCase(screenTitle)) {
                                    performGlobalAction(GLOBAL_ACTION_BACK);
                                }
                            }
                        }

                        if (allowedActivities.contains(className)) {
                            lastActivity = componentName.flattenToShortString();
                            return;
                        }

                        if (blockedActivities.contains(className) && whitelistService.getCurrentDelayMillis() > 0) {
                            lastActivity = componentName.flattenToShortString();
                            blockActivity(className);
                            return;
                        }

                        if (lastActivity != null && WEWORK_LOGIN_ACTIVITIES.contains(lastActivity)) {
                            lastActivity = componentName.flattenToShortString();
                            return;
                        }

                        if (!whitelistService.refreshAndCheckWhitelisted(packageName)) {
                            Log.i(TAG, "Found non-whitelisted foreground activity: " + className);
                            blockActivity(className);
                        }
                        lastActivity = componentName.flattenToShortString();
                    }
                }
            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) {
                if (whitelistService.getCurrentDelayMillis() > 0) {
                    if ("com.android.systemui".equals(event.getPackageName().toString())
                            && event.getText().stream().anyMatch(seq ->
                            "power off".equalsIgnoreCase(seq.toString())
                                    || "restart".equalsIgnoreCase(seq.toString()))) {
                        for (int i = 0; i < 1000; i++) {
                            performGlobalAction(GLOBAL_ACTION_BACK);
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Error while checking screen contents", e);
            throw e;
        }
    }

    private boolean isHomeApp() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        final ResolveInfo res = getPackageManager().resolveActivity(intent, 0);
        return res != null && res.activityInfo != null && getPackageName()
                .equals(res.activityInfo.packageName);
    }

    private void blockActivity(String className) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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