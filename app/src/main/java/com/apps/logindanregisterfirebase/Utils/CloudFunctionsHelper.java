package com.apps.logindanregisterfirebase.Utils;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

public class CloudFunctionsHelper {

    private static CloudFunctionsHelper instance;
    private FirebaseFunctions functions;
    private Context context;

    private CloudFunctionsHelper(Context context) {
        this.context = context.getApplicationContext();
        this.functions = FirebaseFunctions.getInstance();
        // Jika perlu region spesifik: FirebaseFunctions.getInstance("region")
    }

    public static synchronized CloudFunctionsHelper getInstance(Context context) {
        if (instance == null) {
            instance = new CloudFunctionsHelper(context);
        }
        return instance;
    }

    /**
     * Hapus user dari Firebase Auth via Cloud Function
     */
    public void deleteUserFromAuth(String userId, OnCompleteListener listener) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);

        functions.getHttpsCallable("deleteUserAuth")
                .call(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        HttpsCallableResult result = task.getResult();
                        Map<String, Object> resultData = (Map<String, Object>) result.getData();

                        boolean success = resultData != null &&
                                resultData.containsKey("success") &&
                                Boolean.TRUE.equals(resultData.get("success"));

                        if (listener != null) {
                            listener.onComplete(success, resultData);
                        }

                    } else {
                        Exception exception = task.getException();
                        Toast.makeText(context,
                                "Gagal menghapus dari Auth: " + (exception != null ? exception.getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();

                        if (listener != null) {
                            listener.onComplete(false, null);
                        }
                    }
                });
    }

    /**
     * Dapatkan semua users dari Auth (admin only)
     */
    public void getAllAuthUsers(OnCompleteListener listener) {
        functions.getHttpsCallable("listAllUsers")
                .call()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        HttpsCallableResult result = task.getResult();
                        Map<String, Object> resultData = (Map<String, Object>) result.getData();

                        if (listener != null) {
                            listener.onComplete(true, resultData);
                        }

                    } else {
                        if (listener != null) {
                            listener.onComplete(false, null);
                        }
                    }
                });
    }

    public interface OnCompleteListener {
        void onComplete(boolean success, Map<String, Object> result);
    }
}
