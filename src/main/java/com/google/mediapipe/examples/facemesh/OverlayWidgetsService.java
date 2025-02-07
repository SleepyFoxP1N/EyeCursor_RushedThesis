package com.google.mediapipe.examples.facemesh;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.mediapipe.apps.EyeCursor.R;

import java.security.SecureRandom;
import java.util.Random;

public class OverlayWidgetsService extends Service {

    /**
     *  ------------------------------------------------ BINDING
     */

    private final IBinder binder = (IBinder) new MyBinder();
    private float screenHeight = 0, screenWidth = 0;
    private int cursor_PosX = 0, cursor_PosY = 0, cursor_PosZ = 0;
    private int cursor_PosX_EG = 0, cursor_PosY_EG = 0, cursor_PosZ_EG = 0;
    private int cursor_CenterPosX = 500, cursor_CenterPosY = 1000;
    private int cursorWidth = 0, cursorHeight = 0;
    private int cursor_IsMoving = 0;
    private int Mouth_TopYPos = 0, Mouth_BottomYPos;
    private int MouthIs = 0, isFaceOnCam = 1, isFaceOnCenterScreen = 0;
    private int Nose_XPos = 0, Nose_YPos = 0, Nose_ZPos = 0;
    private boolean isCalibrationDone = false, EyeGazeNotifer = false;
    private boolean isGlobalServiceStarted = false;
    private boolean OTP_FaceCallibrationUI = false;

    public class MyBinder extends Binder {
        OverlayWidgetsService getService() {
            return OverlayWidgetsService.this;
        }
    }

    public void setOTP_FaceCallibrationUI(boolean bool){
        this.OTP_FaceCallibrationUI = bool;
    }

    public boolean getOTP_ForCallibrationUI(){
        return this.OTP_FaceCallibrationUI;
    }

    public void setIsGlobalServiceStarted(boolean bool){
        this.isGlobalServiceStarted = bool;
    }

    public boolean getIsGlobalServiceStarted(){
        return this.isGlobalServiceStarted;
    }

    public void setMouthData(int mouth_TopYPos, int mouth_BottomYPos, int isMouthOpen){
        this.Mouth_TopYPos = mouth_TopYPos;
        this.Mouth_BottomYPos = mouth_BottomYPos;
        this.MouthIs = isMouthOpen;
    }

    public void setCursorData(int posX, int posY, int posZ, int isMoving) {
        this.cursor_PosX = posX;
        this.cursor_PosY = posY;
        this.cursor_PosZ = posZ;
        this.cursor_IsMoving = isMoving;
    }

    public void setCursorData_EG(int posX, int posY, int posZ) {
        this.cursor_PosX_EG = posX;
        this.cursor_PosY_EG = posY;
        this.cursor_PosZ_EG = posZ;
    }

    public void setCursorCenterPos(int posX, int posY){
        this.cursor_CenterPosX = posX;
        this.cursor_CenterPosY = posY;
    }

    public int[] getCursorCenterPos(int posX, int posY){
        this.cursor_CenterPosX = posX;
        this.cursor_CenterPosY = posY;
        int cursorCenter[] = {this.cursor_CenterPosX,this.cursor_CenterPosY};
        return cursorCenter;
    }

    public int[] getCursorData() {
        int cursorData[] = {this.cursor_PosX, this.cursor_PosY, this.cursor_PosZ, this.cursor_IsMoving};
        return cursorData;
    }

    public int[] getCursorData_EG() {
        int cursorData[] = {this.cursor_PosX_EG, this.cursor_PosY_EG, this.cursor_PosZ_EG};
        return cursorData;
    }

    public int[] getMouthData(){
        int mouthData[] = {this.Mouth_TopYPos, this.Mouth_BottomYPos, this.MouthIs};
        return mouthData;
    }

    public void setCursorSize(int width, int height) {
        this.cursorWidth = width;
        this.cursorHeight = height;
    }
    public int[] getCursorSize() {
        int[] cursorSize = {this.cursorWidth,this.cursorHeight};
        return cursorSize;
    }

    public void setScreenSize(float width, float height){
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public float[] getScreenSize(){
        return new float[]{this.screenWidth,this.screenHeight};
    }

    public void setIsFaceOnCam(int faceOnCam){
        this.isFaceOnCam = faceOnCam;
    }
    public int getIsFaceOnCam(){
        return this.isFaceOnCam;
    }
    public void setIsFaceOnCenterScreen(int faceOnCenterScreen){
        this.isFaceOnCenterScreen = faceOnCenterScreen;
    }
    public int getIsFaceOnCenterScreen(){
        return this.isFaceOnCenterScreen;
    }

    public void setNosePos(float posX, float posY, float posZ){
        this.Nose_XPos = (int) posX;
        this.Nose_YPos = (int) posY;
        this.Nose_ZPos = (int) posZ;
    }

    public int[] getNosePos(){
        return new int[]{this.Nose_XPos, this.Nose_YPos, this.cursor_PosZ};
    }

    public View getmLayout(){
        return this.mLayout;
    }

    public void setIfCalibrationDone(Boolean calibrationDone){
        this.isCalibrationDone = calibrationDone;
    }

    public boolean getIfCalibrationDone(){
        return this.isCalibrationDone;
    }

    public View getSettingsWidgetView(){
        return this.mSettingsFloatingView;
    }

    public boolean getEyeGazeNotifier(){
        return this.EyeGazeNotifer;
    }
    public void setEyeGazeNotifier(Boolean eyeGazeNotifer){
        this.EyeGazeNotifer = eyeGazeNotifer;
    }

    public void updateWidget() {
        final Handler handler = new Handler();
        Runnable updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
                Boolean WS = sharedPreferences.getBoolean("widgetsService", false);
                if(WS){
                    OTP_countDown();
                    faceChecker();

                    windowManager.updateViewLayout(mLayout, layoutParams);
                    handler.postDelayed(this, 10); // Schedule the code to run again after 0.01 second
                }
            }
        };
        handler.post(updateTimeRunnable);
    }

    private void makeAllInvisible(){
        mSettingsFloatingView.setVisibility(View.INVISIBLE);
        mAccelerationModuleFloatingView.setVisibility(View.INVISIBLE);
        mDwellTimeModuleFloatingView.setVisibility(View.INVISIBLE);
        mSizeModuleFloatingView.setVisibility(View.INVISIBLE);
        mSmoothingModuleFloatingView.setVisibility(View.INVISIBLE);
        mSpeedModuleFloatingView.setVisibility(View.INVISIBLE);
        mThresholdFloatingView.setVisibility(View.INVISIBLE);
    }

    private int counterForCountDown = 201;
    private void OTP_countDown(){
        if(this.OTP_FaceCallibrationUI) {
            if (AccessibilityService()) {
                changeParamsToTouchable(false);
                makeAllInvisible();
                changeParamSize(mLayout, initialWidth_LayoutParam, initialHeight_LayoutParam);
                mLayout.setVisibility(View.VISIBLE);
            }


            if (counterForCountDown >= 0 && mLayout.getVisibility() == View.VISIBLE)
                counterForCountDown = counterForCountDown - 1;

            if (counterForCountDown == 200)
                countDown.setText("5");
            else if (counterForCountDown == 150)
                countDown.setText("4");
            else if (counterForCountDown == 100)
                countDown.setText("3");
            else if (counterForCountDown == 50)
                countDown.setText("2");
            else if (counterForCountDown == 1)
                countDown.setText("1");

            if (counterForCountDown < 0) {
                this.OTP_FaceCallibrationUI = false;
                countDown_Layout.setVisibility(View.INVISIBLE);
                centerYourFace_Layout.setVisibility(View.VISIBLE);
            }

        }
    }

    private void faceChecker(){
        if(!this.OTP_FaceCallibrationUI) {
            if (isFaceOnCam == 0) {
                changeParamsToTouchable(false);
                makeAllInvisible();
                changeParamSize(mLayout, initialWidth_LayoutParam, initialHeight_LayoutParam);
                mLayout.setVisibility(View.VISIBLE);
            }

            if (mLayout.getVisibility() == View.VISIBLE) {
                progressCalibration();
            }

            float posX_percentage = (Nose_XPos / screenWidth) * 100;
            float posY_percentage = (Nose_YPos / screenHeight) * 100;
            if (posX_percentage > 40 && posX_percentage < 60 && posY_percentage > 67 && posY_percentage < 83) {
                calibration_Layout.setVisibility(View.VISIBLE);
                centerYourFace_Layout.setVisibility(View.INVISIBLE);
                this.isFaceOnCenterScreen = 1;
            } else {
                calibration_Layout.setVisibility(View.INVISIBLE);
                centerYourFace_Layout.setVisibility(View.VISIBLE);
                this.isFaceOnCenterScreen = 0;
            }
        }
    }

    int counter = 0, maxCount_Progress = 100, waitCounter = 0;
    private void progressCalibration(){
        if(isFaceOnCenterScreen == 1){ // check if face on center
            if(counter<maxCount_Progress) // increment if less than 100
                counter++;
            if(waitCounter>100) // if beyond 100 change the counter to 101 next condition will make it into 100
                counter = 101;
            if(counter > 99)
                counter--; // if counter is now 101 decrease to make into 100
            if(waitCounter <= new SecureRandom().nextInt(120)) { // randomize waiting
                if(counter < 99)
                    counter--; // decrement in order to remove the increment before
                waitCounter++;
            }
            calibrationProgress_Text1.setText("Calibrating: "+counter+"%");
            calibrationProgress_Text2.setText("Calibrating: "+counter+"%");
        }else{
            counter = 0;
            waitCounter = 0;
        }
        if(counter>99){ // once done calibrating do all these
            calibration_Layout.setVisibility(View.INVISIBLE);
            mLayout.setVisibility(View.INVISIBLE);
            centerYourFace_Layout.setVisibility(View.VISIBLE);
            this.isCalibrationDone = true;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    /**
     *  ------------------------------------------------ Settings Widget codes
     */
    View
            mSettingsFloatingView,
            mAccelerationModuleFloatingView,
            mDwellTimeModuleFloatingView,
            mSizeModuleFloatingView,
            mSmoothingModuleFloatingView,
            mSpeedModuleFloatingView,
            mThresholdFloatingView;

    private void changeParamSize(View view, int width, int height){
        layoutParams.width = width;
        layoutParams.height = height;
        windowManager.updateViewLayout(view, layoutParams);
    }

    private void modifySettingsFloatingView(View view){
        view.setBackgroundResource(0);
        LinearLayout mainLayout = view.findViewById(R.id.mainLayout);
        changeParamSize(view, 800, 1200);
        mainLayout.setBackgroundResource(R.drawable.gradientprogress);
        windowManager.updateViewLayout(view, layoutParams);
    }
    private void settingsFloatingView(){
        changeParamsToTouchable(true);
        windowManager.addView(mSettingsFloatingView, layoutParams);
        modifySettingsFloatingView(mSettingsFloatingView);
        mSettingsFloatingView.setVisibility(View.INVISIBLE);

        // assign all
        Button exit_button = mSettingsFloatingView.findViewById(R.id.settingsbackbutton);
        Button speed_button = mSettingsFloatingView.findViewById(R.id.pointerspeedbutton);
        Button smoothing_button = mSettingsFloatingView.findViewById(R.id.pointersmoothingbutton);
        Button size_button = mSettingsFloatingView.findViewById(R.id.pointersizebutton);
        Button threshold_button = mSettingsFloatingView.findViewById(R.id.pointerthresholdbutton);
        Button paccelerationbutton = mSettingsFloatingView.findViewById(R.id.pointeraccelerationbutton);
        Button pdwelltimebutton = mSettingsFloatingView.findViewById(R.id.pointerdwelltimebutton);

        exit_button.setText("X");
        exit_button.setOnClickListener(view -> mSettingsFloatingView.setVisibility(View.INVISIBLE));

        // make the buttons work
        goToModule(speed_button, mSpeedModuleFloatingView, mSettingsFloatingView);
        goToModule(smoothing_button, mSmoothingModuleFloatingView, mSettingsFloatingView);
        goToModule(size_button, mSizeModuleFloatingView, mSettingsFloatingView);
        goToModule(threshold_button, mThresholdFloatingView, mSettingsFloatingView);
        goToModule(paccelerationbutton, mAccelerationModuleFloatingView, mSettingsFloatingView);
        goToModule(pdwelltimebutton, mDwellTimeModuleFloatingView, mSettingsFloatingView);
        windowManager.updateViewLayout(mSettingsFloatingView, layoutParams);
    }

    private void goToModule(Button button, View viewToOpen, View source){
        button.setOnClickListener(view -> {
            changeParamsToTouchable(true);
            makeAllInvisible();
            viewToOpen.setVisibility(View.VISIBLE);
            modifySettingsFloatingView(viewToOpen);
        });
    }

    private void accelerationModuleFloatingView(){
        windowManager.addView(mAccelerationModuleFloatingView, layoutParams);
        mAccelerationModuleFloatingView.setVisibility(View.INVISIBLE);

        // All views
        RadioButton low_RadioButton = mAccelerationModuleFloatingView.findViewById(R.id.paccelowradioButton);
        RadioButton medium_RadioButton = mAccelerationModuleFloatingView.findViewById(R.id.paccemediumradioButton);
        RadioButton high_RadioButton = mAccelerationModuleFloatingView.findViewById(R.id.paccehighradioButton);
        Button saveAcceleration_Button = mAccelerationModuleFloatingView.findViewById(R.id.saveacceleration_btn);
        mAccelerationModuleFloatingView.findViewById(R.id.paccelerationbackbutton).setOnClickListener(view -> {
            mSettingsFloatingView.setVisibility(View.VISIBLE);
            mAccelerationModuleFloatingView.setVisibility(View.INVISIBLE);
        });

        PointerAccelerationModule object = new PointerAccelerationModule();
        //Recover saved Setting
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
        object.setCursorPropertyValueFromSharedPreferences(low_RadioButton, medium_RadioButton, high_RadioButton, sharedPreferences);
        // Buttons Function
        object.Cursor_Low(low_RadioButton, medium_RadioButton, high_RadioButton);
        object.Cursor_Default(low_RadioButton, medium_RadioButton, high_RadioButton);
        object.Cursor_High(low_RadioButton, medium_RadioButton, high_RadioButton);
        object.setCursorProperty(saveAcceleration_Button, false ,sharedPreferences);
        windowManager.updateViewLayout(mAccelerationModuleFloatingView, layoutParams);
    }

    private void dwellTimeModuleFloatingView(){
        windowManager.addView(mDwellTimeModuleFloatingView, layoutParams);
        mDwellTimeModuleFloatingView.setVisibility(View.INVISIBLE);

        // -- views
        EditText adjustValue_EditText = mDwellTimeModuleFloatingView.findViewById(R.id.adjustDwellTime_Value);
        Button dwellTime_Add = mDwellTimeModuleFloatingView.findViewById(R.id.dwellTime_Add);
        Button dwellTime_Minus = mDwellTimeModuleFloatingView.findViewById(R.id.dwellTime_Minus);
        Button saveDwellTime_Value = mDwellTimeModuleFloatingView.findViewById(R.id.saveDwellTime_Value);
        mDwellTimeModuleFloatingView.findViewById(R.id.dwelltimebackbutton).setOnClickListener(view -> {
            mDwellTimeModuleFloatingView.setVisibility(View.INVISIBLE);
            mSettingsFloatingView.setVisibility(View.VISIBLE);
        });

        PointerDwellTimeModule object = new PointerDwellTimeModule();
        //Recover saved Setting
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
        object.setData(sharedPreferences, adjustValue_EditText);

        // Buttons Function
        object.addCursorDwellTime(adjustValue_EditText, dwellTime_Add);
        object.minusCursorDwellTime(adjustValue_EditText, dwellTime_Minus);
        object.limit_EditText(adjustValue_EditText);
        object.save_send_data(adjustValue_EditText, saveDwellTime_Value, false, sharedPreferences);
        windowManager.updateViewLayout(mDwellTimeModuleFloatingView, layoutParams);
    }

    private void sizeModuleFloatingView(){
        windowManager.addView(mSizeModuleFloatingView, layoutParams);
        mSizeModuleFloatingView.setVisibility(View.INVISIBLE);

        RadioButton small_radioButton = mSizeModuleFloatingView.findViewById(R.id.psizesmallradioButton);
        RadioButton default_radioButton = mSizeModuleFloatingView.findViewById(R.id.psizestandardradioButton);
        RadioButton large_radioButton = mSizeModuleFloatingView.findViewById(R.id.psizelargeradioButton);
        Button savedButton = mSizeModuleFloatingView.findViewById(R.id.savedSize_btn);
        mSizeModuleFloatingView.findViewById(R.id.psizebackbutton).setOnClickListener(view -> {
            mSizeModuleFloatingView.setVisibility(View.INVISIBLE);
            mSettingsFloatingView.setVisibility(View.VISIBLE);
        });
        PointerSizeModule object = new PointerSizeModule();
        //Recover saved Setting
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
        object.setCursorSizeValueFromSharedPreferences(small_radioButton, default_radioButton, large_radioButton, sharedPreferences, "cursorSize_RB");
        // Buttons Function
        object.CursorSmall(small_radioButton, default_radioButton, large_radioButton);
        object.CursorDefaultSize(small_radioButton, default_radioButton, large_radioButton);
        object.CursorLarge(small_radioButton, default_radioButton, large_radioButton);
        object.setCursorSize(savedButton,false, sharedPreferences);
        windowManager.updateViewLayout(mSettingsFloatingView, layoutParams);
    }

    private void smoothingModuleFloatingView(){
        windowManager.addView(mSmoothingModuleFloatingView, layoutParams);
        mSmoothingModuleFloatingView.setVisibility(View.INVISIBLE);

        //All views
        RadioButton low_button = mSmoothingModuleFloatingView.findViewById(R.id.psmoothinglowradioButton);
        RadioButton default_button = mSmoothingModuleFloatingView.findViewById(R.id.psmoothingdefaultradioButton);
        RadioButton high_button = mSmoothingModuleFloatingView.findViewById(R.id.psmoothinghighradioButton);
        Button save_button = mSmoothingModuleFloatingView.findViewById(R.id.savedSmoothing_btn);
        mSmoothingModuleFloatingView.findViewById(R.id.smoothingbackbutton).setOnClickListener(view -> {
            mSmoothingModuleFloatingView.setVisibility(View.INVISIBLE);
            mSettingsFloatingView.setVisibility(View.VISIBLE);
        });

        PointerSmoothingModule object = new PointerSmoothingModule();
        //Recover saved Setting
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
        object.setCursorPropertyValueFromSharedPreferences(sharedPreferences, low_button, default_button, high_button);

        // Buttons Function
        object.Cursor_Low(low_button, default_button, high_button);
        object.Cursor_Default(low_button, default_button, high_button);
        object.Cursor_High(low_button, default_button, high_button);
        object.setCursorProperty(save_button,false, sharedPreferences);
        windowManager.updateViewLayout(mSmoothingModuleFloatingView, layoutParams);
    }

    private void speedModuleFloatingView(){
        windowManager.addView(mSpeedModuleFloatingView, layoutParams);
        mSpeedModuleFloatingView.setVisibility(View.INVISIBLE);

        // ------- Functions of this Activity
        PointerSpeedModule object = new PointerSpeedModule();

        // EditText Limiter
        EditText editText_X = mSpeedModuleFloatingView.findViewById(R.id.adjustSpeedValueX);
        EditText editText_Y = mSpeedModuleFloatingView.findViewById(R.id.adjustSpeedValueY);
        object.limit_EditText(editText_X);
        object.limit_EditText(editText_Y);

        // Increaser
        Button speedAddX = mSpeedModuleFloatingView.findViewById(R.id.speedAddX);
        object.addCursorSpeed(editText_X, speedAddX);
        Button speedAddY = mSpeedModuleFloatingView.findViewById(R.id.speedAddY);
        object.addCursorSpeed(editText_Y, speedAddY);

        // Decreaser
        Button speedMinusX = mSpeedModuleFloatingView.findViewById(R.id.speedMinusX);
        object.minusCursorSpeed(editText_X, speedMinusX);
        Button speedMinusY = mSpeedModuleFloatingView.findViewById(R.id.speedMinusY);
        object.minusCursorSpeed(editText_Y, speedMinusY);

        mSpeedModuleFloatingView.findViewById(R.id.speedbackbutton).setOnClickListener(view -> {
            mSpeedModuleFloatingView.setVisibility(View.INVISIBLE);
            mSettingsFloatingView.setVisibility(View.VISIBLE);
        });

        // Save X and Y values data
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
        Button save_Button = mSpeedModuleFloatingView.findViewById(R.id.saveSpeedValue);
        object.save_send_data(save_Button, editText_X, editText_Y, false, sharedPreferences);

        // Receiver Data from the SharedPreferences
        int valueX = sharedPreferences.getInt("cursorSpeedX", 0);
        int valueY = sharedPreferences.getInt("cursorSpeedY", 0);
        editText_X.setText(String.valueOf(valueX));
        editText_Y.setText(String.valueOf(valueY));
        windowManager.updateViewLayout(mSpeedModuleFloatingView, layoutParams);
    }

    private void thresholdModuleFloatingView(){
        windowManager.addView(mThresholdFloatingView, layoutParams);
        mThresholdFloatingView.setVisibility(View.INVISIBLE);

        // all views
        RadioButton low_radioButton = mThresholdFloatingView.findViewById(R.id.pthresholdmlowradioButton);
        RadioButton default_radioButton = mThresholdFloatingView.findViewById(R.id.pthresholddefaultradioButton);
        RadioButton high_radioButton = mThresholdFloatingView.findViewById(R.id.pthresholdhighradioButton);
        Button save_button = mThresholdFloatingView.findViewById(R.id.savethreshold_btn);
        mThresholdFloatingView.findViewById(R.id.pthresholdbackbutton).setOnClickListener(view -> {
            mThresholdFloatingView.setVisibility(View.INVISIBLE);
            mSettingsFloatingView.setVisibility(View.VISIBLE);
        });

        PointerThresholdModule object = new PointerThresholdModule();
        //Recover saved Setting
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
        object.setCursorPropertyValueFromSharedPreferences(sharedPreferences, low_radioButton, default_radioButton, high_radioButton);

        // Buttons Function
        object.Cursor_Low(low_radioButton, default_radioButton, high_radioButton);
        object.Cursor_Default(low_radioButton, default_radioButton, high_radioButton);
        object.Cursor_High(low_radioButton, default_radioButton, high_radioButton);
        object.setCursorProperty(save_button, false, sharedPreferences);
        windowManager.updateViewLayout(mThresholdFloatingView, layoutParams);
    }

    /**
     *  ------------------------------------------------ Widget codes
     */
    int LAYOUT_TYPE;
    View mLayout;
    RelativeLayout countDown_Layout, centerYourFace_Layout, calibration_Layout, goBackLayout;
    Button goBackButton;
    WindowManager windowManager;
    WindowManager.LayoutParams layoutParams;
    int initialWidth_LayoutParam, initialHeight_LayoutParam;
    TextView calibrationProgress_Text1, calibrationProgress_Text2, countDown;

    @SuppressLint("ClickableViewAccessibility")
    public int onStartCommand(Intent intent, int flags, int startId){
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O)
            LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        else
            LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_PHONE;

        mLayout = LayoutInflater.from(this).inflate(R.layout.recalibrate_layout, null);

        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                LAYOUT_TYPE,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        initialWidth_LayoutParam = layoutParams.width;
        initialHeight_LayoutParam = layoutParams.height;

        // Initial position
        //layoutParams.gravity = Gravity.BOTTOM;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(mLayout, layoutParams);
        mLayout.setVisibility(View.INVISIBLE);

        //ImageView tvCursor = mLayout.findViewById(R.id.cursor_widget);
        //TextView viewer = mLayout.findViewById(R.id.textview_widget);

        // -- Calibrations
        countDown_Layout = mLayout.findViewById(R.id.countDownLayout);
        countDown = mLayout.findViewById(R.id.countDown);
        centerYourFace_Layout = mLayout.findViewById(R.id.centerFaceLayout);
        calibration_Layout = mLayout.findViewById(R.id.calibratingLayout);
        calibrationProgress_Text1 = mLayout.findViewById(R.id.calibrationProgress1);
        calibrationProgress_Text2 = mLayout.findViewById(R.id.calibrationProgress2);

        // Inflate Settings FloatingView
        mSettingsFloatingView = LayoutInflater.from(this).inflate(R.layout.activity_setting_modules, null);
        mAccelerationModuleFloatingView = LayoutInflater.from(this).inflate(R.layout.activity_pointer_acceleration_module, null);
        mDwellTimeModuleFloatingView = LayoutInflater.from(this).inflate(R.layout.activity_pointer_dwell_time_module, null);
        mSizeModuleFloatingView = LayoutInflater.from(this).inflate(R.layout.activity_pointer_size_module, null);
        mSmoothingModuleFloatingView = LayoutInflater.from(this).inflate(R.layout.activity_pointer_smoothing_module, null);
        mSpeedModuleFloatingView = LayoutInflater.from(this).inflate(R.layout.activity_pointer_speed_module, null);
        mThresholdFloatingView = LayoutInflater.from(this).inflate(R.layout.activity_pointer_threshold_module, null);
        settingsFloatingView();
        accelerationModuleFloatingView();
        dwellTimeModuleFloatingView();
        sizeModuleFloatingView();
        smoothingModuleFloatingView();
        speedModuleFloatingView();
        thresholdModuleFloatingView();
        updateWidget();

        // global action go back
        /*goBackLayout = mLayout.findViewById(R.id.goBackLayout);
        goBackButton = mLayout.findViewById(R.id.goBackButton);

        changeParamsToTouchable(true);
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            inputMethodManager.dispatchKeyEventFromInputMethod(goBackButton, keyEvent);
        }*/



        /*tvCursor.setOnTouchListener( new View.OnTouchListener(){
            int initialX, initialY;
            float initialTouchX,initialTouchY;
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent){
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        initialX = layoutParams.x;
                        initialY = layoutParams.y;

                        // touch position
                        initialTouchX = motionEvent.getRawX();
                        initialTouchY = motionEvent.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        // calculate the X and Y coordinate of View
                        layoutParams.x = initialX + (int) (motionEvent.getRawX()-initialTouchX);
                        layoutParams.y = initialY + (int) (motionEvent.getRawY()-initialTouchY);

                        viewer.setText("x="+layoutParams.x+", y="+layoutParams.y);
                        // update layout new coordinate
                        windowManager.updateViewLayout(mLayout, layoutParams);

                        return true;

                }
                return false;
            }
        });*/

        return START_STICKY;
    }
    private void changeParamsToTouchable(Boolean bool){
        if(bool){
            layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    LAYOUT_TYPE,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT);
        }else{
            layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    LAYOUT_TYPE,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        /*if (mLayout != null) {
            ViewGroup parentView = (ViewGroup) mLayout.getParent();
            if (parentView != null) {
                parentView.removeView(mLayout);
            }
            mLayout = null;
        }*/
    }

    private boolean AccessibilityService() {
        String accessibilityServices = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return accessibilityServices != null && accessibilityServices.contains(getPackageName());
    }
}



