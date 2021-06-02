package com.hyperion.skywall.work;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.benny.openlauncher.BuildConfig;
import com.hyperion.skywall.activity.BlockedActivity;
import com.hyperion.skywall.service.WhitelistService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FindForegroundActivityWorker extends Worker {

    private static final String TAG = FindForegroundActivityWorker.class.getSimpleName();

    public static final AtomicBoolean isStarted = new AtomicBoolean(false);
    public static final AtomicBoolean shouldRequeue = new AtomicBoolean(true);
    private static final AtomicInteger queueCount = new AtomicInteger(0);

    private static final Set<String> allowedAndroidActivities = new HashSet<>(Collections.singletonList("com.android.internal.app.ResolverActivity"));
    private final WhitelistService whitelistService;

    public FindForegroundActivityWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        whitelistService = WhitelistService.getInstance(getApplicationContext());
    }

    @NonNull
    @Override
    public Result doWork() {
        isStarted.compareAndSet(false, true);
        queueCount.decrementAndGet();

        Context context = getApplicationContext();

        Optional<UsageEvents.Event> usageEventOpt = getRecentActivity(context);
        usageEventOpt.ifPresent(event -> {
            if (!"android".equals(event.getPackageName()) || !allowedAndroidActivities.contains(event.getClassName())) {
                if (!whitelistService.refreshAndCheckWhitelisted(event.getPackageName())) {
                    Log.i(TAG, "Found non-whitelisted foreground activity: " + event.getPackageName());
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    String canonicalName = BlockedActivity.class.getCanonicalName();
                    intent.setClassName(BuildConfig.APPLICATION_ID, canonicalName);
                    context.startActivity(intent);
                }
            }
        });

        if (shouldRequeue.get()) {
            if (queueCount.incrementAndGet() == 0) {
                WorkManager workManager = WorkManager.getInstance(context);
                workManager.enqueue(new OneTimeWorkRequest.Builder(FindForegroundActivityWorker.class)
                        .setInitialDelay(3, TimeUnit.SECONDS).build());
            } else {
                Log.i(TAG, "Skipping requeue as count above 0");
            }
        }

        return Result.success();
    }

    private Optional<UsageEvents.Event> getRecentActivity(Context context) {
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        long time = System.currentTimeMillis();

        // query events between 3 seconds ago and now
        UsageEvents usageEvents = mUsageStatsManager.queryEvents(time - 30000, time);
        long latestTime = Long.MIN_VALUE;
        UsageEvents.Event latestEvent = null;
        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            usageEvents.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                if (event.getTimeStamp() > latestTime) {
                    latestEvent = event;
                    latestTime = event.getTimeStamp();
                }
            }
        }

        if (latestEvent != null) {
            if (!TextUtils.isEmpty(latestEvent.getPackageName())) {
                return Optional.of(latestEvent);
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }
}
