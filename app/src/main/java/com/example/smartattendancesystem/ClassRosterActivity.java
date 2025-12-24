package com.example.smartattendancesystem;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

import java.util.*;

public class ClassRosterActivity extends AppCompatActivity {

    private DatabaseReference db;

    private TextView txtTitle;
    private EditText etStudentEmail;
    private Button btnAddStudent;
    private ListView listRoster;

    private String classId;
    private String className;

    private final List<String> studentUids = new ArrayList<>();
    private final List<String> rosterItems = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private static String emailKey(String email) {
        return email.trim().toLowerCase().replace(".", "_");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_roster);

        classId = getIntent().getStringExtra("classId");
        className = getIntent().getStringExtra("className");
        if (classId == null) finish();

        txtTitle = findViewById(R.id.txtTitle);
        etStudentEmail = findViewById(R.id.etStudentEmail);
        btnAddStudent = findViewById(R.id.btnAddStudent);
        listRoster = findViewById(R.id.listRoster);

        txtTitle.setText("Class: " + (className == null ? "" : className));

        db = FirebaseDatabase.getInstance().getReference();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rosterItems);
        listRoster.setAdapter(adapter);

        btnAddStudent.setOnClickListener(v -> addStudentByEmail());
        loadRoster();
    }

    private void loadRoster() {
        db.child("classStudents").child(classId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        studentUids.clear();
                        rosterItems.clear();

                        for (DataSnapshot s : snapshot.getChildren()) {
                            String uid = s.getKey();
                            String email = s.child("email").getValue(String.class);
                            if (email == null) email = uid;

                            studentUids.add(uid);
                            rosterItems.add(email);
                        }

                        if (rosterItems.isEmpty()) rosterItems.add("No students added yet");
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(ClassRosterActivity.this,
                                "Error: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void addStudentByEmail() {
        String email = etStudentEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter student email", Toast.LENGTH_SHORT).show();
            return;
        }

        // lookup uid
        db.child("emailToUid").child(emailKey(email))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String uid = snapshot.getValue(String.class);
                        if (uid == null) {
                            Toast.makeText(ClassRosterActivity.this,
                                    "No user found for this email. Student must register first.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        Map<String, Object> studentNode = new HashMap<>();
                        studentNode.put("email", email);
                        studentNode.put("addedAt", System.currentTimeMillis());

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("/classStudents/" + classId + "/" + uid, studentNode);
                        updates.put("/studentClasses/" + uid + "/" + classId, true);

                        db.updateChildren(updates)
                                .addOnSuccessListener(v -> {
                                    etStudentEmail.setText("");
                                    Toast.makeText(ClassRosterActivity.this, "Student added", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(ClassRosterActivity.this, e.getMessage(), Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(ClassRosterActivity.this,
                                "Error: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
