package com.hyperion.skywall.fragment;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.benny.openlauncher.R;
import com.hyperion.skywall.fragment.view.DisplayApp;
import com.hyperion.skywall.service.WhitelistService;

import java.util.List;

public class PendingFragmentAdapter extends BaseAdapter {

    private final Context context;
    private final List<DisplayApp> appList;
    private final WhitelistService whitelistService;

    public PendingFragmentAdapter(Context context, List<DisplayApp> appList, WhitelistService whitelistService) {
        this.context = context;
        this.appList = appList;
        this.whitelistService = whitelistService;
    }

    @Override
    public int getCount() {
        return appList.size();
    }

    @Override
    public Object getItem(int position) {
        return appList.get(position);
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
            v = inflater.inflate(R.layout.item_pending, parent, false);
        } else {
            v = convertView;
        }

        ImageView imageView = v.findViewById(R.id.item_pending_imageview);
        TextView label = v.findViewById(R.id.item_pending_name);
        TextView date = v.findViewById(R.id.item_pending_date);
        Button cancel = v.findViewById(R.id.item_pending_button);

        DisplayApp displayApp = appList.get(position);

        if (displayApp.getIcon() != null) {
            imageView.setImageDrawable(displayApp.getIcon());
            date.setText(PendingFragment.simpleDateFormat.format(appList.get(position).getWhitelistTime()));
            cancel.setOnClickListener(view -> {
                whitelistService.cancelPendingChange(displayApp.getActivityName());
                appList.remove(position);
                notifyDataSetChanged();
            });
        } else {
            imageView.setVisibility(View.GONE);

            Resources res = context.getResources();
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) label.getLayoutParams();
            params.setMargins(Math.round(res.getDimension(R.dimen.default_padding_side)), 0, 0, 0);
            label.setLayoutParams(params);

            cancel.setVisibility(View.INVISIBLE);
            date.setVisibility(View.INVISIBLE);
        }
        label.setText(appList.get(position).getName());

        return v;
    }
}
