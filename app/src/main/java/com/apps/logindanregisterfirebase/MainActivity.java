package com.apps.logindanregisterfirebase;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.apps.logindanregisterfirebase.LoginDanRegister.Login;
import com.apps.logindanregisterfirebase.Utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private TextView tvWelcome;
    private Button btnProfile, btnSettings, btnLogout;
    private SessionManager sessionManager;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Session Manager
        sessionManager = new SessionManager(this);

        // Check login
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        // Initialize views
        tvWelcome = findViewById(R.id.tvWelcome);
        btnProfile = findViewById(R.id.btnProfile);
        btnSettings = findViewById(R.id.btnSettings);
        btnLogout = findViewById(R.id.btnLogout);

        // Display user info
        displayUserInfo();

        // Setup button listeners
        setupButtonListeners();
    }

    private void displayUserInfo() {
        String username = sessionManager.getUserName();
        String email = sessionManager.getUserEmail();
        String role = sessionManager.getUserRole();

        String welcomeText = "Selamat datang, " + username + "!\n"
                + "Email: " + email + "\n"
                + "Role: " + role;

        tvWelcome.setText(welcomeText);
    }

    private void setupButtonListeners() {
        btnProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Buka halaman profile", Toast.LENGTH_SHORT).show();
            // TODO: Implement buka ProfileActivity
        });

        btnSettings.setOnClickListener(v -> {
            Toast.makeText(this, "Buka halaman pengaturan", Toast.LENGTH_SHORT).show();
            // TODO: Implement buka SettingsActivity
        });

        btnLogout.setOnClickListener(v -> {
            logout();
        });
    }

    private void logout() {
        // Clear session menggunakan method yang benar
        sessionManager.logoutUser(); // Bisa juga pakai sessionManager.logout()

        // Sign out from Firebase
        if (mAuth.getCurrentUser() != null) {
            mAuth.signOut();
        }

        // Redirect to login
        redirectToLogin();

        Toast.makeText(this, "Berhasil logout", Toast.LENGTH_SHORT).show();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            logout();
            return true;
        } else if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        // Update btnProfile di MainActivity
        btnProfile.setOnClickListener(view -> {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                Toast.makeText(this, "Buka halaman profile", Toast.LENGTH_SHORT).show();
            });

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Double check login status
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
        }
    }
}