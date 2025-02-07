package com.google.mediapipe.examples.facemesh;

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mediapipe.apps.EyeCursor.R;

import java.util.Objects;

public class SettingModules extends AppCompatActivity {

    /**
     *  ------------------------------------------------ MainProgram
     */

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_modules);

        // Set fullscreen flags
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Hide the navigation and status bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
        Objects.requireNonNull(getSupportActionBar()).hide();

        // all views
        Button back_button = findViewById(R.id.settingsbackbutton);
        Button speed_button = findViewById(R.id.pointerspeedbutton);
        Button smoothing_button = findViewById(R.id.pointersmoothingbutton);
        Button size_button = findViewById(R.id.pointersizebutton);
        Button threshold_button = findViewById(R.id.pointerthresholdbutton);
        Button paccelerationbutton = findViewById(R.id.pointeraccelerationbutton);
        Button pdwelltimebutton = findViewById(R.id.pointerdwelltimebutton);


        // Functions of Buttons
        goSendDataBackMain(back_button);
        goAccelerationModifier(paccelerationbutton);
        goSizeModifier(size_button);
        goDwellTimeModifier(pdwelltimebutton);
        goSmoothingModifier(smoothing_button);
        goThresholdModifier(threshold_button);
        goSpeedModifier(speed_button);
    }
    public void goSendDataBackMain(Button button) {
        button.setOnClickListener(view -> {
            Intent intent = new Intent(SettingModules.this, MainActivity.class);
            startActivity(intent);
        });
    }
    public void goSpeedModifier(Button button) {
        button.setOnClickListener(view -> {
            Intent intent = new Intent(SettingModules.this, PointerSpeedModule.class);
            startActivity(intent);
        });
    }
    public void goSmoothingModifier(Button button) {
        button.setOnClickListener(view -> {
            Intent intent = new Intent(SettingModules.this, PointerSmoothingModule.class);
            startActivity(intent);
        });
    }
    public void goSizeModifier(Button button) {
        button.setOnClickListener(view -> {
            Intent intent = new Intent(SettingModules.this, PointerSizeModule.class);
            startActivity(intent);
        });
    }
    public void goThresholdModifier(Button button) {
        button.setOnClickListener(view -> {
            Intent intent = new Intent(SettingModules.this, PointerThresholdModule.class);
            startActivity(intent);
        });
    }
    public void goAccelerationModifier(Button button) {
        button.setOnClickListener(view -> {
            Intent intent = new Intent(SettingModules.this, PointerAccelerationModule.class);
            startActivity(intent);
        });
    }
    public void goDwellTimeModifier(Button button) {
        button.setOnClickListener(view -> {
            Intent intent = new Intent(SettingModules.this, PointerDwellTimeModule.class);
            startActivity(intent);
        });
    }
}
