package net.skywall.receivers;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.util.Log;

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
                } else if (intent.getAction().equals("android.app.action.DEVICE_ADMIN_ENABLED")) {
                    manager.addUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT);
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to change safe mode restriction: " + intent.getAction(), e);
            }
        }
    }
}
