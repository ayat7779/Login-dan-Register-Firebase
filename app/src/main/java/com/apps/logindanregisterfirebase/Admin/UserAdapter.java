package com.apps.logindanregisterfirebase.Admin;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.apps.logindanregisterfirebase.Entitas.User;
import com.apps.logindanregisterfirebase.R;
import com.apps.logindanregisterfirebase.Utils.SessionManager;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.MyViewHolder> {
    List<User> mlist;
    Context context;
    private DatabaseReference database = FirebaseDatabase.getInstance().getReference("users");
    private SessionManager sessionManager;
    private String currentAdminUid;

    public UserAdapter(List<User> mlist, Context context) {
        this.mlist = mlist;
        this.context = context;
        this.sessionManager = new SessionManager(context);
        this.currentAdminUid = sessionManager.getUserId(); // Get current logged in admin UID
    }

    @NonNull
    @Override
    public UserAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_user, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserAdapter.MyViewHolder holder, int position) {
        final User item = mlist.get(position);

        // Tampilkan data
        holder.tvUsername.setText("Username : " + item.getUsername());
        holder.tvEmail.setText("Email : " + item.getEmail());
        holder.tvNoHp.setText("No HP : " + item.getNoHp());
        holder.tvStatus.setText("Status : " + getStatusText(item.getStatus()));
        holder.tvRole.setText("Role : " + item.getRole());

        // Cek apakah ini admin yang sedang login
        boolean isCurrentAdmin = item.getUid() != null && item.getUid().equals(currentAdminUid);

        // Tombol Hapus
        holder.btnHapus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // CEK: Admin tidak boleh menghapus dirinya sendiri
                if (isCurrentAdmin) {
                    Toast.makeText(context,
                            "Tidak dapat menghapus akun sendiri!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // CEK: Admin tidak boleh menghapus admin lain
                if ("admin".equals(item.getRole())) {
                    Toast.makeText(context,
                            "Tidak dapat menghapus admin lain!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Konfirmasi sebelum hapus
                showDeleteConfirmationDialog(item);
            }
        });

        // Tombol Edit
        holder.btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Admin boleh edit semua user termasuk dirinya sendiri
                Intent edit = new Intent(context, EditDataUser.class);
                edit.putExtra("uid", item.getUid());
                edit.putExtra("username", item.getUsername());
                edit.putExtra("email", item.getEmail());
                edit.putExtra("noHp", item.getNoHp());
                context.startActivity(edit);
            }
        });

        // Tombol Aktifasi/Nonaktifasi
        if (holder.btnAktifasi != null) {
            // Update teks tombol berdasarkan status
            String buttonText = (item.getStatus() == 1) ? "Nonaktifkan" : "Aktifkan";
            holder.btnAktifasi.setText(buttonText);

            holder.btnAktifasi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // CEK: Admin tidak boleh nonaktifkan dirinya sendiri
                    if (isCurrentAdmin && item.getStatus() == 1) {
                        Toast.makeText(context,
                                "Tidak dapat menonaktifkan akun sendiri!",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    toggleUserStatus(item);
                }
            });

            // Sembunyikan tombol aktifasi untuk admin yang sedang login
            if (isCurrentAdmin) {
                holder.btnAktifasi.setVisibility(View.GONE);
            } else {
                holder.btnAktifasi.setVisibility(View.VISIBLE);
            }
        }

        // Tombol Promosi ke Admin (jika ada)
        if (holder.btnPromote != null) {
            // Hanya tampilkan untuk user biasa, bukan admin
            if ("user".equals(item.getRole())) {
                holder.btnPromote.setVisibility(View.VISIBLE);
                holder.btnPromote.setOnClickListener(v -> {
                    promoteToAdmin(item);
                });
            } else {
                holder.btnPromote.setVisibility(View.GONE);
            }
        }

        // Update UI berdasarkan apakah ini admin yang sedang login
        updateUIForCurrentAdmin(holder, isCurrentAdmin, item);
    }

    private void updateUIForCurrentAdmin(MyViewHolder holder, boolean isCurrentAdmin, User user) {
        // Beri tanda khusus untuk admin yang sedang login
        if (isCurrentAdmin) {
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.light_blue));
            holder.tvRole.setText("Role : " + user.getRole() + " (Saya)");
        } else {
            holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
        }

        // Nonaktifkan tombol hapus untuk admin yang sedang login
        if (isCurrentAdmin) {
            holder.btnHapus.setEnabled(false);
            holder.btnHapus.setAlpha(0.5f);
            holder.btnHapus.setText("Hapus (Diri Sendiri)");
        } else {
            holder.btnHapus.setEnabled(true);
            holder.btnHapus.setAlpha(1f);
            holder.btnHapus.setText("Hapus");
        }

        // Nonaktifkan tombol hapus untuk admin lain
        if ("admin".equals(user.getRole()) && !isCurrentAdmin) {
            holder.btnHapus.setEnabled(false);
            holder.btnHapus.setAlpha(0.5f);
            holder.btnHapus.setText("Hapus (Admin)");
        }
    }

    private void showDeleteConfirmationDialog(User user) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Konfirmasi Hapus")
                .setMessage("Apakah Anda yakin ingin menghapus user:\n" +
                        user.getUsername() + "\n" +
                        user.getEmail() + "?")
                .setPositiveButton("Ya, Hapus", (dialog, which) -> {
                    // Hapus dari Firebase menggunakan UID
                    database.child(user.getUid()).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context,
                                        "User berhasil dihapus",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context,
                                        "Gagal menghapus: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private String getStatusText(int status) {
        switch (status) {
            case 0: return "⏳ Pending";
            case 1: return "✅ Aktif";
            case 2: return "❌ Nonaktif";
            default: return "Unknown";
        }
    }

    private void toggleUserStatus(User user) {
        int newStatus = (user.getStatus() == 1) ? 2 : 1; // Toggle antara 1 dan 2
        String statusText = (newStatus == 1) ? "diaktifkan" : "dinonaktifkan";

        database.child(user.getUid()).child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context,
                            "User " + user.getUsername() + " berhasil " + statusText,
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context,
                            "Gagal update status: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void promoteToAdmin(User user) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Promosi ke Admin")
                .setMessage("Promosikan " + user.getUsername() + " menjadi admin?\n\n" +
                        "User akan memiliki hak akses penuh seperti Anda.")
                .setPositiveButton("Ya, Promosikan", (dialog, which) -> {
                    database.child(user.getUid()).child("role").setValue("admin")
                            .addOnSuccessListener(aVoid -> {
                                database.child(user.getUid()).child("status").setValue(1)
                                        .addOnSuccessListener(aVoid2 -> {
                                            Toast.makeText(context,
                                                    user.getUsername() + " sekarang adalah admin",
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context,
                                        "Gagal: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return mlist.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        private TextView tvUsername, tvEmail, tvNoHp, tvStatus, tvRole;
        private Button btnHapus, btnEdit, btnAktifasi, btnPromote;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvNoHp = itemView.findViewById(R.id.tvNoHp);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvRole = itemView.findViewById(R.id.tvRole);
            btnHapus = itemView.findViewById(R.id.btnHapus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnAktifasi = itemView.findViewById(R.id.btnAktifasi);
            btnPromote = itemView.findViewById(R.id.btnPromote);
        }
    }

    private void deleteUser(User user) {
        // Hapus dari Firebase Auth terlebih dahulu
        // Butuh Cloud Function atau admin SDK untuk ini
        // Untuk sekarang, hanya hapus dari database dan beri warning

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Hapus User")
                .setMessage("Hapus user " + user.getUsername() + "?\n\n" +
                        "⚠️ PERHATIAN:\n" +
                        "User akan dihapus dari database, tapi EMAIL MASIH TERDAFTAR di Firebase Auth.\n" +
                        "User tidak bisa daftar ulang dengan email yang sama sampai dihapus manual dari Firebase Console.")
                .setPositiveButton("Ya, Hapus", (dialog, which) -> {
                    // Hapus dari database
                    database.child(user.getUid()).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context,
                                        "User dihapus dari database.\n" +
                                                "Email masih terdaftar di Firebase Auth.",
                                        Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context,
                                        "Gagal menghapus: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
