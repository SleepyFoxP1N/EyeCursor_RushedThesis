package com.google.mediapipe.examples.facemesh;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mediapipe.apps.EyeCursor.R;

import java.util.Objects;

public class PointerDwellTimeModule extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pointer_dwell_time_module);

        // -- Set fullscreen flags
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // -- Hide the navigation and status bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
        Objects.requireNonNull(getSupportActionBar()).hide();

        // -- views
        EditText adjustValue_EditText = findViewById(R.id.adjustDwellTime_Value);
        Button dwellTime_Add = findViewById(R.id.dwellTime_Add);
        Button dwellTime_Minus = findViewById(R.id.dwellTime_Minus);
        Button saveDwellTime_Value = findViewById(R.id.saveDwellTime_Value);
        Button dwelltimeback = findViewById(R.id.dwelltimebackbutton);

        // -- Functions of this Activity
        backButton_noSave(dwelltimeback);
        addCursorDwellTime(adjustValue_EditText, dwellTime_Add);
        minusCursorDwellTime(adjustValue_EditText, dwellTime_Minus);
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
        save_send_data(adjustValue_EditText, saveDwellTime_Value, true, sharedPreferences);

        // -- EditText Limiter
        limit_EditText(adjustValue_EditText);

        setData(sharedPreferences, adjustValue_EditText);
    }

    public void limit_EditText(EditText editText){

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // -- This method is called before the text is changed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // -- This method is called when the text is being changed
            }

            @Override
            public void afterTextChanged(Editable string) {
                // -- This method is called after the text has changed
                if (string != null && string.length() > 0 && Integer.parseInt(string.toString()) < 1)
                {
                    // -- Limit the number to 1
                    editText.setText("4");
                    editText.setSelection(1);
                }
                if (string != null && string.length() > 1) {
                    // -- Limit the number of digits to 1
                    String input = string.toString();
                    String twoDigitInput = input.substring(0, 1);
                    editText.setText(twoDigitInput);
                    editText.setSelection(1); // -- Set the cursor position to the end
                }
            }
        });
    }

    public void backButton_noSave(Button button){
        button.setOnClickListener(view -> {
            Intent intent = new Intent(PointerDwellTimeModule.this,SettingModules.class);
            startActivity(intent);
        });
    }

    public void addCursorDwellTime(EditText adjustValue_EditText, Button button){
        button.setOnClickListener(view -> {
            int value = Integer.parseInt(adjustValue_EditText.getText().toString());
            value = value + 1;
            adjustValue_EditText.setText(String.valueOf(value));
        });
    }

    public void minusCursorDwellTime(EditText adjustValue_EditText, Button button){
        button.setOnClickListener(view -> {
            int value = Integer.parseInt(adjustValue_EditText.getText().toString());
            if (value>0) value = value - 1;
            adjustValue_EditText.setText(String.valueOf(value));
        });
    }

    public void setData(SharedPreferences sharedPreferences, EditText editText){
        // -- Receiver Data from the SharedPreferences
        float value = sharedPreferences.getFloat("cursorDwellTime", 3);
        int updateValue_IntDecisecond = (int) (value*10);
        String stringValue = String.valueOf((int)value);
        // -- Set the saved data
        if (editText != null)
        {
            editText.setText(stringValue);
        }
    }

    public void save_send_data(EditText adjustValue_EditText, Button button, Boolean goToSettingClass, SharedPreferences sharedPreferences){
        button.setOnClickListener(view -> {
            if(adjustValue_EditText.getText().length() > 0)
            {
                float value = Float.parseFloat(adjustValue_EditText.getText().toString());
                float decisecond_value = (float) (value*0.10);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putFloat("cursorDwellTime", value);
                editor.apply();
            }
            if(goToSettingClass){
                Intent intent = new Intent(PointerDwellTimeModule.this,SettingModules.class);
                startActivity(intent);
            }
        });
    }
}