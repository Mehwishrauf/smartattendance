package com.example.smartattendancesystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class AdminHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        Button btnManageTeachers = findViewById(R.id.btnManageTeachers);
        Button btnAnalytics = findViewById(R.id.btnAnalytics);
        Button btnReports = findViewById(R.id.btnReports);
        Button btnDbManager = findViewById(R.id.btnDbManager);

        btnManageTeachers.setOnClickListener(v ->
                startActivity(new Intent(this, AdminManageTeachersActivity.class)));

        btnAnalytics.setOnClickListener(v ->
                startActivity(new Intent(this, AdminAnalyticsActivity.class)));

        btnReports.setOnClickListener(v ->
                startActivity(new Intent(this, AdminReportsActivity.class)));

        btnDbManager.setOnClickListener(v ->
                startActivity(new Intent(this, AdminDatabaseManagerActivity.class)));
    }
}
