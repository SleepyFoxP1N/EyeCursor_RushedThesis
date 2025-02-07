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

public class PointerThresholdModule extends AppCompatActivity {

    public int cursorThreshold_low = 12, cursorThreshold_default = 20, cursorThreshold_RB = 20, cursorThreshold_high = 37;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pointer_threshold_module);

        // Set fullscreen flags
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Hide the navigation and status bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
        Objects.requireNonNull(getSupportActionBar()).hide();

        // all views
        RadioButton low_radioButton = findViewById(R.id.pthresholdmlowradioButton);
        RadioButton default_radioButton = findViewById(R.id.pthresholddefaultradioButton);
        RadioButton high_radioButton = findViewById(R.id.pthresholdhighradioButton);
        Button save_button = findViewById(R.id.savethreshold_btn);

        //Recover saved Setting
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
        setCursorPropertyValueFromSharedPreferences(sharedPreferences, low_radioButton, default_radioButton, high_radioButton);

        // Buttons Function
        goBackToSetting();
        Cursor_Low(low_radioButton, default_radioButton, high_radioButton);
        Cursor_Default(low_radioButton, default_radioButton, high_radioButton);
        Cursor_High(low_radioButton, default_radioButton, high_radioButton);
        setCursorProperty(save_button, true, sharedPreferences);
    }
    public void goBackToSetting(){
        Button back_button = findViewById(R.id.pthresholdbackbutton);
        back_button.setOnClickListener(view -> {
            Intent intent = new Intent(PointerThresholdModule.this,SettingModules.class);
            startActivity(intent);
        });
    }
    public void Cursor_Low(RadioButton low, RadioButton Default, RadioButton high){
        low.setOnClickListener(view -> {
            cursorThreshold_RB = cursorThreshold_low;
            setCursorSizeValue(low, Default, high);
        });
    }

    public void Cursor_Default(RadioButton low, RadioButton Default, RadioButton high){
        Default.setOnClickListener(view -> {
            cursorThreshold_RB = cursorThreshold_default;
            setCursorSizeValue(low, Default, high);
        });
    }
    public void Cursor_High(RadioButton low, RadioButton Default, RadioButton high){
        high.setOnClickListener(view -> {
            cursorThreshold_RB = cursorThreshold_high;
            setCursorSizeValue(low, Default, high);
        });
    }

    public void setCursorProperty(Button button, Boolean goToSettingClass, SharedPreferences sharedPreferences){
        button.setOnClickListener(view -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("cursorThreshold", cursorThreshold_RB);
            editor.apply();

            if(goToSettingClass){
                Intent intent = new Intent(PointerThresholdModule.this,SettingModules.class);
                startActivity(intent);
            }
        });
    }

    public void setCursorPropertyValueFromSharedPreferences(SharedPreferences sharedPreferences, RadioButton low, RadioButton Default, RadioButton high){
        int value = sharedPreferences.getInt("cursorThreshold", cursorThreshold_default);
        switch(value){
            case 12:
                low.setChecked(true);
                Default.setChecked(false);
                high.setChecked(false);
                break;
            case 37:
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
    public void setCursorSizeValue(RadioButton low, RadioButton Default, RadioButton high){
        switch(cursorThreshold_RB){
            case 12:
                low.setChecked(true);
                Default.setChecked(false);
                high.setChecked(false);
                break;
            case 37:
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