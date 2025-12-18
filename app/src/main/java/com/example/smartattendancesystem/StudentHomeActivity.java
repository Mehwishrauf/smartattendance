package com.example.smartattendancesystem;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import androidx.activity.result.ActivityResultLauncher;



public class StudentHomeActivity extends AppCompatActivity {

    EditText etSessionCode;
    TextView txtStatus;
    DatabaseReference db;
    private final ActivityResultLauncher<ScanOptions> qrLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    etSessionCode.setText(result.getContents());
                    submitCode(); // reuse existing logic
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);
        findViewById(R.id.btnScanQr).setOnClickListener(v -> scanQr());

        etSessionCode = findViewById(R.id.etSessionCode);
        txtStatus = findViewById(R.id.txtStatus);
        db = FirebaseDatabase.getInstance().getReference();

        findViewById(R.id.btnSubmitCode).setOnClickListener(v -> submitCode());
    }

    private void scanQr() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan attendance QR");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        qrLauncher.launch(options);
    }

    private void submitCode() {
        String code = etSessionCode.getText().toString().trim().toUpperCase();
        if (TextUtils.isEmpty(code)) {
            txtStatus.setText("Enter session code");
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference sessionRef = db.child("sessions").child(code);

        sessionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    txtStatus.setText("Invalid session code");
                    return;
                }

                Boolean active = snapshot.child("active").getValue(Boolean.class);
                if (active == null || !active) {
                    txtStatus.setText("Session is not active");
                    return;
                }

                // Mark attendance
                db.child("attendance")
                        .child(code)
                        .child(uid)
                        .setValue(System.currentTimeMillis())
                        .addOnSuccessListener(v ->
                                txtStatus.setText("Attendance marked successfully")
                        )
                        .addOnFailureListener(e ->
                                txtStatus.setText("Error: " + e.getMessage())
                        );
            }

            @Override
            public void onCancelled(DatabaseError error) {
                txtStatus.setText("DB error: " + error.getMessage());
            }
        });
    }
}
