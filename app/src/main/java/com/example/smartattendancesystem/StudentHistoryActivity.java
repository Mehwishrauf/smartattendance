package com.example.smartattendancesystem;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class StudentHistoryActivity extends AppCompatActivity {

    private DatabaseReference db;
    private ListView listHistory;

    private final List<String> items = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_history);

        listHistory = findViewById(R.id.listHistory);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        listHistory.setAdapter(adapter);

        db = FirebaseDatabase.getInstance().getReference();
        loadMyAttendance();
    }

    private void loadMyAttendance() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.child("studentAttendance").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        items.clear();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                        for (DataSnapshot s : snapshot.getChildren()) {
                            String sessionId = s.getKey();
                            Long ts = s.getValue(Long.class);
                            String timeStr = (ts != null) ? sdf.format(new Date(ts)) : "-";
                            items.add(sessionId + " | " + timeStr);
                        }

                        if (items.isEmpty()) items.add("No attendance history yet");
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(StudentHistoryActivity.this,
                                "Error: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
