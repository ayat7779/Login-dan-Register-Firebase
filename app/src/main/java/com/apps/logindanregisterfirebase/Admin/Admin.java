package com.apps.logindanregisterfirebase.Admin;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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
import com.apps.logindanregisterfirebase.Utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class Admin extends AppCompatActivity {
    private ImageView ivKeluar;
    private RecyclerView rvUser;
    private DatabaseReference database;
    private UserAdapter adapter;
    private ArrayList<User> arrayList;

    private SessionManager sessionManager;
    private FirebaseAuth mAuth;

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
        ivKeluar = findViewById(R.id.ivKeluar);

        ivKeluar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Logout
                sessionManager.logoutUser();
                mAuth.signOut();
                Intent intent = new Intent(Admin.this, Login.class);
                startActivity(intent);
                finish();
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

    private void tampilData() {
        database.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                arrayList = new ArrayList<>();
                for (DataSnapshot item : snapshot.getChildren()) {
                    User user = item.getValue(User.class);
                    if (user != null) {
                        arrayList.add(user);
                    }
                }

                adapter = new UserAdapter(arrayList, Admin.this);
                rvUser.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
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
}