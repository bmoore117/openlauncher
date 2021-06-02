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

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FindForegroundActivityWorker extends Worker {

    private static final String TAG = FindForegroundActivityWorker.class.getSimpleName();
    private final WhitelistService whitelistService;
    public static final AtomicBoolean isStarted = new AtomicBoolean(false);
    public static final AtomicBoolean shouldRequeue = new AtomicBoolean(true);

    public FindForegroundActivityWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        whitelistService = WhitelistService.getInstance(getApplicationContext());
    }

    @NonNull
    @Override
    public Result doWork() {
        isStarted.compareAndSet(false, true);

        Context context = getApplicationContext();

        Optional<String> recentActivity = getRecentActivity(context);
        recentActivity.ifPresent(activityName -> {
            if (!whitelistService.refreshAndCheckWhitelisted(activityName)) {
                Log.i(TAG, "Found non-whitelisted foreground activity: " + activityName);
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                String canonicalName = BlockedActivity.class.getCanonicalName();
                intent.setClassName(BuildConfig.APPLICATION_ID, canonicalName);
                context.startActivity(intent);
            }
        });

        if (shouldRequeue.get()) {
            WorkManager workManager = WorkManager.getInstance(context);
            workManager.enqueue(new OneTimeWorkRequest.Builder(FindForegroundActivityWorker.class)
                    .setInitialDelay(3, TimeUnit.SECONDS).build());
        }

        return Result.success();
    }

    private Optional<String> getRecentActivity(Context context) {
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        long time = System.currentTimeMillis();

        // query events between 3 seconds ago and now
        UsageEvents usageEvents = mUsageStatsManager.queryEvents(time - 30000, time);
        List<UsageEvents.Event> events = new LinkedList<>();
        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            usageEvents.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                events.add(event);
            }
        }

        Optional<UsageEvents.Event> mostRecent = events.stream().max(Comparator.comparing(UsageEvents.Event::getTimeStamp));

        if (mostRecent.isPresent()) {
            UsageEvents.Event eventObj = mostRecent.get();
            if (!TextUtils.isEmpty(eventObj.getClassName())) {
                return Optional.of(eventObj.getClassName());
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }
}
