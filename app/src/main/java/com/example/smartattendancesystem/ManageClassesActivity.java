package com.example.smartattendancesystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class ManageClassesActivity extends AppCompatActivity {

    private DatabaseReference db;
    private EditText etClassName;
    private Button btnCreateClass;
    private ListView listClasses;

    private final List<String> classIds = new ArrayList<>();
    private final List<String> classNames = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_classes);

        etClassName = findViewById(R.id.etClassName);
        btnCreateClass = findViewById(R.id.btnCreateClass);
        listClasses = findViewById(R.id.listClasses);

        db = FirebaseDatabase.getInstance().getReference();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, classNames);
        listClasses.setAdapter(adapter);

        btnCreateClass.setOnClickListener(v -> createClass());

        listClasses.setOnItemClickListener((parent, view, position, id) -> {
            String classId = classIds.get(position);
            String className = classNames.get(position);

            Intent i = new Intent(this, ClassRosterActivity.class);
            i.putExtra("classId", classId);
            i.putExtra("className", className);
            startActivity(i);
        });

        loadMyClasses();
    }

    private void loadMyClasses() {
        String teacherUid = FirebaseAuth.getInstance().getUid();
        if (teacherUid == null) return;

        Query q = db.child("classes").orderByChild("teacherId").equalTo(teacherUid);
        q.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                classIds.clear();
                classNames.clear();

                for (DataSnapshot c : snapshot.getChildren()) {
                    classIds.add(c.getKey());
                    String name = c.child("name").getValue(String.class);
                    if (name == null) name = "(Unnamed Class)";
                    classNames.add(name);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ManageClassesActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void createClass() {
        String teacherUid = FirebaseAuth.getInstance().getUid();
        if (teacherUid == null) return;

        String name = etClassName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Enter class name", Toast.LENGTH_SHORT).show();
            return;
        }

        String classId = db.child("classes").push().getKey();
        if (classId == null) return;

        Map<String, Object> cls = new HashMap<>();
        cls.put("name", name);
        cls.put("teacherId", teacherUid);
        cls.put("createdAt", System.currentTimeMillis());

        db.child("classes").child(classId).setValue(cls)
                .addOnSuccessListener(v -> {
                    etClassName.setText("");
                    Toast.makeText(this, "Class created", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
