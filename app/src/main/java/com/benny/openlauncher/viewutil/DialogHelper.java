package com.benny.openlauncher.viewutil;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.Gravity;

import com.afollestad.materialdialogs.MaterialDialog;
import net.skywall.openlauncher.R;
import com.benny.openlauncher.activity.HomeActivity;
import com.benny.openlauncher.model.App;
import com.benny.openlauncher.model.Item;
import com.benny.openlauncher.util.AppManager;
import com.benny.openlauncher.util.AppSettings;
import com.benny.openlauncher.util.Tool;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;

import java.util.ArrayList;
import java.util.List;

public class DialogHelper {
    public static void editItemDialog(String title, String defaultText, Context c, final OnItemEditListener listener) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(c);
        builder.title(title)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .input(null, defaultText, (dialog, input) -> listener.itemLabel(input.toString())).show();
    }

    public static void alertDialog(Context context, String title, String msg, MaterialDialog.SingleButtonCallback onPositive) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        builder.title(title)
                .onPositive(onPositive)
                .content(msg)
                .negativeText(android.R.string.cancel)
                .positiveText(android.R.string.ok)
                .show();
    }

    public static void alertDialog(Context context, String title, String message, String positive, MaterialDialog.SingleButtonCallback onPositive) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        builder.title(title)
                .onPositive(onPositive)
                .content(message)
                .negativeText(android.R.string.cancel)
                .positiveText(positive)
                .show();
    }

    public static void selectActionDialog(final Context context, MaterialDialog.ListCallback callback) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        builder.title(R.string.action)
                .items(R.array.entries__gesture_action)
                .itemsCallback(callback)
                .show();
    }

    public static void selectDesktopActionDialog(final Context context, MaterialDialog.ListCallback callback) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        builder.title(R.string.action)
                .items(R.array.entries__desktop_actions)
                .itemsCallback(callback)
                .show();
    }

    public static void selectGestureDialog(final Context context, String title, MaterialDialog.ListCallback callback) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        builder.title(title)
                .items(R.array.entries__gesture)
                .itemsCallback(callback)
                .show();
    }

    public static void selectAppDialog(final Context context, final OnAppSelectedListener onAppSelectedListener) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        FastItemAdapter<IconLabelItem> fastItemAdapter = new FastItemAdapter<>();
        builder.title(R.string.select_app)
                .adapter(fastItemAdapter, new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false))
                .negativeText(android.R.string.cancel);

        final MaterialDialog dialog = builder.build();
        List<IconLabelItem> items = new ArrayList<>();
        final List<App> apps = AppManager.getInstance(context).getApps();
        for (int i = 0; i < apps.size(); i++) {
            items.add(new IconLabelItem(apps.get(i).getIcon(), apps.get(i).getLabel())
                    .withIconSize(50)
                    .withIsAppLauncher(true)
                    .withIconGravity(Gravity.START)
                    .withIconPadding(8));
        }
        fastItemAdapter.set(items);
        fastItemAdapter.withOnClickListener((v, adapter, item, position) -> {
            if (onAppSelectedListener != null) {
                onAppSelectedListener.onAppSelected(apps.get(position));
            }
            dialog.dismiss();
            return true;
        });
        dialog.show();
    }

    public static void startPickIconPackIntent(final Context context) {
        PackageManager packageManager = context.getPackageManager();
        Activity activity = (Activity) context;
        AppManager appManager = AppManager.getInstance(context);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory("com.anddoes.launcher.THEME");

        FastItemAdapter<IconLabelItem> fastItemAdapter = new FastItemAdapter<>();

        final List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);
        resolveInfos.sort(new ResolveInfo.DisplayNameComparator(packageManager));
        final MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .adapter(fastItemAdapter, null)
                .title((activity.getString(R.string.select_icon_pack)))
                .build();

        fastItemAdapter.add(new IconLabelItem(activity, R.mipmap.ic_launcher_2, R.string.default_icons)
                .withIconPadding(16)
                .withIconGravity(Gravity.START)
                .withOnClickListener(v -> {
                    AppSettings.get().setIconPack("");
                    appManager.refreshApps(true);
                    dialog.dismiss();
                }));
        for (int i = 0; i < resolveInfos.size(); i++) {
            final int mI = i;
            fastItemAdapter.add(new IconLabelItem(resolveInfos.get(i).loadIcon(packageManager), resolveInfos.get(i).loadLabel(packageManager).toString())
                    .withIconPadding(16)
                    .withIconSize(50)
                    .withIsAppLauncher(true)
                    .withIconGravity(Gravity.START)
                    .withOnClickListener(v -> {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            AppSettings.get().setIconPack(resolveInfos.get(mI).activityInfo.packageName);
                            appManager.refreshApps(true);
                            dialog.dismiss();
                        } else {
                            Tool.toast(context, (activity.getString(R.string.toast_icon_pack_error)));
                            ActivityCompat.requestPermissions(HomeActivity.getCurrentInstance(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, HomeActivity.REQUEST_PERMISSION_STORAGE);
                        }
                    }));
        }
        dialog.show();
    }

    public static void deletePackageDialog(Context context, Item item) {
        if (item.getType() == Item.Type.APP) {
            try {
                Uri packageURI = Uri.parse("package:" + item.getIntent().getComponent().getPackageName());
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                context.startActivity(uninstallIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public interface OnAppSelectedListener {
        void onAppSelected(App app);
    }

    public interface OnItemEditListener {
        void itemLabel(String label);
    }
}
