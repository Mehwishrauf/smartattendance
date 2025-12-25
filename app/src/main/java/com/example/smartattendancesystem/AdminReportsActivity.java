package com.example.smartattendancesystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

public class AdminReportsActivity extends AppCompatActivity {

    EditText etSessionId;
    Button btnOpen;
    TextView txtInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_reports);

        etSessionId = findViewById(R.id.etSessionId);
        btnOpen = findViewById(R.id.btnOpenSession);
        txtInfo = findViewById(R.id.txtInfo);

        btnOpen.setOnClickListener(v -> {
            String sid = etSessionId.getText().toString().trim().toUpperCase();
            if (TextUtils.isEmpty(sid)) {
                txtInfo.setText("Enter session code");
                return;
            }
            Intent i = new Intent(this, SessionDetailsActivity.class);
            i.putExtra("sessionId", sid);
            startActivity(i);
        });
    }
}
