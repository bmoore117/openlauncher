package com.benny.openlauncher.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;

import androidx.annotation.NonNull;

import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.interfaces.AppDeleteListener;
import com.benny.openlauncher.interfaces.AppUpdateListener;
import com.benny.openlauncher.manager.Setup;
import com.benny.openlauncher.model.App;
import com.benny.openlauncher.model.Item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppManager {
    private static final Logger LOG = LoggerFactory.getLogger(AppManager.class.getSimpleName());

    private static AppManager appManager;

    public static AppManager getInstance(Context context) {
        return appManager == null ? (appManager = new AppManager(context)) : appManager;
    }

    private final PackageManager _packageManager;
    private final LauncherApps launcherApps;
    public final List<AppUpdateListener> _updateListeners;
    public final List<AppDeleteListener> _deleteListeners;
    private final Executor appExecutor;
    private final Handler handler;

    private List<App> _apps = new ArrayList<>();
    private List<App> _nonFilteredApps = new ArrayList<>();

    public AppManager(Context context) {
        _packageManager = context.getPackageManager();
        launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        _updateListeners = new ArrayList<>();
        _deleteListeners = new ArrayList<>();
        appExecutor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
    }

    public PackageManager getPackageManager() {
        return _packageManager;
    }

    public Context getContext() {
        return Setup.appContext();
    }

    public App findApp(Intent intent) {
        if (intent == null || intent.getComponent() == null) return null;

        String packageName = intent.getComponent().getPackageName();
        String className = intent.getComponent().getClassName();
        for (App app : _apps) {
            if (app._className.equals(className) && app._packageName.equals(packageName)) {
                return app;
            }
        }
        return null;
    }

    public List<App> getApps() {
        return _apps;
    }

    public List<App> getNonFilteredApps() {
        return _nonFilteredApps;
    }

    public void refreshApps(boolean recreateHomeActivity) {
        appExecutor.execute(() -> {
            List<App> appsTemp = new ArrayList<>();
            List<App> nonFilteredAppsTemp = new ArrayList<>();
            List<App> removedApps;

            List<UserHandle> profiles = launcherApps.getProfiles();
            for (UserHandle userHandle : profiles) {
                List<LauncherActivityInfo> apps = launcherApps.getActivityList(null, userHandle);
                for (LauncherActivityInfo info : apps) {
                    List<ShortcutInfo> shortcutInfo = Tool.getShortcutInfo(getContext(), info.getComponentName().getPackageName());
                    App app = new App(_packageManager, info, shortcutInfo);
                    app._userHandle = userHandle;
                    LOG.debug("adding work profile to non filtered list: {}, {}, {}", app._label, app._packageName, app._className);
                    nonFilteredAppsTemp.add(app);
                }
            }

            // sort the apps by label here
            nonFilteredAppsTemp.sort((one, two) -> Collator.getInstance().compare(one._label, two._label));

            List<String> hiddenList = AppSettings.get().getHiddenAppsList();
            if (hiddenList != null) {
                for (int i = 0; i < nonFilteredAppsTemp.size(); i++) {
                    boolean shouldGetAway = false;
                    for (String hidItemRaw : hiddenList) {
                        if ((nonFilteredAppsTemp.get(i).getComponentName()).equals(hidItemRaw)) {
                            shouldGetAway = true;
                            break;
                        }
                    }
                    if (!shouldGetAway) {
                        appsTemp.add(nonFilteredAppsTemp.get(i));
                    }
                }
            } else {
                appsTemp.addAll(nonFilteredAppsTemp);
            }

            removedApps = getRemovedApps(_apps, appsTemp);

            for (App app : removedApps) {
                HomeActivity._db.deleteItems(app);
            }

            AppSettings appSettings = AppSettings.get();
            if (!appSettings.getIconPack().isEmpty() && Tool.isPackageInstalled(appSettings.getIconPack(), _packageManager)) {
                IconPackHelper.applyIconPack(AppManager.this, Tool.dp2px(appSettings.getIconSize()), appSettings.getIconPack(), appsTemp);
            }

            _apps = appsTemp;
            _nonFilteredApps = nonFilteredAppsTemp;

            // update UI using correct thread
            handler.post(() -> {
                if (removedApps.size() > 0) {
                    notifyRemoveListeners(removedApps);
                }

                notifyUpdateListeners(appsTemp);

                if (recreateHomeActivity) {
                    HomeActivity._launcher.recreate();
                }
            });
        });
    }

    public List<App> getAllApps(boolean includeHidden) {
        return includeHidden ? getNonFilteredApps() : getApps();
    }

    public App findItemApp(Item item) {
        return findApp(item.getIntent());
    }

    public App createApp(Intent intent) {
        try {
            ResolveInfo info = _packageManager.resolveActivity(intent, 0);
            List<ShortcutInfo> shortcutInfo = Tool.getShortcutInfo(getContext(), intent.getComponent().getPackageName());
            return new App(_packageManager, info, shortcutInfo);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void onAppUpdated() {
        refreshApps(false);
    }

    public void addUpdateListener(AppUpdateListener updateListener) {
        _updateListeners.add(updateListener);
    }

    public void addDeleteListener(AppDeleteListener deleteListener) {
        _deleteListeners.add(deleteListener);
    }

    public void notifyUpdateListeners(@NonNull List<App> apps) {
        _updateListeners.removeIf(appUpdateListener -> appUpdateListener.onAppUpdated(apps));
    }

    public void notifyRemoveListeners(@NonNull List<App> apps) {
        _deleteListeners.removeIf(appDeleteListener -> appDeleteListener.onAppDeleted(apps));
    }

    public static List<App> getRemovedApps(List<App> oldApps, List<App> newApps) {
        List<App> removed = new ArrayList<>();
        // if this is the first call then return an empty list
        if (oldApps.size() == 0) {
            return removed;
        }
        for (int i = 0; i < oldApps.size(); i++) {
            if (!newApps.contains(oldApps.get(i))) {
                removed.add(oldApps.get(i));
                break;
            }
        }
        return removed;
    }
}
