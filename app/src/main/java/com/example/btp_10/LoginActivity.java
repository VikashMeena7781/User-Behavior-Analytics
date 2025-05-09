package com.example.btp_10;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "Logs";

    private EditText etLoginEmail, etLoginPassword;
    private Button btnLogin;
    private TextView tvSignUp, tvForgotPassword;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: called");
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> {
            Log.d(TAG, "Login button clicked");
            loginUser();
        });

        tvSignUp.setOnClickListener(v -> {
            Log.d(TAG, "Navigating to SignUpActivity");
            navigateToSignUp();
        });

        tvForgotPassword.setOnClickListener(v -> {
            Log.d(TAG, "Forgot password clicked");
            resetPassword();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: checking for existing user");
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already logged in: " + currentUser.getEmail());
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        } else {
            Log.d(TAG, "No user logged in");
        }
    }

    private void loginUser() {
        String email = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        Log.d(TAG, "Attempting login with email: " + email);

        if (TextUtils.isEmpty(email)) {
            Log.d(TAG, "Email is empty");
            etLoginEmail.setError("Email is required");
            etLoginEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Log.d(TAG, "Password is empty");
            etLoginPassword.setError("Password is required");
            etLoginPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        btnLogin.setEnabled(true);

                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d(TAG, "Login successful. User: " + (user != null ? user.getEmail() : "null"));
                            Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Log.e(TAG, "Login failed: " + task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed: " +
                                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void navigateToSignUp() {
        startActivity(new Intent(LoginActivity.this, SignupActivity.class));
    }

    private void resetPassword() {
        String email = etLoginEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Log.d(TAG, "Reset password - email empty");
            etLoginEmail.setError("Enter your email to reset password");
            etLoginEmail.requestFocus();
            return;
        }

        Log.d(TAG, "Sending password reset email to: " + email);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent successfully");
                        Toast.makeText(LoginActivity.this, "Password reset email sent",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Failed to send reset email", task.getException());
                        Toast.makeText(LoginActivity.this, "Failed to send reset email: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}