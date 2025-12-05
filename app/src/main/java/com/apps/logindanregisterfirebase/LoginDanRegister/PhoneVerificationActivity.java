package com.apps.logindanregisterfirebase.LoginDanRegister;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.apps.logindanregisterfirebase.MainActivity;
import com.apps.logindanregisterfirebase.R;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hbb20.CountryCodePicker;

import java.util.concurrent.TimeUnit;

public class PhoneVerificationActivity extends AppCompatActivity {
    // Views
    private CountryCodePicker ccp;
    private EditText etPhoneNumber, etVerificationCode;
    private Button btnSendCode, btnVerify, btnSkip;
    private TextView tvCountdown, tvResend;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    // Phone Auth
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private CountDownTimer countDownTimer;

    // User data dari intent
    private String userId, userEmail, userName;
    private boolean isVerificationRequired = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_phone_verification);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get user data dari intent
        Intent intent = getIntent();
        if (intent != null) {
            userId = intent.getStringExtra("userId");
            userEmail = intent.getStringExtra("email");
            userName = intent.getStringExtra("username");
            isVerificationRequired = intent.getBooleanExtra("required", true);
        }

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("users");

        // Initialize views
        initializeViews();
        setupListeners();

        // Jika verifikasi tidak required, tampilkan skip button
        if (!isVerificationRequired) {
            btnSkip.setVisibility(View.VISIBLE);
        }
    }

    private void initializeViews() {
        ccp = findViewById(R.id.ccp);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etVerificationCode = findViewById(R.id.etVerificationCode);
        btnSendCode = findViewById(R.id.btnSendCode);
        btnVerify = findViewById(R.id.btnVerify);
        btnSkip = findViewById(R.id.btnSkip);
        tvCountdown = findViewById(R.id.tvCountdown);
        tvResend = findViewById(R.id.tvResend);
        progressBar = findViewById(R.id.progressBar);

        // Setup Country Code Picker
        ccp.registerCarrierNumberEditText(etPhoneNumber);
        ccp.setDefaultCountryUsingNameCode("ID"); // Indonesia default

        // Set default phone format
        etPhoneNumber.setHint("8123456789");

        // Initially disable verify button
        btnVerify.setEnabled(false);
        tvResend.setVisibility(View.GONE);
    }

    private void setupListeners() {
        // Auto-format phone number
        etPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Enable send button jika phone number valid
                btnSendCode.setEnabled(isValidPhoneNumber());
            }
        });

        // Auto-verify jika code sudah 6 digit
        etVerificationCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 6) {
                    // Auto verify
                    verifyPhoneNumberWithCode(s.toString());
                }
                btnVerify.setEnabled(s.length() >= 6);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSendCode.setOnClickListener(v -> {
            if (isValidPhoneNumber()) {
                sendVerificationCode();
            }
        });

        btnVerify.setOnClickListener(v -> {
            String code = etVerificationCode.getText().toString().trim();
            if (code.length() >= 6) {
                verifyPhoneNumberWithCode(code);
            }
        });

        btnSkip.setOnClickListener(v -> {
            skipPhoneVerification();
        });

        tvResend.setOnClickListener(v -> {
            resendVerificationCode();
        });
    }

    private boolean isValidPhoneNumber() {
        String fullNumber = ccp.getFullNumberWithPlus();
        return fullNumber.length() >= 10 && ccp.isValidFullNumber();
    }

    private void sendVerificationCode() {
        String phoneNumber = ccp.getFullNumberWithPlus();

        progressBar.setVisibility(View.VISIBLE);
        btnSendCode.setEnabled(false);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        // Auto verification (jika device sama)
                        progressBar.setVisibility(View.GONE);
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        progressBar.setVisibility(View.GONE);
                        btnSendCode.setEnabled(true);

                        if (e.getMessage().contains("invalid phone number")) {
                            Toast.makeText(PhoneVerificationActivity.this,
                                    "Nomor telepon tidak valid",
                                    Toast.LENGTH_SHORT).show();
                        } else if (e.getMessage().contains("quota exceeded")) {
                            Toast.makeText(PhoneVerificationActivity.this,
                                    "Kuota SMS habis, coba lagi nanti",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(PhoneVerificationActivity.this,
                                    "Verifikasi gagal: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        progressBar.setVisibility(View.GONE);

                        PhoneVerificationActivity.this.verificationId = verificationId;
                        resendToken = token;

                        // Update UI
                        btnSendCode.setVisibility(View.GONE);
                        etVerificationCode.setVisibility(View.VISIBLE);
                        btnVerify.setVisibility(View.VISIBLE);
                        tvCountdown.setVisibility(View.VISIBLE);
                        tvResend.setVisibility(View.GONE);

                        // Start countdown
                        startCountdownTimer();

                        Toast.makeText(PhoneVerificationActivity.this,
                                "Kode verifikasi telah dikirim via SMS",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void resendVerificationCode() {
        if (resendToken == null) {
            Toast.makeText(this, "Tidak dapat mengirim ulang kode", Toast.LENGTH_SHORT).show();
            return;
        }

        String phoneNumber = ccp.getFullNumberWithPlus();

        progressBar.setVisibility(View.VISIBLE);
        tvResend.setEnabled(false);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setForceResendingToken(resendToken)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        progressBar.setVisibility(View.GONE);
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        progressBar.setVisibility(View.GONE);
                        tvResend.setEnabled(true);
                        Toast.makeText(PhoneVerificationActivity.this,
                                "Gagal: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        progressBar.setVisibility(View.GONE);

                        PhoneVerificationActivity.this.verificationId = verificationId;
                        resendToken = token;

                        // Reset countdown
                        if (countDownTimer != null) {
                            countDownTimer.cancel();
                        }
                        startCountdownTimer();

                        Toast.makeText(PhoneVerificationActivity.this,
                                "Kode baru telah dikirim",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyPhoneNumberWithCode(String code) {
        if (verificationId == null) {
            Toast.makeText(this, "Silakan minta kode verifikasi dulu", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnVerify.setEnabled(false);

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        // Phone verification successful
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null) {
                            updateUserPhoneVerified();
                        }
                    } else {
                        btnVerify.setEnabled(true);

                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(this,
                                    "Kode verifikasi salah",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this,
                                    "Verifikasi gagal: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateUserPhoneVerified() {
        String phoneNumber = ccp.getFullNumberWithPlus();

        if (userId != null) {
            // Update phone number di database
            databaseRef.child(userId).child("phoneNumber").setValue(phoneNumber);
            databaseRef.child(userId).child("phoneVerified").setValue(true);
            databaseRef.child(userId).child("phoneVerifiedAt").setValue(System.currentTimeMillis());
        }

        // Success
        Toast.makeText(this, "Nomor telepon berhasil diverifikasi!", Toast.LENGTH_SHORT).show();

        // Redirect ke MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void skipPhoneVerification() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Lewati Verifikasi Telepon")
                .setMessage("Anda yakin ingin melewati verifikasi telepon?\n\n" +
                        "Anda dapat menambahkannya nanti di pengaturan profil.")
                .setPositiveButton("Ya, Lewati", (dialog, which) -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Verifikasi Sekarang", null)
                .show();
    }

    private void startCountdownTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                tvCountdown.setText("Kode kadaluarsa dalam: " + seconds + " detik");
            }

            @Override
            public void onFinish() {
                tvCountdown.setVisibility(View.GONE);
                tvResend.setVisibility(View.VISIBLE);
                tvResend.setEnabled(true);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}