package com.hyperion.skywall.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.hyperion.skywall.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.stream.Collectors;

public class AuthService {

    private static final String TAG = AuthService.class.getSimpleName();
    public static final String IS_LICENSED = "isLicensed";
    public static final String USERNAME = "USERNAME";
    public static final String PASSWORD = "PASSWORD";

    private SharedPreferences authCache;
    private boolean isInit;
    private static final AuthService instance = new AuthService();

    private AuthService() {}

    public static AuthService getInstance(Context context) {
        if (!instance.isInit) {
            instance.init(context.getApplicationContext());
        }
        return instance;
    }

    public void init(Context context) {
        authCache = context.getSharedPreferences("AuthService", Context.MODE_PRIVATE);
        isInit = true;
    }

    public Pair<Integer, Boolean> authenticate(String username, String password) throws IOException, InterruptedException, JSONException {
        byte[] base64Bytes = Base64.getEncoder().encode((username + ":" + password).getBytes());

        URL url = new URL("https://sky-wall.net/wp-json/wp/v2/users/me?context=edit&_fields=roles,id");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Authorization", "Basic " + new String(base64Bytes));
        urlConnection.setRequestMethod("GET");
        try {
            if (urlConnection.getResponseCode() != 200) {
                return new Pair<>(null, Boolean.FALSE);
            }
            try (InputStream in = new BufferedInputStream(urlConnection.getInputStream())) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String output = reader.lines().collect(Collectors.joining("\n"));
                JSONObject results = new JSONObject(output);

                JSONArray roles = results.getJSONArray("roles");
                boolean isSubscriber = false;
                for (int i = 0; i < roles.length(); i++) {
                    String role = roles.getString(i);
                    if ("paying-subscriber".equalsIgnoreCase(role) || "administrator".equalsIgnoreCase(role)) {
                        isSubscriber = true;
                        break;
                    }
                }

                return new Pair<>(results.getInt("id"), isSubscriber);
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    public String createOrReplaceAppPassword(String username, String password, Integer userId) throws IOException, InterruptedException, JSONException {
        byte[] base64Bytes = Base64.getEncoder().encode((username + ":" + password).getBytes());

        String name = "skywall-android";
        String baseUrl = String.format("https://sky-wall.net/wp-json/wp/v2/users/%d/application-passwords", userId);
        URL url = new URL(baseUrl);
        HttpURLConnection existsRequest = (HttpURLConnection) url.openConnection();
        existsRequest.setRequestProperty("Authorization", "Basic " + new String(base64Bytes));
        existsRequest.setRequestMethod("GET");
        try (InputStream in = new BufferedInputStream(existsRequest.getInputStream())) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String output = reader.lines().collect(Collectors.joining("\n"));
            JSONArray jsonArray = new JSONArray(output);
            String uuid = findExistingAppPasswordUuid(jsonArray, name);
            if (uuid != null) {
                HttpURLConnection delete = (HttpURLConnection) new URL(baseUrl + "/" + uuid).openConnection();
                try {
                    delete.setRequestProperty("Authorization", "Basic " + new String(base64Bytes));
                    delete.setRequestMethod("DELETE");
                    delete.setDoOutput(true);
                    delete.connect();
                    Log.i(TAG, "Delete of prior app password finished with status: " + delete.getResponseCode());
                } finally {
                    delete.disconnect();
                }
            }
        } finally {
            existsRequest.disconnect();
        }

        HttpURLConnection createRequest = (HttpURLConnection) (new URL(baseUrl + "?name=" + name)).openConnection();
        createRequest.setRequestProperty("Authorization", "Basic " + new String(base64Bytes));
        createRequest.setRequestMethod("POST");
        try (InputStream in = new BufferedInputStream(createRequest.getInputStream())) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String output = reader.lines().collect(Collectors.joining("\n"));
            JSONObject responseObj = new JSONObject(output);
            return responseObj.getString("password");
        } finally {
            createRequest.disconnect();
        }
    }

    private String findExistingAppPasswordUuid(JSONArray jsonArray, String name) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            if (name.equals(object.getString("name"))) {
                return object.getString("uuid");
            }
        }
        return null;
    }

    //@Scheduled(cron = "0 0 12 * * *")
    public boolean checkAndUpdateLicense() {
        String appPassword = getPassword();
        if (appPassword != null) {
            try {
                Pair<Integer, Boolean> authResult = authenticate(getUsername(), appPassword);
                if (!authResult.second) {
                    setLicensed(false);
                }
            } catch (IOException | InterruptedException | JSONException e) {
                Log.e(TAG, "Error authenticating", e);
            }
        }
        return isLicensed();
    }

    public boolean isLicensed() {
        return authCache.getBoolean(IS_LICENSED, false);
    }

    public void setLicensed(boolean licensed) {
        authCache.edit().putBoolean(IS_LICENSED, licensed).apply();
    }

    public void logout() {
        authCache.edit().clear().apply();
    }

    public String getUsername() {
        return authCache.getString(USERNAME, null);
    }

    public void setUsername(String username) {
        authCache.edit().putString(USERNAME, username).apply();
    }

    public void setPassword(String password) {
        authCache.edit().putString(PASSWORD, password).apply();
    }

    public String getPassword() {
        return authCache.getString(PASSWORD, null);
    }
}
