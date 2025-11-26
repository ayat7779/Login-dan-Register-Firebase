package com.apps.logindanregisterfirebase.Entitas;

public class User {

    private String noHp, username, password;

    public User() {

    }

    public User(String noHp, String username, String password) {
        this.noHp = noHp;
        this.username = username;
        this.password = password;
    }

    public String getNoHp() {
        return noHp;
    }

    public void setNoHp(String noHp) {
        this.noHp = noHp;
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
}
