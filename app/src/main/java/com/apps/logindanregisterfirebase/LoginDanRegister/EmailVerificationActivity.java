package com.apps.logindanregisterfirebase.LoginDanRegister;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.apps.logindanregisterfirebase.MainActivity;
import com.apps.logindanregisterfirebase.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

@SuppressWarnings("ALL")
public class EmailVerificationActivity extends AppCompatActivity {
    private TextView tvCountdown;
    private Button btnResend, btnCheck, btnLogin;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private CountDownTimer resendTimer;
    private static final long RESEND_COOLDOWN = 60000; // 60 detik
    private boolean canResend = false;
    private Handler verificationHandler;
    private Runnable verificationRunnable;
    private static final long CHECK_INTERVAL = 5000; // 5 detik

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_email_verification);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Check if user is logged in
        if (currentUser == null) {
            // No user logged in, go to login
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        // Initialize views
        TextView tvEmail = findViewById(R.id.tvEmail);
        tvCountdown = findViewById(R.id.tvCountdown);
        TextView tvInstructions = findViewById(R.id.tvInstructions);
        btnResend = findViewById(R.id.btnResend);
        btnCheck = findViewById(R.id.btnCheck);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        // Display user email
        String email = currentUser.getEmail();
        tvEmail.setText(email);

        // Setup instructions
        String instructions = "1. Buka email di " + email + "\n" +
                              "2. Cari email dari Firebase\n" +
                              "3. Klik link verifikasi\n" +
                              "4. Kembali ke aplikasi dan tekan 'Cek Status'";
        tvInstructions.setText(instructions);

        // Setup button listeners
        setupButtonListeners();

        // Start countdown timer for resend
        startResendCountdown();

        // Check initial verification status
        checkVerificationStatus(false);

        setupAutoVerificationCheck();
    }

    private void setupButtonListeners() {
        btnResend.setOnClickListener(v -> {
            if (canResend) {
                resendVerificationEmail();
            } else {
                Toast.makeText(this,
                        "Tunggu " + getRemainingTime() + " detik sebelum kirim ulang",
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnCheck.setOnClickListener(v -> checkVerificationStatus(true));

        btnLogin.setOnClickListener(v -> {
            // Go to login page
            startActivity(new Intent(this, Login.class));
            finish();
        });
    }

    private void resendVerificationEmail() {
        progressBar.setVisibility(View.VISIBLE);
        btnResend.setEnabled(false);

        currentUser.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Email verifikasi telah dikirim ulang",
                                Toast.LENGTH_LONG).show();

                        // Start countdown again
                        startResendCountdown();
                    } else {
                        Toast.makeText(this,
                                "Gagal mengirim email: " + Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_SHORT).show();
                        btnResend.setEnabled(true);
                    }
                });
    }

    private void checkVerificationStatus(boolean showToast) {
        progressBar.setVisibility(View.VISIBLE);
        btnCheck.setEnabled(false);

        // Reload user data to get latest verification status
        currentUser.reload()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnCheck.setEnabled(true);

                    if (task.isSuccessful()) {
                        currentUser = mAuth.getCurrentUser();

                        if (currentUser != null && currentUser.isEmailVerified()) {
                            // Email verified!
                            if (showToast) {
                                Toast.makeText(this,
                                        "Email berhasil diverifikasi!",
                                        Toast.LENGTH_SHORT).show();
                            }

                            // Redirect to MainActivity
                            redirectToMainActivity();
                        } else {
                            if (showToast) {
                                Toast.makeText(this,
                                        "Email belum diverifikasi",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Toast.makeText(this,
                                "Gagal memuat status: " + Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupAutoVerificationCheck() {
        verificationHandler = new Handler();
        verificationRunnable = new Runnable() {
            @Override
            public void run() {
                checkVerificationStatus(false);
                verificationHandler.postDelayed(this, CHECK_INTERVAL);
            }
        };
        verificationHandler.postDelayed(verificationRunnable, CHECK_INTERVAL);
    }

    private void redirectToMainActivity() {
        // User sudah verified, langsung redirect ke MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void startResendCountdown() {
        canResend = false;
        btnResend.setEnabled(false);

        if (resendTimer != null) {
            resendTimer.cancel();
        }

        resendTimer = new CountDownTimer(RESEND_COOLDOWN, 1000) {
            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                tvCountdown.setText("Kirim ulang dalam: " + secondsRemaining + " detik");
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFinish() {
                canResend = true;
                btnResend.setEnabled(true);
                tvCountdown.setText("Kirim ulang email verifikasi");
            }
        }.start();
    }

    private String getRemainingTime() {
        // Helper method to get remaining time
        if (resendTimer != null) {
            // This is simplified - in real app you'd track remaining time
            return "30";
        }
        return "0";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (verificationHandler != null && verificationRunnable != null) {
            verificationHandler.removeCallbacks(verificationRunnable);
        }
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }


    @SuppressLint({"MissingSuperCall", "GestureBackNavigation"})
    @Override
    public void onBackPressed() {
        // Prevent going back to register
        startActivity(new Intent(this, Login.class));
        finish();
    }
}