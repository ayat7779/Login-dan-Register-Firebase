package com.apps.logindanregisterfirebase.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.apps.logindanregisterfirebase.R;
import com.apps.logindanregisterfirebase.Utils.SessionManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class EditDataUser extends AppCompatActivity {
    private EditText etUsername, etEmail, etPassword, etNoHp;
    private Button btnSave, btnBatal;
    private String uid, username, email, noHp, password;
    private DatabaseReference database = FirebaseDatabase.getInstance().getReference("users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_data_user);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etNoHp = findViewById(R.id.etNoHp);
        btnSave = findViewById(R.id.btnSave);
        btnBatal = findViewById(R.id.btnBatal);

        // Ambil data dari intent
        if (getIntent().hasExtra("uid")) {
            uid = getIntent().getStringExtra("uid");
            username = getIntent().getStringExtra("username");
            email = getIntent().getStringExtra("email");
            noHp = getIntent().getStringExtra("noHp");
            password = getIntent().getStringExtra("password");
        } else {
            // Jika tidak ada uid, kembali
            Toast.makeText(this, "Data user tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set nilai ke EditText
        etUsername.setText(username);
        etEmail.setText(email);
        etNoHp.setText(noHp);
        etPassword.setText(password);

        // Nonaktifkan edit email (karena email adalah primary key di Firebase Auth)
        etEmail.setEnabled(false);

        // Jika ingin mengedit password, perlu implementasi Firebase Auth
        // Untuk sekarang, sembunyikan atau nonaktifkan
        etPassword.setEnabled(false);

        btnBatal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), Admin.class));
                finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String usernameBaru = etUsername.getText().toString().trim();
                String noHpBaru = etNoHp.getText().toString().trim();

                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("username", usernameBaru); // jika boleh edit username
                hashMap.put("noHp", noHpBaru);

                // Validasi input
                if (usernameBaru.isEmpty()) {
                    etUsername.setError("Username tidak boleh kosong");
                    etUsername.requestFocus();
                    return;
                }

                if (noHpBaru.isEmpty()) {
                    etNoHp.setError("Nomor HP tidak boleh kosong");
                    etNoHp.requestFocus();
                    return;
                }

                // Update data ke Firebase
                updateUserData(usernameBaru, noHpBaru);
            }
        });
    }

    // Di EditDataUser.java - Tambah validasi
    private void updateUserData(String usernameBaru, String noHpBaru) {
        // Cek apakah ini admin yang sedang login
        SessionManager sessionManager = new SessionManager(this);
        String currentUserId = sessionManager.getUserId();

        // Jika admin mengedit akun sendiri, beberapa field tidak boleh diubah
        if (uid != null && uid.equals(currentUserId)) {
            // Admin tidak bisa mengubah role sendiri
            Toast.makeText(this,
                    "Beberapa pengubahan dibatasi untuk akun sendiri",
                    Toast.LENGTH_SHORT).show();
        }

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("username", usernameBaru);
        hashMap.put("noHp", noHpBaru);

        database.child(uid).updateChildren(hashMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getApplicationContext(),
                            "Data user berhasil diupdate!",
                            Toast.LENGTH_SHORT).show();

                    // Update session jika ini admin yang sedang login
                    if (uid.equals(currentUserId)) {
                        sessionManager.updateUserProfile(usernameBaru, noHpBaru);
                    }

                    Intent intent = new Intent(getApplicationContext(), Admin.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getApplicationContext(), Admin.class));
        finish();
    }
}