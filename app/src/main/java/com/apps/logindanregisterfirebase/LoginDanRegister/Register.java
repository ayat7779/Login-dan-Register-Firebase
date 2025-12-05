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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.apps.logindanregisterfirebase.Entitas.User;
import com.apps.logindanregisterfirebase.MainActivity;
import com.apps.logindanregisterfirebase.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.regex.Pattern;

public class Register extends AppCompatActivity {
    private EditText etUsername, etEmail, etPassword, etConfirmPassword, etNoHp, etSecretCode;
    private Button btnRegister, btnCancel;
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
        btnCancel = findViewById(R.id.btnCancel);
        progressBar = findViewById(R.id.progressBar);
        etSecretCode = findViewById(R.id.etSecretCode);
        TextView tvLogin = findViewById(R.id.tvLogin);

        tvLogin.setOnClickListener(view -> {
            Intent login = new Intent(Register.this, Login.class);
            startActivity(login);
            finish();
        });

        btnRegister.setOnClickListener(view -> {
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();
            String noHp = etNoHp.getText().toString().trim();
            String secretCode = etSecretCode.getText().toString().trim();

            if (validateInputs(username, email, password, confirmPassword, noHp, secretCode)) {
                registerUser(username, email, password, noHp, secretCode);
            }
        });

        btnCancel.setOnClickListener(view -> {
            // Kembali ke Login
            Intent intent = new Intent(Register.this, Login.class);
            startActivity(intent);
            finish();
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

//    private void registerUser(final String username, final String email,
//                              final String password, final String noHp,
//                              final String secretCode) {
//
//        progressBar.setVisibility(View.VISIBLE);
//        btnRegister.setEnabled(false);
//        btnCancel.setEnabled(false);
//
//        // Cek dulu apakah email sudah terdaftar di database kita
//        checkEmailAvailability(username, email, password, noHp);
//
//        mAuth.createUserWithEmailAndPassword(email, password)
//                .addOnCompleteListener(this, task -> {
//                    if (task.isSuccessful()) {
//                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
//                        String userId = firebaseUser.getUid();
//
//                        // Tentukan role berdasarkan secret code
//                        String role = secretCode.equals("ADMIN2024") ? "admin" : "user";
//                        int status = role.equals("admin") ? 1 : 0; // Admin aktif langsung
//
//                        User user = new User(userId, username, email, noHp);
//                        user.setRole(role);
//                        user.setStatus(status);
//
//                        // Save to database
//                        databaseRef.child(userId).setValue(user)
//                                .addOnCompleteListener(dbTask -> {
//                                    if (dbTask.isSuccessful()) {
//                                        if (role.equals("admin")) {
//                                            Toast.makeText(Register.this,
//                                                    "Admin berhasil didaftarkan!",
//                                                    Toast.LENGTH_SHORT).show();
//                                        }
//                                        // Kirim email verifikasi
//                                        sendEmailVerification(firebaseUser, username, email);
//                                    }
//                                });
//                    } else {
//                        // Registration failed
//                        String errorMessage = task.getException().getMessage();
//                        Toast.makeText(Register.this,
//                                "Registrasi gagal: " + errorMessage,
//                                Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }

    private void registerUser(final String username, final String email,
                              final String password, final String noHp,
                              final String secretCode) {

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);
        btnCancel.setEnabled(false);

        // 1. Cek di database lokal
        checkEmailInDatabase(username, email, password, noHp);
    }

    private void checkEmailInDatabase(final String username, final String email,
                                      final String password, final String noHp) {

        // Cek di Realtime Database
        databaseRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Email sudah ada di database
                            progressBar.setVisibility(View.GONE);
                            btnRegister.setEnabled(true);
                            btnCancel.setEnabled(true);

                            handleExistingEmailInDatabase(email, snapshot);
                        } else {
                            // Email belum ada di database, cek di Auth
                            checkEmailInAuth(username, email, password, noHp);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        btnCancel.setEnabled(true);

                        Toast.makeText(Register.this,
                                "Error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkEmailInAuth(final String username, final String email,
                                  final String password, final String noHp) {

        // Coba fetch user dari Auth dengan email
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> signInMethods = task.getResult().getSignInMethods();

                        if (signInMethods != null && !signInMethods.isEmpty()) {
                            // Email sudah terdaftar di Auth
                            progressBar.setVisibility(View.GONE);
                            btnRegister.setEnabled(true);
                            btnCancel.setEnabled(true);

                            handleExistingEmailInAuth(email);
                        } else {
                            // Email belum terdaftar di Auth, lanjut registrasi
                            createUserInFirebaseAuth(username, email, password, noHp);
                        }
                    } else {
                        // Error fetching, lanjut saja (mungkin network error)
                        createUserInFirebaseAuth(username, email, password, noHp);
                    }
                });
    }

    private void handleExistingEmailInDatabase(String email, DataSnapshot snapshot) {
        // Tampilkan data user yang sudah ada
        StringBuilder existingUsers = new StringBuilder();
        existingUsers.append("Email ").append(email).append(" sudah terdaftar oleh:\n\n");

        for (DataSnapshot userSnap : snapshot.getChildren()) {
            String uid = userSnap.getKey();
            String username = userSnap.child("username").getValue(String.class);
            String role = userSnap.child("role").getValue(String.class);
            Integer status = userSnap.child("status").getValue(Integer.class);

            existingUsers.append("• ").append(username)
                    .append(" (").append(role).append(")")
                    .append(" - Status: ").append(getStatusText(status))
                    .append("\n");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Email Sudah Terdaftar")
                .setMessage(existingUsers.toString())
                .setPositiveButton("Login", (dialog, which) -> {
                    Intent intent = new Intent(Register.this, Login.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Gunakan Email Lain", (dialog, which) -> {
                    etEmail.setText("");
                    etEmail.requestFocus();
                })
                .setNeutralButton("Reset Password", (dialog, which) -> {
                    sendPasswordResetEmail(email);
                })
                .show();
    }

    private void handleExistingEmailInAuth(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Email Sudah Terdaftar di Sistem")
                .setMessage("Email " + email + " sudah terdaftar di Firebase Authentication.\n\n" +
                        "Kemungkinan:\n" +
                        "1. User sudah ada di Auth tapi tidak di database\n" +
                        "2. Terjadi duplicate entry\n\n" +
                        "Hubungi admin untuk membersihkan data.")
                .setPositiveButton("Reset Password", (dialog, which) -> {
                    sendPasswordResetEmail(email);
                })
                .setNegativeButton("Gunakan Email Lain", (dialog, which) -> {
                    etEmail.setText("");
                    etEmail.requestFocus();
                })
                .setNeutralButton("Login", (dialog, which) -> {
                    Intent intent = new Intent(Register.this, Login.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private String getStatusText(Integer status) {
        if (status == null) return "Unknown";
        switch (status) {
            case 0: return "Pending";
            case 1: return "Aktif";
            case 2: return "Nonaktif";
            default: return "Unknown";
        }
    }

    private void checkEmailAvailability(final String username, final String email,
                                        final String password, final String noHp) {

        // Cek di Realtime Database apakah email sudah digunakan
        databaseRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Email sudah ada di database kita
                            progressBar.setVisibility(View.GONE);
                            btnRegister.setEnabled(true);
                            btnCancel.setEnabled(true);

                            showEmailAlreadyRegisteredDialog(email);
                        } else {
                            // Email belum ada di database, lanjut ke Firebase Auth
                            createUserInFirebaseAuth(username, email, password, noHp);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        btnCancel.setEnabled(true);

                        Toast.makeText(Register.this,
                                "Error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createUserInFirebaseAuth(final String username, final String email,
                                          final String password, final String noHp) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registration success di Firebase Auth
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();

                            // Create User object
                            User user = new User(userId, username, email, noHp);
                            user.setRole("user");
                            user.setStatus(0); // pending activation

                            // Save user to Realtime Database
                            saveUserToDatabase(user, firebaseUser);
                        }
                    } else {
                        // Registration failed di Firebase Auth
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        btnCancel.setEnabled(true);

                        handleRegistrationError(task.getException(), email);
                    }
                });
    }

    private void saveUserToDatabase(User user, FirebaseUser firebaseUser) {
        databaseRef.child(user.getUid()).setValue(user)
                .addOnCompleteListener(dbTask -> {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    btnCancel.setEnabled(true);

                    if (dbTask.isSuccessful()) {
                        // Send email verification
                        sendEmailVerification(firebaseUser, user.getUsername(), user.getEmail());
                    } else {
                        // Jika gagal save ke database, hapus user dari Firebase Auth
                        firebaseUser.delete()
                                .addOnCompleteListener(deleteTask -> {
                                    if (!deleteTask.isSuccessful()) {
                                        mAuth.signOut();
                                    }
                                });

                        Toast.makeText(Register.this,
                                "Gagal menyimpan data user: " + dbTask.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleRegistrationError(Exception exception, String email) {
        String errorMessage = exception != null ? exception.getMessage() : "Unknown error";

        if (exception instanceof FirebaseAuthUserCollisionException) {
            // Email sudah digunakan di Firebase Auth
            showEmailInUseDialog(email, errorMessage);
        } else {
            // Error lainnya
            Toast.makeText(Register.this,
                    "Registrasi gagal: " + errorMessage,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showEmailAlreadyRegisteredDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Email Sudah Terdaftar")
                .setMessage("Email " + email + " sudah terdaftar di sistem kami.\n\n" +
                        "Silakan gunakan email lain atau login dengan email ini.")
                .setPositiveButton("Login", (dialog, which) -> {
                    // Redirect ke Login dengan email pre-filled
                    Intent intent = new Intent(Register.this, Login.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Gunakan Email Lain", (dialog, which) -> {
                    // Clear email field
                    etEmail.setText("");
                    etEmail.requestFocus();
                })
                .setNeutralButton("Batal", null)
                .setCancelable(true)
                .show();
    }

    private void showEmailInUseDialog(String email, String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Email Sudah Digunakan")
                .setMessage("Email " + email + " sudah digunakan di Firebase Authentication.\n\n" +
                        "Kemungkinan:\n" +
                        "1. User dihapus dari database tapi masih ada di Auth\n" +
                        "2. Email memang sudah terdaftar\n\n" +
                        "Error: " + errorMessage)
                .setPositiveButton("Reset Password", (dialog, which) -> {
                    // Kirim reset password email
                    sendPasswordResetEmail(email);
                })
                .setNegativeButton("Gunakan Email Lain", (dialog, which) -> {
                    // Clear email field
                    etEmail.setText("");
                    etEmail.requestFocus();
                })
                .setNeutralButton("Batal", null)
                .setCancelable(true)
                .show();
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(Register.this,
                                "Instruksi reset password telah dikirim ke " + email,
                                Toast.LENGTH_LONG).show();

                        // Redirect ke Login
                        Intent intent = new Intent(Register.this, Login.class);
                        intent.putExtra("email", email);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(Register.this,
                                "Gagal mengirim email reset: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Di Register.java - Update method sendEmailVerification()
    private void sendEmailVerification(FirebaseUser user, String username, String email) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Redirect ke Phone Verification setelah email verification
                        Intent intent = new Intent(Register.this, PhoneVerificationActivity.class);
                        intent.putExtra("userId", user.getUid());
                        intent.putExtra("email", email);
                        intent.putExtra("username", username);
                        intent.putExtra("required", true); // Phone verification required
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    // Juga perbaiki showVerificationDialog() dan showVerificationWarningDialog() untuk redirect ke EmailVerificationActivity

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

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        // Override back button untuk mencegah stuck
        super.onBackPressed();
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
        finish();
    }
}