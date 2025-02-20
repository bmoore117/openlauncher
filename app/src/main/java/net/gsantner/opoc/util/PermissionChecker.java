/*#######################################################
 *
 *   Maintained 2017-2023 by Gregor Santner <gsantner AT mailbox DOT org>
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     https://github.com/gsantner/opoc/#licensing
 *
#########################################################*/
package net.gsantner.opoc.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

import java.io.File;

@SuppressWarnings({"unused", "WeakerAccess"})
public class PermissionChecker {
    protected static final int CODE_PERMISSION_EXTERNAL_STORAGE = 4000;

    protected Activity _activity;

    public PermissionChecker(Activity activity) {
        _activity = activity;
    }

    public boolean doIfExtStoragePermissionGranted(String... optionalToastMessageForKnowingWhyNeeded) {
        if (ContextCompat.checkSelfPermission(_activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (optionalToastMessageForKnowingWhyNeeded != null && optionalToastMessageForKnowingWhyNeeded.length > 0 && optionalToastMessageForKnowingWhyNeeded[0] != null) {
                new AlertDialog.Builder(_activity)
                        .setMessage(optionalToastMessageForKnowingWhyNeeded[0])
                        .setCancelable(false)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            if (android.os.Build.VERSION.SDK_INT >= 23) {
                                ActivityCompat.requestPermissions(_activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, CODE_PERMISSION_EXTERNAL_STORAGE);
                            }
                        })
                        .show();
                return false;
            }
            ActivityCompat.requestPermissions(_activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, CODE_PERMISSION_EXTERNAL_STORAGE);
            return false;
        }
        return true;
    }

    public boolean checkPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0) {
            switch (requestCode) {
                case CODE_PERMISSION_EXTERNAL_STORAGE: {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean mkdirIfStoragePermissionGranted(File dir) {
        return doIfExtStoragePermissionGranted() && (dir.exists() || dir.mkdirs());
    }
}
