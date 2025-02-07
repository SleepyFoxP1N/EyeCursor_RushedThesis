//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.google.mediapipe.examples.facemesh;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.google.mediapipe.apps.EyeCursor.R;
import com.google.mediapipe.components.ExternalTextureConverter;
import com.google.mediapipe.components.TextureFrameConsumer;
import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.framework.MediaPipeException;
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

import java.io.IOException;

public class FloatingCamera_Front extends Service {
    int LAYOUT_FLAG;
    WindowManager windowManager;
    WindowManager.LayoutParams params;
    View vG;
    Context context;
    Camera mCamera;
    PreviewFront mPreview;
    private static final String TAG = "Floating_Camera";

    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        else{
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }
        this.windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        int H = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 320, this.getResources().getDisplayMetrics());
        int w = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 220, this.getResources().getDisplayMetrics());
        params = new WindowManager.LayoutParams(
                w,
                H,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        // Initial position
        params.gravity = Gravity.TOP|Gravity.RIGHT;
        params.x = 0;
        params.y = 100;
        LayoutInflater layoutInflater = (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.vG = (ViewGroup)layoutInflater.inflate(R.layout.full_front, (ViewGroup)null, false);
        //this.vG = LayoutInflater.from(this).inflate(R.layout.full_front, null);
        this.windowManager.addView(vG, params);
        this.mCamera = getCameraInstance();
        //onStart_SetupLiveDemoUiComponents(this.mCamera);
        this.mPreview = new PreviewFront(this, this.mCamera);
        FrameLayout preview = this.vG.findViewById(R.id.previewFrame);
        preview.addView(this.mPreview);

        try {
            this.vG.setOnTouchListener(new View.OnTouchListener() {
                private WindowManager.LayoutParams paramsF;
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                {
                    this.paramsF = params;
                }

                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case 0:
                            System.out.println("Touch ACTION_DOWN");
                            this.initialX = this.paramsF.x;
                            this.initialY = this.paramsF.y;
                            this.initialTouchX = event.getRawX();
                            this.initialTouchY = event.getRawY();
                            break;
                        case 1:
                            System.out.println("Touch ACTION_UP");
                            break;
                        case 2:
                            System.out.println("Touch ACTION_MOVE");
                            this.paramsF.x = this.initialX + (int)(event.getRawX() - this.initialTouchX);
                            this.paramsF.y = this.initialY + (int)(event.getRawY() - this.initialTouchY);
                            FloatingCamera_Front.this.windowManager.updateViewLayout(FloatingCamera_Front.this.vG, this.paramsF);
                    }

                    return false;
                }
            });
        } catch (Exception var10) {
            System.out.println(var10);
        }

        return START_STICKY;
    }

    public void onCreate() {
        super.onCreate();
        this.context = this.getApplicationContext();
        System.out.println("FloatingCamera");
    }

    public void onDestroy() {
        super.onDestroy();
        if (this.vG != null) {
            this.windowManager.removeView(this.vG);
            //this.stopService(new Intent(this.getApplicationContext(), FloatingCamera_Back.class));
            this.releaseCamera();
            System.out.println("Channel FloatingCamera Reset");
        }

    }

    public Camera getCameraInstance() {
        Camera c = null;

        try {
            c = Camera.open(1);
            //onStart_SetupLiveDemoUiComponents(c);
        } catch (Exception var2) {
            var2.printStackTrace();
        }

        return c;
    }

    private void releaseCamera() {
        if (this.mCamera != null) {
            this.mCamera.stopPreview();
            this.mCamera.release();
            this.mCamera = null;
        }
    }


    /**
     *  ------------------------------------------------ FACEMESH
     */

    private FaceMesh facemesh;
    // Run the pipeline and the model inference on GPU or CPU.
    private static final boolean RUN_ON_GPU = true;

    private SolutionGlSurfaceView<FaceMeshResult> glSurfaceView;

    private Button cursor;

    //* Sets up the UI components for the live demo with camera input.
    private void onStart_SetupLiveDemoUiComponents(Camera camera) {

        //stopCurrentPipeline();
        setupStreamingModePipeline(camera);
        //startCameraX();
        //});
    }

    //* Sets up core workflow for streaming mode.
    private void setupStreamingModePipeline(Camera camera) {
        cursor = this.vG.findViewById(R.id.capture);

        // Initializes a new MediaPipe Face Mesh solution instance in the streaming mode.
        facemesh =
                new FaceMesh(
                        getApplicationContext(),
                        FaceMeshOptions.builder()
                                .setStaticImageMode(false)
                                .setRefineLandmarks(true)
                                .setRunOnGpu(RUN_ON_GPU)
                                .build());
        facemesh.setErrorListener((message, e) -> Log.e(TAG, "MediaPipe Face Mesh error:" + message));

        setNewFrameListener(textureFrame -> facemesh.send(textureFrame));

        // Initializes a new Gl surface view with a user-defined FaceMeshResultGlRenderer.
        glSurfaceView =
                new SolutionGlSurfaceView<>(getApplicationContext(), facemesh.getGlContext(), facemesh.getGlMajorVersion());
        glSurfaceView.setSolutionResultRenderer(new FaceMeshResultGlRenderer());
        glSurfaceView.setRenderInputImage(true);
        facemesh.setResultListener(
                faceMeshResult -> {
                    //logEyesLandmarks(faceMeshResult /*showPixelValues=*/);
                    glSurfaceView.setRenderData(faceMeshResult);
                    glSurfaceView.requestRender();
                });

        // The runnable to start camera after the gl surface view is attached.
        // For video input source, videoInput.start() will be called when the video uri is available.
        glSurfaceView.post(() -> startCamera(camera));


        // Updates the preview layout.
        FrameLayout preview = this.vG.findViewById(R.id.previewFrame);

        //FrameLayout frameLayout = this.vG.findViewById(R.id.preview_display_layout_widget);
        preview.removeAllViewsInLayout();
        preview.addView(glSurfaceView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        glSurfaceView.setVisibility(View.VISIBLE);
        preview.requestLayout();
    }

    //////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////

    private TextureFrameConsumer newFrameListener;
    // {@link SurfaceTexture} where the camera-preview frames can be accessed.
    private SurfaceTexture frameTexture;
    private ExternalTextureConverter converter;

    public void setNewFrameListener(TextureFrameConsumer listener) {
        newFrameListener = listener;
    }

    //////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////

    private void startCamera(Camera camera) {

        int width = glSurfaceView.getWidth();
        int height = glSurfaceView.getHeight();

        if (converter == null) {
            converter = new ExternalTextureConverter(facemesh.getGlContext(), 2);
        }
        if (newFrameListener == null) {
            throw new MediaPipeException(
                    MediaPipeException.StatusCode.FAILED_PRECONDITION.ordinal(),
                    "newFrameListener is not set.");
        }
        frameTexture = converter.getSurfaceTexture(); // -- surfacetexture
        converter.setConsumer(newFrameListener);
        try {
            camera.setPreviewTexture(frameTexture);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*cameraHelper.setOnCameraStartedListener(
                surfaceTexture -> {
                    if (width != 0 && height != 0) {
                        // Sets the size of the output texture frame.
                        updateOutputSize(width, height);
                    }
                    if (customOnCameraStartedListener != null) {
                        customOnCameraStartedListener.onCameraStarted(surfaceTexture);
                    }
                });*/
    }

    private void stopCurrentPipeline() {
        if (glSurfaceView != null) {
            glSurfaceView.setVisibility(View.GONE);
        }
        if (facemesh != null) {
            facemesh.close();
        }
    }

    private final int
            // RIGHT IRIS
            RightEye_LEFT = 362,
            RightEye_RIGHT = 263,
            RightEye_TOP = 386,
            RightEye_BOTTOM = 374,
            RightEye_TOPTORIGTH1 = 387,
            RightEye_TOPTOLEFT1 = 385,
            RightEye_BOTTOMTORIGTH1 = 373,
            RightEye_BOTTOMTOLEFT1 = 380,
            RightEye_CENTER_IRIS = 473, // 473-477 landmarks of Right Iris
    // LEFT IRIS
    LeftEye_LEFT = 33,
            LeftEye_RIGHT = 133,
            LeftEye_TOP = 159,
            LeftEye_BOTTOM = 145,
            LeftEye_CENTER_IRIS = 468; // 468-472 landmarks of Left Iris

    private float euclideanDistance(LandmarkProto.NormalizedLandmark point1, LandmarkProto.NormalizedLandmark point2){
        float
                x1 = point1.getX(),  y1 = point1.getY(), //point1
                x2 = point2.getX(),  y2 = point2.getY(); //point2
        float distance = (float) Math.sqrt(Math.pow((x2-x1),2) + Math.pow((y2-y1),2));
        return distance;
    }
    private float euclideanDistance_wArray1(LandmarkProto.NormalizedLandmark point1, float point2[]){
        float
                x1 = point1.getX(),  y1 = point1.getY(), //point1
                x2 = point2[0],  y2 = point2[1]; //point2
        float distance = (float) Math.sqrt(Math.pow((x2-x1),2) + Math.pow((y2-y1),2));
        return distance;
    }

    private float euclideanDistance_wArray2(float point1[], float point2[]){
        float
                x1 = point1[0],  y1 = point1[1], //point1
                x2 = point2[0],  y2 = point2[1]; //point2
        float distance = (float) Math.sqrt(Math.pow((x2-x1),2) + Math.pow((y2-y1),2));
        return distance;
    }

    // ---------- point1 is the right or top, point2 is the left or bottom
    private float[] irisXPostion(
            LandmarkProto.NormalizedLandmark irisCenter,
            LandmarkProto.NormalizedLandmark leftEyeLid,
            LandmarkProto.NormalizedLandmark rightEyeLid,
            LandmarkProto.NormalizedLandmark topRight1EyeLid,
            LandmarkProto.NormalizedLandmark topLeft1EyeLid,
            LandmarkProto.NormalizedLandmark bottomRight1EyeLid,
            LandmarkProto.NormalizedLandmark bottomLeft1EyeLid
    )
    {
        //float centerToSidePoint_Distance = euclideanDistance(irisCenter, leftEyeLid); // point1 is zero percentage
        // ----------- new centerToSidePoint_Distance is centerOf_topLeft1EyeLid_bottomLeft1EyeLid[]
        float centerOf_topLeft1EyeLid_bottomLeft1EyeLid[] = {topLeft1EyeLid.getX(), (topLeft1EyeLid.getY()+bottomLeft1EyeLid.getY())/2, topLeft1EyeLid.getZ()};
        float centerToSidePoint_Distance =  irisCenter.getX() - topLeft1EyeLid.getX();//euclideanDistance_wArray1(irisCenter, centerOf_topLeft1EyeLid_bottomLeft1EyeLid);

        //float total_Distance = euclideanDistance(leftEyeLid, rightEyeLid); // total distance of eye left lid to right lid
        // ----------- new total_Distance is centerOf_topRight1EyeLid_bottomRight1EyeLid[]
        float centerOf_topRight1EyeLid_bottomRight1EyeLid[] = {topRight1EyeLid.getX(), (topRight1EyeLid.getY()+bottomRight1EyeLid.getY())/2, topRight1EyeLid.getZ()};
        float total_Distance = topRight1EyeLid.getX() - topLeft1EyeLid.getX();//euclideanDistance_wArray2(centerOf_topLeft1EyeLid_bottomLeft1EyeLid, centerOf_topRight1EyeLid_bottomRight1EyeLid); // total distance of eye left lid to right lid

        float percentage_Ratio = (centerToSidePoint_Distance/total_Distance);
    /*float irisMax_Distance = euclideanDistance_wArray1(centerOf_topRight1EyeLid_bottomRight1EyeLid, rightEyeLid);
    float irisMin_Distance = euclideanDistance_wArray(centerOf_topLeft1EyeLid_bottomLeft1EyeLid, rightEyeLid);
    float irisMin_DistancePercentageRatio = (irisMin_Distance/total_Distance)*100;
    float irisMax_DistancePercentageRatio = ((irisMax_Distance - irisMin_Distance)/total_Distance)*100;
    float centerIris_ReachablePercentageRatio = ((percentage_Ratio-irisMin_DistancePercentageRatio) / irisMax_DistancePercentageRatio);
  */
        float x[]= {
                percentage_Ratio,
                centerToSidePoint_Distance,
                total_Distance
                /*irisMax_Distance,
                irisMin_Distance,
                irisMin_DistancePercentageRatio,
                irisMax_DistancePercentageRatio,
                centerIris_ReachablePercentageRatio*/};

        return x;
    }

    private float landmarkPos = 0, savedLPX = 0, savedLPY = 0, savedLP = 0, IncrementOrDecrement = 0;
    private float cursorPosition_IOD(float landmarkPos, int IOD, boolean isHorizontal){
        //Incrementataion and Decrementation
        if(isHorizontal==true) savedLP = savedLPX; // save landmark horizontal
        else savedLP = savedLPY; //save landmark vertical

        if(savedLP != 0)
        {
            if(landmarkPos>savedLP) //increment
                IncrementOrDecrement = IncrementOrDecrement+IOD;
            else if(landmarkPos<savedLP) //decrement
                IncrementOrDecrement = IncrementOrDecrement-IOD;
        }

        if(isHorizontal==true) savedLPX = landmarkPos;
        else savedLPY = landmarkPos;

        return IncrementOrDecrement;
    }
    private float
            Cursor_XPos = 0, // Horizontal
            Cursor_YPos = 0, // Vertical
            rawCXPos,
            rawCYPos,
            Screen_XCenterPos,
            Screen_YCenterPos,
            Screen_TotalWidth,
            Screen_TotalHeight,
            Cursor_XAdjustment = 0,
            Cursor_YAdjustment = 0,
            IncrementOrDecrement_CX = 0,
            IncrementOrDecrement_CY = 0,
            tempCXPos = 0,
            tempCYPos = 0,
            irisRight_XPosition, irisRight_YPosition, eyeRight_XRatio[], eyeRight_YRatio[];
    private boolean
            debounceAdjustment = false,
            debounceIAD = false,
            debounceThreshold = false;
    private int counterA = 0, counterIOD = 0, maxCount = 7; // delaying the cursor to adjust properly
    private void logEyesLandmarks(FaceMeshResult result) {
        if (result == null || result.multiFaceLandmarks().isEmpty()) {
            return;
        }

        // For Bitmaps, show the pixel values. For texture inputs, show the normalized coordinates
        // --------- RIGHT EYE LANDMAKRS
        LandmarkProto.NormalizedLandmark RE_LEFTLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_LEFT);
        LandmarkProto.NormalizedLandmark RE_RIGHTLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_RIGHT);
        LandmarkProto.NormalizedLandmark RE_TOPLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_TOP);
        LandmarkProto.NormalizedLandmark RE_TOPTORIGTH1Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_TOPTORIGTH1);
        LandmarkProto.NormalizedLandmark RE_TOPTOLEFT1Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_TOPTOLEFT1);

        LandmarkProto.NormalizedLandmark RE_BOTTOMTORIGTH1Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_BOTTOMTORIGTH1);
        LandmarkProto.NormalizedLandmark RE_BOTTOMTOLEFT1Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_BOTTOMTOLEFT1);
        LandmarkProto.NormalizedLandmark RE_BOTTOMLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_BOTTOM);
        LandmarkProto.NormalizedLandmark rightCenterIrisLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_CENTER_IRIS);
        // --------- LEFT EYE LANDMAKRS
        LandmarkProto.NormalizedLandmark LeftEye_LEFTLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(LeftEye_LEFT);
        LandmarkProto.NormalizedLandmark LeftEye_RIGHTLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(LeftEye_RIGHT);
        LandmarkProto.NormalizedLandmark LeftEye_TOPLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(LeftEye_TOP);
        LandmarkProto.NormalizedLandmark LeftEye_BOTTOMLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(LeftEye_BOTTOM);
        LandmarkProto.NormalizedLandmark LeftCenterIrisLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(LeftEye_CENTER_IRIS);

        cursor = this.vG.findViewById(R.id.capture);
        float cursorPos = cursor.getLeft();

        // Source position of x and y landmark of rightCenterIris
        rawCXPos = cursor.getLeft()+(rightCenterIrisLandmark.getX()*1000);
        rawCYPos = cursor.getLeft()+(rightCenterIrisLandmark.getY()*1000);

        // Getting screen width and height

        Screen_TotalWidth = params.x;
        Screen_TotalHeight = params.y;
        Screen_XCenterPos = (Screen_TotalWidth / 2);
        Screen_YCenterPos = (Screen_TotalHeight / 2);

        // ---------- Iris Position based on Eye surface length
        eyeRight_XRatio = irisXPostion(
                rightCenterIrisLandmark,
                RE_RIGHTLandmark,
                RE_LEFTLandmark,
                RE_TOPTORIGTH1Landmark,
                RE_TOPTOLEFT1Landmark,
                RE_BOTTOMTORIGTH1Landmark,
                RE_BOTTOMTOLEFT1Landmark);
        irisRight_XPosition = Screen_TotalWidth * eyeRight_XRatio[0];
        //eyeRight_YRatio = irisYPostion(rightCenterIrisLandmark, RightEye_TOPLandmark, RightEye_BOTTOMLandmark, RightEye_TOPTORIGTH1Landmark, RightEye_TOPTOLEFT1Landmark);
        //irisRight_YPosition = Screen_TotalWidth - (Screen_TotalWidth * eyeRight_YRatio[3]);

        ViewGroup rootLayout = this.vG.findViewById(R.id.capture); // TODO: CHANGE THIS IN SCREEN NOT IN ACTIVITY
        rootLayout.setOnTouchListener((view1, motionEvent) -> {
            if(debounceAdjustment==false){
                Cursor_XAdjustment =  Cursor_XAdjustment + (Screen_XCenterPos - Cursor_XPos);
                Cursor_YAdjustment = Cursor_YAdjustment + (Screen_YCenterPos - Cursor_YPos);
            }
            debounceAdjustment = true;
            return true;
        });
        if(debounceAdjustment==false && debounceIAD==false && debounceThreshold==false)
        {
            // ----------- Used for thresholding, used for the difference of old and current
            tempCXPos = rawCXPos;
            tempCYPos = rawCYPos;
            //debounceThreshold=true;
            // ----------- For increasing the position of cursor (increment and decrement)
            IncrementOrDecrement_CX = cursorPosition_IOD(rawCXPos, 5,true);
            IncrementOrDecrement_CY = cursorPosition_IOD(rawCYPos, 0, false);
            //debounceIAD=true;

            Cursor_XPos = rawCXPos + IncrementOrDecrement_CX + Cursor_XAdjustment;
            Cursor_YPos = rawCYPos + IncrementOrDecrement_CY + Cursor_YAdjustment;

            cursor.setX(Cursor_XPos);
            cursor.setY(Screen_YCenterPos);
        }
        else
        {
            if (debounceAdjustment==true && counterA < maxCount) {counterA = counterA + 1;}
            else {counterA = 0; debounceAdjustment = false;}
            if (debounceIAD==true && counterIOD < 2) {counterIOD = counterIOD + 1;}
            else {counterIOD = 0; debounceIAD = false;}
            // ---------- Threshold for X and Y, distance between the last and current position
            if (debounceThreshold==true && (tempCXPos-rawCXPos) > 5 || (tempCXPos-rawCXPos) < -5){
                debounceThreshold = false;
            }
        }

        Log.i(
                TAG,
                String.format(
                        "MediaPipe Face Mesh nose normalized coordinates (value range: [X:Horizontal, Y:Vertical]): " +
                                "[Rx=%f, Ry=%f] - " +
                                "[Cx=%f, Cy=%f] - " +
                                "[IODx=%f, IODy=%f] - " +
                                "[xA=%f, yA=%f] - " +
                                "[Sx=%f, Sy=%f] - "+
                                "[SCx=%f, SCy=%f] - "+
                                "[CLx=%f, CLy=%f, CLz=%f] - "+
                                "[eyeXratio=%f, eyeXC2R_D=%f, eyeXl2R_D=%f]"+// irisXmax_D=%f, irisXmin_D=%f, irisXminratio_D=%f, irisXmaxratio_D=%f, XcenterIris_ReachableRatio=%f] - "+
                                //"[YeyeRatio=%f, YcenterToRight_Distance=%f, YleftToRigth_Distance=%f, YcenterIris_ReachableRatio=%f] - "+
                                "[irisRight_XPosition=%f, irisRight_YPosition=%f]",
                        rawCXPos, rawCYPos,
                        Cursor_XPos, Cursor_YPos,
                        IncrementOrDecrement_CX, IncrementOrDecrement_CY,
                        Cursor_XAdjustment, Cursor_YAdjustment,
                        Screen_TotalWidth, Screen_TotalHeight,
                        Screen_XCenterPos, Screen_YCenterPos,
                        rightCenterIrisLandmark.getX(), rightCenterIrisLandmark.getY(), rightCenterIrisLandmark.getZ(),
                        eyeRight_XRatio[0], eyeRight_XRatio[1], eyeRight_XRatio[2], //eyeRight_XRatio[3], eyeRight_XRatio[4], eyeRight_XRatio[5],eyeRight_XRatio[6], eyeRight_XRatio[7],
                        //eyeRight_YRatio[0], eyeRight_YRatio[1], eyeRight_YRatio[2], eyeRight_YRatio[3],
                        irisRight_XPosition, irisRight_YPosition)
        );
    }
}
