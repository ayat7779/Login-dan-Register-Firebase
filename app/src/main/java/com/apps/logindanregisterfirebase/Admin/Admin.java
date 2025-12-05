package com.apps.logindanregisterfirebase.Admin;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.apps.logindanregisterfirebase.Entitas.User;
import com.apps.logindanregisterfirebase.LoginDanRegister.Login;
import com.apps.logindanregisterfirebase.R;
import com.apps.logindanregisterfirebase.Utils.CloudFunctionsHelper;
import com.apps.logindanregisterfirebase.Utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Admin extends AppCompatActivity {
    private RecyclerView rvUser;
    private DatabaseReference database;
    private UserAdapter adapter;
    private ArrayList<User> arrayList;
    private SessionManager sessionManager;
    private FirebaseAuth mAuth;
    private Button btnScan;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Check if user is admin
        sessionManager = new SessionManager(this);
        mAuth = FirebaseAuth.getInstance();

        if (!"admin".equals(sessionManager.getUserRole())) {
            // Not admin, redirect to main
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
            return;
        }

        // Setup views
        rvUser = findViewById(R.id.rvUser);
        ImageView ivKeluar = findViewById(R.id.ivKeluar);
        btnScan = findViewById(R.id.btnScan);


        ivKeluar.setOnClickListener(view -> {
            // Logout
            sessionManager.logoutUser();
            mAuth.signOut();
            Intent intent = new Intent(Admin.this, Login.class);
            startActivity(intent);
            finish();
        });

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Admin.this, DuplicateManagerActivity.class);
                startActivity(intent);
            }
        });

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().getReference("users");

        // Setup RecyclerView
        rvUser.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        rvUser.setLayoutManager(layoutManager);
        rvUser.setItemAnimator(new DefaultItemAnimator());

        // Load data
        tampilData();
    }

    // Admin.java - tampilData() method
    private void tampilData() {
        database.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                arrayList = new ArrayList<>();

                for (DataSnapshot item : snapshot.getChildren()) {
                    try {
                        // Cara 1: Menggunakan getValue()
                        User user = item.getValue(User.class);

                        // Cara 2: Manual mapping (jika cara 1 error)
                        if (user == null) {
                            user = new User();
                            user.setUid(item.getKey());

                            if (item.hasChild("username")) {
                                user.setUsername(item.child("username").getValue(String.class));
                            }
                            if (item.hasChild("email")) {
                                user.setEmail(item.child("email").getValue(String.class));
                            }
                            if (item.hasChild("noHp")) {
                                user.setNoHp(item.child("noHp").getValue(String.class));
                            }
                            if (item.hasChild("role")) {
                                user.setRole(item.child("role").getValue(String.class));
                            }
                            if (item.hasChild("status")) {
                                user.setStatus(item.child("status").getValue(Integer.class));
                            }
                        }

                        if (user.getUid() != null) {
                            arrayList.add(user);
                        }

                    } catch (Exception e) {
                        Log.e("Admin", "Error parsing user: " + e.getMessage());
                        // Skip user yang error
                    }
                }

                adapter = new UserAdapter(arrayList, Admin.this);
                rvUser.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Admin", "Database error: " + error.getMessage());
                Toast.makeText(Admin.this,
                        "Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Admin.java - Tambah method baru
    public void promoteToAdmin(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Promosi ke Admin")
                .setMessage("Promosikan " + user.getUsername() + " menjadi admin?")
                .setPositiveButton("Ya", (dialog, which) -> {
                    DatabaseReference userRef = database.child(user.getUid());

                    HashMap<String, Object> updates = new HashMap<>();
                    updates.put("role", "admin");
                    updates.put("status", 1);

                    userRef.updateChildren(updates)
                            .addOnSuccessListener(aVoid -> Toast.makeText(Admin.this,
                                    user.getUsername() + " sekarang adalah admin",
                                    Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(Admin.this,
                                    "Gagal: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Tidak", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Double check admin role
        if (!"admin".equals(sessionManager.getUserRole())) {
            finish();
        }
    }

    // Di Admin.java - Tambah method refresh data
    private void refreshUserList() {
        if (adapter != null) {
            tampilData(); // Load ulang data dari Firebase
        }
    }

    // Atau di onResume()
    @Override
    protected void onResume() {
        super.onResume();
        refreshUserList();
    }

    // Di Admin.java - tambah method
    private void showDeleteWarning() {
        Toast.makeText(this,
                "PERHATIAN: Menghapus user dari database TIDAK menghapus dari Firebase Auth.\n" +
                        "User tidak bisa daftar ulang dengan email yang sama.",
                Toast.LENGTH_LONG).show();
    }

    // Panggil sebelum hapus
    // showDeleteWarning();

    // Admin.java - Tambah menu untuk manajemen Auth users
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_auth_users) {
            showAuthUsers();
            return true;
        } else if (id == R.id.action_sync) {
            syncAuthWithDatabase();
            return true;
        } else if (id == R.id.action_logout) {
            sessionManager.logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAuthUsers() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Memuat users dari Auth...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        CloudFunctionsHelper helper = CloudFunctionsHelper.getInstance(this);

        helper.getAllAuthUsers((success, result) -> {
            progressDialog.dismiss();

            if (success && result != null) {
                showAuthUsersDialog(result);
            } else {
                Toast.makeText(this,
                        "Gagal memuat users dari Auth",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAuthUsersDialog(Map<String, Object> result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Users di Firebase Auth")
                .setMessage(formatAuthUsers(result))
                .setPositiveButton("OK", null)
                .show();
    }

    private String formatAuthUsers(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();

        if (result.containsKey("total")) {
            sb.append("Total users: ").append(result.get("total")).append("\n\n");
        }

        if (result.containsKey("users")) {
            List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("users");

            for (Map<String, Object> user : users) {
                sb.append("Email: ").append(user.get("email")).append("\n");
                sb.append("UID: ").append(user.get("uid")).append("\n");
                sb.append("Verified: ").append(user.get("emailVerified")).append("\n");
                sb.append("Status: ").append(Boolean.TRUE.equals(user.get("disabled")) ? "Disabled" : "Active").append("\n");
                sb.append("---\n");
            }
        }

        return sb.toString();
    }

    private void syncAuthWithDatabase() {
        Toast.makeText(this,
                "Sync feature akan diimplementasi nanti",
                Toast.LENGTH_SHORT).show();
    }
}