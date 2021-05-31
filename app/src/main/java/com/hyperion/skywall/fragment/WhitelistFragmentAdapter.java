package com.hyperion.skywall.fragment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.benny.openlauncher.R;
import com.hyperion.skywall.fragment.view.DisplayApp;

import java.util.List;

public class WhitelistFragmentAdapter extends BaseAdapter {

    private final Context context;
    private final List<DisplayApp> appList;

    public WhitelistFragmentAdapter(Context context, List<DisplayApp> appList) {
        this.context = context;
        this.appList = appList;
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
            v = inflater.inflate(R.layout.item_whitelist, parent, false);
        } else {
            v = convertView;
        }

        ImageView imageView = v.findViewById(R.id.item_whitelist_imageview);
        TextView label = v.findViewById(R.id.item_whitelist_textview);
        CheckBox checkBox = v.findViewById(R.id.item_whitelist_checkbox);

        DisplayApp app = appList.get(position);
        imageView.setImageDrawable(app.getIcon());
        label.setText(app.getName());
        // note, the listener has to come first before the setting of checked, otherwise it unsets :/
        checkBox.setOnCheckedChangeListener((cb, checked) -> app.setSelected(checked));
        checkBox.setChecked(app.isSelected());

        return v;
    }
}
