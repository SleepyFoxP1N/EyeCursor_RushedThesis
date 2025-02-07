package com.google.mediapipe.examples.facemesh;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.google.mediapipe.apps.EyeCursor.R;

public class UserLicenseAgreement extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_user_license_agreement);

        Button ulicenseback_button = findViewById(R.id.userlicensebackbutton);
        ulicenseback_button.setOnClickListener(view -> {
            Intent intent = new Intent(UserLicenseAgreement.this, MainActivity.class);
            startActivity(intent);
            finish();
        });


    }
}