package net.skywall.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.skywall.fragment.view.Phone;
import net.skywall.openlauncher.R;
import net.skywall.service.SkywallService;
import net.skywall.service.WhitelistService;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeprovisionFragmentAdapter extends BaseAdapter {

    private static final String TAG = DeprovisionFragmentAdapter.class.getSimpleName();

    private final Context context;
    private final List<Phone> phones;
    private final WhitelistService whitelistService;
    private final SkywallService skywallService;

    public DeprovisionFragmentAdapter(Context context, List<Phone> phones, WhitelistService whitelistService,
                                      SkywallService skywallService) {
        this.context = context;
        this.phones = phones;
        this.whitelistService = whitelistService;
        this.skywallService = skywallService;
    }

    @Override
    public int getCount() {
        return phones.size();
    }

    @Override
    public Object getItem(int position) {
        return phones.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, final View convertView, ViewGroup parent) {
        View v;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.item_deprovision, parent, false);
        } else {
            v = convertView;
        }

        TextView label = v.findViewById(R.id.item_deprovision_name);
        Button deprovision = v.findViewById(R.id.item_deprovision_button);

        final Handler handler = new Handler(Looper.getMainLooper());

        Phone phone = phones.get(position);
        deprovision.setOnClickListener(view -> showConfirmDialog(() -> {
            showInitiatedDialog();
            CompletableFuture.runAsync(() -> {
                try {
                    skywallService.deprovisionPhone(phone);
                    phones.remove(position);
                    notifyDataSetChanged();
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Error deprovisioning phone", e);
                    handler.post(() -> Toast.makeText(context.getApplicationContext(), "Error deprovisioning phone, try again later", Toast.LENGTH_LONG).show());
                }
            });
        }));
        deprovision.setEnabled(whitelistService.getCurrentDelayMillis() == 0);
        label.setText(phone.getPhoneName());

        return v;
    }

    private void showConfirmDialog(Runnable positiveAction) {
        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.confirm_reset)
                .setMessage(R.string.confirm_reset_message)
                .setCancelable(true)
                .setPositiveButton("OK", (dialog, id) -> positiveAction.run())
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel())
                .create();
        alertDialog.show();
    }

    private void showInitiatedDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.reset_initiated)
                .setMessage(R.string.reset_initiated_message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, id) -> dialog.dismiss())
                .create();
        alertDialog.show();
    }
}
