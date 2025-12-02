package com.apps.logindanregisterfirebase.LoginDanRegister;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.apps.logindanregisterfirebase.Entitas.User;
import com.apps.logindanregisterfirebase.MainActivity;
import com.apps.logindanregisterfirebase.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.regex.Pattern;

public class Register extends AppCompatActivity {
    private EditText etUsername, etEmail, etPassword, etConfirmPassword, etNoHp, etSecretCode;
    private Button btnRegister;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    // Password pattern: minimal 8 karakter, 1 huruf besar, 1 angka
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$");

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("users");

        // Initialize views
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etNoHp = findViewById(R.id.etNoHp);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        etSecretCode = findViewById(R.id.etSecretCode);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = etUsername.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString();
                String confirmPassword = etConfirmPassword.getText().toString();
                String noHp = etNoHp.getText().toString().trim();
                String secretCode = etSecretCode.getText().toString().trim();

                if (validateInputs(username, email, password, confirmPassword, noHp, secretCode)) {
                    registerUser(username, email, password, noHp, secretCode);
                }
            }
        });
    }

    private boolean validateInputs(String username, String email, String password,
                                   String confirmPassword, String noHp, String secretCode) {

        // Check if any field is empty
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username harus diisi");
            etUsername.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email harus diisi");
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Format email tidak valid");
            etEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password harus diisi");
            etPassword.requestFocus();
            return false;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            etPassword.setError("Password minimal 8 karakter, mengandung huruf besar, kecil, dan angka");
            etPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Password tidak cocok");
            etConfirmPassword.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(noHp)) {
            etNoHp.setError("Nomor HP harus diisi");
            etNoHp.requestFocus();
            return false;
        }

        // Validate phone number (minimal 10 digit, maksimal 13 digit)
        String phoneRegex = "^[0-9]{10,13}$";
        if (!noHp.matches(phoneRegex)) {
            etNoHp.setError("Nomor HP harus 10-13 digit angka");
            etNoHp.requestFocus();
            return false;
        }

        // Validasi secret code untuk admin
        if (!secretCode.isEmpty() && !secretCode.equals("ADMIN2024")) {
           etSecretCode.setError("Kode rahasia salah");
           etSecretCode.requestFocus();
           return false;
        }

        return true;
    }

    private void registerUser(final String username, final String email,
                              final String password, final String noHp,
                              final String secretCode) {

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        String userId = firebaseUser.getUid();

                        // Tentukan role berdasarkan secret code
                        String role = secretCode.equals("ADMIN2024") ? "admin" : "user";
                        int status = role.equals("admin") ? 1 : 0; // Admin aktif langsung

                        User user = new User(userId, username, email, noHp);
                        user.setRole(role);
                        user.setStatus(status);

                        // Save to database
                        databaseRef.child(userId).setValue(user)
                                .addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        if (role.equals("admin")) {
                                            Toast.makeText(Register.this,
                                                    "Admin berhasil didaftarkan!",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                        // Kirim email verifikasi
                                        sendEmailVerification(firebaseUser, username, email);
                                    }
                                });
                    } else {
                        // Registration failed
                        String errorMessage = task.getException().getMessage();
                        Toast.makeText(Register.this,
                                "Registrasi gagal: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendEmailVerification(FirebaseUser user, String username, String email) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showVerificationDialog(username, email);
                    } else {
                        Toast.makeText(Register.this,
                                "Gagal mengirim email verifikasi: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();

                        // Still show success but warn about verification
                        showVerificationWarningDialog(username, email);
                    }
                });
    }

    private void showVerificationDialog(String username, String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Registrasi Berhasil!")
                .setMessage("Halo " + username + "!\n\n" +
                        "Akun Anda telah berhasil dibuat.\n" +
                        "Kami telah mengirim email verifikasi ke:\n" +
                        email + "\n\n" +
                        "Silakan verifikasi email Anda sebelum login.")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Redirect to Login
                    Intent intent = new Intent(Register.this, Login.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void showVerificationWarningDialog(String username, String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Registrasi Berhasil!")
                .setMessage("Halo " + username + "!\n\n" +
                        "Akun Anda telah berhasil dibuat.\n" +
                        "Namun, gagal mengirim email verifikasi ke:\n" +
                        email + "\n\n" +
                        "Silakan coba verifikasi email nanti di pengaturan akun.")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(Register.this, Login.class);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            // User is already logged in and verified, redirect to MainActivity
            Intent intent = new Intent(Register.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}