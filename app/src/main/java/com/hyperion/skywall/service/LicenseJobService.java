package com.hyperion.skywall.service;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import java.util.concurrent.CompletableFuture;

/**
 * JobService to be scheduled by the JobScheduler.
 * start another service
 */
public class LicenseJobService extends JobService {

    private static final String TAG = LicenseJobService.class.getSimpleName();

    private static boolean isScheduled = false;

    private static final int JOB_ID = 1;
    private static final long ONE_DAY_INTERVAL = 24 * 60 * 60 * 1000L; // 1 Day

    public static void schedule(Context context) {
        if (!isScheduled) {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            ComponentName componentName = new ComponentName(context, LicenseJobService.class);
            JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, componentName);
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
            builder.setPeriodic(ONE_DAY_INTERVAL);
            jobScheduler.schedule(builder.build());
            isScheduled = true;
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "Starting LicenseJobService");
        Context applicationContext = getApplicationContext();
        CompletableFuture.runAsync(() -> {
            AuthService authService = AuthService.getInstance(applicationContext);
            WhitelistService whitelistService = WhitelistService.getInstance(applicationContext);
            if (!authService.checkSubscriberActive()) {
                whitelistService.setDelay(0);
            }
            jobFinished(params, true); // do not reschedule this job, it is periodic already
        });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true; // do retry this run instance if we got cancelled for whatever reason
    }
}
