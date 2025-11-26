package com.apps.logindanregisterfirebase.Admin;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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
        holder.tvUsername.setText(String.format("Username : %s", item.getUsername()));
        holder.tvNoHp.setText(String.format("No HP : %s", item.getNoHp()));
        holder.tvPassword.setText(String.format("Password : %s", item.getPassword()));

        holder.btnHapus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                database.child(item.getUsername()).setValue(null);
            }
        });

        holder.btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String username = item.getUsername();
                String noHp = item.getNoHp();
                String password = item.getPassword();

                Intent edit = new Intent(context, EditDataUser.class);
                edit.putExtra("username", username);
                edit.putExtra("noHp", noHp);
                edit.putExtra("password", password);
                context.startActivity(edit);



            }
        });
    }

    @Override
    public int getItemCount() {
        return mlist.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        private TextView tvUsername, tvNoHp, tvPassword;
        private Button btnHapus, btnEdit;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvNoHp = itemView.findViewById(R.id.tvNoHp);
            tvPassword = itemView.findViewById(R.id.tvPassword);
            btnHapus = itemView.findViewById(R.id.btnHapus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }
}
