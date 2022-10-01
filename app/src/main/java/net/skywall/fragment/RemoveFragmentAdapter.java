package net.skywall.fragment;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import net.skywall.openlauncher.R;
import net.skywall.fragment.view.DisplayApp;
import net.skywall.service.SkywallService;
import net.skywall.service.WhitelistService;
import net.skywall.utils.LicenseUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RemoveFragmentAdapter extends BaseAdapter implements Filterable {

    private final Context context;
    private final List<DisplayApp> filterableAppList;
    private final List<DisplayApp> originalAppList;
    private final RemoveFilter filter;
    private final WhitelistService whitelistService;
    private final SkywallService skywallService;

    public RemoveFragmentAdapter(Context context, List<DisplayApp> appList, WhitelistService whitelistService,
                                 SkywallService skywallService) {
        this.context = context;
        this.filterableAppList = appList;
        this.originalAppList = new ArrayList<>(appList);
        filter = new RemoveFilter();
        this.whitelistService = whitelistService;
        this.skywallService = skywallService;
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
            v = inflater.inflate(R.layout.item_remove, parent, false);
        } else {
            v = convertView;
        }

        ImageView imageView = v.findViewById(R.id.item_remove_imageview);
        TextView label = v.findViewById(R.id.item_remove_name);
        Button cancel = v.findViewById(R.id.item_remove_button);

        DisplayApp displayApp = filterableAppList.get(position);

        if (displayApp.getIcon() != null) {
            imageView.setImageDrawable(displayApp.getIcon());
            cancel.setOnClickListener(view -> LicenseUtils.performOrShowMessage(() -> {
                whitelistService.removeWhitelistedApp(displayApp.getActivityName());
                filterableAppList.remove(position);
                notifyDataSetChanged();
            }, skywallService::isLicensed, context));
        } else {
            imageView.setVisibility(View.GONE);

            Resources res = context.getResources();
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) label.getLayoutParams();
            params.setMargins(Math.round(res.getDimension(R.dimen.default_padding_side)), 0, 0, 0);
            label.setLayoutParams(params);

            cancel.setVisibility(View.INVISIBLE);
        }
        label.setText(displayApp.getName());

        return v;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    private class RemoveFilter extends Filter {
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
