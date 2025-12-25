package com.example.smartattendancesystem;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

import java.util.*;

public class AdminManageTeachersActivity extends AppCompatActivity {

    private DatabaseReference db;
    private EditText etTeacherEmail;
    private Button btnAddPending;
    private ListView listTeachers;

    private final List<String> teacherUids = new ArrayList<>();
    private final List<String> teacherItems = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private static String emailKey(String email) {
        return email.trim().toLowerCase().replace(".", "_");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_teachers);

        db = FirebaseDatabase.getInstance().getReference();

        etTeacherEmail = findViewById(R.id.etTeacherEmail);
        btnAddPending = findViewById(R.id.btnAddPending);
        listTeachers = findViewById(R.id.listTeachers);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, teacherItems);
        listTeachers.setAdapter(adapter);

        btnAddPending.setOnClickListener(v -> addPendingTeacher());
        loadTeachers();

        listTeachers.setOnItemClickListener((parent, view, position, id) -> {
            String uid = teacherUids.get(position);
            showToggleDialog(uid);
        });
    }

    private void addPendingTeacher() {
        String email = etTeacherEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter teacher email", Toast.LENGTH_SHORT).show();
            return;
        }

        db.child("pendingTeachers").child(emailKey(email)).setValue(true)
                .addOnSuccessListener(v -> {
                    etTeacherEmail.setText("");
                    Toast.makeText(this, "Added to pending teachers", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void loadTeachers() {
        Query q = db.child("users").orderByChild("role").equalTo("teacher");
        q.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                teacherUids.clear();
                teacherItems.clear();

                for (DataSnapshot u : snapshot.getChildren()) {
                    String uid = u.getKey();
                    String email = u.child("email").getValue(String.class);
                    Boolean active = u.child("active").getValue(Boolean.class);

                    if (email == null) email = uid;
                    String state = (active == null || active) ? "ACTIVE" : "DISABLED";

                    teacherUids.add(uid);
                    teacherItems.add(email + " (" + state + ")");
                }

                if (teacherItems.isEmpty()) teacherItems.add("No teachers yet");
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AdminManageTeachersActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showToggleDialog(String uid) {
        // Simple toggle active
        db.child("users").child(uid).child("active")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snap) {
                        Boolean current = snap.getValue(Boolean.class);
                        boolean newValue = (current == null) || current ? false : true;

                        db.child("users").child(uid).child("active").setValue(newValue)
                                .addOnSuccessListener(v -> Toast.makeText(AdminManageTeachersActivity.this,
                                        newValue ? "Teacher enabled" : "Teacher disabled", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(AdminManageTeachersActivity.this,
                                        e.getMessage(), Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(AdminManageTeachersActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
