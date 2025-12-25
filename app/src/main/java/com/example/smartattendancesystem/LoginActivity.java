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
    TextView txtStatus;

    FirebaseAuth auth;
    DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        txtStatus = findViewById(R.id.txtStatus);

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();

        findViewById(R.id.btnRegister).setOnClickListener(v -> register());
        findViewById(R.id.btnLogin).setOnClickListener(v -> login());
    }

    private static String emailKey(String email) {
        return email.trim().toLowerCase().replace(".", "_");
    }

    private void register() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            txtStatus.setText("Email & Password required");
            return;
        }

        txtStatus.setText("Registering...");

        auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(res -> {
                    FirebaseUser u = auth.getCurrentUser();
                    if (u == null) {
                        txtStatus.setText("Register ok but user is null");
                        return;
                    }

                    // Decide role automatically:
                    // If email in pendingTeachers -> teacher else student
                    String eKey = emailKey(email);

                    db.child("pendingTeachers").child(eKey)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot pendingSnap) {

                                    String role = pendingSnap.exists() ? "teacher" : "student";

                                    Map<String, Object> user = new HashMap<>();
                                    user.put("email", email);
                                    user.put("role", role);
                                    user.put("active", true);

                                    Map<String, Object> updates = new HashMap<>();

                                    updates.put("/users/" + u.getUid(), user);
                                    updates.put("/emailToUid/" + eKey, u.getUid());

                                    // consume pendingTeachers entry once used
                                    if (pendingSnap.exists()) {
                                        updates.put("/pendingTeachers/" + eKey, null);
                                    }

                                    db.updateChildren(updates)
                                            .addOnSuccessListener(v -> txtStatus.setText("Registered as " + role + ". Now Login"))
                                            .addOnFailureListener(e -> txtStatus.setText("DB save failed: " + e.getMessage()));
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    txtStatus.setText("DB error: " + error.getMessage());
                                }
                            });
                })
                .addOnFailureListener(e -> txtStatus.setText(e.getMessage()));
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            txtStatus.setText("Email & Password required");
            return;
        }

        txtStatus.setText("Logging in...");

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

                        Boolean active = snapshot.child("active").getValue(Boolean.class);
                        if (active != null && !active) {
                            txtStatus.setText("Account disabled. Contact admin.");
                            FirebaseAuth.getInstance().signOut();
                            return;
                        }

                        String role = snapshot.child("role").getValue(String.class);
                        if (role == null) {
                            txtStatus.setText("Role is NULL in DB");
                            return;
                        }

                        if ("admin".equals(role)) {
                            startActivity(new Intent(LoginActivity.this, AdminHomeActivity.class));
                            finish();
                        } else if ("teacher".equals(role)) {
                            startActivity(new Intent(LoginActivity.this, TeacherHomeActivity.class));
                            finish();
                        } else {
                            startActivity(new Intent(LoginActivity.this, StudentHomeActivity.class));
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        txtStatus.setText("DB error: " + error.getMessage());
                    }
                });
    }
}
