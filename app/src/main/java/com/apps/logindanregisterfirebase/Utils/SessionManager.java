package com.apps.logindanregisterfirebase.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    // Shared Preferences
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    // Context
    private Context _context;

    // Shared pref mode
    private int PRIVATE_MODE = 0;

    // Sharedpref file name
    private static final String PREF_NAME = "FirebaseLoginPref";

    // All Shared Preferences Keys
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_USER_PHONE = "userPhone";
    private static final String KEY_REMEMBER_ME = "rememberMe";

    // Constructor
    public SessionManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    /**
     * Create login session
     */
    public void createLoginSession(String userId, String email, String name,
                                   String role, String phone, boolean rememberMe) {
        // Storing login value as TRUE
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_ROLE, role);
        editor.putString(KEY_USER_PHONE, phone);
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe);

        // commit changes
        editor.apply(); // Changed from commit() to apply()
    }

    /**
     * Check login method will check user login status
     * If false it will redirect user to login page
     * Else won't do anything
     */
    public boolean checkLogin() {
        // Check login status
        return this.isLoggedIn();
    }

    /**
     * Quick check for login
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Get stored session data
     */
    public String getUserId() {
        return pref.getString(KEY_USER_ID, null);
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, null);
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, null);
    }

    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, "user");
    }

    public String getUserPhone() {
        return pref.getString(KEY_USER_PHONE, "");
    }

    public boolean isRememberMe() {
        return pref.getBoolean(KEY_REMEMBER_ME, false);
    }

    /**
     * Clear session details - FIXED METHOD NAME
     */
    public void logoutUser() {  // Nama method yang benar
        // Clearing all data from Shared Preferences
        editor.clear();
        editor.apply(); // Changed from commit() to apply()
    }

    /**
     * Alias for logoutUser() - untuk kompatibilitas
     */
    public void logout() {  // Tambah method alias
        logoutUser();
    }

    /**
     * Update user profile in session
     */
    public void updateUserProfile(String name, String phone) {
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_PHONE, phone);
        editor.apply(); // Changed from commit() to apply()
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return "admin".equals(getUserRole());
    }

    /**
     * Get all session data as string (for debugging)
     */
    public String getSessionInfo() {
        return "User: " + getUserName() +
                "\nEmail: " + getUserEmail() +
                "\nRole: " + getUserRole() +
                "\nLoggedIn: " + isLoggedIn();
    }
}
