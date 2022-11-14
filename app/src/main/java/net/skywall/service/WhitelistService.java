package net.skywall.service;

import android.content.Context;
import android.content.SharedPreferences;

import com.benny.openlauncher.R;

import net.skywall.utils.Pair;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class WhitelistService {

    private static final String PENDING_PREFS = "pendingPrefs";
    private static final String PENDING_PREFS_VALUES = "pendingPrefsValues";
    private static final String ACTIVE_PREFS = "activePrefs";

    public static final String DELAY_KEY = "skywall.delay";
    private static final String APPS_KEY = "skywall.apps";

    private static final String[] ALLOWED_PACKAGES = new String[] { "com.android",
            "com.benny",
            "com.flask",
            "com.google.android.apps.inputmethod",
            "com.google.android.documentsui",
            "com.google.android.gms",
            "com.google.android.location",
            "com.google.android.permissioncontroller",
            "com.google.android.settings",
            "net.gsantner",
            "net.skywall",
    };

    private static long currentDelayMillis;

    private SharedPreferences pendingChanges;
    private SharedPreferences pendingValues;
    private SharedPreferences activePrefs;

    private Map<Long, String> delayValuesToDisplayValues;
    private Map<String, Integer> displayValueToIndex;

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
        currentDelayMillis = activePrefs.getLong(DELAY_KEY, 0);

        String[] delayValues = context.getResources().getStringArray(R.array.delay_values);
        delayValuesToDisplayValues = new HashMap<>();
        for (String delayValue : delayValues) {
            delayValuesToDisplayValues.put(valueInMilliSeconds(delayValue), delayValue);
        }
        displayValueToIndex = new HashMap<>();
        for (int i = 0; i < delayValues.length; i++) {
            displayValueToIndex.put(delayValues[i], i);
        }

        isInit = true;
    }

    private Set<String> currentActiveApps() {
        return new HashSet<>(activePrefs.getStringSet(APPS_KEY, new HashSet<>()));
    }

    public boolean refreshAndCheckWhitelisted(String packageName) {
        if (getCurrentDelayMillis() == 0) {
            return true;
        }

        return refreshAndCheckWhitelistedInternal(packageName);
    }

    private boolean refreshAndCheckWhitelistedInternal(String packageName) {
        if (currentActiveApps().contains(packageName)) {
            return true;
        }

        if (isPredefinedAllowedApp(packageName)) {
            return true;
        }

        long now = new Date().getTime();

        long delayTimeChange = pendingChanges.getLong(DELAY_KEY, Long.MAX_VALUE);
        long appTimeChange = pendingChanges.getLong(packageName, Long.MAX_VALUE);
        long delayTimeValue = pendingValues.getLong(DELAY_KEY, 0);

        if (delayTimeChange == Long.MAX_VALUE) {
            // if no delay pending change, just look at scheduled app time vs now
            if (appTimeChange <= now) {
                updateFiles(packageName);
                return true;
            }
        } else if (delayTimeChange <= now) {
            long appQueueStartTime = pendingValues.getLong(packageName, 0);
            if (appQueueStartTime + delayTimeValue <= now) {
                updateFiles(packageName);
                updateFiles(DELAY_KEY);
                return true;
            }
        }

        return false;
    }

    public static boolean isPredefinedAllowedApp(String packageName) {
        for (String val : ALLOWED_PACKAGES) {
            if (packageName.startsWith(val)) {
                if (!"com.android.chrome".equals(packageName)
                        && !"com.android.vending".equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateFiles(String appName) {
        if (DELAY_KEY.equals(appName)) {
            long delayValue = pendingValues.getLong(DELAY_KEY, currentDelayMillis);
            activePrefs.edit().putLong(DELAY_KEY, delayValue).apply();
            currentDelayMillis = delayValue;
        } else {
            Set<String> apps = currentActiveApps();
            apps.add(appName);
            activePrefs.edit().putStringSet(APPS_KEY, apps).apply();
        }

        pendingValues.edit().remove(appName).apply();
        pendingChanges.edit().remove(appName).apply();
    }

    public void setDelay(long newDelayMillis) {
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
        long delayTimeChange = pendingChanges.getLong(DELAY_KEY, Long.MAX_VALUE);
        long now = new Date().getTime();
        if (delayTimeChange == Long.MAX_VALUE) {
            return currentDelayMillis;
        } else if (delayTimeChange <= now) {
            updateFiles(DELAY_KEY);
        }

        return currentDelayMillis;
    }

    public Set<String> getCurrentWhitelistedApps() {
        getPendingApps();
        return currentActiveApps();
    }

    public Map<String, Long> getPendingApps() {
        Map<String, ?> pending = pendingChanges.getAll();
        for (String key : pending.keySet()) {
            refreshAndCheckWhitelistedInternal(key);
        }

        pending = pendingChanges.getAll();
        Map<String, Long> pendingApps = new HashMap<>();
        for (String key : pending.keySet()) {
            if (!DELAY_KEY.equals(key)) {
                pendingApps.put(key, (Long) pending.get(key));
            }
        }

        return pendingApps;
    }

    public void cancelPendingChange(String appName) {
        pendingValues.edit().remove(appName).apply();
        pendingChanges.edit().remove(appName).apply();
    }

    public boolean isAppPending(String appName) {
        return pendingChanges.contains(appName);
    }

    public Optional<Pair<Long, Long>> getPendingDelay() {
        long delayTimeChange = pendingChanges.getLong(DELAY_KEY, Long.MAX_VALUE);
        if (delayTimeChange == Long.MAX_VALUE) {
            return Optional.empty();
        } else {
            return Optional.of(new Pair<>(delayTimeChange, pendingValues.getLong(DELAY_KEY, Long.MAX_VALUE)));
        }
    }

    public String getDisplayValue(long milliseconds) {
        return delayValuesToDisplayValues.get(milliseconds);
    }

    public int getDelayDisplayValuePosition() {
        return displayValueToIndex.get(getDisplayValue(getCurrentDelayMillis()));
    }

    public void removeWhitelistedApp(String appName) {
        Set<String> activeApps = currentActiveApps();
        activeApps.remove(appName);
        activePrefs.edit().putStringSet(APPS_KEY, activeApps).apply();
    }

    public static long valueInMilliSeconds(String delay) {
        String[] parts = delay.split(" ");
        if ("0".equals(parts[0])) {
            return 0;
        } else if (parts[1].contains("minute")) {
            return Long.parseLong(parts[0])*60*1000;
        } else {
            return Long.parseLong(parts[0])*60*60*1000;
        }
    }
}
