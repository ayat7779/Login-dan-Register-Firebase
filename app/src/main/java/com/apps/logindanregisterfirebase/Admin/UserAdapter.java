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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.MyViewHolder> {

    List<User> mlist;
    Context context;
    private DatabaseReference database = FirebaseDatabase.getInstance().getReference("users");

    public UserAdapter(List<User> mlist, Context context) {
        this.mlist = mlist;
        this.context = context;
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

        // Tombol Hapus
        holder.btnHapus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Konfirmasi sebelum hapus
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
                builder.setTitle("Konfirmasi Hapus")
                        .setMessage("Apakah Anda yakin ingin menghapus user " + item.getUsername() + "?")
                        .setPositiveButton("Ya", (dialog, which) -> {
                            // Hapus dari Firebase menggunakan UID
                            database.child(item.getUid()).removeValue()
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
                        .setNegativeButton("Tidak", null)
                        .show();
            }
        });

        // Tombol Edit
        holder.btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Kirim data ke EditDataUser
                Intent edit = new Intent(context, EditDataUser.class);
                edit.putExtra("uid", item.getUid());
                edit.putExtra("username", item.getUsername());
                edit.putExtra("email", item.getEmail());
                edit.putExtra("noHp", item.getNoHp());
                // Note: Password tidak disimpan di database, jadi tidak perlu dikirim
                context.startActivity(edit);
            }
        });

        // Tombol Aktifasi (jika ada)
        if (holder.btnAktifasi != null) {
            holder.btnAktifasi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    toggleUserStatus(item);
                }
            });
        }

        Button btnPromote = holder.itemView.findViewById(R.id.btnPromote);
        if (btnPromote != null) {
            btnPromote.setVisibility(View.VISIBLE);
            btnPromote.setOnClickListener(v -> {
                // Panggil method promote dari activity
                if (context instanceof Admin) {
                    ((Admin) context).promoteToAdmin(item);
                }
            });

            // Sembunyikan jika sudah admin
            if ("admin".equals(item.getRole())) {
                btnPromote.setVisibility(View.GONE);
            }
        }
    }

    private String getStatusText(int status) {
        switch (status) {
            case 0: return "Pending";
            case 1: return "Aktif";
            case 2: return "Nonaktif";
            default: return "Unknown";
        }
    }

    private void toggleUserStatus(User user) {
        int newStatus = (user.getStatus() == 1) ? 0 : 1;

        database.child(user.getUid()).child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    String statusText = (newStatus == 1) ? "diaktifkan" : "dinonaktifkan";
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

    @Override
    public int getItemCount() {
        return mlist.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        private TextView tvUsername, tvEmail, tvNoHp, tvStatus, tvRole;
        private Button btnHapus, btnEdit, btnAktifasi;

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
        }
    }
}
