package com.example.smartattendancesystem;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class SessionDetailsActivity extends AppCompatActivity {

    private DatabaseReference db;

    private TextView txtHeader;
    private ListView listPresent, listAbsent;

    private final List<String> presentItems = new ArrayList<>();
    private final List<String> absentItems = new ArrayList<>();
    private ArrayAdapter<String> presentAdapter, absentAdapter;

    private String sessionId;
    private String classId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_details);

        txtHeader = findViewById(R.id.txtHeader);
        listPresent = findViewById(R.id.listPresent);
        listAbsent = findViewById(R.id.listAbsent);

        presentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, presentItems);
        absentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, absentItems);
        listPresent.setAdapter(presentAdapter);
        listAbsent.setAdapter(absentAdapter);

        db = FirebaseDatabase.getInstance().getReference();

        sessionId = getIntent().getStringExtra("sessionId");
        if (sessionId == null) {
            finish();
            return;
        }

        txtHeader.setText("Session: " + sessionId);

        loadSessionThenCompute();
    }

    private void loadSessionThenCompute() {
        db.child("sessions").child(sessionId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot s) {
                        classId = s.child("classId").getValue(String.class);
                        if (classId == null) {
                            Toast.makeText(SessionDetailsActivity.this, "Session missing classId", Toast.LENGTH_LONG).show();
                            return;
                        }
                        computePresentAbsent();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(SessionDetailsActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void computePresentAbsent() {
        // Load roster + attendance, then diff
        db.child("classStudents").child(classId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot rosterSnap) {
                        Map<String, String> roster = new HashMap<>(); // uid->email
                        for (DataSnapshot st : rosterSnap.getChildren()) {
                            String uid = st.getKey();
                            String email = st.child("email").getValue(String.class);
                            if (email == null) email = uid;
                            roster.put(uid, email);
                        }

                        db.child("attendance").child(sessionId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot attSnap) {
                                        presentItems.clear();
                                        absentItems.clear();

                                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM HH:mm:ss", Locale.getDefault());

                                        // Present = those in attendance
                                        Set<String> presentUids = new HashSet<>();
                                        for (DataSnapshot a : attSnap.getChildren()) {
                                            String uid = a.getKey();
                                            presentUids.add(uid);

                                            String email = roster.containsKey(uid) ? roster.get(uid) : uid;
                                            Long ts = a.getValue(Long.class);
                                            String timeStr = (ts != null) ? sdf.format(new Date(ts)) : "-";
                                            presentItems.add(email + " | " + timeStr);
                                        }

                                        // Absent = roster - present
                                        for (Map.Entry<String, String> entry : roster.entrySet()) {
                                            if (!presentUids.contains(entry.getKey())) {
                                                absentItems.add(entry.getValue());
                                            }
                                        }

                                        if (presentItems.isEmpty()) presentItems.add("No present records");
                                        if (absentItems.isEmpty()) absentItems.add("No absentees (all present or no roster)");

                                        presentAdapter.notifyDataSetChanged();
                                        absentAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        Toast.makeText(SessionDetailsActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(SessionDetailsActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
