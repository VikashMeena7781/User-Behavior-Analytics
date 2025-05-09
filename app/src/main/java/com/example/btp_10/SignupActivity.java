package com.example.btp_10;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText etSignupName, etSignupEmail, etSignupPassword, etSignupConfirmPassword;
    private Button btnSignup;
    private TextView tvLogin;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("users");

        // Initialize UI components
        etSignupName = findViewById(R.id.etSignupName);
        etSignupEmail = findViewById(R.id.etSignupEmail);
        etSignupPassword = findViewById(R.id.etSignupPassword);
        etSignupConfirmPassword = findViewById(R.id.etSignupConfirmPassword);
        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLogin);

        // Set click listeners
        btnSignup.setOnClickListener(v -> createAccount());
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void createAccount() {
        String name = etSignupName.getText().toString().trim();
        String email = etSignupEmail.getText().toString().trim();
        String password = etSignupPassword.getText().toString().trim();
        String confirmPassword = etSignupConfirmPassword.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(name)) {
            etSignupName.setError("Name is required");
            etSignupName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etSignupEmail.setError("Email is required");
            etSignupEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etSignupPassword.setError("Password is required");
            etSignupPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etSignupPassword.setError("Password must be at least 6 characters");
            etSignupPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etSignupConfirmPassword.setError("Passwords do not match");
            etSignupConfirmPassword.requestFocus();
            return;
        }

        // Show loading indicator
        btnSignup.setEnabled(false);

        // Create user account
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            user.updateProfile(profileUpdates).addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    saveUserToRealtimeDatabase(user.getUid(), name, email);
                                } else {
                                    Toast.makeText(SignupActivity.this,
                                            "Failed to update profile", Toast.LENGTH_SHORT).show();
                                    btnSignup.setEnabled(true);
                                }
                            });
                        } else {
                            Toast.makeText(SignupActivity.this,
                                    "User creation failed", Toast.LENGTH_SHORT).show();
                            btnSignup.setEnabled(true);
                        }
                    } else {
                        String errorMessage = "Authentication failed";
                        if (task.getException() != null) {
                            errorMessage += ": " + task.getException().getMessage();
                        }
                        Toast.makeText(SignupActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        btnSignup.setEnabled(true);
                    }
                });
    }

    private void saveUserToRealtimeDatabase(String userId, String name, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("createdAt", System.currentTimeMillis());

        databaseRef.child(userId)
                .setValue(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignupActivity.this, "Account created successfully",
                            Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignupActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    String errorMessage = "Error saving user data";
                    if (e.getMessage() != null) {
                        errorMessage += ": " + e.getMessage();
                    }
                    Toast.makeText(SignupActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    btnSignup.setEnabled(true);
                });
    }
}