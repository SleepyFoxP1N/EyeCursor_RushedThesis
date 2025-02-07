package com.google.mediapipe.examples.facemesh;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mediapipe.apps.EyeCursor.R;

import java.util.Objects;

public class PointerSpeedModule extends AppCompatActivity {

    /**-----------------------------------------
     *  ----------------------------------------- Main Program
     -----------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pointer_speed_module);

        // Set fullscreen flags
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Hide the navigation and status bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
        Objects.requireNonNull(getSupportActionBar()).hide();

        // ------- Functions of this Activity
        backButton_noSave();

        // EditText Limiter
        EditText editText_X = findViewById(R.id.adjustSpeedValueX);
        EditText editText_Y = findViewById(R.id.adjustSpeedValueY);
        limit_EditText(editText_X);
        limit_EditText(editText_Y);

        // Increaser
        Button speedAddX = findViewById(R.id.speedAddX);
        addCursorSpeed(editText_X, speedAddX);
        Button speedAddY = findViewById(R.id.speedAddY);
        addCursorSpeed(editText_Y, speedAddY);

        // Decreaser
        Button speedMinusX = findViewById(R.id.speedMinusX);
        minusCursorSpeed(editText_X, speedMinusX);
        Button speedMinusY = findViewById(R.id.speedMinusY);
        minusCursorSpeed(editText_Y, speedMinusY);

        // Save X and Y values data
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
        Button save_Button = findViewById(R.id.saveSpeedValue);
        save_send_data(save_Button, editText_X, editText_Y, true, sharedPreferences);

        // Receiver Data from the SharedPreferences
        int valueX = sharedPreferences.getInt("cursorSpeedX", 0);
        int valueY = sharedPreferences.getInt("cursorSpeedY", 0);
        editText_X.setText(String.valueOf(valueX));
        editText_Y.setText(String.valueOf(valueY));
    }

    public void limit_EditText(EditText editText){

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // This method is called before the text is changed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // This method is called when the text is being changed
            }

            @Override
            public void afterTextChanged(Editable s) {
                // This method is called after the text has changed
                if (s != null && s.length() > 2) {
                    // Limit the number of digits to 2
                    String input = s.toString();
                    String twoDigitInput = input.substring(0, 2);
                    editText.setText(twoDigitInput);
                    editText.setSelection(2); // Set the cursor position to the end
                }
            }
        });

    }

    public void backButton_noSave(){
        Button speed_button = findViewById(R.id.speedbackbutton);
        speed_button.setOnClickListener(view -> {
            Intent intent = new Intent(PointerSpeedModule.this,SettingModules.class);
            startActivity(intent);
        });
    }
    public void addCursorSpeed(EditText adjustValue_EditText, Button button){
        button.setOnClickListener(view -> {
            int value = Integer.parseInt(adjustValue_EditText.getText().toString());
            value = value + 1;
            adjustValue_EditText.setText(String.valueOf(value));
        });
    }
    public void minusCursorSpeed(EditText adjustValue_EditText, Button button){
        button.setOnClickListener(view -> {
            int value = Integer.parseInt(adjustValue_EditText.getText().toString());
            if (value>0) value = value - 1;
            adjustValue_EditText.setText(String.valueOf(value));
        });
    }
    public void save_send_data(Button button, EditText adjustValue_EditTextX, EditText adjustValue_EditTextY, Boolean goToSettingClass, SharedPreferences sharedPreferences){
        button.setOnClickListener(view -> {
            if(adjustValue_EditTextX.getText().length() > 0)
            {
                int value = Integer.parseInt(adjustValue_EditTextX.getText().toString());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("cursorSpeedX", value);
                editor.apply();
            }
            if(adjustValue_EditTextY.getText().length() > 0)
            {
                int value = Integer.parseInt(adjustValue_EditTextY.getText().toString());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("cursorSpeedY", value);
                editor.apply();
            }

            if(goToSettingClass){
                Intent intent = new Intent(PointerSpeedModule.this,SettingModules.class);
                startActivity(intent);
            }
        });
    }
}