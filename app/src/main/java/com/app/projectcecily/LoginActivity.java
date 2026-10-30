package com.app.projectcecily;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etIdentifier, etPassword;
    private Button btnLogin, btnGoToSignUp, btnGuestAppearance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        if (UserManager.isLoggedIn(this)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        UIUtils.applyTopInsetToAppBar(findViewById(R.id.appBarLayout));
        UIUtils.applyBottomPadding(findViewById(R.id.login_scroll));

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Log In");
            }
        }

        etIdentifier = findViewById(R.id.etLoginIdentifier);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToSignUp = findViewById(R.id.btnGoToSignUp);
        btnGuestAppearance = findViewById(R.id.btnGuestAppearance);

        btnLogin.setOnClickListener(v -> handleLogin());

        btnGoToSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        btnGuestAppearance.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, AppearanceActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeUtils.applyAccentToActivity(this);
    }

    private void handleLogin() {
        String identifier = etIdentifier.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (identifier.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username/email and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        UserManager.login(this, identifier, password, new UserManager.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Log In");
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
