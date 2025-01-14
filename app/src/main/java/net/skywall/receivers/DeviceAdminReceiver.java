package net.skywall.receivers;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserManager;
import android.util.Log;

import com.benny.openlauncher.activity.HomeActivity;

public class DeviceAdminReceiver extends BroadcastReceiver {

    public static final String TAG = "DeviceAdminReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "DeviceAdmin received");

        DevicePolicyManager manager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(context.getApplicationContext(), DeviceAdminReceiver.class);
        if (manager.isAdminActive(admin)) {
            try {
                if (intent.getAction().equals("android.app.action.DEVICE_ADMIN_DISABLE_REQUESTED")) {
                    manager.clearUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT);
                    // not sure if needed - .\adb.exe shell dpm remove-active-admin doesn't trip
                    // this code anyway yet we can still change default home app afterward
                    manager.clearPackagePersistentPreferredActivities(admin, context.getPackageName());
                } else if (intent.getAction().equals("android.app.action.DEVICE_ADMIN_ENABLED")) {
                    manager.addUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT);

                    // Create an intent filter to specify the Home category.
                    IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
                    filter.addCategory(Intent.CATEGORY_HOME);
                    filter.addCategory(Intent.CATEGORY_DEFAULT);

                    // Set the activity as the preferred option for the device.
                    ComponentName activity = new ComponentName(context, HomeActivity.class);
                    manager.addPersistentPreferredActivity(admin, filter, activity);
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to change policy: " + intent.getAction(), e);
            }
        }
    }
}
