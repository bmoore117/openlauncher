package com.hyperion.skywall.service;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * JobService to be scheduled by the JobScheduler.
 * start another service
 */
public class LicenseAndUpdateService extends JobService {

    private static final String TAG = LicenseAndUpdateService.class.getSimpleName();
    public static final String VERSION = "version";

    private static boolean isScheduled = false;

    private static final int JOB_ID = 1;
    private static final long ONE_DAY_INTERVAL = 24 * 60 * 60 * 1000L; // 1 Day

    public static void schedule(Context context) {
        if (!isScheduled) {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            ComponentName componentName = new ComponentName(context, LicenseAndUpdateService.class);
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

            try {
                downloadUpdateIfAvailable();
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error checking for updates", e);
            }

            jobFinished(params, true); // do not reschedule this job, it is periodic already
        });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true; // do retry this run instance if we got cancelled for whatever reason
    }

    private void downloadUpdateIfAvailable() throws IOException, JSONException {
        URL url = new URL("https://sky-wall.net/wp-content/uploads/app-version.json");
        HttpURLConnection versionRequest = (HttpURLConnection) url.openConnection();
        versionRequest.setRequestMethod("GET");
        try (InputStream versionJson = new BufferedInputStream(versionRequest.getInputStream())) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(versionJson));
            String output = reader.lines().collect(Collectors.joining("\n"));
            JSONObject results = new JSONObject(output);

            String newVersion = results.getString(VERSION);
            String currentVersion = getVersion(this);

            if (newVersion.compareTo(currentVersion) > 0) {
                HttpURLConnection apkRequest = (HttpURLConnection) new URL("https://sky-wall.net/wp-content/uploads/skywall.apk").openConnection();
                apkRequest.setRequestMethod("GET");
                try (InputStream apk = new BufferedInputStream(versionRequest.getInputStream())) {
                    Path appPath = getUpdateLocation(this).toPath().toAbsolutePath();
                    Files.copy(apk, appPath, StandardCopyOption.REPLACE_EXISTING);

                    Path versionPath = getUpdateVersionLocation(this).toPath().toAbsolutePath();
                    Files.write(versionPath, output.getBytes());
                } finally {
                    apkRequest.disconnect();
                }
            }
        } finally {
            versionRequest.disconnect();
        }
    }

    public static String getVersion(Context context) {
        String result = null;
        PackageManager pm = context.getPackageManager();
        String pkgName = context.getPackageName();
        try {
            PackageInfo pkgInfo = pm.getPackageInfo(pkgName, 0);
            result = pkgInfo.versionName;
        } catch (PackageManager.NameNotFoundException ignored) {}
        return result;
    }

    public static String getUpdateVersion(Context context) throws IOException, JSONException {
        Path path = getUpdateVersionLocation(context).toPath();
        String json = Files.readAllLines(path).stream().collect(Collectors.joining("\n"));
        JSONObject jsonObject = new JSONObject(json);
        return jsonObject.getString(VERSION);
    }

    public static File getUpdateLocation(Context context) {
        return new File(context.getFilesDir(), "skywall.apk");
    }

    private static File getUpdateVersionLocation(Context context) {
        return new File(context.getFilesDir(), "app-version.json");
    }

    public static void deleteUpdateFiles(Context context) {
        try {
            Files.delete(getUpdateLocation(context).toPath());
            Files.delete(getUpdateVersionLocation(context).toPath());
        } catch (IOException e) {
            Log.e(TAG, "Error deleting update files", e);
        }
    }
}
