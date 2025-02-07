package com.google.mediapipe.examples.facemesh;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mediapipe.apps.EyeCursor.R;

import java.util.Objects;

public class PointerAccelerationModule extends AppCompatActivity {
    public float cursorAcceleration = 1.05f;
    public String cursorAcceleration_RB = "default";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pointer_acceleration_module);

        // Set fullscreen flags
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Hide the navigation and status bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
        Objects.requireNonNull(getSupportActionBar()).hide();

        // All views
        Button back_button = findViewById(R.id.paccelerationbackbutton);
        RadioButton low_RadioButton = findViewById(R.id.paccelowradioButton);
        RadioButton medium_RadioButton = findViewById(R.id.paccemediumradioButton);
        RadioButton high_RadioButton = findViewById(R.id.paccehighradioButton);
        Button saveAcceleration_Button = findViewById(R.id.saveacceleration_btn);
        
        //Recover saved Setting
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
        setCursorPropertyValueFromSharedPreferences(low_RadioButton, medium_RadioButton, high_RadioButton, sharedPreferences);
                
        // Buttons Function
        goBackToSetting(back_button);
        Cursor_Low(low_RadioButton, medium_RadioButton, high_RadioButton);
        Cursor_Default(low_RadioButton, medium_RadioButton, high_RadioButton);
        Cursor_High(low_RadioButton, medium_RadioButton, high_RadioButton);
        setCursorProperty(saveAcceleration_Button, true, sharedPreferences);
    }
    public void goBackToSetting(Button button){
        button.setOnClickListener(view -> {
            Intent intent = new Intent(PointerAccelerationModule.this,SettingModules.class);
            startActivity(intent);
        });
    }
    public void Cursor_Low(RadioButton low, RadioButton Default, RadioButton high){
        low.setOnClickListener(view -> {
            cursorAcceleration = 1.02f;
            cursorAcceleration_RB = "low";
            setCursorValue(low, Default, high);
        });
    }

    public void Cursor_Default(RadioButton low, RadioButton Default, RadioButton high){
        Default.setOnClickListener(view -> {
            cursorAcceleration = 1.05f;
            cursorAcceleration_RB = "default";
            setCursorValue(low, Default, high);
        });
    }
    public void Cursor_High(RadioButton low, RadioButton Default, RadioButton high){
        high.setOnClickListener(view -> {
            cursorAcceleration = 1.1f;
            cursorAcceleration_RB = "high";
            setCursorValue(low, Default, high);
        });
    }

    public void setCursorProperty(Button button, Boolean goToSettingClass, SharedPreferences sharedPreferences){
        button.setOnClickListener(view -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putFloat("cursorAcceleration", cursorAcceleration);
            editor.apply();
            if(goToSettingClass){
                Intent intent = new Intent(PointerAccelerationModule.this,SettingModules.class);
                startActivity(intent);
            }
        });
    }

    public void setCursorPropertyValueFromSharedPreferences(RadioButton low, RadioButton Default,RadioButton high, SharedPreferences sharedPreferences){
        Float value = sharedPreferences.getFloat("cursorAcceleration", 1.05f);
        String value_s;
        if(value==1.02f)
            value_s = "low";
        else if(value==1.1f)
            value_s = "high";
        else
            value_s = "default";

        switch(value_s){
            case "low":
                low.setChecked(true);
                Default.setChecked(false);
                high.setChecked(false);
                break;
            case "high":
                low.setChecked(false);
                Default.setChecked(false);
                high.setChecked(true);
                break;
            default:
                low.setChecked(false);
                Default.setChecked(true);
                high.setChecked(false);
        }
    }
    public void setCursorValue(RadioButton low, RadioButton Default, RadioButton high){

        switch(cursorAcceleration_RB){
            case "low":
                low.setChecked(true);
                Default.setChecked(false);
                high.setChecked(false);
                break;
            case "high":
                low.setChecked(false);
                Default.setChecked(false);
                high.setChecked(true);
                break;
            default:
                low.setChecked(false);
                Default.setChecked(true);
                high.setChecked(false);
        }
    }
}