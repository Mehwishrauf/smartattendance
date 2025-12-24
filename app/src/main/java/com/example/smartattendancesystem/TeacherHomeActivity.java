package com.example.smartattendancesystem;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.*;

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

    private Spinner spClasses;
    private Button btnManageClasses;
    private Button btnCreateSession, btnEndSession, btnViewPresentAbsent, btnExportCsv;
    private TextView txtSessionCode;
    private ImageView imgQr;
    private ListView listAttendance;

    private final List<String> classIds = new ArrayList<>();
    private final List<String> classNames = new ArrayList<>();
    private ArrayAdapter<String> classesAdapter;

    private final List<String> presentItems = new ArrayList<>();
    private ArrayAdapter<String> presentAdapter;

    private String currentSessionId = null;
    private String currentClassId = null;

    private ValueEventListener attendanceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_home);

        // Views
        spClasses = findViewById(R.id.spClasses);
        btnManageClasses = findViewById(R.id.btnManageClasses);
        btnCreateSession = findViewById(R.id.btnCreateSession);
        btnEndSession = findViewById(R.id.btnEndSession);
        btnViewPresentAbsent = findViewById(R.id.btnViewPresentAbsent);
        btnExportCsv = findViewById(R.id.btnExportCsv);

        txtSessionCode = findViewById(R.id.txtSessionCode);
        imgQr = findViewById(R.id.imgQr);
        listAttendance = findViewById(R.id.listAttendance);

        db = FirebaseDatabase.getInstance().getReference();

        // Present list adapter
        presentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, presentItems);
        listAttendance.setAdapter(presentAdapter);

        // Classes adapter
        classesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, classNames);
        spClasses.setAdapter(classesAdapter);

        // Load classes
        loadTeacherClasses();

        // Restore session if saved
        currentSessionId = getSharedPreferences("app", MODE_PRIVATE).getString("currentSessionId", null);
        currentClassId = getSharedPreferences("app", MODE_PRIVATE).getString("currentClassId", null);

        if (currentSessionId != null) {
            txtSessionCode.setText("Session Code: " + currentSessionId);
            generateQr(currentSessionId);
            startLiveAttendanceListener(currentSessionId);
        }

        btnManageClasses.setOnClickListener(v ->
                startActivity(new Intent(TeacherHomeActivity.this, ManageClassesActivity.class))
        );

        btnCreateSession.setOnClickListener(v -> createSessionForSelectedClass());
        btnEndSession.setOnClickListener(v -> endSession());

        btnViewPresentAbsent.setOnClickListener(v -> {
            if (currentSessionId == null) {
                Toast.makeText(this, "No active session", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(this, SessionDetailsActivity.class);
            i.putExtra("sessionId", currentSessionId);
            startActivity(i);
        });

        btnExportCsv.setOnClickListener(v -> exportCsv());
    }

    private void loadTeacherClasses() {
        String teacherUid = FirebaseAuth.getInstance().getUid();
        if (teacherUid == null) return;

        Query q = db.child("classes").orderByChild("teacherId").equalTo(teacherUid);
        q.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                classIds.clear();
                classNames.clear();

                for (DataSnapshot c : snapshot.getChildren()) {
                    String classId = c.getKey();
                    String name = c.child("name").getValue(String.class);
                    if (name == null) name = "(Unnamed Class)";

                    classIds.add(classId);
                    classNames.add(name);
                }

                classesAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(TeacherHomeActivity.this, "Classes error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void createSessionForSelectedClass() {
        String teacherId = FirebaseAuth.getInstance().getUid();
        if (teacherId == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (classIds.isEmpty()) {
            Toast.makeText(this, "Create a class first", Toast.LENGTH_SHORT).show();
            return;
        }

        int pos = spClasses.getSelectedItemPosition();
        if (pos < 0 || pos >= classIds.size()) {
            Toast.makeText(this, "Select a class", Toast.LENGTH_SHORT).show();
            return;
        }

        currentClassId = classIds.get(pos);

        // 8-char code as sessionId
        currentSessionId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        long now = System.currentTimeMillis();

        Map<String, Object> session = new HashMap<>();
        session.put("sessionId", currentSessionId);
        session.put("classId", currentClassId);
        session.put("teacherId", teacherId);
        session.put("startTime", now);
        session.put("expiryTime", now + (10 * 60 * 1000)); // 10 minutes
        session.put("active", true);

        // Write session + classSessions index (helps monthly)
        Map<String, Object> updates = new HashMap<>();
        updates.put("/sessions/" + currentSessionId, session);

        Map<String, Object> classSessionNode = new HashMap<>();
        classSessionNode.put("startTime", now);
        classSessionNode.put("active", true);
        updates.put("/classSessions/" + currentClassId + "/" + currentSessionId, classSessionNode);

        db.updateChildren(updates)
                .addOnSuccessListener(v -> {
                    getSharedPreferences("app", MODE_PRIVATE).edit()
                            .putString("currentSessionId", currentSessionId)
                            .putString("currentClassId", currentClassId)
                            .apply();

                    txtSessionCode.setText("Session Code: " + currentSessionId);
                    generateQr(currentSessionId);

                    Toast.makeText(this, "Session Created", Toast.LENGTH_SHORT).show();
                    startLiveAttendanceListener(currentSessionId);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Create failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void startLiveAttendanceListener(String sessionId) {
        stopLiveAttendanceListener();

        presentItems.clear();
        presentAdapter.notifyDataSetChanged();

        DatabaseReference ref = db.child("attendance").child(sessionId);

        attendanceListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                presentItems.clear();
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM HH:mm:ss", Locale.getDefault());

                for (DataSnapshot child : snapshot.getChildren()) {
                    String uid = child.getKey();
                    Long ts = child.getValue(Long.class);
                    String timeStr = (ts != null) ? sdf.format(new Date(ts)) : "-";

                    db.child("users").child(uid).child("email")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snap) {
                                    String email = snap.getValue(String.class);
                                    if (email == null) email = uid;
                                    presentItems.add(email + " | " + timeStr);
                                    presentAdapter.notifyDataSetChanged();
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    presentItems.add(uid + " | " + timeStr);
                                    presentAdapter.notifyDataSetChanged();
                                }
                            });
                }

                if (presentItems.isEmpty()) {
                    presentItems.add("No attendance yet");
                    presentAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(TeacherHomeActivity.this, "Attendance error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        };

        ref.addValueEventListener(attendanceListener);
    }

    private void stopLiveAttendanceListener() {
        if (attendanceListener != null && currentSessionId != null) {
            db.child("attendance").child(currentSessionId).removeEventListener(attendanceListener);
        }
        attendanceListener = null;
    }

    private void endSession() {
        if (currentSessionId == null) {
            Toast.makeText(this, "No active session", Toast.LENGTH_SHORT).show();
            return;
        }

        String sid = currentSessionId;
        String cid = currentClassId;

        Map<String, Object> updates = new HashMap<>();
        updates.put("/sessions/" + sid + "/active", false);
        if (cid != null) updates.put("/classSessions/" + cid + "/" + sid + "/active", false);

        db.updateChildren(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Session Ended", Toast.LENGTH_SHORT).show();

                    stopLiveAttendanceListener();
                    currentSessionId = null;
                    currentClassId = null;

                    getSharedPreferences("app", MODE_PRIVATE).edit()
                            .remove("currentSessionId")
                            .remove("currentClassId")
                            .apply();

                    txtSessionCode.setText("Session Code: -");
                    imgQr.setImageDrawable(null);

                    presentItems.clear();
                    presentItems.add("Session ended.");
                    presentAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "End failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void generateQr(String text) {
        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(text, BarcodeFormat.QR_CODE, 400, 400);

            int w = matrix.getWidth();
            int h = matrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);

            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            imgQr.setImageBitmap(bmp);
        } catch (Exception e) {
            Toast.makeText(this, "QR error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportCsv() {
        if (currentSessionId == null) {
            Toast.makeText(this, "No active session", Toast.LENGTH_SHORT).show();
            return;
        }

        File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) {
            Toast.makeText(this, "Storage not available", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(dir, "attendance_" + currentSessionId + ".csv");

        db.child("attendance").child(currentSessionId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            Toast.makeText(TeacherHomeActivity.this, "No attendance data", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Map<String, Long> attendanceMap = new HashMap<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            attendanceMap.put(child.getKey(), child.getValue(Long.class));
                        }

                        if (attendanceMap.isEmpty()) {
                            Toast.makeText(TeacherHomeActivity.this, "No attendance to export", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        writeCsvAsync(file, attendanceMap);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(TeacherHomeActivity.this, "Read failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void writeCsvAsync(File file, Map<String, Long> attendanceMap) {
        try {
            FileWriter writer = new FileWriter(file);
            writer.append("Email,Timestamp\n");

            List<String> uids = new ArrayList<>(attendanceMap.keySet());
            final int total = uids.size();
            final int[] completed = {0};

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            Runnable finish = () -> {
                try {
                    writer.flush();
                    writer.close();
                    Toast.makeText(TeacherHomeActivity.this, "CSV saved: " + file.getName(), Toast.LENGTH_SHORT).show();
                    shareCsv(file);
                } catch (Exception e) {
                    Toast.makeText(TeacherHomeActivity.this, "Finalize failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            };

            for (String uid : uids) {
                db.child("users").child(uid).child("email")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snap) {
                                String email = snap.getValue(String.class);
                                if (email == null) email = uid;

                                Long ts = attendanceMap.get(uid);
                                String timeStr = (ts != null) ? sdf.format(new Date(ts)) : "-";

                                try {
                                    writer.append(email).append(",").append(timeStr).append("\n");
                                } catch (Exception ignored) { }

                                completed[0]++;
                                if (completed[0] == total) finish.run();
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                completed[0]++;
                                if (completed[0] == total) finish.run();
                            }
                        });
            }

        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

            startActivity(Intent.createChooser(intent, "Share Attendance CSV"));
        } catch (Exception e) {
            Toast.makeText(this, "Share failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        stopLiveAttendanceListener();
        super.onDestroy();
    }
}
