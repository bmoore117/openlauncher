package com.hyperion.skywall.fragment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.benny.openlauncher.R;
import com.hyperion.skywall.service.WhitelistService;

import java.util.List;

public class RemoveFragmentAdapter extends BaseAdapter {

    private final Context context;
    private final List<DisplayApp> appList;
    private final WhitelistService whitelistService;

    public RemoveFragmentAdapter(Context context, List<DisplayApp> appList, WhitelistService whitelistService) {
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
            v = inflater.inflate(R.layout.item_remove, parent, false);
        } else {
            v = convertView;
        }

        ImageView imageView = v.findViewById(R.id.item_remove_imageview);
        TextView label = v.findViewById(R.id.item_remove_name);
        Button cancel = v.findViewById(R.id.item_remove_button);

        DisplayApp displayApp = appList.get(position);

        if (displayApp.getIcon() != null) {
            imageView.setImageDrawable(displayApp.getIcon());
            cancel.setOnClickListener(view -> {
                whitelistService.removeWhitelistedApp(displayApp.getActivityName());
                appList.remove(position);
                notifyDataSetChanged();
            });
        } else {
            imageView.setVisibility(View.INVISIBLE);
            cancel.setVisibility(View.INVISIBLE);
        }
        label.setText(displayApp.getName());

        return v;
    }
}
