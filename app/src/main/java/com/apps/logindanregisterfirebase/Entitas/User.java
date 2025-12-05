package com.apps.logindanregisterfirebase.Entitas;

public class User {
    private String uid;
    private String username;
    private String email;
    private String noHp;
    private String phoneNumber;
    private Boolean phoneVerified;
    private Long phoneVerifiedAt;
    private String role;
    private Integer status;
    private Long createdAt;
    private Long updatedAt;

    // 1. NO-ARGUMENT CONSTRUCTOR (WAJIB!)
    public User() {
        // Constructor kosong WAJIB ada untuk Firebase
    }

    // 2. Constructor dengan parameter (optional)
    public User(String uid, String username, String email, String noHp) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.noHp = noHp;
        this.phoneVerified = false;
        this.phoneVerifiedAt = 0L;
        this.role = "user";
        this.status = 0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // 3. GETTERS & SETTERS untuk SEMUA fields (WAJIB!)
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Boolean getPhoneVerified() {
        return phoneVerified != null ? phoneVerified : false;
    }

    public void setPhoneVerified(Boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    public Long getPhoneVerifiedAt() {
        return phoneVerifiedAt != null ? phoneVerifiedAt : 0L;
    }

    public void setPhoneVerifiedAt(Long phoneVerifiedAt) {
        this.phoneVerifiedAt = phoneVerifiedAt;
    }

    public String getRole() {
        return role != null ? role : "user";
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getStatus() {
        return status != null ? status : 0;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getCreatedAt() {
        return createdAt != null ? createdAt : 0L;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt != null ? updatedAt : 0L;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    public boolean PhoneVerified() {
        return phoneVerified != null && phoneVerified;
    }

    public String getStatusText() {
        switch (getStatus()) {
            case 0: return "Pending";
            case 1: return "Aktif";
            case 2: return "Nonaktif";
            default: return "Unknown";
        }
    }
}