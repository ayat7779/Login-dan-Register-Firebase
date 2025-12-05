package com.apps.logindanregisterfirebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.apps.logindanregisterfirebase.LoginDanRegister.PhoneVerificationActivity;
import com.apps.logindanregisterfirebase.Utils.SessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {
    private TextView tvUsername, tvEmail, tvPhone, tvPhoneStatus;
    private Button btnVerifyPhone, btnChangePhone;
    private SessionManager sessionManager;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        sessionManager = new SessionManager(this);
        String userId = sessionManager.getUserId();

        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        initializeViews();
        loadUserData();
        setupListeners();
    }

    private void initializeViews() {
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvPhoneStatus = findViewById(R.id.tvPhoneStatus);
        btnVerifyPhone = findViewById(R.id.btnVerifyPhone);
        btnChangePhone = findViewById(R.id.btnChangePhone);
    }

    private void loadUserData() {
        tvUsername.setText(sessionManager.getUserName());
        tvEmail.setText(sessionManager.getUserEmail());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String phone = snapshot.child("phoneNumber").getValue(String.class);
                    Boolean verified = snapshot.child("phoneVerified").getValue(Boolean.class);

                    if (phone != null && !phone.isEmpty()) {
                        tvPhone.setText(phone);

                        if (verified != null && verified) {
                            tvPhoneStatus.setText("✅ Terverifikasi");
                            tvPhoneStatus.setTextColor(getColor(R.color.success));
                            btnVerifyPhone.setVisibility(View.GONE);
                            btnChangePhone.setText("Ubah Nomor");
                        } else {
                            tvPhoneStatus.setText("❌ Belum diverifikasi");
                            tvPhoneStatus.setTextColor(getColor(R.color.error));
                            btnVerifyPhone.setVisibility(View.VISIBLE);
                            btnChangePhone.setText("Ganti Nomor");
                        }
                    } else {
                        tvPhone.setText("Belum ditambahkan");
                        tvPhoneStatus.setText("📱 Tambah nomor telepon");
                        btnVerifyPhone.setText("Verifikasi Sekarang");
                        btnChangePhone.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ProfileActivity.this,
                        "Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        btnVerifyPhone.setOnClickListener(v -> {
            verifyPhoneNumber();
        });

        btnChangePhone.setOnClickListener(v -> {
            changePhoneNumber();
        });
    }

    private void verifyPhoneNumber() {
        Intent intent = new Intent(this, PhoneVerificationActivity.class);
        intent.putExtra("userId", sessionManager.getUserId());
        intent.putExtra("email", sessionManager.getUserEmail());
        intent.putExtra("username", sessionManager.getUserName());
        intent.putExtra("required", false); // Optional
        startActivity(intent);
    }

    private void changePhoneNumber() {
        // Clear existing phone verification
        userRef.child("phoneNumber").setValue(null);
        userRef.child("phoneVerified").setValue(false);
        userRef.child("phoneVerifiedAt").setValue(0)
                .addOnSuccessListener(aVoid -> {
                    verifyPhoneNumber(); // Mulai verifikasi baru
                });
    }
}