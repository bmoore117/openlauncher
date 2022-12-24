package net.skywall.fragment;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import net.skywall.activity.SkyWallActivity;
import net.skywall.openlauncher.R;
import net.skywall.service.SkywallService;

public class InfoFragment extends Fragment {

    private static final String TAG = InfoFragment.class.getSimpleName();

    private SkywallService skywallService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        skywallService = SkywallService.getInstance(getContext());
        View view = inflater.inflate(R.layout.fragment_info, container, false);

        TextView content = view.findViewById(R.id.fragment_info_content);
        content.setText(Html.fromHtml("<h1>SkyWall Additional Features</h1><p>SkyWall blocks a few things that aren't visible from the normal whitelisting pages, such as areas in the Settings app that might allow you to circumvent or disable SkyWall. It only does this while delay is above zero; when delay is zero nothing is blocked. Specifically, SkyWall blocks:</p><ul><li>&nbsp;Clearing the SkyWall app cache & storage</li><li>&nbsp;Google Assistant explicit search result settings</li><li>&nbsp;Device Admin app settings</li><li>&nbsp;Default home app settings</li><li>&nbsp;SkyWall accessibility service settings</li><li>&nbsp;Enablement of app sideloading</li><li>&nbsp;Developer options</li><li>&nbsp;Disablement of the SkyWall Firefox browser add-on, if installed in Firefox, Firefox Beta, or Firefox Nightly</li><li><li>&nbsp;Clearing Firefox, Firefox Beta, or Firefox Nightly app cache & storage</li></ul><p>All other actions in Settings & beyond are allowed, with other apps allowed as whitelisted. If you attempt to access any of the aforementioned activities or settings, SkyWall will block them by simply swiping back virtually on the screen to take you to the previous screen.</p>", 0));
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.info, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_info_logout_btn) {
            skywallService.logout();
            SkyWallActivity.doTransition(SkyWallActivity.getLoginFragment());
            return true;
        } else if (item.getItemId() == R.id.menu_info_back_btn) {
            SkyWallActivity.doTransition(SkyWallActivity.getMainFragment());
        }
        return super.onOptionsItemSelected(item); // important line
    }
}
