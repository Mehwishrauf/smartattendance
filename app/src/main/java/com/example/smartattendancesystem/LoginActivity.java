package com.example.smartattendancesystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    RadioButton rbStudent, rbTeacher;
    TextView txtStatus;

    FirebaseAuth auth;
    DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        rbStudent = findViewById(R.id.rbStudent);
        rbTeacher = findViewById(R.id.rbTeacher);
        txtStatus = findViewById(R.id.txtStatus);

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();

        findViewById(R.id.btnRegister).setOnClickListener(v -> register());
        findViewById(R.id.btnLogin).setOnClickListener(v -> login());
    }

    private String getRole() {
        return rbTeacher.isChecked() ? "teacher" : "student";
    }

    private void register() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String role = getRole();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            txtStatus.setText("Email & Password required");
            return;
        }

        auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(res -> {
                    FirebaseUser u = auth.getCurrentUser();
                    if (u == null) return;

                    Map<String, Object> user = new HashMap<>();
                    user.put("email", email);
                    user.put("role", role);

                    db.child("users").child(u.getUid()).setValue(user)
                            .addOnSuccessListener(v -> txtStatus.setText("Registered. Now Login"));
                })
                .addOnFailureListener(e -> txtStatus.setText(e.getMessage()));
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(res -> routeUser())
                .addOnFailureListener(e -> txtStatus.setText(e.getMessage()));
    }

    private void routeUser() {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) {
            txtStatus.setText("Login ok but user is null");
            return;
        }

        txtStatus.setText("Login ok. Fetching role...");

        db.child("users").child(u.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            txtStatus.setText("No user profile found in DB");
                            return;
                        }

                        String role = snapshot.child("role").getValue(String.class);

                        if (role == null) {
                            txtStatus.setText("Role is NULL in DB");
                            return;
                        }

                        txtStatus.setText("Role = " + role + ", routing...");

                        if ("teacher".equals(role)) {
                            startActivity(new Intent(LoginActivity.this, TeacherHomeActivity.class));
                            finish();
                        } else if ("student".equals(role)) {
                            startActivity(new Intent(LoginActivity.this, StudentHomeActivity.class));
                            finish();
                        } else {
                            txtStatus.setText("Unknown role: " + role);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        txtStatus.setText("DB error: " + error.getMessage());
                    }
                });
    }

}
