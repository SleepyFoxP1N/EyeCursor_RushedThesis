// Copyright 2016 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.mediapipe.examples.facemesh;

import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_LONG_CLICKED;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;

import com.google.mediapipe.apps.EyeCursor.R;

import java.util.ArrayDeque;
import java.util.Deque;

public class GlobalActionAccessabilityService extends AccessibilityService {
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

    @Override
    public void onCreate() {
        startCursorOverlayService();
    }

    @Override
    public void onDestroy() {
        if (is_owBound) {
            unbindService(owConnection);
            is_owBound = false;
        }
    }

    private int[] getCursorDataFromService() {
        // Call the setData method of the service
        if (is_owBound) return owService.getCursorData();

        int default_cursorData[] = {cursor_CenterPosX,cursor_CenterPosX,0 /*default time*/ ,0 /*not Moving*/};
        return default_cursorData;
    }
    private int[] getCursorDataFromService_EG() {
        // Call the setData method of the service
        if (is_owBound) return owService.getCursorData_EG();

        int default_cursorData[] = {cursor_CenterPosX,cursor_CenterPosX,0 /*default time*/ ,0 /*not Moving*/};
        return default_cursorData;
    }

    private int[] getMouthData(){
        if (is_owBound) return owService.getMouthData();
        int defaultData[] = {0,0,0}; // TODO: CHANGE THIS INTO ACTIONBAR DEFAULT POSITION
        return defaultData;
    }

    private void startCursorOverlayService(){
        Intent intent = new Intent(this, OverlayWidgetsService.class);
        bindService(intent, owConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     *  ------------------------------------------------ GLOBAL ACTION BAR
     */

    float screenHeight = 0, screenWidth = 0;
    int counter = 0, maxCount = 3;
    private int mouth_TopPos = 0, mouth_BottomPos = 0, mouthIs = 0;
    private int cursor_PosX = 0, cursor_PosY = 0, cursor_PosZ = 0;
    private int nose_PosX = 0, nose_PosY = 0, nose_PosZ = 0;
    private int cursor_CenterPosX = 500, getCursor_CenterPosY = 1000;
    private float cursorDwellTime = 4, dwellTimeCount = 0;
    private int cursorWidth = 0, cursorHeight = 0;
    private int cursor_IsMoving = 0;
    private int isFaceOnCam = 1;
    private boolean  isStopUIShowed = true;
    private String[] progressBarWidth = {
            "", // 0 second
            "aa",
            "aabb",
            "aabbcc",
            "aabbccdd", // 4 seconds,
            "aabbccddee", // 5 seconds
            "aabbccddeeff" ,
            "aabbccddeeffgg", // 7 seconds dwell time
            "aabbccddeeffgghh",
            "aabbccddeeffgghhii"
    };

    int viewSelectedX = 5; // stopOrPlayButton
    int viewSelectedY = 0; // notifier eyeGaze/actionBar
    private void updateWidget(){
        final Handler handler = new Handler();
        Runnable updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                if(owService.getOTP_ForCallibrationUI()){ // set isFace false to open the calibration once
                    owService.setIsFaceOnCam(0);
                }
                SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
                Boolean GS = sharedPreferences.getBoolean("globalService", false);
                if(GS){
                    mFloatingView.setVisibility(View.VISIBLE);
                    // -- checks eyeGazeNotifier
                    /*if(eyeGazeNotifier.getVisibility() == View.VISIBLE)
                        owService.setEyeGazeNotifier(true);
                    else
                        owService.setEyeGazeNotifier(false);*/
                    // -- get face value if there is
                    isFaceOnCam = owService.getIsFaceOnCam();
                    // -- Nose Position
                    int[] nosePos = owService.getNosePos();
                    nose_PosX = nosePos[0];
                    nose_PosY = nosePos[1];
                    nose_PosZ = nosePos[2];
                    // -- Cursor Position
                    cursorDwellTime = sharedPreferences.getFloat("cursorDwellTime", 3.0f);
                    // shorten the dwell time for actionBar if mouth is open
                    if(mouthIs == 1) cursorDwellTime = 2.0f; // 2 seconds

                    /*if(eyeGazeNotifier.getVisibility() == View.VISIBLE) {
                        int[] cursorData = getCursorDataFromService_EG();
                        cursor_PosX = cursorData[0];
                        cursor_PosY = cursorData[1];
                        cursor_PosZ = cursorData[2];
                        owService.setEyeGazeNotifier(true);*/
                    int[] cursorData = getCursorDataFromService();
                    cursor_PosX = cursorData[0];
                    cursor_PosY = cursorData[1];
                    cursor_PosZ = cursorData[2];
                    cursor_IsMoving = cursorData[3];
                    owService.setEyeGazeNotifier(false);

                    int[] mouthData = getMouthData();
                    mouth_TopPos = mouthData[0];
                    mouth_BottomPos = mouthData[1];
                    mouthIs = mouthData[2];

                    if(cursor_IsMoving == 0 && nosePosUI.getVisibility() == View.INVISIBLE) {
                        // -- Click time and making the click notifier visible
                        if (dwellTimeCount >= 0) {
                            dwellTimeCount = (float) (dwellTimeCount - 0.1);
                        } else {
                            //performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
                            if(mouthIs == 1){
                                //if (actionBarNotifier.getVisibility() == View.VISIBLE) {
                                if(viewSelectedX>= 1 && viewSelectedX <= 4) // swipe buttons from 1 to 5
                                    configureSwipeButton(viewSelectedX);
                                if(viewSelectedX == 5)
                                    configureStopOrPlayButton(viewSelectedX);
                                if(viewSelectedX == 6)
                                    startSettingsWidget();
                                if(viewSelectedX == 0)
                                    startLongPress();
                                //}
                            }
                            else if (isStopUIShowed) {
                                click();
                            }
                            dwellTimeCount = cursorDwellTime;
                        }
                        // -- progressBar adjustment
                        String width = updateProgressBar(dwellTimeCount);
                        progressBar.setText(width);

                        // if stop is visible means it should be clicking
                        if(stopSelected.getVisibility() == View.INVISIBLE)
                            progressLayout.setVisibility(View.VISIBLE);
                        else
                            progressLayout.setVisibility(View.INVISIBLE);

                        // -- Adjustment of clickNotifier
                        if (dwellTimeCount < cursorDwellTime) {
                            clickNotifier.setVisibility(View.INVISIBLE);
                            counter = 0;
                        } else if (counter < maxCount && isStopUIShowed) { // -- Red notifier visibility for 0.30 seconds.
                            counter = counter + 1;
                            clickNotifier.setVisibility(View.VISIBLE);
                        }
                    }
                    else {
                        progressLayout.setVisibility(View.INVISIBLE);
                        dwellTimeCount = cursorDwellTime;
                        counter = 0;
                        progressBar.setText("");
                    /*if(mouthIs == 1){
                        if(viewSelectedY == 0)
                            notifyEyeGaze();
                        if(viewSelectedY == 1)
                            notifyActionBar();
                    }*/
                    }

                    // remove cursor when callibrating
                    if(owService.getOTP_ForCallibrationUI()) // remove cursor while OTP is not done
                        removeCursorFromScreen();

                    // Hide progressBar when stop is hidden
                    if(owService.getmLayout().getVisibility() == View.VISIBLE || isStopUIShowed == false) {
                        progressLayout.setVisibility(View.INVISIBLE);
                        removeCursorFromScreen();
                    }else {
                        progressLayout.setVisibility(View.VISIBLE);
                    }

                    // Show progressBar when mouth is open
                    if(mouthIs == 1)
                        progressLayout.setVisibility(View.VISIBLE);

                    resizeCursor();
                    faceNotOnScreen();
                    isFaceCentered();
                    selectButtonInActionBar(nose_PosX,30);
                    //selectNotifier(nose_PosY,150);

                    cursorRL.setX(cursor_PosX);
                    cursorRL.setY(cursor_PosY);
                    //Update Text
                    viewer.setVisibility(View.INVISIBLE);
                    /*viewer.setText(
                            "x:"+Integer.toString(cursor_PosX)+"," +
                                    "y:"+Integer.toString(cursor_PosY)+"," +
                                    "z:"+Integer.toString(cursor_PosZ)+", "+
                                    "M:"+Integer.toString(cursor_IsMoving)
                    );
                    if(cursor_PosZ>40 && cursor_PosZ<65){
                        viewer.setTextColor(Color.parseColor("#00FF00"));
                    }else{
                        viewer.setTextColor(Color.parseColor("#FF0000"));
                    }*/
                    windowManager.updateViewLayout(mFloatingView, layoutParams);
                    handler.postDelayed(this, 100); // Schedule the code to run again after 0.1 second
                }
                else{
                    mFloatingView.setVisibility(View.INVISIBLE);
                }
            }
        };
        handler.post(updateTimeRunnable);
    }

    private void isFaceCentered(){
        ShapeDrawable rectShapeDrawable = new ShapeDrawable(); // pre defined class

        // get paint
        Paint paint = rectShapeDrawable.getPaint();

        // set border color, stroke and stroke width when mouth is opened
        // this is for eye gaze alerting if the face is centered
        if(mouthIs == 1 && owService.getmLayout().getVisibility() == View.INVISIBLE){
            actionBarLayout.setVisibility(View.VISIBLE);
            if(owService.getIsFaceOnCenterScreen() == 1)
                paint.setColor(Color.GREEN);
            else
                paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(30); // you can change the value of 5
            mFloatingView.findViewById(R.id.rootLayout).setBackgroundDrawable(rectShapeDrawable);
        }else{
            actionBarLayout.setVisibility(View.INVISIBLE);
            mFloatingView.findViewById(R.id.rootLayout).setBackgroundDrawable(null);

        }
    }

    private void selectNotifier(int pos, int threshold){
        if(mouthIs == 1 && isFaceOnCam != 0){ // Cursor on actionBar if mouth is open = true(1)
            removeCursorFromScreen();
            viewSelectedY = selectActionBarOrEyeGaze(pos, threshold);
            modifySelectedViewY(viewSelectedY);
        }else{
            if(notifiers[viewSelectedY].getVisibility() == View.VISIBLE)
                notifiers[viewSelectedY].setVisibility(View.INVISIBLE);
        }
    }

    private void selectButtonInActionBar(int pos, int threshold){
        if(mouthIs == 1 && isFaceOnCam != 0 && owService.getmLayout().getVisibility() == View.INVISIBLE){ // Cursor on actionBar if mouth is open = true(1)
            viewSelectedX = getPositionOnActionBar(pos, threshold);
            modifySelectedViewX(viewSelectedX);
        }else{
            if(actionBar_Play[viewSelectedX].getVisibility() == View.VISIBLE)
                actionBar_Play[viewSelectedX].setVisibility(View.INVISIBLE);
            if(actionBar_Stop[viewSelectedX].getVisibility() == View.VISIBLE)
                actionBar_Stop[viewSelectedX].setVisibility(View.INVISIBLE);
        }
    }
    private void faceNotOnScreen(){
        if(!owService.getOTP_ForCallibrationUI()){
            View mLayout = owService.getmLayout(); // mLayout from OverlayWidgetsService
            if(isFaceOnCam == 0 || mLayout.getVisibility() == View.VISIBLE){
                removeCursorFromScreen();
                nosePosUI.setVisibility(View.VISIBLE);
            }
            if(nosePosUI.getVisibility() == View.VISIBLE){
                removeCursorFromScreen();
                nosePosUI.setX(nose_PosX-(nosePosUI.getWidth()/2));
                nosePosUI.setY(nose_PosY-(nosePosUI.getHeight()/2)-(screenHeight*0.25f));
            }
            // if done hide the nosePose Red UI
            if(mLayout.getVisibility() == View.INVISIBLE){
                nosePosUI.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void removeCursorFromScreen(){
        cursor_PosX = (int) screenWidth*3; cursor_PosY = (int) screenHeight*3;
    }

    // -- Use this function to change the selection if actionBar or eyeGaze
    // -- Return the placement number
    private float initial_nosePosY = -1, range_nosePosY = 0;
    int posY_mouthSelecting = 1; // position on actionbar
    private int selectActionBarOrEyeGaze(float nosePosY, float threshold){
        if (initial_nosePosY == -1){
            initial_nosePosY = nosePosY;
            return posY_mouthSelecting;
        }

        range_nosePosY = Math.abs(initial_nosePosY - nosePosY);
        if (range_nosePosY > threshold){
            if(nosePosY > initial_nosePosY)
                posY_mouthSelecting++;
            else
                posY_mouthSelecting--;
            initial_nosePosY = nosePosY;
        }

        int length = notifiers.length-1;
        if(posY_mouthSelecting>length)
            posY_mouthSelecting = 0;
        if(posY_mouthSelecting<0)
            posY_mouthSelecting = length;

        return posY_mouthSelecting;
    }

    // -- Use this function to change the selection in actionbar
    // -- Return the placement number of button
    private float initial_nosePosX = -1, range_nosePosX = 0;
    int posX_mouthSelecting = 6; // position on play/pause click dwelltime
    private int getPositionOnActionBar(float nosePosX, float threshold) {
        if (initial_nosePosX == -1){
            initial_nosePosX = nosePosX;
            return posX_mouthSelecting;
        }

        range_nosePosX = Math.abs(initial_nosePosX - nosePosX);
        if (range_nosePosX > threshold){
            if(nosePosX > initial_nosePosX)
                posX_mouthSelecting++;
            else
                posX_mouthSelecting--;
            initial_nosePosX = nosePosX;
        }

        int length = actionBar_Play.length-1;
        if(posX_mouthSelecting>length)
            posX_mouthSelecting = 0;
        if(posX_mouthSelecting<0)
            posX_mouthSelecting = length;

        return posX_mouthSelecting;
    }

    int initialYPos_selected = 999;
    private void modifySelectedViewY(int pos){
        //magnifyView(pos, false);
        if(initialYPos_selected != pos) {
            if(initialYPos_selected!=999) {
                notifiers[initialYPos_selected].setVisibility(View.INVISIBLE);
            }
            notifiers[pos].setVisibility(View.VISIBLE);
            initialYPos_selected = pos;
        }
    }

    int initialPos_selected = 1;
    private void modifySelectedViewX(int pos){
        //magnifyView(pos, false);
        if(initialPos_selected != pos) {
            actionBar_Stop[initialPos_selected].setVisibility(View.INVISIBLE);
            actionBar_Play[initialPos_selected].setVisibility(View.INVISIBLE);
        }
        if(isStopUIShowed){
            actionBar_Play[pos].setVisibility(View.INVISIBLE);
            actionBar_Stop[pos].setVisibility(View.VISIBLE);
        }else{
            actionBar_Play[pos].setVisibility(View.VISIBLE);
            actionBar_Stop[pos].setVisibility(View.INVISIBLE);
        }

        initialPos_selected = pos;
    }

    private void magnifyView(int pos, Boolean isDone){
        if(initialPos_selected == pos  && !isDone)
            return;

        ViewGroup.LayoutParams params = actionBar_Play[pos-1].getLayoutParams();
        int initialWidth = params.width;
        int initialHeight = params.height;
        int magnifiedWidth, magnifiedHeight;

        // remove magnify to past selected view
        if(isDone){ // resize back
            magnifiedWidth = initialWidth / 2;
            magnifiedHeight = initialHeight / 2;
        }else{ // enlarge to magnify
            magnifiedWidth = initialWidth * 2;
            magnifiedHeight = initialHeight * 2;
        }

        if(initialPos_selected != 999 && !isDone)
            magnifyView(initialPos_selected, true);

        params.width = magnifiedWidth;
        params.height = magnifiedHeight;
        actionBar_Play[pos-1].setLayoutParams(params);
    }

    private int[] getCenterPosition(View button){
        int[] location = new int[2];
        button.getLocationOnScreen(location);
        int x = location[0] + button.getWidth() / 2;
        int y = location[1] + button.getHeight() / 2;

        return new int[]{x,y};
    }

    private String updateProgressBar(float counter){
        int index = (int) counter;
        String width;
        switch(index){
            case 10:
                width = progressBarWidth[index];
                break;
            case 9:
                width = progressBarWidth[index];
                break;
            case 8:
                width = progressBarWidth[index];
                break;
            case 7:
                width = progressBarWidth[index];
                break;
            case 6:
                width = progressBarWidth[index];
                break;
            case 5:
                width = progressBarWidth[index];
                break;
            case 4:
                width = progressBarWidth[index];
                break;
            case 3:
                width = progressBarWidth[index];
                break;
            case 2:
                width = progressBarWidth[index];
                break;
            case 1:
                width = progressBarWidth[index];
                break;
            default:
                width = progressBarWidth[index];
        }

        return width;
    }

    private void click() {
        int[] location = new int[2];
        cursorRL.getLocationOnScreen(location);
        int posX_View = location[0];
        int posY_View = location[1];

        Log.d( "CLICK: ",String.format("x=%d, y=%d",posX_View,posY_View));
        Path path = new Path();
        path.moveTo((float) posX_View, (float) posY_View);
        GestureDescription.Builder builder = null;
        GestureDescription gestureDescription = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder = new GestureDescription.Builder();
            gestureDescription = builder
                    .addStroke(new GestureDescription.StrokeDescription(path, 10, 10))
                    .build();
            dispatchGesture(gestureDescription, null, null);
        }
    }


    /*private void cursorClick() {
        Button powerButton = mFloatingView.findViewById(R.id.swipeButton);
        powerButton.setOnClickListener(view -> {
            int[] cursorPositions = getCursorDataFromService();
            posX = cursorPositions[0];
            posY = cursorPositions[1];
            //performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
            click();
        });
    }

    private void configureVolumeButton() {
        Button volumeUpButton = (Button) mFloatingView.findViewById(R.id.swipeButton);
        volumeUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            }
        });
    }

    private AccessibilityNodeInfo findScrollableNode(AccessibilityNodeInfo root) {
        Deque<AccessibilityNodeInfo> deque = new ArrayDeque<>();
        deque.add(root);
        while (!deque.isEmpty()) {
            AccessibilityNodeInfo node = deque.removeFirst();
            if (node.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)) {
                return node;
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                deque.addLast(node.getChild(i));
            }
        }
        return null;
    }

    private void configureScrollButton() {
        Button scrollButton = (Button) mFloatingView.findViewById(R.id.swipeButton);
        scrollButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AccessibilityNodeInfo scrollable = findScrollableNode(getRootInActiveWindow());
                if (scrollable != null) {
                    scrollable.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.getId());
                }
            }
        });
    }

    */

    private void startLongPress(){
        int[] location = new int[2];
        cursorRL.getLocationOnScreen(location);
        int posX_View = location[0];
        int posY_View = location[1];

        Log.d( "LONG PRESS: ",String.format("x=%d, y=%d",posX_View,posY_View));
        Path path = new Path();
        path.moveTo((float) posX_View, (float) posY_View);
        GestureDescription.Builder builder = null;
        GestureDescription gestureDescription = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder = new GestureDescription.Builder();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                gestureDescription = builder
                        .addStroke(new GestureDescription.StrokeDescription(path, 0, 1, true))
                        .build();
            }
            dispatchGesture(gestureDescription, null, null);
        }
    }

    private void startSettingsWidget(){
        View mSettingsWidget = owService.getSettingsWidgetView();
        if(mSettingsWidget.getVisibility() == View.INVISIBLE)
            mSettingsWidget.setVisibility(View.VISIBLE);
    }
    /*private void notifyEyeGaze(){
        owService.setEyeGazeNotifier(true);
        if(eyeGazeNotifier.getVisibility() == View.INVISIBLE)
            eyeGazeNotifier.setVisibility(View.VISIBLE);
        actionBarNotifier.setVisibility(View.INVISIBLE);
    }
    private void notifyActionBar(){
        owService.setEyeGazeNotifier(false);
        if(actionBarNotifier.getVisibility() == View.INVISIBLE)
            actionBarNotifier.setVisibility(View.VISIBLE);
        eyeGazeNotifier.setVisibility(View.INVISIBLE);
    }*/

    private void configureStopOrPlayButton(int pos){
        isStopUIShowed = !isStopUIShowed;
        if(isStopUIShowed){
            actionBar_Play[pos].setVisibility(View.INVISIBLE);
            actionBar_Stop[pos].setVisibility(View.VISIBLE);
        }else{
            actionBar_Play[pos].setVisibility(View.VISIBLE);
            actionBar_Stop[pos].setVisibility(View.INVISIBLE);
        }
    }

    int initial_X = 0, initial_Y = 0;
    int goTo_X  = 0, goTo_Y = 0;
    String swipe;
    private void configureSwipeButton(int pos) {
        switch(pos){
            case 1: swipe = "downSwipe"; break;
            case 2: swipe = "upSwipe"; break;
            case 3: swipe = "rightSwipe"; break;
            case 4: swipe = "leftSwipe"; break;
        }

        switch(swipe){
            case "upSwipe":
                initial_X = (int)(screenWidth * 0.50);
                initial_Y = (int)(screenHeight * 0.35);
                goTo_X = initial_X;
                goTo_Y = (int)(screenHeight * 0.75);
                break;
            case "downSwipe":
                initial_X = (int)(screenWidth * 0.50);
                initial_Y = (int)(screenHeight * 0.75);
                goTo_X = initial_X;
                goTo_Y = (int)(screenHeight * 0.35);
                break;
            case "rightSwipe":
                initial_X = (int)(screenWidth * 0.980);
                initial_Y = (int)(screenHeight * 0.50);
                goTo_X = (int)(screenWidth * 0.20);
                goTo_Y = initial_Y;
                break;
            case "leftSwipe":
                initial_X = (int)(screenWidth * 0.20);
                initial_Y = (int)(screenHeight * 0.50);
                goTo_X = (int)(screenWidth * 0.80);
                goTo_Y = initial_Y;
                break;
        }

        Path swipePath = new Path();
        swipePath.moveTo(initial_X, initial_Y);
        swipePath.lineTo(goTo_X, goTo_Y);
        GestureDescription.Builder gestureBuilder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 300));
            dispatchGesture(gestureBuilder.build(), null, null);
        }
    }

    private void resizeCursor(){
        ImageView cursor = mFloatingView.findViewById(R.id.cursor_widget);
        ViewGroup.LayoutParams params = cursor.getLayoutParams();
        int[] cursorSize = owService.getCursorSize();
        cursorWidth = cursorSize[0];
        cursorHeight = cursorSize[1];
        if(cursorWidth==0 && cursorHeight==0)
        {
            cursorWidth = 63*2;
            cursorHeight = 93*2;
        }
        params.width = cursorWidth;
        params.height = cursorHeight;
        cursor.setLayoutParams(params);
    }


    /**
     *  ------------------------------------------------ Widget codes
     */
    int LAYOUT_TYPE;
    View mFloatingView, mMainActionBarView;
    RelativeLayout cursorRL, nosePosUI, eyeGazeNotifier, actionBarLayout;
    RelativeLayout[] notifiers;
    LinearLayout progressLayout;
    TextView viewer, progressBar, clickNotifier;
    ImageView
            playSelected,
                    play_swipeSelected, play_upSwipeSelected, play_downSwipeSelected,
                    play_rightSwipeSelected, play_leftSwipeSelected, play_settingSelected,
            stopSelected,
                    stop_swipeSelected, stop_upSwipeSelected, stop_downSwipeSelected,
                    stop_rightSwipeSelected, stop_leftSwipeSelected, stop_settingSelected;

    ImageView[] actionBar_Play, actionBar_Stop;

    LinearLayout mainActionBarLayout;
    ImageButton goBackButton, goHomeButton;

    WindowManager windowManager;
    WindowManager.LayoutParams layoutParams, layoutParams_mainAction;

    @Override
    protected void onServiceConnected() {
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O)
            LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        else
            LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_PHONE;
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_widgets, null);
        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                LAYOUT_TYPE,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        // Initial position
        layoutParams.y = 0;
        layoutParams.x = 0;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(mFloatingView, layoutParams);

        // Create an overlay and display the action bar
    /*wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
    lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
    lp.format = PixelFormat.TRANSLUCENT;
    lp.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
    lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
    lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
    lp.gravity = Gravity.BOTTOM;

    mLayout = LayoutInflater.from(this).inflate(R.layout.layout_widgets, null);
    wm.addView(mLayout, lp)*/;
    /*cursorClick();
    configureVolumeButton();
    configureScrollButton();
    configureSwipeButton();*/

        // -- nosePos UI on screen
        nosePosUI = mFloatingView.findViewById(R.id.nosePosUI);

        // -- Getting screen width and height
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
        // -- set screenSize to widgetService
        owService.setScreenSize(screenWidth, screenHeight);

        // -- progress Layout
        viewer = mFloatingView.findViewById(R.id.textview_widget);
        progressBar = mFloatingView.findViewById(R.id.progressBar);
        clickNotifier = mFloatingView.findViewById(R.id.clickNotifier);
        cursorRL = mFloatingView.findViewById(R.id.cursor_layout);
        progressLayout = mFloatingView.findViewById(R.id.progressLayout);

        // -- Action Bar Buttons
        playSelected = mFloatingView.findViewById(R.id.playSelected);
        play_swipeSelected = mFloatingView.findViewById(R.id.play_swipeSelected);
        play_upSwipeSelected = mFloatingView.findViewById(R.id.play_swipeUpSelected);
        play_downSwipeSelected = mFloatingView.findViewById(R.id.play_swipeDownSelected);
        play_rightSwipeSelected = mFloatingView.findViewById(R.id.play_swipeRightSelected);
        play_leftSwipeSelected = mFloatingView.findViewById(R.id.play_swipeLeftSelected);
        play_settingSelected = mFloatingView.findViewById(R.id.play_cursorSetting);
        actionBar_Play = new ImageView[]{
                play_swipeSelected, play_upSwipeSelected, play_downSwipeSelected, play_rightSwipeSelected,
                play_leftSwipeSelected, playSelected, play_settingSelected
        };
        stopSelected = mFloatingView.findViewById(R.id.stopSelected);
        stop_swipeSelected = mFloatingView.findViewById(R.id.stop_swipeSelected);
        stop_upSwipeSelected = mFloatingView.findViewById(R.id.stop_swipeUpSelected);
        stop_downSwipeSelected = mFloatingView.findViewById(R.id.stop_swipeDownSelected);
        stop_rightSwipeSelected = mFloatingView.findViewById(R.id.stop_swipeRightSelected);
        stop_leftSwipeSelected = mFloatingView.findViewById(R.id.stop_swipeLeftSelected);
        stop_settingSelected = mFloatingView.findViewById(R.id.stop_cursorSetting);
        actionBar_Stop = new ImageView[]{
                stop_swipeSelected, stop_upSwipeSelected, stop_downSwipeSelected, stop_rightSwipeSelected,
                stop_leftSwipeSelected, stopSelected, stop_settingSelected
        };

        //eyeGazeNotifier = mFloatingView.findViewById(R.id.eyeGazeSelected);
        actionBarLayout = mFloatingView.findViewById(R.id.actionBarLayout);
        //notifiers = new RelativeLayout[]{eyeGazeNotifier, actionBarNotifier};

        updateWidget();

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
}
