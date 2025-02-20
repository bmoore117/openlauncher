package com.benny.openlauncher.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.util.Log;

import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.activity.homeparts.HpInitSetup;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.App;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.util.Definitions;
import com.benny.openlauncher.util.Tool;

import net.skywall.openlauncher.R;

public class ShortcutReceiver extends BroadcastReceiver {

    public ShortcutReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getExtras() == null) return;

        // this will only work before Android Oreo
        // was deprecated in favor of ShortcutManager.pinRequestShortcut()
        String shortcutLabel = intent.getExtras().getString(Intent.EXTRA_SHORTCUT_NAME);
        Intent shortcutIntent = (Intent) intent.getExtras().get(Intent.EXTRA_SHORTCUT_INTENT);
        Drawable shortcutIcon = null;

        try {
            Parcelable parcelable = intent.getExtras().getParcelable(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (parcelable instanceof Intent.ShortcutIconResource) {
                Intent.ShortcutIconResource iconResource = (Intent.ShortcutIconResource) parcelable;
                Resources resources = context.getPackageManager().getResourcesForApplication(iconResource.packageName);
                if (resources != null) {
                    int id = resources.getIdentifier(iconResource.resourceName, null, null);
                    shortcutIcon = resources.getDrawable(id);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (shortcutIcon == null)
                shortcutIcon = new BitmapDrawable(context.getResources(), (Bitmap) intent.getExtras().getParcelable(Intent.EXTRA_SHORTCUT_ICON));
        }

        if (!Setup.wasInitialised()) {
            Setup.init(new HpInitSetup(context));
        }

        App app = Setup.appLoader().createApp(shortcutIntent);
        Item item;
        if (app != null) {
            item = Item.newAppItem(app);
        } else {
            item = Item.newShortcutItem(shortcutIntent, shortcutIcon, shortcutLabel);
        }
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
    }
}
