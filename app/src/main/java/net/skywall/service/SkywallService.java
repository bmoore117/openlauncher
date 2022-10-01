package net.skywall.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import net.skywall.fragment.view.Phone;
import net.skywall.utils.Pair;

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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SkywallService {

    private static final String TAG = SkywallService.class.getSimpleName();
    public static final String IS_LICENSED = "isLicensed";
    public static final String USERNAME = "USERNAME";
    public static final String PASSWORD = "PASSWORD";

    private SharedPreferences authCache;
    private boolean isInit;
    private static final SkywallService instance = new SkywallService();

    private SkywallService() {}

    public static SkywallService getInstance(Context context) {
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

        URL url = new URL("https://skywall.net/wp-json/wp/v2/users/me?context=edit&_fields=roles,id");
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
        String baseUrl = String.format("https://skywall.net/wp-json/wp/v2/users/%d/application-passwords", userId);
        URL url = new URL(baseUrl);
        HttpURLConnection existsRequest = (HttpURLConnection) url.openConnection();
        existsRequest.setRequestProperty("Authorization", "Basic " + new String(base64Bytes));
        existsRequest.setRequestMethod("GET");
        int max;
        try (InputStream in = new BufferedInputStream(existsRequest.getInputStream())) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String output = reader.lines().collect(Collectors.joining("\n"));
            JSONArray jsonArray = new JSONArray(output);
            max = findMaxSuffix(jsonArray);
        } finally {
            existsRequest.disconnect();
        }

        HttpURLConnection createRequest = (HttpURLConnection) (new URL(baseUrl + "?name=" + name + "-" + (max + 1))).openConnection();
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

    private int findMaxSuffix(JSONArray jsonArray) throws JSONException {
        int max = 0;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            String name = object.getString("name");
            if (!name.contains("android")) {
                continue;
            }
            int idx = Integer.parseInt(name.split("-")[2]);
            if (idx > max) {
                max = idx;
            }
        }
        return max;
    }

    public void checkAndUpdateLicense() {
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
    }

    public List<Phone> fetchUserPhones() throws IOException, JSONException {
        String appPassword = getPassword();
        if (appPassword == null) {
            return Collections.emptyList();
        }

        byte[] base64Bytes = Base64.getEncoder().encode((getUsername() + ":" + appPassword).getBytes());

        URL url = new URL("https://skywall-361905.uc.r.appspot.com/get-user-phones");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Authorization", "Basic " + new String(base64Bytes));
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("Accept", "application/json");
        urlConnection.setRequestMethod("POST");
        urlConnection.setDoOutput(true);
        JSONObject body = new JSONObject();
        body.put("skywallUsername", getUsername());
        body.put("skywallPassword", appPassword);
        urlConnection.getOutputStream().write(body.toString(4).getBytes());
        try {
            if (urlConnection.getResponseCode() != 200) {
                return Collections.emptyList();
            }
            try (InputStream in = new BufferedInputStream(urlConnection.getInputStream())) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String output = reader.lines().collect(Collectors.joining("\n"));
                JSONArray results = new JSONArray(output);
                List<Phone> phones = new ArrayList<>(results.length());
                for (int i = 0; i < results.length(); i++) {
                    JSONObject object = results.getJSONObject(i);
                    phones.add(new Phone(object.getString("phoneName"), object.getString("enrollmentTokenName")));
                }
                return phones;
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    public void deprovisionPhone(Phone phone) throws IOException, JSONException {
        String appPassword = getPassword();
        if (appPassword == null) {
            return;
        }

        byte[] base64Bytes = Base64.getEncoder().encode((getUsername() + ":" + appPassword).getBytes());
        HttpURLConnection request = (HttpURLConnection) (new URL("https://skywall-361905.uc.r.appspot.com/deprovision")).openConnection();
        request.setRequestProperty("Authorization", "Basic " + new String(base64Bytes));
        request.setRequestProperty("Accept", "application/json");
        request.setRequestMethod("POST");
        request.setDoOutput(true);
        JSONObject body = new JSONObject();
        body.put("skywallUsername", getUsername());
        body.put("skywallPassword", appPassword);
        body.put("enrollmentTokenName", phone.getEnrollmentTokenName());
        request.getOutputStream().write(body.toString(4).getBytes());
        try {
            request.getResponseCode(); // forces sending
        } finally {
            request.disconnect();
        }
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
