package com.apps.logindanregisterfirebase.Entitas;

public class User {
    private String uid;
    private String username;
    private String password;
    private String email;
    private String noHp;
    private String role; // "user" or "admin"
    private int status; // 0: pending, 1: active, 2: banned
    private long createdAt;

    // Empty constructor required for Firebase
    public User() {
    }

    public User(String uid, String username, String email, String noHp) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.noHp = noHp;
        this.role = "user";
        this.status = 0; // default pending
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNoHp() {
        return noHp;
    }

    public void setNoHp(String noHp) {
        this.noHp = noHp;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
