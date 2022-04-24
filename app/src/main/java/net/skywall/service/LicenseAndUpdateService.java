package net.skywall.service;

import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;

import net.gsantner.opoc.util.ContextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
    public static final String SKYWALL = "skywall";

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
            authService.checkAndUpdateLicense();

            /*if (downloadUpdateIfAvailable(this)) {
                installPackage(this);
            }*/

            jobFinished(params, false); // do not reschedule this job, it is periodic already
        });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true; // do retry this run instance if we got cancelled for whatever reason
    }

    public static boolean downloadUpdateIfAvailable(Context context) {
        boolean result = false;
        try {
            URL url = new URL("https://skywall.net/wp-content/uploads/app-version.json");
            HttpURLConnection versionRequest = (HttpURLConnection) url.openConnection();
            versionRequest.setRequestMethod("GET");
            try (InputStream versionJson = new BufferedInputStream(versionRequest.getInputStream())) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(versionJson));
                String output = reader.lines().collect(Collectors.joining("\n"));
                JSONObject results = new JSONObject(output);

                String newVersion = results.getString(VERSION);
                String currentVersion = getVersion(context);

                if (newVersion.compareTo(currentVersion) > 0) {
                    HttpURLConnection apkRequest = (HttpURLConnection) new URL("https://skywall.net/wp-content/uploads/" + SKYWALL + ".apk").openConnection();
                    apkRequest.setRequestMethod("GET");
                    try (InputStream apk = new BufferedInputStream(apkRequest.getInputStream())) {
                        Path appPath = getUpdateLocation(context).toPath().toAbsolutePath();
                        Files.copy(apk, appPath, StandardCopyOption.REPLACE_EXISTING);
                        result = true;
                    } finally {
                        apkRequest.disconnect();
                    }
                }
            } finally {
                versionRequest.disconnect();
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error checking for updates", e);
        }
        Log.i(TAG, "Finished update check");
        return result;
    }

    public static String getVersion(Context context) {
        ContextUtils contextUtils = new ContextUtils(context);
        return contextUtils.getAppVersionName();
    }

    public static File getUpdateLocation(Context context) {
        return new File(context.getFilesDir(), SKYWALL + ".apk");
    }

    public static void deleteUpdateFiles(Context context) {
        try {
            Files.deleteIfExists(getUpdateLocation(context).toPath());
        } catch (IOException e) {
            Log.e(TAG, "Error deleting update files", e);
        }
    }

    public static void installPackage(Context context) {
        Log.i(TAG, "Installing update");
        PackageInstaller.Session session = null;
        try {
            PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int sessionId = packageInstaller.createSession(params);
            session = packageInstaller.openSession(sessionId);
            try (OutputStream packageInSession = session.openWrite("package", 0, -1)) {
                Files.copy(LicenseAndUpdateService.getUpdateLocation(context).toPath(), packageInSession);
            }

            LicenseAndUpdateService.deleteUpdateFiles(context);

            // Create an install status receiver.
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            session.commit(pendingIntent.getIntentSender());
        } catch (IOException e) {
            throw new RuntimeException("Couldn't install package", e);
        } catch (RuntimeException e) {
            if (session != null) {
                session.abandon();
            }
            throw e;
        }
    }
}
