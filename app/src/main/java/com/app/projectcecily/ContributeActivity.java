package com.app.projectcecily;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ContributeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contribute);

        View appBarView = findViewById(R.id.appBarLayout);
        UIUtils.applyTopInsetToAppBar(appBarView);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Contribute & Support");
            }
        }

        View btnKofi = findViewById(R.id.btnKofiSupport);
        View btnMonthly = findViewById(R.id.btnSubMonthly);
        View btnLifetime = findViewById(R.id.btnSubLifetime);

        if (btnKofi != null) {
            btnKofi.setOnClickListener(v -> Toast.makeText(this, "Ko-fi Donation Link (Dummy for presentation)", Toast.LENGTH_SHORT).show());
        }

        if (btnMonthly != null) {
            btnMonthly.setOnClickListener(v -> Toast.makeText(this, "Monthly Impulse Pass $0.99 (Dummy for presentation)", Toast.LENGTH_SHORT).show());
        }

        if (btnLifetime != null) {
            btnLifetime.setOnClickListener(v -> Toast.makeText(this, "Lifetime Freedom Pass $9.99 (Dummy for presentation)", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeUtils.applyAccentToActivity(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
