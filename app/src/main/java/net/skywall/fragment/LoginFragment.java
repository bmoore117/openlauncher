package net.skywall.fragment;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import net.skywall.openlauncher.R;

import net.skywall.utils.Pair;
import net.skywall.activity.SkyWallActivity;
import net.skywall.service.SkywallService;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class LoginFragment extends Fragment {

    private static final String TAG = LoginFragment.class.getSimpleName();

    private SkywallService skywallService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        skywallService = SkywallService.getInstance(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        TextView errorMessage = view.findViewById(R.id.fragment_login_error_message);
        EditText username = view.findViewById(R.id.fragment_login_username);
        EditText password = view.findViewById(R.id.fragment_login_password);
        Button loginButton = view.findViewById(R.id.fragment_login_login_button);
        ProgressBar progressBar = view.findViewById(R.id.fragment_login_progress_bar);
        LinearLayout progressBarPlate = view.findViewById(R.id.fragment_login_progress_bar_plate);

        TextView forgotPassword = view.findViewById(R.id.fragment_login_forgot_password);
        forgotPassword.setMovementMethod(LinkMovementMethod.getInstance());

        TextView signupLink = view.findViewById(R.id.fragment_login_signup_link);
        signupLink.setMovementMethod(LinkMovementMethod.getInstance());

        loginButton.setOnClickListener(button -> {
            progressBarPlate.setVisibility(View.VISIBLE);
            progressBarPlate.setZ(1000.0f);
            progressBar.setIndeterminate(true);
            CompletableFuture.runAsync(() -> {
                String usernameVal = username.getText().toString();
                String passwordVal = password.getText().toString();
                try {
                    Pair<Integer, Boolean> authResult = skywallService.authenticate(usernameVal, passwordVal);
                    if (!authResult.second) {
                        errorMessage.post(() -> errorMessage.setText(R.string.invalid_credentials));
                        progressBarPlate.post(() -> progressBarPlate.setVisibility(View.GONE));
                        return;
                    }

                    String appPassword = skywallService.createOrReplaceAppPassword(usernameVal, passwordVal, authResult.first);
                    skywallService.setUsername(usernameVal);
                    skywallService.setPassword(appPassword);
                    skywallService.setLicensed(true);

                    SkyWallActivity.doTransition(SkyWallActivity.getMainFragment());
                } catch (RuntimeException | IOException | InterruptedException | JSONException e) {
                    errorMessage.post(() -> errorMessage.setText(R.string.unexpected_response_from_server));
                    progressBarPlate.post(() -> progressBarPlate.setVisibility(View.GONE));
                    Log.e(TAG, "Error authenticating", e);
                }
            });
        });

        return view;
    }
}
