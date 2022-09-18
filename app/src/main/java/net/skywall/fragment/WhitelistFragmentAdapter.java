package net.skywall.fragment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import net.skywall.openlauncher.R;
import net.skywall.fragment.view.DisplayApp;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WhitelistFragmentAdapter extends BaseAdapter implements Filterable {

    private final Context context;
    private final List<DisplayApp> filterableAppList;
    private final List<DisplayApp> originalAppList;
    private final WhitelistFilter filter;

    public WhitelistFragmentAdapter(Context context, List<DisplayApp> appList) {
        this.context = context;
        this.filterableAppList = appList;
        this.originalAppList = new ArrayList<>(appList);
        filter = new WhitelistFilter();
    }

    @Override
    public int getCount() {
        return filterableAppList.size();
    }

    @Override
    public Object getItem(int position) {
        return filterableAppList.get(position);
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

        DisplayApp app = filterableAppList.get(position);
        imageView.setImageDrawable(app.getIcon());
        label.setText(app.getName());
        // note, the listener has to come first before the setting of checked, otherwise it unsets :/
        checkBox.setOnCheckedChangeListener((cb, checked) -> app.setSelected(checked));
        checkBox.setChecked(app.isSelected());

        return v;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    private class WhitelistFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            String input = charSequence.toString().toLowerCase();
            List<DisplayApp> filteredList;
            if (input.isEmpty()) {
                filteredList = originalAppList;
            } else {
                filteredList = originalAppList.stream().filter(app -> app.getName().toLowerCase().contains(input)).collect(Collectors.toList());
            }

            FilterResults filterResults = new FilterResults();
            filterResults.values = filteredList;
            return filterResults;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            filterableAppList.clear();
            filterableAppList.addAll((List<DisplayApp>) filterResults.values);
            notifyDataSetChanged();
        }
    }
}
