package com.example.btp1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText etSignupName, etSignupEmail, etSignupPassword, etSignupConfirmPassword;
    private Button btnSignup;
    private TextView tvLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign up success
                            FirebaseUser user = mAuth.getCurrentUser();

                            // Set display name for the user
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                // Now save user details in Firestore
                                                saveUserToFirestore(user.getUid(), name, email);
                                            } else {
                                                Toast.makeText(SignupActivity.this,
                                                        "Failed to update profile", Toast.LENGTH_SHORT).show();
                                                btnSignup.setEnabled(true);
                                            }
                                        }
                                    });
                        } else {
                            // If sign up fails, display a message to the user
                            Toast.makeText(SignupActivity.this, "Authentication failed: " +
                                    task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            btnSignup.setEnabled(true);
                        }
                    }
                });
    }

    private void saveUserToFirestore(String userId, String name, String email) {
        // Create a user document in Firestore
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignupActivity.this, "Account created successfully",
                            Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignupActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignupActivity.this, "Error creating user profile: " +
                            e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSignup.setEnabled(true);
                });
    }
}