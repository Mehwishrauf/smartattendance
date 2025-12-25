package com.example.smartattendancesystem;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

public class AdminDatabaseManagerActivity extends AppCompatActivity {

    private DatabaseReference db;
    private TextView txtSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_database_manager);

        txtSummary = findViewById(R.id.txtSummary);
        db = FirebaseDatabase.getInstance().getReference();

        loadSummary();
    }

    private void loadSummary() {
        db.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snap) {
                long users = snap.child("users").getChildrenCount();
                long classes = snap.child("classes").getChildrenCount();
                long sessions = snap.child("sessions").getChildrenCount();
                long pending = snap.child("pendingTeachers").getChildrenCount();

                txtSummary.setText(
                        "Database Summary\n\n" +
                                "Users: " + users + "\n" +
                                "Classes: " + classes + "\n" +
                                "Sessions: " + sessions + "\n" +
                                "Pending Teachers: " + pending
                );
            }

            @Override
            public void onCancelled(DatabaseError error) {
                txtSummary.setText("Error: " + error.getMessage());
            }
        });
    }
}
