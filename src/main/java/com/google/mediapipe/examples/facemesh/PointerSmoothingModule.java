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

public class PointerSmoothingModule extends AppCompatActivity {

    public int cursorSmoothing_RB = 40;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pointer_smoothing_module);

        // Set fullscreen flags
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Hide the navigation and status bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
        Objects.requireNonNull(getSupportActionBar()).hide();

        //All views
        Button back_button = findViewById(R.id.smoothingbackbutton);
        RadioButton low_button = findViewById(R.id.psmoothinglowradioButton);
        RadioButton default_button = findViewById(R.id.psmoothingdefaultradioButton);
        RadioButton high_button = findViewById(R.id.psmoothinghighradioButton);
        Button save_button = findViewById(R.id.savedSmoothing_btn);

        //Recover saved Setting
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
        setCursorPropertyValueFromSharedPreferences(sharedPreferences, low_button, default_button, high_button);

        // Buttons Function
        goBackToSetting(back_button);
        Cursor_Low(low_button, default_button, high_button);
        Cursor_Default(low_button, default_button, high_button);
        Cursor_High(low_button, default_button, high_button);
        setCursorProperty(save_button,true, sharedPreferences);
    }
    public void goBackToSetting(Button back_button){
        back_button.setOnClickListener(view -> {
            Intent intent = new Intent(PointerSmoothingModule.this,SettingModules.class);
            startActivity(intent);
        });
    }
    public void Cursor_Low(RadioButton low, RadioButton Default, RadioButton high){
        low.setOnClickListener(view -> {
            //Decrease the size
            /*desiredWidth = (int) (cursor_defaultWidth/1.5);
            desiredHeight = (int) (cursor_defaultHeight/1.5);*/
            cursorSmoothing_RB = 20;
            setCursorSizeValue(low, Default, high);
        });
    }

    public void Cursor_Default(RadioButton low, RadioButton Default, RadioButton high){
        Default.setOnClickListener(view -> {
            /*desiredWidth = cursor_defaultWidth;
            desiredHeight = cursor_defaultHeight;*/
            cursorSmoothing_RB = 40;
            setCursorSizeValue(low, Default, high);
        });
    }

    public void Cursor_High(RadioButton low, RadioButton Default, RadioButton high){
        high.setOnClickListener(view -> {
            //Increase the size
            /*desiredWidth = (int) (cursor_defaultWidth*1.5);
            desiredHeight = (int) (cursor_defaultHeight*1.5);*/
            cursorSmoothing_RB = 60;
            setCursorSizeValue(low, Default, high);
        });
    }

    public void setCursorProperty(Button button, Boolean goToSettingClass, SharedPreferences sharedPreferences){
        button.setOnClickListener(view -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("cursorSmoothening", cursorSmoothing_RB);
            /*editor.putInt("cursorSize_Width", desiredWidth);
            editor.putInt("cursorSize_Height", desiredHeight);*/
            editor.apply();
            if(goToSettingClass){
                Intent intent = new Intent(PointerSmoothingModule.this,SettingModules.class);
                startActivity(intent);
            }
        });
    }

    public void setCursorPropertyValueFromSharedPreferences(SharedPreferences sharedPreferences, RadioButton low, RadioButton Default, RadioButton high){
        int value = sharedPreferences.getInt("cursorSmoothening", 40);
        switch(value){
            case 20:
                low.setChecked(true);
                Default.setChecked(false);
                high.setChecked(false);
                break;
            case 60:
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
        switch(cursorSmoothing_RB){
            case 20:
                low.setChecked(true);
                Default.setChecked(false);
                high.setChecked(false);
                break;
            case 60:
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