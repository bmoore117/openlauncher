package net.skywall.utils;

import android.content.Context;
import android.widget.Toast;

import com.benny.openlauncher.R;

import java.util.function.Supplier;

public class LicenseUtils {

    public static void performOrShowMessage(Runnable runnable, Supplier<Boolean> isLicensed, Context context) {
        if (isLicensed.get()) {
            runnable.run();
        } else {
            Toast.makeText(context, R.string.invalid_license_message, Toast.LENGTH_LONG).show();
        }
    }
}

