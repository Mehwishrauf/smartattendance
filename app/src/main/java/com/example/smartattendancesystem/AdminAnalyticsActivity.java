package com.example.smartattendancesystem;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

public class AdminAnalyticsActivity extends AppCompatActivity {

    private DatabaseReference db;

    private TextView txtTeachers, txtStudents, txtClasses, txtSessions, txtAttendanceMarks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_analytics);

        db = FirebaseDatabase.getInstance().getReference();

        txtTeachers = findViewById(R.id.txtTeachers);
        txtStudents = findViewById(R.id.txtStudents);
        txtClasses = findViewById(R.id.txtClasses);
        txtSessions = findViewById(R.id.txtSessions);
        txtAttendanceMarks = findViewById(R.id.txtAttendanceMarks);

        loadCounts();
    }

    private void loadCounts() {
        countByRole("teacher", txtTeachers);
        countByRole("student", txtStudents);

        db.child("classes").addListenerForSingleValueEvent(new CountListener(txtClasses, "Total Classes: "));
        db.child("sessions").addListenerForSingleValueEvent(new CountListener(txtSessions, "Total Sessions: "));

        // total attendance marks = sum of children counts under attendance/*
        db.child("attendance").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snap) {
                long total = 0;
                for (DataSnapshot sess : snap.getChildren()) {
                    total += sess.getChildrenCount();
                }
                txtAttendanceMarks.setText("Attendance Marks: " + total);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                txtAttendanceMarks.setText("Attendance Marks: error");
            }
        });
    }

    private void countByRole(String role, TextView target) {
        Query q = db.child("users").orderByChild("role").equalTo(role);
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snap) {
                target.setText("Total " + role + "s: " + snap.getChildrenCount());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                target.setText("Total " + role + "s: error");
            }
        });
    }

    static class CountListener implements ValueEventListener {
        private final TextView tv;
        private final String prefix;

        CountListener(TextView tv, String prefix) {
            this.tv = tv;
            this.prefix = prefix;
        }

        @Override
        public void onDataChange(DataSnapshot snapshot) {
            tv.setText(prefix + snapshot.getChildrenCount());
        }

        @Override
        public void onCancelled(DatabaseError error) {
            tv.setText(prefix + "error");
        }
    }
}
