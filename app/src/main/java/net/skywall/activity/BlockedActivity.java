package net.skywall.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.benny.openlauncher.R;
import net.skywall.service.WindowChangeDetectingService;

public class BlockedActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked);

        Intent intent = getIntent();
        String activityName = intent.getStringExtra(WindowChangeDetectingService.ACTIVITY_NAME);
        String appName = intent.getStringExtra(WindowChangeDetectingService.APP_NAME);

        TextView activity = findViewById(R.id.activity_blocked_activity_name);
        activity.setText(getResources().getString(R.string.blocked_activity) + ": " + activityName);

        TextView app = findViewById(R.id.activity_blocked_app_name);
        app.setText(getResources().getString(R.string.blocked_app) + ": " + appName);
    }
}
