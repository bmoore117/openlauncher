package com.benny.openlauncher.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.benny.openlauncher.AppObject;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.util.Definitions;
import com.benny.openlauncher.util.Tool;

import net.skywall.openlauncher.R;

public class AddShortcutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null && getIntent().getAction().equalsIgnoreCase(LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT)) {
            LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
            LauncherApps.PinItemRequest request = launcherApps.getPinItemRequest(getIntent());
            if (request == null) {
                finish();
                return;
            }

            if (request.getRequestType() == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
                
                ShortcutInfo info = request.getShortcutInfo();
                String shortcutLabel = info.getShortLabel().toString();
                Intent shortcutIntent = new Intent(Intent.ACTION_MAIN)
                    .setComponent(info.getActivity())
                    .setPackage(info.getPackage())
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    .putExtra("shortcut_id", info.getId());

                Context context = AppObject.get().getApplicationContext();
                Drawable shortcutIcon = launcherApps.getShortcutIconDrawable(info, context.getResources().getDisplayMetrics().densityDpi);

                Item item = Item.newShortcutItem(shortcutIntent, shortcutIcon, shortcutLabel);
                Point preferredPos = HomeActivity.getCurrentInstance().getDesktop().getPages().get(HomeActivity.getCurrentInstance().getDesktop().getCurrentItem()).findFreeSpace();
                
                if (preferredPos == null) {
                    Tool.toast(HomeActivity.getCurrentInstance(), R.string.toast_not_enough_space);
                } else {
                    item.setX(preferredPos.x);
                    item.setY(preferredPos.y);
                    Setup.dataManager().saveItem(item, HomeActivity.getCurrentInstance().getDesktop().getCurrentItem(), Definitions.ItemPosition.Desktop);
                    HomeActivity.getCurrentInstance().getDesktop().addItemToPage(item, HomeActivity.getCurrentInstance().getDesktop().getCurrentItem());
                    Log.d(this.getClass().toString(), "shortcut installed");
                }

                request.accept();
                finish();
            }
        }
    }
}
