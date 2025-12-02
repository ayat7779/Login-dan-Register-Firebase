package com.apps.logindanregisterfirebase.LoginDanRegister;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.apps.logindanregisterfirebase.Admin.Admin;
import com.apps.logindanregisterfirebase.MainActivity;
import com.apps.logindanregisterfirebase.R;
import com.apps.logindanregisterfirebase.Utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class Login extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private CheckBox cbRememberMe;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private SessionManager sessionManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("users");

        // Initialize Session Manager
        sessionManager = new SessionManager(this);

        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            redirectBasedOnRole();
            return;
        }

        // Initialize views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        progressBar = findViewById(R.id.progressBar);

        // Load remembered email if exists
        if (sessionManager.isRememberMe()) {
            String rememberedEmail = sessionManager.getUserEmail();
            if (rememberedEmail != null) {
                etEmail.setText(rememberedEmail);
                cbRememberMe.setChecked(true);
            }
        }

        // Login button click
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString();

                if (validateInputs(email, password)) {
                    loginUser(email, password);
                }
            }
        });

        // Register text click
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent register = new Intent(Login.this, Register.class);
                startActivity(register);
            }
        });

        // Forgot password text click
        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showForgotPasswordDialog();
            }
        });
    }

    private boolean validateInputs(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email harus diisi");
            etEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password harus diisi");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void loginUser(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        // Login success
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            checkEmailVerification(firebaseUser);
                        }
                    } else {
                        // Login failed
                        String errorMessage = task.getException().getMessage();

                        // User-friendly error messages
                        if (errorMessage.contains("invalid credential") ||
                                errorMessage.contains("password is invalid")) {
                            Toast.makeText(Login.this,
                                    "Email atau password salah",
                                    Toast.LENGTH_SHORT).show();
                        } else if (errorMessage.contains("no user record")) {
                            Toast.makeText(Login.this,
                                    "Email belum terdaftar",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(Login.this,
                                    "Login gagal: " + errorMessage,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void checkEmailVerification(FirebaseUser user) {
        if (user.isEmailVerified()) {
            // Email verified, check user role in database
            checkUserRole(user.getUid(), user.getEmail());
        } else {
            // Email not verified
            Toast.makeText(Login.this,
                    "Email belum diverifikasi. Silakan cek email Anda.",
                    Toast.LENGTH_LONG).show();

            // Optionally, you can offer to resend verification email
            mAuth.signOut();
        }
    }

    private void checkUserRole(String userId, String email) {
        databaseRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get user data
                    String username = snapshot.child("username").getValue(String.class);
                    String noHp = snapshot.child("noHp").getValue(String.class);
                    String role = snapshot.child("role").getValue(String.class);
                    Integer status = snapshot.child("status").getValue(Integer.class);

                    // Check account status
                    if (status == null || status == 0) {
                        // Account pending activation
                        Toast.makeText(Login.this,
                                "Akun Anda belum diaktifkan oleh admin. Silakan tunggu.",
                                Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        return;
                    } else if (status == 2) {
                        // Account banned
                        Toast.makeText(Login.this,
                                "Akun Anda telah dinonaktifkan.",
                                Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        return;
                    }

                    // Save to session
                    boolean rememberMe = cbRememberMe.isChecked();
                    sessionManager.createLoginSession(
                            userId, email, username, role, noHp, rememberMe
                    );

                    // Redirect based on role
                    redirectBasedOnRole();

                } else {
                    // User data not found in database (should not happen)
                    Toast.makeText(Login.this,
                            "Data user tidak ditemukan",
                            Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Login.this,
                        "Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                mAuth.signOut();
            }
        });
    }

    private void redirectBasedOnRole() {
        String role = sessionManager.getUserRole();

        Intent intent;
        if ("admin".equals(role)) {
            intent = new Intent(Login.this, Admin.class);
            Toast.makeText(Login.this, "Login Admin Berhasil!", Toast.LENGTH_SHORT).show();
        } else {
            intent = new Intent(Login.this, MainActivity.class);
            Toast.makeText(Login.this, "Login Berhasil!", Toast.LENGTH_SHORT).show();
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showForgotPasswordDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Lupa Password")
                .setMessage("Masukkan email Anda untuk reset password:")

                .setView(getLayoutInflater().inflate(R.layout.dialog_forgot_password, null))

                .setPositiveButton("Kirim", (dialog, which) -> {
                    // Get email from dialog
                    android.app.AlertDialog alertDialog = (android.app.AlertDialog) dialog;
                    EditText etDialogEmail = alertDialog.findViewById(R.id.etDialogEmail);

                    if (etDialogEmail != null) {
                        String email = etDialogEmail.getText().toString().trim();

                        if (!TextUtils.isEmpty(email)) {
                            sendPasswordResetEmail(email);
                        } else {
                            Toast.makeText(Login.this,
                                    "Email harus diisi",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })

                .setNegativeButton("Batal", null)
                .show();
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(Login.this,
                                "Instruksi reset password telah dikirim ke email",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(Login.this,
                                "Gagal mengirim email reset: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Auto-login if session exists
        if (sessionManager.isLoggedIn()) {
            redirectBasedOnRole();
        }
    }
}