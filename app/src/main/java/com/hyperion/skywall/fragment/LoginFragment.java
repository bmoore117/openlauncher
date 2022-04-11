package com.hyperion.skywall.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.Fragment;

import com.benny.openlauncher.R;
import com.hyperion.skywall.Pair;
import com.hyperion.skywall.activity.SkyWallActivity;
import com.hyperion.skywall.service.AuthService;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class LoginFragment extends Fragment {

    private static final String TAG = LoginFragment.class.getSimpleName();

    private AuthService authService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authService = AuthService.getInstance(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        EditText username = view.findViewById(R.id.fragment_login_username);
        EditText password = view.findViewById(R.id.fragment_login_password);
        Button loginButton = view.findViewById(R.id.fragment_login_login_button);

        loginButton.setOnClickListener(button -> CompletableFuture.runAsync(() -> {
            String usernameVal = username.getText().toString();
            String passwordVal = password.getText().toString();
            try {
                Pair<Integer, Boolean> authResult = authService.authenticate(usernameVal, passwordVal);
                if (!authResult.second) {
                    // TODO set invalid username/password on UI
                    return;
                }

                String appPassword = authService.createOrReplaceAppPassword(usernameVal, passwordVal, authResult.first);
                authService.setUsername(usernameVal);
                authService.setPassword(appPassword);
                //TODO re-enable this, after correcting DELETE request specificity in AuthService: authService.setLicensed(true);

                SkyWallActivity.doTransition(SkyWallActivity.getMainFragment());
            } catch (RuntimeException | IOException | InterruptedException | JSONException e) {
                //TODO set error on UI
                Log.e(TAG, "Error authenticating", e);
            }
        }));

        return view;
    }
}
