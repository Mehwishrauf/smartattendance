package com.example.smartattendancesystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class StudentHomeActivity extends AppCompatActivity {

    EditText etSessionCode;
    TextView txtStatus;
    DatabaseReference db;

    private final ActivityResultLauncher<ScanOptions> qrLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    etSessionCode.setText(result.getContents().trim().toUpperCase());
                    submitCode();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        etSessionCode = findViewById(R.id.etSessionCode);
        txtStatus = findViewById(R.id.txtStatus);
        db = FirebaseDatabase.getInstance().getReference();

        findViewById(R.id.btnSubmitCode).setOnClickListener(v -> submitCode());
        findViewById(R.id.btnScanQr).setOnClickListener(v -> scanQr());
        findViewById(R.id.btnMyHistory).setOnClickListener(v ->
                startActivity(new Intent(StudentHomeActivity.this, StudentHistoryActivity.class))
        );
    }

    private void scanQr() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan attendance QR");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        qrLauncher.launch(options);
    }

    private void submitCode() {
        String sessionId = etSessionCode.getText().toString().trim().toUpperCase();
        if (TextUtils.isEmpty(sessionId)) {
            txtStatus.setText("Enter session code");
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        txtStatus.setText("Checking session...");

        db.child("sessions").child(sessionId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot sessionSnap) {

                        if (!sessionSnap.exists()) {
                            txtStatus.setText("Invalid session code");
                            return;
                        }

                        Boolean active = sessionSnap.child("active").getValue(Boolean.class);
                        if (active == null || !active) {
                            txtStatus.setText("Session is not active");
                            return;
                        }

                        String classId = sessionSnap.child("classId").getValue(String.class);
                        if (classId == null) {
                            txtStatus.setText("Session missing classId (contact teacher)");
                            return;
                        }

                        Long expiry = sessionSnap.child("expiryTime").getValue(Long.class);
                        if (expiry != null && System.currentTimeMillis() > expiry) {
                            txtStatus.setText("Session expired");
                            return;
                        }

                        // ✅ Must be enrolled in class roster
                        db.child("classStudents").child(classId).child(uid)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot rosterSnap) {
                                        if (!rosterSnap.exists()) {
                                            txtStatus.setText("You are not enrolled in this class");
                                            return;
                                        }

                                        markAttendance(sessionId, uid);
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        txtStatus.setText("DB error: " + error.getMessage());
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        txtStatus.setText("DB error: " + error.getMessage());
                    }
                });
    }

    private void markAttendance(String sessionId, String uid) {
        txtStatus.setText("Marking attendance...");

        DatabaseReference attRef = db.child("attendance").child(sessionId).child(uid);

        // Duplicate prevention
        attRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot attSnap) {
                if (attSnap.exists()) {
                    txtStatus.setText("Attendance already marked");
                    Toast.makeText(StudentHomeActivity.this, "Attendance already marked", Toast.LENGTH_SHORT).show();
                    return;
                }

                long now = System.currentTimeMillis();

                // Write both:
                // 1) attendance/session/uid = timestamp
                // 2) studentAttendance/uid/session = timestamp (fast history)
                attRef.setValue(now)
                        .addOnSuccessListener(v -> {
                            db.child("studentAttendance").child(uid).child(sessionId).setValue(now);
                            txtStatus.setText("Attendance marked successfully");
                            Toast.makeText(StudentHomeActivity.this, "Attendance marked successfully", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> txtStatus.setText("Error: " + e.getMessage()));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                txtStatus.setText("DB error: " + error.getMessage());
            }
        });
    }
}
