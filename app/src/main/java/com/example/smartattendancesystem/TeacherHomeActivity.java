package com.example.smartattendancesystem;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class TeacherHomeActivity extends AppCompatActivity {

    private DatabaseReference db;

    private TextView txtSessionCode;
    private ImageView imgQr;
    private ListView listAttendance;
    private Button btnExportCsv;

    private ArrayAdapter<String> adapter;
    private final List<String> items = new ArrayList<>();

    private String currentSessionId;
    private ValueEventListener attendanceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_home);

        // 1️⃣ INIT VIEWS FIRST
        txtSessionCode = findViewById(R.id.txtSessionCode);
        imgQr = findViewById(R.id.imgQr);
        listAttendance = findViewById(R.id.listAttendance);
        btnExportCsv = findViewById(R.id.btnExportCsv);

        Button btnCreate = findViewById(R.id.btnCreateSession);
        Button btnEnd = findViewById(R.id.btnEndSession);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        listAttendance.setAdapter(adapter);

        db = FirebaseDatabase.getInstance().getReference();

        // 2️⃣ RESTORE SESSION
        currentSessionId = getSharedPreferences("app", MODE_PRIVATE)
                .getString("currentSessionId", null);

        if (currentSessionId != null) {
            txtSessionCode.setText("Session Code: " + currentSessionId);
            generateQr(currentSessionId);
            startLiveAttendanceListener();
        }

        // 3️⃣ BUTTONS
        btnCreate.setOnClickListener(v -> createSession());
        btnEnd.setOnClickListener(v -> endSession());
        btnExportCsv.setOnClickListener(v -> exportCsv());
    }

    // ================= SESSION =================

    private void createSession() {
        String teacherId = FirebaseAuth.getInstance().getUid();
        if (teacherId == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        currentSessionId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        getSharedPreferences("app", MODE_PRIVATE)
                .edit()
                .putString("currentSessionId", currentSessionId)
                .apply();

        Map<String, Object> session = new HashMap<>();
        session.put("teacherId", teacherId);
        session.put("startTime", System.currentTimeMillis());
        session.put("active", true);

        db.child("sessions").child(currentSessionId).setValue(session)
                .addOnSuccessListener(v -> {
                    txtSessionCode.setText("Session Code: " + currentSessionId);
                    generateQr(currentSessionId);
                    startLiveAttendanceListener();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void endSession() {
        if (currentSessionId == null) return;

        db.child("sessions").child(currentSessionId).child("active").setValue(false);
        stopLiveAttendanceListener();

        getSharedPreferences("app", MODE_PRIVATE)
                .edit()
                .remove("currentSessionId")
                .apply();

        currentSessionId = null;
        txtSessionCode.setText("Session Code: -");
        items.clear();
        items.add("Session ended");
        adapter.notifyDataSetChanged();
    }

    // ================= LIVE ATTENDANCE =================

    private void startLiveAttendanceListener() {
        stopLiveAttendanceListener();
        items.clear();
        adapter.notifyDataSetChanged();

        DatabaseReference ref = db.child("attendance").child(currentSessionId);

        attendanceListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                items.clear();
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM HH:mm:ss", Locale.getDefault());

                for (DataSnapshot child : snapshot.getChildren()) {
                    String uid = child.getKey();
                    Long ts = child.getValue(Long.class);
                    String time = ts != null ? sdf.format(new Date(ts)) : "-";

                    db.child("users").child(uid).child("email")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snap) {
                                    String email = snap.getValue(String.class);
                                    if (email == null) email = uid;
                                    items.add(email + " | " + time);
                                    adapter.notifyDataSetChanged();
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    items.add(uid + " | " + time);
                                    adapter.notifyDataSetChanged();
                                }
                            });
                }

                if (items.isEmpty()) {
                    items.add("No attendance yet");
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(TeacherHomeActivity.this,
                        error.getMessage(), Toast.LENGTH_LONG).show();
            }
        };

        ref.addValueEventListener(attendanceListener);
    }

    private void stopLiveAttendanceListener() {
        if (attendanceListener != null && currentSessionId != null) {
            db.child("attendance").child(currentSessionId)
                    .removeEventListener(attendanceListener);
        }
    }

    // ================= QR =================

    private void generateQr(String text) {
        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(text, BarcodeFormat.QR_CODE, 400, 400);

            Bitmap bmp = Bitmap.createBitmap(400, 400, Bitmap.Config.RGB_565);
            for (int x = 0; x < 400; x++) {
                for (int y = 0; y < 400; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            imgQr.setImageBitmap(bmp);
        } catch (Exception ignored) {}
    }

    // ================= CSV EXPORT =================

    private void exportCsv() {
        if (currentSessionId == null) {
            Toast.makeText(this, "No active session", Toast.LENGTH_SHORT).show();
            return;
        }

        File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(dir, "attendance_" + currentSessionId + ".csv");

        db.child("attendance").child(currentSessionId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(TeacherHomeActivity.this,
                                    "No attendance data", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Map<String, Long> map = new HashMap<>();
                        for (DataSnapshot c : snapshot.getChildren()) {
                            map.put(c.getKey(), c.getValue(Long.class));
                        }

                        writeCsvAsync(file, map);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void writeCsvAsync(File file, Map<String, Long> map) {
        try {
            FileWriter writer = new FileWriter(file);
            writer.append("Email,Timestamp\n");

            List<String> uids = new ArrayList<>(map.keySet());
            int total = uids.size();
            int[] done = {0};

            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            Runnable finish = () -> {
                try {
                    writer.flush();
                    writer.close();
                    shareCsv(file);
                } catch (Exception ignored) {}
            };

            for (String uid : uids) {
                db.child("users").child(uid).child("email")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snap) {
                                try {
                                    String email = snap.getValue(String.class);
                                    if (email == null) email = uid;
                                    writer.append(email)
                                            .append(",")
                                            .append(sdf.format(new Date(map.get(uid))))
                                            .append("\n");
                                } catch (Exception ignored) {}

                                done[0]++;
                                if (done[0] == total) finish.run();
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                done[0]++;
                                if (done[0] == total) finish.run();
                            }
                        });
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareCsv(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    "com.example.smartattendancesystem.fileprovider",
                    file
            );

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share CSV"));
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
