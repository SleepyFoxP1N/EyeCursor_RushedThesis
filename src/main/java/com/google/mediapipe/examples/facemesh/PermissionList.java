package com.google.mediapipe.examples.facemesh;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mediapipe.apps.EyeCursor.R;

public class PermissionList extends AppCompatActivity {

    /**-----------------------------------------
     *  ----------------------------------------- BINDING
     -----------------------------------------*/
    private OverlayWidgetsService owService;

    private boolean is_owBound = false;

    private ServiceConnection owConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            OverlayWidgetsService.MyBinder owBinder = (OverlayWidgetsService.MyBinder) service;
            owService = owBinder.getService();
            is_owBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            is_owBound = false;
        }
    };

    private CheckBox requestOverlayPermissionButton;
    private CheckBox requestCameraPermissionButton;
    private Button nextPermissionButton;
    //private CheckBox accessibilityServiceButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_permission_list);

        requestOverlayPermissionButton = findViewById(R.id.requestOverlayPermissionButton);
        requestCameraPermissionButton = findViewById(R.id.requestCameraPermissionButton);
        CheckBox acceptEndUserLicenseButton = findViewById(R.id.AcceptenduserButton);
        nextPermissionButton = findViewById(R.id.nextpermission_btn);

        acceptEndUserLicenseButton.setChecked(true);
        acceptEndUserLicenseButton.setClickable(false);
        acceptEndUserLicenseButton.setEnabled(false);
        nextPermissionButton.setEnabled(false);

        requestOverlayPermissionButton.setOnClickListener(v -> {
            if (!OverlayPermission()) {
                requestOverlayPermission();
            } else {
                showToast("Overlay Permission Granted");
                NextButton();
                requestOverlayPermissionButton.setChecked(true);
                requestOverlayPermissionButton.setEnabled(false);
            }
        });

        requestCameraPermissionButton.setOnClickListener(v -> {
            if (!CameraPermission()) {
                requestCameraPermission();
            } else {
                showToast("Camera Permission Granted");
                NextButton();
                requestCameraPermissionButton.setChecked(true);
                requestCameraPermissionButton.setEnabled(false);
            }
        });

        /*accessibilityServiceButton.setOnClickListener(v -> {
            if (!AccessibilityService()) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivityForResult(intent, 125);
            } else {
                showToast("Accessibility Service Permission Granted");
                NextButton();
                accessibilityServiceButton.setChecked(true);
                accessibilityServiceButton.setEnabled(false);
            }
        });*/

        nextPermissionButton.setOnClickListener(view -> {
            permissionIsDone();
            Intent intent = new Intent(PermissionList.this, MainActivity.class);
            nextPermissionButton.setText("Next");
            startActivity(intent);
        });
    }

    public void permissionsNotDone(){
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("permissions", false);
        editor.apply();
    }

    private void permissionIsDone(){
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("permissions", true);
        editor.apply();
    }

    private boolean OverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return false;
    }

    private boolean CameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, 80);
    }

    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 80);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 80) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Camera Permission Granted");
                NextButton();
                requestCameraPermissionButton.setChecked(true);
                requestCameraPermissionButton.setEnabled(false);
            } else {
                permissionsNotDone();
                showToast("Camera Permission Denied");
                requestCameraPermissionButton.setChecked(false);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 80) {
            if (OverlayPermission()) {
                showToast("Overlay Permission Granted");
                NextButton();
                requestOverlayPermissionButton.setChecked(true);
                requestOverlayPermissionButton.setEnabled(false);
            } else {
                permissionsNotDone();
                showToast("Overlay Permission Denied");
                requestOverlayPermissionButton.setChecked(false);
            }
        } /*else if (requestCode == 125) {
            if (AccessibilityService()) {
                showToast("Accessibility Service Permission Granted");
                NextButton();
                accessibilityServiceButton.setChecked(true);
                accessibilityServiceButton.setEnabled(false);
            } else {
                permissionsNotDone();
                showToast("Accessibility Service Permission Denied");
                accessibilityServiceButton.setChecked(false);
            }
        }*/
    }

    private void NextButton() {
        if (OverlayPermission() && CameraPermission()) {
            nextPermissionButton.setText("Next");
            nextPermissionButton.setEnabled(true);
        }
    }

    private boolean AccessibilityService() {
        String accessibilityServices = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return accessibilityServices != null && accessibilityServices.contains(getPackageName());
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

