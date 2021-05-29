package com.hyperion.skywall.service;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class WhitelistService {

    private static final String PENDING_PREFS = "pendingPrefs";
    private static final String PENDING_PREFS_VALUES = "pendingPrefsValues";
    private static final String ACTIVE_PREFS = "activePrefs";

    private static final String DELAY_KEY = "skywall.delay";
    private static final String APPS_KEY = "skywall.apps";

    private static long currentDelayMillis;
    private static Set<String> currentActiveApps;

    private SharedPreferences pendingChanges;
    private SharedPreferences pendingValues;
    private SharedPreferences activePrefs;

    private boolean isInit;

    private static final WhitelistService instance = new WhitelistService();

    private WhitelistService() {}

    public static WhitelistService getInstance(Context context) {
        if (!instance.isInit) {
            instance.init(context.getApplicationContext());
        }
        return instance;
    }

    public void init(Context context) {
        pendingChanges = context.getSharedPreferences(PENDING_PREFS, Context.MODE_PRIVATE);
        activePrefs = context.getSharedPreferences(ACTIVE_PREFS, Context.MODE_PRIVATE);
        pendingValues = context.getSharedPreferences(PENDING_PREFS_VALUES, Context.MODE_PRIVATE);

        currentActiveApps = activePrefs.getStringSet(APPS_KEY, new HashSet<>());
        currentDelayMillis = activePrefs.getLong(DELAY_KEY, 0);
        isInit = true;
    }

    public boolean refreshAndCheckWhitelisted(String appName) {
        if (currentDelayMillis == 0) {
            return true;
        }

        if (currentActiveApps.contains(appName)) {
            return true;
        }

        long now = new Date().getTime();

        long delayTimeChange = pendingChanges.getLong(DELAY_KEY, Long.MAX_VALUE);
        long appTimeChange = pendingChanges.getLong(appName, Long.MAX_VALUE);
        long delayTimeValue = pendingValues.getLong(DELAY_KEY, 0);

        if (delayTimeChange == Long.MAX_VALUE) {
            // if no delay pending change, just look at scheduled app time vs now
            if (appTimeChange <= now) {
                updateFiles(appName);
                return true;
            }
        } else if (delayTimeChange <= now) {
            long appQueueStartTime = pendingValues.getLong(appName, 0);
            if (appQueueStartTime + delayTimeValue <= now) {
                updateFiles(appName);
                updateFiles(DELAY_KEY);
                return true;
            }
        }

        return false;
    }

    private void updateFiles(String appName) {
        pendingValues.edit().remove(appName).apply();
        pendingChanges.edit().remove(appName).apply();
        if (!DELAY_KEY.equals(appName)) {
            currentActiveApps.add(appName);
            activePrefs.edit().putStringSet(APPS_KEY, currentActiveApps).apply();
        }
    }

    public void increaseDelay(long newDelayMillis) {
        currentDelayMillis = newDelayMillis;
        activePrefs.edit().putLong(DELAY_KEY, currentDelayMillis).apply();
    }

    public void queueDelayReduction(long newDelayMillis) {
        long now = new Date().getTime();
        pendingChanges.edit().putLong(DELAY_KEY, now + currentDelayMillis).apply();
        pendingValues.edit().putLong(DELAY_KEY, newDelayMillis).apply();
    }

    public void queueApp(String appName) {
        long now = new Date().getTime();
        pendingChanges.edit().putLong(appName, now + currentDelayMillis).apply();
        pendingValues.edit().putLong(appName, now).apply();
    }

    public long getCurrentDelayMillis() {
        // TODO make this update delay as well, need to as we need to check what the new delay is if the user only ever queues delay changes and does nothing else
        return currentDelayMillis;
    }

    public Set<String> getCurrentActiveApps() {
        return new HashSet<>(currentActiveApps);
    }
}
