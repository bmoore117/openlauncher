package com.hyperion.skywall.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.benny.openlauncher.R;
import com.hyperion.skywall.service.WindowChangeDetectingService;

public class BlockedActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked);

        Intent intent = getIntent();
        String activityName = intent.getStringExtra(WindowChangeDetectingService.ACTIVITY_NAME);

        TextView label = findViewById(R.id.activity_blocked_label);
        label.setText(getResources().getString(R.string.blocked_activity) + ": " + activityName);
    }
}
