package com.google.mediapipe.examples.facemesh;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.mediapipe.apps.EyeCursor.R;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

public class PointerSizeModule extends AppCompatActivity {

    public String cursorSize_RB = "default";
    ImageView cursor;
    public int desiredWidth, desiredHeight;
    public int cursor_defaultWidth = 63*2, cursor_defaultHeight = 93*2;

    // Convert Bitmap to String
    public String[] paramsToString(ViewGroup.LayoutParams params) {
        String widthString = String.valueOf(params.width);
        String heightString = String.valueOf(params.height);
        String[] array = {widthString, heightString};
        return array;
    }

    // Function to resize the image
    public ViewGroup.LayoutParams getResizeImage(ImageView cursor, int desiredWidth, int desiredHeight) {

        // Get the current layout parameters of the ImageView
        ViewGroup.LayoutParams params = cursor.getLayoutParams();

        // Set the new width and height for the ImageView
        params.width = 200; // New width in pixels
        params.height = 200; // New height in pixels

        // Apply the updated layout parameters to the ImageView
        return params;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pointer_size_module);

        // Set fullscreen flags
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Hide the navigation and status bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
        Objects.requireNonNull(getSupportActionBar()).hide();

        RadioButton small_radioButton = findViewById(R.id.psizesmallradioButton);
        RadioButton default_radioButton = findViewById(R.id.psizestandardradioButton);
        RadioButton large_radioButton = findViewById(R.id.psizelargeradioButton);
        Button savedButton = findViewById(R.id.savedSize_btn);
        Button backButton = findViewById(R.id.psizebackbutton);

        //Recover saved Setting
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
        setCursorSizeValueFromSharedPreferences(small_radioButton, default_radioButton, large_radioButton, sharedPreferences, "cursorSize_RB");
        // Buttons Function
        goBackToSetting(backButton);
        CursorSmall(small_radioButton, default_radioButton, large_radioButton);
        CursorDefaultSize(small_radioButton, default_radioButton, large_radioButton);
        CursorLarge(small_radioButton, default_radioButton, large_radioButton);
        setCursorSize(savedButton, true, sharedPreferences);
    }
    public void goBackToSetting(Button button){
        button.setOnClickListener(view -> {
            Intent intent = new Intent(PointerSizeModule.this,SettingModules.class);
            startActivity(intent);
        });
    }
    public void CursorSmall(RadioButton small, RadioButton Default, RadioButton large){
        small.setOnClickListener(view -> {
            //Decrease the size
            desiredWidth = (int) (cursor_defaultWidth/1.5);
            desiredHeight = (int) (cursor_defaultHeight/1.5);
            cursorSize_RB = "small";
            setCursorSizeValue(small, Default, large);
        });
    }

    public void CursorDefaultSize(RadioButton small, RadioButton Default, RadioButton large){
        Default.setOnClickListener(view -> {
            desiredWidth = cursor_defaultWidth;
            desiredHeight = cursor_defaultHeight;
            cursorSize_RB = "default";
            setCursorSizeValue(small, Default, large);
        });
    }
    public void CursorLarge(RadioButton small, RadioButton Default, RadioButton large){
        large.setOnClickListener(view -> {
            //Increase the size
            desiredWidth = (int) (cursor_defaultWidth*1.5);
            desiredHeight = (int) (cursor_defaultHeight*1.5);
            cursorSize_RB = "large";
            setCursorSizeValue(small, Default, large);
        });
    }

    public void setCursorSize(Button button, Boolean goToSettingClass, SharedPreferences sharedPreferences){
        button.setOnClickListener(view -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("cursorSize_RB", cursorSize_RB);
            editor.putInt("cursorSize_Width", desiredWidth);
            editor.putInt("cursorSize_Height", desiredHeight);
            editor.apply();
            if(goToSettingClass){
                Intent intent = new Intent(PointerSizeModule.this,SettingModules.class);
                startActivity(intent);
            }
        });
    }

    
    public void setCursorSizeValueFromSharedPreferences(RadioButton small, RadioButton Default, RadioButton large, SharedPreferences sharedPreferences, String key){
        String value = sharedPreferences.getString(key, "default");
        
        switch(value){
            case "small":
                small.setChecked(true);
                Default.setChecked(false);
                large.setChecked(false);
                break;
            case "large":
                small.setChecked(false);
                Default.setChecked(false);
                large.setChecked(true);
                break;
            default:
                small.setChecked(false);
                Default.setChecked(true);
                large.setChecked(false);
        }
    }
    public void setCursorSizeValue(RadioButton small, RadioButton Default, RadioButton large){
        switch(cursorSize_RB){
            case "small":
                small.setChecked(true);
                Default.setChecked(false);
                large.setChecked(false);
                break;
            case "large":
                small.setChecked(false);
                Default.setChecked(false);
                large.setChecked(true);
                break;
            default:
                small.setChecked(false);
                Default.setChecked(true);
                large.setChecked(false);
        }
    }
}