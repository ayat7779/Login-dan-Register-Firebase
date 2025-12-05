package com.apps.logindanregisterfirebase.Admin;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.apps.logindanregisterfirebase.R;
import com.apps.logindanregisterfirebase.Utils.SessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DuplicateManagerActivity extends AppCompatActivity {
    private TextView tvDuplicates;
    private Button btnScan, btnCleanup;
    private DatabaseReference databaseRef;
    private SessionManager sessionManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_duplicate_manager);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        sessionManager = new SessionManager(this);
        databaseRef = FirebaseDatabase.getInstance().getReference("users");

        tvDuplicates = findViewById(R.id.tvDuplicates);
        btnScan = findViewById(R.id.btnScan);
        btnCleanup = findViewById(R.id.btnCleanup);

        btnScan.setOnClickListener(v -> scanForDuplicates());
        btnCleanup.setOnClickListener(v -> cleanupDuplicates());

        // Auto scan on start
        scanForDuplicates();
    }

    private void scanForDuplicates() {
        tvDuplicates.setText("Scanning for duplicate emails...");

        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, List<String>> emailMap = new HashMap<>();

                // Group users by email
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String uid = userSnap.getKey();
                    String email = userSnap.child("email").getValue(String.class);

                    if (email != null && !email.isEmpty()) {
                        if (!emailMap.containsKey(email)) {
                            emailMap.put(email, new ArrayList<>());
                        }
                        emailMap.get(email).add(uid);
                    }
                }

                // Find duplicates
                StringBuilder result = new StringBuilder();
                result.append("📊 DUPLICATE EMAIL REPORT\n\n");
                result.append("Total users: ").append(snapshot.getChildrenCount()).append("\n");
                result.append("Unique emails: ").append(emailMap.size()).append("\n\n");

                int duplicateCount = 0;

                for (Map.Entry<String, List<String>> entry : emailMap.entrySet()) {
                    if (entry.getValue().size() > 1) {
                        duplicateCount++;
                        result.append("❌ DUPLICATE: ").append(entry.getKey()).append("\n");
                        result.append("   Users: ").append(entry.getValue().size()).append("\n");

                        for (String uid : entry.getValue()) {
                            DataSnapshot user = snapshot.child(uid);
                            String username = user.child("username").getValue(String.class);
                            String role = user.child("role").getValue(String.class);
                            result.append("   - ").append(uid).append(": ")
                                    .append(username).append(" (").append(role).append(")\n");
                        }
                        result.append("\n");
                    }
                }

                if (duplicateCount == 0) {
                    result.append("✅ No duplicate emails found!");
                } else {
                    result.append("⚠️ Found ").append(duplicateCount).append(" duplicate emails!");
                    btnCleanup.setEnabled(true);
                }

                tvDuplicates.setText(result.toString());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvDuplicates.setText("Error: " + error.getMessage());
            }
        });
    }

    private void cleanupDuplicates() {
        // Implement cleanup logic
        Toast.makeText(this,
                "Cleanup feature requires Cloud Function. Contact developer.",
                Toast.LENGTH_LONG).show();
    }
}