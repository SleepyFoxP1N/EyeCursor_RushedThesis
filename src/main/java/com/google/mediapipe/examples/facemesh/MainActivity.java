package com.google.mediapipe.examples.facemesh;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.IBinder;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mediapipe.apps.EyeCursor.R;
import com.google.mediapipe.components.CameraHelper;
import com.google.mediapipe.components.CameraXPreviewHelper;
import com.google.mediapipe.components.ExternalTextureConverter;
import com.google.mediapipe.components.FrameProcessor;
import com.google.mediapipe.components.PermissionHelper;
import com.google.mediapipe.components.TextureFrameConsumer;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.framework.MediaPipeException;
import com.google.mediapipe.glutil.EglManager;
import com.google.mediapipe.solutioncore.CameraInput;
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.microedition.khronos.egl.EGLContext;

import io.hamed.floatinglayout.FloatingLayout;
import io.hamed.floatinglayout.callback.FloatingListener;

/** Main activity of MediaPipe */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

  /**
   *  ----------------------------------------- FLOATING LAYOUT
   */

  FloatingLayout floatingLayout;

  private static final int START_BUTTON_ID = R.id.start_button;

  private static final int STOP_BUTTON_ID = R.id.stop_button;
  private static final int SETTINGS_BUTTON_ID = R.id.settings_button;
  private static final int EXIT_BUTTON_ID = R.id.exit_button;

  private FloatingListener listener = new FloatingListener() {
    @Override
    public void onCreateListener(View view) {
      onStart_SetupLiveDemoUiComponents(/*view*/);
    }

    @Override
    public void onCloseListener() {

    }
  };

  private void showFloating(){
    floatingLayout = new FloatingLayout(getApplicationContext(), R.layout.activity_face_mesh_layout);
    floatingLayout.setFloatingListener(listener);
    floatingLayout.create();
  }

  @Override
  public void onClick(View view) {
    int id = view.getId();

    if (id == START_BUTTON_ID) {
      new AlertDialog.Builder(this).setTitle("For proper use of the system:")
                .setMessage("Only use in pop-up view or similar setting.")
                .setCancelable(true)
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialogInterface, int i) {
                    onStartApplication();
                  }
                })
        .setNegativeButton("Not yet", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.cancel();
          }
        })
        .show();
    }
    else if (id == STOP_BUTTON_ID)
      onStopApplication();
    else if (id == EXIT_BUTTON_ID)
      onExitApplication();
    else if (id == SETTINGS_BUTTON_ID)
      onSwitchToSettings();
  }

  private void accessAccessibilityService() {
    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
    startActivity(intent);
  }

  /**
   *  ----------------------------------------- CAMERA X
   */
  /*private ProcessCameraProvider cameraProvider;
  private PreviewView previewView;
  int cameraFacing = CameraSelector.LENS_FACING_FRONT;
  private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
    @Override
    public void onActivityResult(Boolean result) {
      if (result) {
        startCamera(cameraFacing);
      }
    }
  });

  public void startCameraX() {

    previewView = findViewById(R.id.cameraPreview);

    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      activityResultLauncher.launch(Manifest.permission.CAMERA);
    } else {
      startCamera(cameraFacing);
    }

  }

  public void startCamera(int cameraFacing) {
    int aspectRatio = aspectRatio(previewView.getWidth(), previewView.getHeight());
    ListenableFuture<ProcessCameraProvider> listenableFuture = ProcessCameraProvider.getInstance(this);

    listenableFuture.addListener(() -> {
      try {
        cameraProvider = (ProcessCameraProvider) listenableFuture.get();

        Preview preview = new Preview.Builder().setTargetAspectRatio(aspectRatio).build();

        ImageCapture imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cameraFacing).build();

        cameraProvider.unbindAll();

        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
      } catch (ExecutionException | InterruptedException e) {
        e.printStackTrace();
      }
    }, ContextCompat.getMainExecutor(this));
  }

  private int aspectRatio(int width, int height) {
    double previewRatio = (double) Math.max(width, height) / Math.min(width, height);
    if (Math.abs(previewRatio - 4.0 / 3.0) <= Math.abs(previewRatio - 16.0 / 9.0)) {
      return AspectRatio.RATIO_4_3;
    }
    return AspectRatio.RATIO_16_9;
  }*/



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
  protected void onStart() {
    super.onStart();
    startCursorOverlayService();
    if(AccessibilityService()){
      SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
      SharedPreferences.Editor editor = sharedPreferences.edit();
      editor.putBoolean("widgetsService", true);
      editor.putBoolean("globalService", true);
      editor.apply();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();/*
    if (is_owBound) {
      unbindService(owConnection);
      is_owBound = false;
    }*//*
    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean("widgetsService", false);
    editor.putBoolean("globalService", false);
    editor.apply();*/
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();/*
    if (is_owBound) {
      unbindService(owConnection);
      is_owBound = false;
    }*/
    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean("widgetsService", false);
    editor.putBoolean("globalService", false);
    editor.apply();
  }

  private void sendMouthDataToService(int mouth_TopYPos, int mouth_BottomYPos, int isMouthOpen) {
    // Call the setData method of the service
    if (is_owBound) {
      owService.setMouthData(mouth_TopYPos, mouth_BottomYPos, isMouthOpen);
    }
  }

  private void sendCursorDataToService(float posX, float posY, float posZ, int isNoseMoving) {
    // Call the setData method of the service
    if (is_owBound) {
      owService.setCursorData((int)posX, (int)posY, (int) posZ, isNoseMoving);
    }
  }
  private void sendCursorSizeToService(int width, int height) {
    if (is_owBound) {
      owService.setCursorSize(width,height);
    }
  }

  private void startCursorOverlayService(){
    Intent intent = new Intent(this, OverlayWidgetsService.class);
    bindService(intent, owConnection, Context.BIND_AUTO_CREATE);
  }

  /**-----------------------------------------
   *  -----------------------------------------  basic camera
   -----------------------------------------*/
/*

  // Flips the camera-preview frames vertically by default, before sending them into FrameProcessor
  // to be processed in a MediaPipe graph, and flips the processed frames back when they are
  // displayed. This maybe needed because OpenGL represents images assuming the image origin is at
  // the bottom-left corner, whereas MediaPipe in general assumes the image origin is at the
  // top-left corner.
  // NOTE: use "flipFramesVertically" in manifest metadata to override this behavior.
  private static final boolean FLIP_FRAMES_VERTICALLY = true;

  // Number of output frames allocated in ExternalTextureConverter.
  // NOTE: use "converterNumBuffers" in manifest metadata to override number of buffers. For
  // example, when there is a FlowLimiterCalculator in the graph, number of buffers should be at
  // least `max_in_flight + max_in_queue + 1` (where max_in_flight and max_in_queue are used in
  // FlowLimiterCalculator options). That's because we need buffers for all the frames that are in
  // flight/queue plus one for the next frame from the camera.
  private static final int NUM_BUFFERS = 2;

  static {
    // Load all native libraries needed by the app.
    System.loadLibrary("mediapipe_jni");
    try {
      System.loadLibrary("opencv_java3");
    } catch (java.lang.UnsatisfiedLinkError e) {
      // Some example apps (e.g. template matching) require OpenCV 4.
      System.loadLibrary("opencv_java4");
    }
  }

  // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
  // frames onto a {@link Surface}.
  protected FrameProcessor processor;
  // Handles camera access via the {@link CameraX} Jetpack support library.
  protected CameraXPreviewHelper cameraHelper;

  // {@link SurfaceTexture} where the camera-preview frames can be accessed.
  private SurfaceTexture previewFrameTexture;
  // {@link SurfaceView} that displays the camera-preview frames processed by a MediaPipe graph.
  private SurfaceView previewDisplayView;

  // Creates and manages an {@link EGLContext}.
  private EglManager eglManager;
  // Converts the GL_TEXTURE_EXTERNAL_OES texture from Android camera into a regular texture to be
  // consumed by {@link FrameProcessor} and the underlying MediaPipe graph.
  private ExternalTextureConverter converter;

  // ApplicationInfo for retrieving metadata defined in the manifest.
  private ApplicationInfo applicationInfo;

  protected void onCreate_basic() {

    try {
      applicationInfo =
              getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
    } catch (PackageManager.NameNotFoundException e) {
      Log.e(TAG, "Cannot find application info: " + e);
    }

    previewDisplayView = new SurfaceView(this);
    setupPreviewDisplayView();

    // Initialize asset manager so that MediaPipe native libraries can access the app assets, e.g.,
    // binary graphs.
    AndroidAssetUtil.initializeNativeAssetManager(this);
    eglManager = new EglManager(null);
    processor =
            new FrameProcessor(
                    this,
                    eglManager.getNativeContext(),
                    applicationInfo.metaData.getString("binaryGraphName"),
                    applicationInfo.metaData.getString("inputVideoStreamName"),
                    applicationInfo.metaData.getString("outputVideoStreamName"));
    processor
            .getVideoSurfaceOutput()
            .setFlipY(
                    applicationInfo.metaData.getBoolean("flipFramesVertically", FLIP_FRAMES_VERTICALLY));

    PermissionHelper.checkAndRequestCameraPermissions(this);
  }

  // Used to obtain the content view for this application. If you are extending this class, and
  // have a custom layout, override this method and return the custom layout.
  protected int getContentViewLayoutResId() {
    return R.layout.activity_main;
  }

  @Override
  protected void onResume() {
    super.onResume();
    converter =
            new ExternalTextureConverter(
                    eglManager.getContext(),
                    applicationInfo.metaData.getInt("converterNumBuffers", NUM_BUFFERS));
    converter.setFlipY(
            applicationInfo.metaData.getBoolean("flipFramesVertically", FLIP_FRAMES_VERTICALLY));
    converter.setConsumer(processor);
    if (PermissionHelper.cameraPermissionsGranted(this)) {
      startCamera_basic();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    converter.close();

    // Hide preview display until we re-open the camera again.
    previewDisplayView.setVisibility(View.GONE);
  }

  @Override
  public void onRequestPermissionsResult(
          int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  protected void onCameraStarted(SurfaceTexture surfaceTexture) {
    previewFrameTexture = surfaceTexture;
    // Make the display view visible to start showing the preview. This triggers the
    // SurfaceHolder.Callback added to (the holder of) previewDisplayView.
    previewDisplayView.setVisibility(View.VISIBLE);
  }

  protected Size cameraTargetResolution() {
    return null; // No preference and let the camera (helper) decide.
  }

  public void startCamera_basic() {
    cameraHelper = new CameraXPreviewHelper();
    previewFrameTexture = converter.getSurfaceTexture();
    cameraHelper.setOnCameraStartedListener(
            surfaceTexture -> {
              onCameraStarted(surfaceTexture);
            });
    CameraHelper.CameraFacing cameraFacing =
            applicationInfo.metaData.getBoolean("cameraFacingFront", false)
                    ? CameraHelper.CameraFacing.FRONT
                    : CameraHelper.CameraFacing.BACK;
    cameraHelper.startCamera(
            this, cameraFacing, previewFrameTexture, cameraTargetResolution());
  }

  protected Size computeViewSize(int width, int height) {
    return new Size(width, height);
  }

  protected void onPreviewDisplaySurfaceChanged(
          SurfaceHolder holder, int format, int width, int height) {
    // (Re-)Compute the ideal size of the camera-preview display (the area that the
    // camera-preview frames get rendered onto, potentially with scaling and rotation)
    // based on the size of the SurfaceView that contains the display.
    Size viewSize = computeViewSize(width, height);
    Size displaySize = cameraHelper.computeDisplaySizeFromViewSize(viewSize);
    boolean isCameraRotated = cameraHelper.isCameraRotated();

    // Configure the output width and height as the computed display size.
    converter.setDestinationSize(
            isCameraRotated ? displaySize.getHeight() : displaySize.getWidth(),
            isCameraRotated ? displaySize.getWidth() : displaySize.getHeight());
  }

  private void setupPreviewDisplayView() {
    previewDisplayView.setVisibility(View.GONE);
    ViewGroup viewGroup = findViewById(R.id.preview_display_layout);
    viewGroup.addView(previewDisplayView);

    previewDisplayView
            .getHolder()
            .addCallback(
                    new SurfaceHolder.Callback() {
                      @Override
                      public void surfaceCreated(SurfaceHolder holder) {
                        processor.getVideoSurfaceOutput().setSurface(holder.getSurface());
                      }

                      @Override
                      public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                        onPreviewDisplaySurfaceChanged(holder, format, width, height);
                      }

                      @Override
                      public void surfaceDestroyed(SurfaceHolder holder) {
                        processor.getVideoSurfaceOutput().setSurface(null);
                      }
                    });
  }*/

  /**-----------------------------------------
   *  -----------------------------------------  Reworking camera input
   -----------------------------------------*/

  /*private TextureFrameConsumer newFrameListener;
  // {@link SurfaceTexture} where the camera-preview frames can be accessed.
  private SurfaceTexture frameTexture;
  private ExternalTextureConverter converter;
  private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
  // Size of the camera-preview frames from the camera.
  private Size frameSize;

  public void setNewFrameListener(TextureFrameConsumer listener) {
    newFrameListener = listener;
  }
  public static enum CameraFacing {
    FRONT,
    BACK
  };

  public static boolean permissionsGranted(Activity context, String[] permissions) {
    for (String permission : permissions) {
      int permissionStatus = ContextCompat.checkSelfPermission(context, permission);
      if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }
  public static boolean cameraPermissionsGranted(Activity context) {
    return permissionsGranted(context, new String[] {CAMERA_PERMISSION});
  }

  public void updateOutputSize(int width, int height) {
    Log.i(
            TAG,
            "Set camera output texture frame size to width="
                    + width
                    + " , height="
                    + height);
    // Configure the output width and height as the computed
    // display size.
    converter.setDestinationSize(width,height);
  }

  public void start(
          Activity activity, EGLContext eglContext, CameraFacing cameraFacing, int width, int height) {
    if (!cameraPermissionsGranted(activity)) {
      return;
    }
    if (converter == null) {
      converter = new ExternalTextureConverter(eglContext, 2); // -- ExternalTextureConverter imported
    }
    if (newFrameListener == null) {
      throw new MediaPipeException(
              MediaPipeException.StatusCode.FAILED_PRECONDITION.ordinal(),
              "newFrameListener is not set.");
    }

    frameTexture = converter.getSurfaceTexture();
    converter.setConsumer(newFrameListener);
    cameraHelper.setOnCameraStartedListener(
            surfaceTexture -> {
              if (width != 0 && height != 0) {
                // Sets the size of the output texture frame.
                updateOutputSize(width, height);
              }
              if (customOnCameraStartedListener != null) {
                customOnCameraStartedListener.onCameraStarted(surfaceTexture);
              }
            });
    cameraHelper.startCamera(
            activity,
            cameraFacing == CameraFacing.FRONT
                    ? CameraHelper.CameraFacing.FRONT
                    : CameraHelper.CameraFacing.BACK,
            *//*surfaceTexture=*//* frameTexture,
            (width == 0 || height == 0) ? null : new Size(width, height));
  }*/

  /**-----------------------------------------
   *  ----------------------------------------- MEDIAPIPE
   -----------------------------------------*/
  private static final String TAG = "MainActivity";

  private FaceMesh facemesh;
  // Run the pipeline and the model inference on GPU or CPU.
  private static final boolean RUN_ON_GPU = true;

  private enum InputSource {
    UNKNOWN,
//    IMAGE,
//    VIDEO,
    CAMERA,
  }
  private InputSource inputSource = InputSource.UNKNOWN;
  private CameraInput cameraInput;

  private SolutionGlSurfaceView<FaceMeshResult> glSurfaceView;

  private ImageView cursor;

  // Cursor Properties
  private int cursorSpeedX, cursorSpeedY;
  private int cursor_DefaultSpeed = 0;
  private int cursorSize_Width, cursorSize_Height;
  private int cursorThreshold;
  private int cursorSmoothening;
  private float cursorAcceleration;
  private int cursor_DefaultWidth = 63*2, cursor_DefaultHeight = 93*2;
  private int cursor_DefaultThreshold = 30;
  private int cursor_DefaultSmoothening = 40;
  private float cursor_DefaultAcceleration = 1.1f;
  Boolean isPermission = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    // Set fullscreen flags
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    // Hide the navigation and status bar
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
    Objects.requireNonNull(getSupportActionBar()).hide();

    onCreateAgreementPage();
    checkPermissions();

    // Initialization of onClickListener_buttons to MainActivity
    findViewById(R.id.start_button).setOnClickListener(this);
    findViewById(R.id.stop_button).setOnClickListener(this);
    findViewById(R.id.exit_button).setOnClickListener(this);
    findViewById(R.id.settings_button).setOnClickListener(this);

    // Switch between permissionPage or mainPage
    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
    if(AccessibilityService() && CameraPermission() && OverlayPermission()){
      onStartApplication();
    }
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean("globalService", false);
    editor.putBoolean("widgetsService", false);
    editor.apply();
    isPermission = sharedPreferences.getBoolean("permissions", false);
    if(isPermission){
      findViewById(R.id.mainpage).setVisibility(View.VISIBLE);
      findViewById(R.id.permission).setVisibility(View.INVISIBLE);
    }else{
      findViewById(R.id.mainpage).setVisibility(View.INVISIBLE);
      findViewById(R.id.permission).setVisibility(View.VISIBLE);
    }
  }

  @Override
  public void onResume(){
    super.onResume();
    setCursorProperties();
    checkPermissions();
    // Switch between permissionPage or mainPage
    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
    isPermission = sharedPreferences.getBoolean("permissions", false);
    if(isPermission){
      findViewById(R.id.mainpage).setVisibility(View.VISIBLE);
      findViewById(R.id.permission).setVisibility(View.INVISIBLE);
    }else{
      findViewById(R.id.mainpage).setVisibility(View.INVISIBLE);
      findViewById(R.id.permission).setVisibility(View.VISIBLE);
    }
    if(AccessibilityService()){
      SharedPreferences.Editor editor = sharedPreferences.edit();
      editor.putBoolean("widgetsService", true);
      editor.putBoolean("globalService", true);
      editor.apply();
    }
  }

  private void onCreateAgreementPage(){
    TextView User_agreement = findViewById(R.id.endUserAgreementTextView);

    Button agree_button = findViewById(R.id.agreecontinueButton);
    agree_button.setOnClickListener(view -> {
      Intent intent = new Intent(MainActivity.this, PermissionList.class);
      startActivity(intent);
    });

    User_agreement.setOnClickListener(view -> {
      Intent intent = new Intent(MainActivity.this, UserLicenseAgreement.class);
      startActivity(intent);
    });

    String buttonText = "End User License Agreement";
    SpannableString spannable = new SpannableString(buttonText);
    spannable.setSpan(new UnderlineSpan(), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    User_agreement.setText(spannable);
  }

  private void setCursorProperties(){
    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
    // Getting properties
    cursorSpeedX = sharedPreferences.getInt("cursorSpeedX", cursor_DefaultSpeed);
    cursorSpeedY = sharedPreferences.getInt("cursorSpeedY", cursor_DefaultSpeed);
    cursorSize_Width = sharedPreferences.getInt("cursorSize_Width", cursor_DefaultWidth);
    cursorSize_Height = sharedPreferences.getInt("cursorSize_Height", cursor_DefaultHeight);
    cursorThreshold = sharedPreferences.getInt("cursorThreshold", cursor_DefaultThreshold);
    cursorSmoothening = sharedPreferences.getInt("cursorSmoothening", cursor_DefaultSmoothening);
    cursorAcceleration = sharedPreferences.getFloat("cursorAcceleration", cursor_DefaultAcceleration);
  }

  /** Others uses of buttons */
  private void onStartApplication(){
    //accessAccessibilityService();
    if (!AccessibilityService()) {
      Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
      startActivityForResult(intent, 125);
      Toast.makeText(this, "Turn Accessibility On", Toast.LENGTH_SHORT).show();
    }
    checkPermissions();
    // Switch between permissionPage or mainPage
    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean("globalService", true);
    editor.putBoolean("widgetsService", true);
    editor.apply();
    isPermission = sharedPreferences.getBoolean("permissions", false);
    if(isPermission){
      findViewById(R.id.mainpage).setVisibility(View.VISIBLE);
      findViewById(R.id.permission).setVisibility(View.INVISIBLE);
    }else{
      findViewById(R.id.mainpage).setVisibility(View.INVISIBLE);
      findViewById(R.id.permission).setVisibility(View.VISIBLE);
    }

    // start globalService
    owService.setIsGlobalServiceStarted(true);

    Intent CursorWidget_Service = new Intent(MainActivity.this, OverlayWidgetsService.class);
    startService(CursorWidget_Service);

    Intent GlobalActionBar_Service = new Intent(MainActivity.this, GlobalActionAccessabilityService.class);
    startService(GlobalActionBar_Service);

        /*Intent CameraFloatingWindow_Service = new Intent(MainActivity.this, FloatingCamera_Front.class);
        CameraFloatingWindow_Service.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startService(CameraFloatingWindow_Service);*/

        /*Intent intent2 = new Intent(MainActivity.this, FaceMeshLayout.class);
        startActivity(intent2);*/

    findViewById(R.id.start_button).setEnabled(false);
    findViewById(R.id.stop_button).setEnabled(true);
    onStart_SetupLiveDemoUiComponents();
    //showFloating();
    owService.setOTP_FaceCallibrationUI(true); // set OTP for face callibration
  }

  private void onStopApplication(){
    moveTaskToBack( true);
    android.os.Process.killProcess(android.os.Process.myPid());
    System.exit(1);
    finish();
  }
  private void onSwitchToSettings(){
    Intent intent = new Intent(MainActivity.this, SettingModules.class);
    startActivity(intent);
  }
  private void onExitApplication(){
    moveTaskToBack( true);
  }

  /** Sets up the UI components for the live demo with camera input. */
  private void onStart_SetupLiveDemoUiComponents(/*View view*/) {
    /*startCameraButton = findViewById(R.id.start_button);
    startCameraButton.setOnClickListener(
        v -> {}

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
          {
            if (!Settings.canDrawOverlays(MainActivity.this))
            {
              getPermissions();
            }
            else
            {
              Intent CursorWidget_Service = new Intent(MainActivity.this, OverlayWidgetsService.class);
              startService(CursorWidget_Service);

              Intent CameraFloatingWindow_Service = new Intent(MainActivity.this, FloatingCamera_Front.class);
              CameraFloatingWindow_Service.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              startService(CameraFloatingWindow_Service);

              Intent intent2 = new Intent(MainActivity.this, FaceMeshLayout.class);
              startActivity(intent2);
            }
          }*/

          if (inputSource == InputSource.CAMERA)
          {
            return;
          }
          stopCurrentPipeline();
          setupStreamingModePipeline(/*view*/);
          //startCameraX();
        //});
  }

  /** Sets up core workflow for streaming mode. */
  private void setupStreamingModePipeline(/*View view*/) {
    this.inputSource = InputSource.CAMERA;
    cursor = findViewById(R.id.cursor);

    // Initializes a new MediaPipe Face Mesh solution instance in the streaming mode.
    facemesh =
        new FaceMesh(
            this,
            FaceMeshOptions.builder()
                .setStaticImageMode(false)
                .setRefineLandmarks(true)
                .setRunOnGpu(RUN_ON_GPU)
                .build());
    facemesh.setErrorListener((message, e) -> Log.e(TAG, "MediaPipe Face Mesh error:" + message));

    cameraInput = new CameraInput(this);
    cameraInput.setNewFrameListener(textureFrame -> facemesh.send(textureFrame));

    // Initializes a new Gl surface view with a user-defined FaceMeshResultGlRenderer.
    glSurfaceView =
        new SolutionGlSurfaceView<>(this, facemesh.getGlContext(), facemesh.getGlMajorVersion());

    glSurfaceView.setSolutionResultRenderer(new FaceMeshResultGlRenderer());
    glSurfaceView.setRenderInputImage(true);
    facemesh.setResultListener(
        faceMeshResult -> {
          logEyesLandmarks(faceMeshResult /*showPixelValues=*/);
          glSurfaceView.setRenderData(faceMeshResult);
          glSurfaceView.requestRender();
        });

    // The runnable to start camera after the gl surface view is attached.
    // For video input source, videoInput.start() will be called when the video uri is available.
    if (inputSource == InputSource.CAMERA) {
      glSurfaceView.post(this::startCamera);
    }

    // Updates the preview layout.
    FrameLayout frameLayout = findViewById(R.id.preview_display_layout);
    frameLayout.removeAllViewsInLayout();
    frameLayout.addView(glSurfaceView);
    cursor.setVisibility(View.VISIBLE);
    glSurfaceView.setVisibility(View.VISIBLE);
    frameLayout.requestLayout();

    // -- Getting screen width and height
    WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    DisplayMetrics displayMetrics = new DisplayMetrics();
    windowManager.getDefaultDisplay().getMetrics(displayMetrics);
    float app_TotalWidth = displayMetrics.widthPixels;
    float app_TotalHeight = displayMetrics.heightPixels;

    int newCamereWidth = (int) (app_TotalWidth * 0.20);
    int newCamereHeight = (int) (app_TotalHeight * 0.14);
    frameLayout.setLayoutParams(new RelativeLayout.LayoutParams(newCamereWidth,newCamereHeight));

  }

  private void startCamera() {
    cameraInput.start(
        this,
        facemesh.getGlContext(),
        CameraInput.CameraFacing.FRONT,
        glSurfaceView.getWidth(),
        glSurfaceView.getHeight());
  }

  private void stopCurrentPipeline() {
    if (cameraInput != null) {
      cameraInput.setNewFrameListener(null);
      cameraInput.close();
    }
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
          RightEye_TOP1 = 386,
          RightEye_TOP2 = 257,
          RightEye_TOP3 = 443,
          RightEye_BOTTOM1 = 374,
          RightEye_BOTTOM3 = 253,
          RightEye_TOPTORIGTH1 = 387,
          RightEye_TOPTOLEFT1 = 385,
          RightEye_BOTTOMTORIGTH1 = 373,
          RightEye_BOTTOMTOLEFT1 = 380,

          RightEye_RIGHTTOTOPTOP1 = 260,
          RightEye_RIGHTTOBOTTOMBOTTOM1 = 339,
          RightEye_RIGHTTOTOP1 = 466,
          RightEye_RIGHTTOBOTTOM1 = 249,
          RightEye_RIGHTTOTOP2 = 388,
          RightEye_RIGHTTOBOTTOM2 = 390,
          RightEye_RIGHTTOTOP3 = 387,
          RightEye_RIGHTTOBOTTOM3 = 373,

          RightEye_LEFT2TOTOP1 = 414,
          RightEye_LEFT2TOBOTTOM1 = 341,
          RightEye_CENTER_IRIS = 473, // 473-477 landmarks of Right Iris
          // LEFT IRIS
          LeftEye_LEFT = 33,
          LeftEye_RIGHT = 133,
          LeftEye_TOP = 159,
          LeftEye_BOTTOM = 145,
          LeftEye_CENTER_IRIS = 468; // 468-472 landmarks of Left Iris
  private final int
          MouthTop = 13,
          MouthBottom = 14;

  private float euclideanDistance(NormalizedLandmark point1, NormalizedLandmark point2){
    float
            x1 = point1.getX(),  y1 = point1.getY(), //point1
            x2 = point2.getX(),  y2 = point2.getY(); //point2
    float distance = (float) Math.sqrt(Math.pow((x2-x1),2) + Math.pow((y2-y1),2));
    return distance;
  }
  private float euclideanDistance_wArray1(NormalizedLandmark point1, float point2[]){
    float
            x1 = point1.getX(),  y1 = point1.getY(), //point1
            x2 = point2[0],  y2 = point2[1]; //point2
    float distance = (float) Math.sqrt(Math.pow((x2-x1),2) + Math.pow((y2-y1),2));
    return distance;
  }

  private float euclideanDistance_wArray2(float point1[], NormalizedLandmark point2){
    float
            x1 = point1[0],  y1 = point1[1], //point1
            x2 = point2.getX(),  y2 = point2.getY(); //point2
    float distance = (float) Math.sqrt(Math.pow((x2-x1),2) + Math.pow((y2-y1),2));
    return distance;
  }

  private float euclideanDistance_wArray3(float point1[], float point2[]){
    float
            x1 = point1[0],  y1 = point1[1], //point1
            x2 = point2[0],  y2 = point2[1]; //point2
    float distance = (float) Math.sqrt(Math.pow((x2-x1),2) + Math.pow((y2-y1),2));
    return distance;
  }

  // ---------- point1 is the right or top, point2 is the left or bottom
  private float[] irisXPostion(
          NormalizedLandmark irisCenter,
          NormalizedLandmark topRight1EyeLid, // X higher value
          NormalizedLandmark topLeft1EyeLid
  )
  {
    float low = topLeft1EyeLid.getX();
    float high = topRight1EyeLid.getX();
    // ----------- DESCRIPTION:
    // ----------- distanceOf_CenterEye_SideLeftEye acts as active changer
    // ----------- distanceOf_TopRight1EyeLid_TopLeft1EyeLid acts as the 100% or the max
    // ----------- percentageRatio is the ratio of distanceOf_CenterEye_SideLeftEye to distanceOf_TopRight1EyeLid_TopLeft1EyeLid
    float distanceOf_CenterEye_SideLeftEye =  irisCenter.getX() - low; // 0 - max distance
    float distanceOf_TopRight1EyeLid_TopLeft1EyeLid = high - low; // return maximum distance
    float percentageRatio = (distanceOf_CenterEye_SideLeftEye/distanceOf_TopRight1EyeLid_TopLeft1EyeLid);

    float x[]= {
            percentageRatio,
            distanceOf_CenterEye_SideLeftEye,
            distanceOf_TopRight1EyeLid_TopLeft1EyeLid};

    return x;
  }

  // TODO: Do the same with irisXPosition
  // ---------- point1 is the right or top, point2 is the left or bottom
  private float[] irisYPostion(
          NormalizedLandmark irisCenter,
          NormalizedLandmark rightBottom1EyeLid, // Y higher value
          NormalizedLandmark rightTop1EyeLid
  )
  {
    float low = rightTop1EyeLid.getY();
    float high = rightBottom1EyeLid.getY();
    // ----------- DESCRIPTION:
    // ----------- distanceOf_CenterEye_SideBottomEye acts as active changer
    // ----------- distanceOf_RightBottom1EyeLid_RightTop1EyeLid acts as the 100% or the max
    // ----------- percentageRatio is the ratio of distanceOf_CenterEye_SideBottomEye to distanceOf_RightBottom1EyeLid_RightTop1EyeLid
    float distanceOf_CenterEye_SideBottomEye =  irisCenter.getY() - low;
    float distanceOf_RightBottom1EyeLid_RightTop1EyeLid = high-low;
    float percentageRatio = (distanceOf_CenterEye_SideBottomEye/distanceOf_RightBottom1EyeLid_RightTop1EyeLid);

    float x[]= {
            percentageRatio,
            distanceOf_CenterEye_SideBottomEye,
            distanceOf_RightBottom1EyeLid_RightTop1EyeLid};

    return x;
  }

  /*private float savedLP_X = 0, savedLP_Y = 0, savedLP = 0, IncrementOrDecrement = 0;
  private float cursorPosition_IOD(float landmarkPos, int IOD, boolean isHorizontal){
    //Incrementataion and Decrementation
    if(isHorizontal==true) savedLP = savedLP_X; // save landmark horizontal
        else savedLP = savedLP_Y; //save landmark vertical

    double temp = (double) IOD * 0.05; // Example. 2 = 0.2 pixel, convert
    float IOD_f = (float) temp;

    if(savedLP != 0 || (cursorThreshold*2) < Math.abs(savedLP - landmarkPos))
    {
      if(landmarkPos>savedLP) //increment
        IncrementOrDecrement = IncrementOrDecrement+IOD_f;
      else //decrement
        IncrementOrDecrement = IncrementOrDecrement-IOD_f;
    }

    if(isHorizontal==true) savedLP_X = landmarkPos;
        else savedLP_Y = landmarkPos;

    return IncrementOrDecrement;
  }*/


  //Incrementataion and Decrementation for X Axis
  private float savedX_LP = 0, posX_IncrementOrDecrement = 0, restXCounter = 3, resetRestXCounter = 3,speedXCounter = 0, resetSpeedXCounter = 2;
  private float cursorPosX_IOD(float landmarkPos, int IOD, int threshold, float cursorPos){
    float maxPos = 0.999F * Screen_TotalWidth;
    float minPos = 0.001F * Screen_TotalWidth;

    double temp = (double) IOD * 1; // Example. 2 x 2 = 4 pixel, convert
    float IOD_f = (float) temp;

    if(savedX_LP == 0){
      savedX_LP = landmarkPos;
      return posX_IncrementOrDecrement;
    }

    Boolean isPassThreshold = (Math.abs(savedX_LP - landmarkPos) > threshold);

    restXCounter--;

    // -- for replacing the initial savedX_LP to reset the threshold
    // -- in order to not start speeding up
    if(restXCounter <= 0) {
      savedX_LP = landmarkPos;
      restXCounter = resetRestXCounter;
    }

    // -- if passed the threshold both counter will start
    if(isPassThreshold || speedXCounter >= 0)
    {
      restXCounter = resetRestXCounter;

      if(landmarkPos>savedX_LP && cursorPos < maxPos) //increment
        posX_IncrementOrDecrement = posX_IncrementOrDecrement+IOD_f;
      if(landmarkPos<savedX_LP && cursorPos > minPos) //decrement
        posX_IncrementOrDecrement = posX_IncrementOrDecrement-IOD_f;

      savedX_LP = landmarkPos;
      speedXCounter--;

      if(isPassThreshold)
        speedXCounter = resetSpeedXCounter;
    }

    return posX_IncrementOrDecrement;
  }

  //Incrementataion and Decrementation for Y Axis
  private float savedY_LP = 0, posY_IncrementOrDecrement = 0, restYCounter = 3, resetRestYCounter = 3,speedYCounter = 0, resetSpeedYCounter = 2;
  private float cursorPosY_IOD(float landmarkPos, int IOD, int threshold, float cursorPos){
    float maxPos = 0.999F * Screen_TotalHeight;
    float minPos = 0.001F * Screen_TotalHeight;

    double temp = (double) IOD * 1; // Example. 2 = 0.2 pixel, convert
    float IOD_f = (float) temp;

    if(savedY_LP == 0){
      savedY_LP = landmarkPos;
      return posY_IncrementOrDecrement;
    }

    Boolean isPassThreshold = (Math.abs(savedY_LP - landmarkPos) > threshold);

    restYCounter--;

    // -- for replacing the initial savedY_LP to reset the threshold
    // -- in order to not start speeding up
    if(restYCounter <= 0) {
      savedY_LP = landmarkPos;
      restYCounter = resetRestYCounter;
    }

    // -- if passed the threshold both counter will start
    if(isPassThreshold || speedYCounter >= 0)
    {
      restYCounter = resetRestYCounter;

      if(landmarkPos>savedY_LP && cursorPos < maxPos) //increment
        posY_IncrementOrDecrement = posY_IncrementOrDecrement+IOD_f;
      if(landmarkPos<savedY_LP && cursorPos > minPos) //decrement
        posY_IncrementOrDecrement = posY_IncrementOrDecrement-IOD_f;

      savedY_LP = landmarkPos;
      speedYCounter--;

      if(isPassThreshold)
        speedYCounter = resetSpeedYCounter;
    }

    return posY_IncrementOrDecrement;
  }

  private float initialNosePosX = 0, initialNosePosY = 0, noseThresholdX = 0, noseThresholdY = 0, nosePos[];
  private float[] getNosePos_withThreshold(float XPos_Nose, float YPos_Nose, float threshold){
    float isNoseMoving = 0; // 0 = false, 1 = true
    nosePos = new float[]{
      XPos_Nose, YPos_Nose,
      initialNosePosX, initialNosePosY,
      isNoseMoving
    };

    // -- Differentiating if there is a saved from initial value
    if(initialNosePosX != 0) { // there should be an initial value
      noseThresholdX = Math.abs(nosePos[0] - initialNosePosX);
    }else{initialNosePosX = nosePos[0];} // else save first
    if(initialNosePosY != 0) {
      noseThresholdY = Math.abs(nosePos[1] - initialNosePosY);
    }else{initialNosePosY = nosePos[1];}

    // Comparing if the threshold has not been pass
    // if not, maintain same value of the last
    // if yes, overwrite the savedNosePos
    if(noseThresholdX < threshold) { // must be less to threshold
      nosePos[0] = initialNosePosX; //if not pass threshold, overwrite the nosPos value by old position to maintain same initial position
    } else {
      initialNosePosX = nosePos[0]; // if yes, create new initial value
      isNoseMoving = 1;
      nosePos[4] = isNoseMoving; // means it moves
    }
    if(noseThresholdY < threshold) {
      nosePos[1] = initialNosePosY;
    } else {
      initialNosePosY = nosePos[1];
      isNoseMoving = 1;
      nosePos[4] = isNoseMoving; // means it moves
    }

    // -- returns initial value or the new value that has been pass the threshold
    return nosePos;
  }

  // TODO: Get the getNosePos_withThreshold() return both the saved and the updated x and y
  // TODO: Use these x and y to smoothen the movement of the cursor
  // TODO: return the smoothenPath
  // TODO: create time delay in looping the smoothPath
  // TODO: Each path will change the position of the cursor
  // TODO: currenly 5 pixel per move
  // TODO: increase cursor threshold if possible
  // TODO: Ojbective: 10 threshold above
  int[][] defaultPath = {{0,0,0}};
  public int[][] getSmoothPath(float NosePos[], float stepCount, float time) {
    ArrayList<String> PathList = new ArrayList<>();
    String Path;

    float XPos_Nose = NosePos[0];
    float YPos_Nose = NosePos[1];
    float initialXPos_Nose = NosePos[2];
    float initialYPos_Nose = NosePos[3];
    float isNoseMoving = NosePos[4];

    if((int) initialXPos_Nose == 0)
      initialXPos_Nose = XPos_Nose;
    if((int) initialYPos_Nose == 0)
      initialYPos_Nose = YPos_Nose;

    if(XPos_Nose == initialXPos_Nose && YPos_Nose == initialYPos_Nose){
      defaultPath = new int[][]{{(int) XPos_Nose, (int) YPos_Nose, 1, (int) isNoseMoving}};
      return defaultPath; // returns list of array{posX,posY,timePerStep,isNoseMoving}
    }

    if(stepCount<1)
      stepCount=1;
    if(time<1)
      time=1;

    // TODO: If savedPos has no value don't do smoothening at first
    double pixelStepX = (XPos_Nose - initialXPos_Nose) / ((double) stepCount);
    double pixelStepY = (YPos_Nose - initialYPos_Nose) / ((double) stepCount);
    double timePerStep = time / ((double) stepCount);
    for (int step = 1; step <= stepCount; step++) {
      Path = (int) (initialXPos_Nose + pixelStepX * step)+", "+(int) (initialYPos_Nose + pixelStepY * step) + ", " + (int) timePerStep + ", " + (int) isNoseMoving;
      PathList.add(Path);
    }
    int[][] PathList_ArrayConverted = smoothPath_ToArray(PathList,4);

    return PathList_ArrayConverted; // returns list of array{posX,posY,timePerStep,isNoseMoving}
  }

  private int[][] smoothPath_ToArray(ArrayList<String> smoothPath, int indexPath){
    int[][] PathList = new int[smoothPath.toArray().length][indexPath];
    String[] tempArrayHolder;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      for (int path = 0; path < PathList.length; path++) {
        tempArrayHolder = smoothPath.get(path).split(", ");
        for(int index = 0; index<tempArrayHolder.length; index++) {
          PathList[path][index] = Integer.parseInt(tempArrayHolder[index]);
        }
      }
    }

    return PathList;
  }

  private float calibrateCursorAxis(float screenSize, float axisPos){
    float maxBoundary = screenSize * 0.999F;
    float minBoundary = screenSize * 0.001F;

    if(axisPos >= maxBoundary)
      return maxBoundary;
    if(axisPos <= minBoundary)
      return minBoundary;

    return axisPos;
  }

  float isGazeMoving = 0; // 0 = false, 1 = true
  private void IOD_XnY(float posX, float posY, float Xmax, float Xmin, float Ymax, float Ymin){
    float XPos_Nose = posX;
    float YPos_Nose = posY;

    // ----- X iris pos -- this is now converted to Ypos using vertical Gaze
    if((eyeRight_XRatio[0] >= Xmax/100 || eyeRight_XRatio[0] <= Xmin/100)){
      IOD_eyeGazeX(eyeRight_XRatio[0], Xmin, Xmax, Cursor_XPos);
      posX = XPos_Nose + Cursor_XAdjustment;
      isGazeMoving = 1; // Eye gazing is working
    }
    else{
      isGazeMoving = 0;
    }
    // ----- Y iris Pos
    if(eyeRight_YRatio[0] >= Ymax/100 || eyeRight_YRatio[0] <= Ymin/100){
      IOD_eyeGazeY(eyeRight_YRatio[0], Ymin, Ymax, Cursor_YPos);
      posY = YPos_Nose + Cursor_YAdjustment;
      isGazeMoving = 1; // Eye gazing is working
    }
    else{
      isGazeMoving = 0;
    }

    Cursor_XPos = posX;
    Cursor_YPos = posY;
  }

  private void IOD_eyeGazeX(float posX, float min, float max, float cursorPos){
    float minEyeGazePos = (min/100);
    float maxEyeGazePos = (max/100);
    float maxPos = 0.99F * Screen_TotalWidth;
    float minPos = 0.01F * Screen_TotalWidth;

    if(posX >= maxEyeGazePos && cursorPos < maxPos)
      Cursor_XAdjustment = Cursor_XAdjustment+20;
    if(posX <= minEyeGazePos && cursorPos > minPos)
      Cursor_XAdjustment = Cursor_XAdjustment-20;
  }
  private void IOD_eyeGazeY(float posY, float min, float max, float cursorPos){
    float minEyeGazePos = (min/100);
    float maxEyeGazePos = (max/100);
    float maxPos = 0.99F * Screen_TotalHeight;
    float minPos = 0.01F * Screen_TotalHeight;

    if(posY >= maxEyeGazePos && cursorPos < maxPos)
      Cursor_YAdjustment = Cursor_YAdjustment+40;
    if(posY <= minEyeGazePos && cursorPos > minPos)
      Cursor_YAdjustment = Cursor_YAdjustment-40;
  }

  private float calibrateEyeRightRatioX(float min, float max, float eyeRight_XRatio){
    float newEyeRightXRatio;
    float pos = eyeRight_XRatio;
    float maxPos = 0.999F * Screen_TotalWidth;
    float minPos = 0.001F * Screen_TotalWidth;
    float minEyeGazePos = min/100;
    float maxEyeGazePos = max/100;

    // if they over extend from the min and max of EyeGazePos that means its the end
    // this is to create a new min and max for gaze instead of 1 - 100
    // it will be 35 - 50 of the eye ratio 35:0 and 50:100
    if(pos >= maxEyeGazePos)
      return maxPos;
    if(pos <= minEyeGazePos)
      return minPos;

    // -- check again with new minimun and maximum threshold
    float maxRatio = maxEyeGazePos - minEyeGazePos; // new fixed Max where 0 to min is remove
    float minRatio = pos - minEyeGazePos; // remove min from gazePos this will be the controller
    newEyeRightXRatio = minRatio/maxRatio; // this is within min and max

    pos = newEyeRightXRatio * Screen_TotalWidth;
    if(pos >= maxPos)
      return maxPos;
    if(pos <= minPos)
      return minPos;

    return pos;
  }

  private float calibrateEyeRightRatioY(float min, float max, float eyeRight_YRatio){
    float newEyeRightYRatio;
    float pos = eyeRight_YRatio;
    float maxPos = 0.999F * Screen_TotalHeight;
    float minPos = 0.001F * Screen_TotalHeight;
    float minEyeGazePos = min/100;
    float maxEyeGazePos = max/100;

    // if they over extend from the min and max of EyeGazePos that means its the end
    // this is to create a new min and max for gaze instead of 1 - 100
    // it will be 35 - 50 of the eye ratio 35:0 and 50:100
    if(pos >= maxEyeGazePos)
      return maxPos;
    if(pos <= minEyeGazePos)
      return minPos;

    // -- check again with new minimun and maximum threshold
    float maxRatio = maxEyeGazePos - minEyeGazePos; // new fixed Max where 0 to min is remove
    float minRatio = pos - minEyeGazePos; // remove min from gazePos this will be the controller
    newEyeRightYRatio = minRatio/maxRatio; // this is within min and max

    pos = newEyeRightYRatio * Screen_TotalHeight;
    if(pos >= maxPos)
      return maxPos;
    if(pos <= minPos)
      return minPos;

    return pos;
  }

  private float[] getGridGazePosition(float eyeXRatioPosition, float eyeYRatioPosition){
    float[] gazePosition = {Screen_XCenterPos, Screen_YCenterPos};
    float eyeGazePos = eyeXRatioPosition;

    float rowCenter_One = Screen_TotalHeight * 0.25f;
    float rowOne_Min = Screen_TotalHeight * 0.01f;
    float rowOne_Max = Screen_TotalHeight * 0.58f;

    float rowCenter_Two = Screen_TotalHeight * 0.75f;
    float rowTwo_Min = Screen_TotalWidth * 0.42f;
    float rowTwo_Max = Screen_TotalHeight * 0.99f;

    float colCenter_One = Screen_TotalWidth * 0.1f;
    float colOne_Min = Screen_TotalWidth * 0.001f;
    float colOne_Max = Screen_TotalWidth * 0.199f;

    float colCenter_Two = Screen_TotalWidth * 0.3f;
    float colTwo_Min = Screen_TotalWidth * 0.2f;
    float colTwo_Max = Screen_TotalWidth * 0.399f;

    float colCenter_Three = Screen_TotalWidth * 0.5f;
    float colThree_Min = Screen_TotalWidth * 0.4f;
    float colThree_Max = Screen_TotalWidth * 0.599f;

    float colCenter_Four = Screen_TotalWidth * 0.7f;
    float colFour_Min = Screen_TotalWidth * 0.6f;
    float colFour_Max = Screen_TotalWidth * 0.799f;

    float colCenter_Five = Screen_TotalWidth * 0.9f;
    float colFive_Min = Screen_TotalWidth * 0.8f;
    float colFive_Max = Screen_TotalWidth * 0.999f;

    // horizontal
    if(eyeXRatioPosition >= colOne_Min && eyeXRatioPosition <= colOne_Max)
      gazePosition[0] = colCenter_One;
    if(eyeXRatioPosition >= colTwo_Min && eyeXRatioPosition <= colTwo_Max)
      gazePosition[0] = colCenter_Two;
    if(eyeXRatioPosition >= colThree_Min && eyeXRatioPosition <= colThree_Max)
      gazePosition[0] = colCenter_Three;
    if(eyeXRatioPosition >= colFour_Min && eyeXRatioPosition <= colFour_Max)
      gazePosition[0] = colCenter_Four;
    if(eyeXRatioPosition >= colFive_Min && eyeXRatioPosition <= colFive_Max)
      gazePosition[0] = colCenter_Five;

    // vertical
    if(eyeYRatioPosition >= rowOne_Min && eyeYRatioPosition <= rowOne_Max)
      gazePosition[1] = rowCenter_One;
    if(eyeYRatioPosition >= rowTwo_Min && eyeYRatioPosition <= rowTwo_Max)
      gazePosition[1] = rowCenter_Two;

    return gazePosition;
  }
  private float initialGazePos = 0;
  public float getReset_IfGazeNotTheSame(float gazePos, float cursorPos){
    float pos = gazePos; // if gaze is not same return new position based on gaze

    if(initialGazePos == 0 || gazePos == initialGazePos)
      pos = cursorPos; // if gaze is not zero or no same value return same cursorPos

    initialGazePos = gazePos;
    return pos;
  }

  private int isMouthOpen(float mouth_TopYPos, float mouth_BottomYPos){
    int mouthIsOpen = 0;
    float mouthOpenThreshold = 70;

    float TopToBottomMouth_Distance = Math.abs(mouth_TopYPos - mouth_BottomYPos);
    if(TopToBottomMouth_Distance>mouthOpenThreshold)
      mouthIsOpen = 1;

    return mouthIsOpen;
  }

  private float
          Mouth_TopYPos = 0, Mouth_BottomYPos = 0,
          Cursor_XPos = 0 /*Horizontal*/,  Cursor_YPos = 0 /*Vertical*/, Cursor_ZPos /*Distance*/,
          Nose_XPos, Nose_YPos, Nose_ZPos,
          RI_XPos, RI_YPos,
          RawXPos_LE, RawYPos_LE,
          RawXPos_RE, RawYPos_RE,
          Screen_XCenterPos, Screen_YCenterPos,
          Screen_TotalWidth, Screen_TotalHeight,
          Cursor_XAdjustment = 0, Cursor_YAdjustment = 0,
          IncrementOrDecrement_cursorSpeed[] = {/* x */ 0, /* y */ 0},
          tempCXPos = 0, tempCYPos = 0,
          irisRight_XPosition, irisRight_YPosition,
          irisRight_X_IOD, irisRight_Y_IOD,
          eyeRight_XRatio[]={0,0,0}, eyeRight_YRatio[]={0,0,0};

  private int[][] Nose_SmoothPathList, InitialNose_SmoothPathList;

  private boolean
          debounceAdjustment = false,
          debounceIAD = false,
          debounceThreshold = false,
          isFaceMissing = true;
  private int counterA = 0, counterIOD = 0, maxCount = 7; // delaying the cursor to adjust properly
  private int otp = 1;
  private void logEyesLandmarks(FaceMeshResult result) {
    // -- set all the sharedpreferences data
    setCursorProperties();

    // -- set ScreenSize based from globalActionAccessabilityService
    float screenSize[] = owService.getScreenSize();
    Screen_TotalWidth = screenSize[0];
    Screen_TotalHeight = screenSize[1];
    if(Screen_TotalHeight == 0 || Screen_TotalWidth == 0){
      WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
      DisplayMetrics displayMetrics = new DisplayMetrics();
      windowManager.getDefaultDisplay().getMetrics(displayMetrics);
      if(Screen_TotalWidth == 0)
        Screen_TotalWidth = displayMetrics.widthPixels;
      if(Screen_TotalHeight == 0)
        Screen_TotalHeight = displayMetrics.heightPixels;
    }

    // ---------- CALIBRATION STARTS HERE
    // -- Setting the value of isFaceOnCam
    if (result == null || result.multiFaceLandmarks().isEmpty()) {
      if(AccessibilityService()) { // only send the data if the accessibility is running already
        owService.setIsFaceOnCam(0); // false = no face
        isFaceMissing = true;
      }
      return;
    }
    owService.setIsFaceOnCam(1); // true = face found
    // > For Bitmaps, show the pixel values.
    // > For texture inputs, show the normalized coordinates
    /*
        =======================================
          ====== Initializing Landmarks =====
        =======================================
    */

    // -- NOSE FACE LANDMARK
    NormalizedLandmark NoseFace_Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(0);

    // -- RIGHT EYE LANDMAKRS
    NormalizedLandmark RE_LEFTLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_LEFT);
    NormalizedLandmark RE_RIGHTLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_RIGHT);
    NormalizedLandmark RE_TOP1Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_TOP1);
    NormalizedLandmark RE_TOP2Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_TOP2);
    NormalizedLandmark RE_TOP3Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_TOP3);
    NormalizedLandmark RE_TOPTORIGTH1Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_TOPTORIGTH1);
    NormalizedLandmark RE_TOPTOLEFT1Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_TOPTOLEFT1);
    NormalizedLandmark RE_BOTTOMTORIGTH1Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_BOTTOMTORIGTH1);
    NormalizedLandmark RE_BOTTOMTOLEFT1Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_BOTTOMTOLEFT1);

    NormalizedLandmark RE_RIGHTTOBOTTOMBOTTOM1Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_RIGHTTOBOTTOMBOTTOM1);
    NormalizedLandmark RE_RIGHTTOTOPTOP1Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_RIGHTTOTOPTOP1);
    NormalizedLandmark RE_RIGHTTOBOTTOM1Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_RIGHTTOBOTTOMBOTTOM1);
    NormalizedLandmark RE_RIGHTTOTOP1Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_RIGHTTOTOPTOP1);
    NormalizedLandmark RE_RIGHTTOBOTTOM2Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_RIGHTTOBOTTOM2);
    NormalizedLandmark RE_RIGHTTOTOP2Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_RIGHTTOTOP2);
    NormalizedLandmark RE_RIGHTTOBOTTOM3Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_RIGHTTOBOTTOM3);
    NormalizedLandmark RE_RIGHTTOTOP3Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_RIGHTTOTOP3);

    NormalizedLandmark RE_LEFT2TOTOP1Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_LEFT2TOTOP1);
    NormalizedLandmark RE_LEFT2TOBOTTOM1Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_LEFT2TOBOTTOM1);
    NormalizedLandmark RE_BOTTOM1Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_BOTTOM1);
    NormalizedLandmark RE_BOTTOM3Landmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_BOTTOM3);
    NormalizedLandmark rightCenterIrisLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(RightEye_CENTER_IRIS);
    // -- LEFT EYE LANDMAKRS
    NormalizedLandmark LeftEye_LEFTLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(LeftEye_LEFT);
    NormalizedLandmark LeftEye_RIGHTLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(LeftEye_RIGHT);
    NormalizedLandmark LeftEye_TOPLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(LeftEye_TOP);
    NormalizedLandmark LeftEye_BOTTOMLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(LeftEye_BOTTOM);
    NormalizedLandmark LeftCenterIrisLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(LeftEye_CENTER_IRIS);
    // -- MOUTH LANDMARKS
    NormalizedLandmark Mouth_TOPLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(MouthTop);
    NormalizedLandmark Mouth_BOTTOMLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(MouthBottom);
    /*
        ===================================
          ====== Updating Variables =====
        ===================================
    */
    // ------------ MOUTH VALUE MANIPULATION
    // -- Replace value container of mouth landmarks -- MOUTH LANDMARKS
    Mouth_TopYPos = (float) Math.floor(Mouth_TOPLandmark.getY()*(Screen_TotalHeight));
    Mouth_BottomYPos = (float) Math.floor(Mouth_BOTTOMLandmark.getY()*(Screen_TotalHeight));

    int mouthIs = isMouthOpen(Mouth_TopYPos, Mouth_BottomYPos);

    // -- Source position of x and y landmark of rightCenterIris
    cursor = findViewById(R.id.cursor);
    cursor.setVisibility(View.INVISIBLE);
    //float cursorPos = cursor.getLeft();
    // -- Multiply the cursor position with screen size for optimize scaling position
      Nose_XPos = (float) Math.floor(/*cursorPos+(*/NoseFace_Landmark.getX() * (Screen_TotalWidth)); // X-Axis = Horizontal true
      Nose_YPos = (float) Math.floor(/*cursorPos+(*/NoseFace_Landmark.getY() * (Screen_TotalHeight)); // Y-Axis
      Nose_ZPos = Math.abs(NoseFace_Landmark.getZ());
    RI_XPos = (float) Math.floor(/*cursorPos+(*/rightCenterIrisLandmark.getX() * (Screen_TotalWidth)); // X-Axis = Horizontal true
    RI_YPos = (float) Math.floor(/*cursorPos+(*/rightCenterIrisLandmark.getY() * (Screen_TotalHeight)); // Y-Axis

    // -- call the recalibrateCursorToCenterScreen function to overwrite Cursor values from nose values
    if(owService.getIfCalibrationDone() == true){
      initialNosePosX = Screen_XCenterPos;
      initialNosePosY = Screen_YCenterPos;
      Nose_XPos = Screen_XCenterPos;
      Nose_YPos = Screen_YCenterPos;
      RI_XPos = Screen_XCenterPos;
      RI_YPos = Screen_YCenterPos;
      Cursor_XAdjustment = 0;
      Cursor_YAdjustment = 0-(Screen_TotalHeight*0.25f);
      IncrementOrDecrement_cursorSpeed[0] = 0;
      IncrementOrDecrement_cursorSpeed[1] = 0;
      savedX_LP = Screen_XCenterPos; posX_IncrementOrDecrement = 0;
      savedY_LP = Screen_YCenterPos; posY_IncrementOrDecrement = 0;
      owService.setIfCalibrationDone(false);
    }

    // -- Send Nose values to service
    owService.setNosePos(Nose_XPos, Nose_YPos, Nose_ZPos);

    // -- Get screen center
    Screen_XCenterPos = (Screen_TotalWidth / 2);
    Screen_YCenterPos = (Screen_TotalHeight / 2);
    owService.setCursorCenterPos((int)Screen_XCenterPos, (int)Screen_YCenterPos);

    // ----------------- Iris Position based on Eye surface length
    // ----- X iris pos
    float Xmin = 5f/*left*/, Xmax = 50/*right*/; float Ymin = 0/*up*/, Ymax = 40/*down*/;
    eyeRight_XRatio = irisXPostion(rightCenterIrisLandmark, RE_TOPTORIGTH1Landmark, RE_TOPTOLEFT1Landmark);
    irisRight_XPosition = calibrateEyeRightRatioX(20,30,eyeRight_XRatio[0]);
    // ----- Y iris Pos
    eyeRight_YRatio = irisYPostion(rightCenterIrisLandmark, RE_LEFT2TOBOTTOM1Landmark, RE_LEFT2TOTOP1Landmark);
    irisRight_YPosition = calibrateEyeRightRatioY(10,40,eyeRight_YRatio[0]);

      owService.setCursorData_EG((int)Cursor_XPos, (int)Screen_YCenterPos, (int)Cursor_ZPos);
      // -- Reposition cursor based on eye gaze using grid position
      float[] eyeGazePosition = getGridGazePosition(irisRight_XPosition, Screen_YCenterPos);
      //Cursor_XPos = getReset_IfGazeNotTheSame(eyeGazePosition[0], Cursor_XPos);

    if(mouthIs == 1 && owService.getIsFaceOnCenterScreen() == 1){
      IOD_XnY(Cursor_XPos, Cursor_YPos, Xmax, Xmin, Ymax, Ymin);
    }

    int speedXThreshold = (int)(cursorThreshold-(cursorThreshold*0.3f)), speedYThreshold = cursorThreshold;
      if(debounceAdjustment==false && debounceIAD==false)
      {

        // ----------- For increasing the speed of cursor (increment and decrement)
        IncrementOrDecrement_cursorSpeed[0] = cursorPosX_IOD(RI_XPos, cursorSpeedX, speedXThreshold, Cursor_XPos);
        IncrementOrDecrement_cursorSpeed[1] = cursorPosY_IOD(RI_YPos, cursorSpeedY, speedYThreshold, Cursor_YPos);
        //debounceIAD=true;

        // -- Call the functions to update the cursor properties
        //updateCursorVelocity();
        updateCursorWithAccelerationAndDeceleration(Cursor_XPos, Cursor_YPos, cursorAcceleration);

        // -- Set the Properties to cursor main position
        Cursor_XPos = RI_XPos + IncrementOrDecrement_cursorSpeed[0] + Cursor_XAdjustment + accelerationX;
        Cursor_YPos = RI_YPos + IncrementOrDecrement_cursorSpeed[1] + Cursor_YAdjustment + accelerationY;
        Cursor_ZPos = Nose_ZPos * 1000;

        // -- Calibrate Cursor boundaries
        Cursor_XPos = calibrateCursorAxis(Screen_TotalWidth, Cursor_XPos);
        Cursor_YPos = calibrateCursorAxis(Screen_TotalHeight+(Screen_TotalHeight*0.05f), Cursor_YPos);

        float NosePos[] = getNosePos_withThreshold(Cursor_XPos, Cursor_YPos, cursorThreshold);

        Cursor_XPos = NosePos[0];
        Cursor_YPos = NosePos[1];

        float stepCount = cursorSmoothening, time = cursorSmoothening;
        Nose_SmoothPathList = getSmoothPath(NosePos, stepCount, time); // returns list of path
        if(InitialNose_SmoothPathList == null)
          InitialNose_SmoothPathList = Nose_SmoothPathList;

        // -- irisRight_XPosition repositioning
        /*cursor.setX(Cursor_XPos);
        cursor.setY(Cursor_YPos);*/

        if(Nose_SmoothPathList.length >= stepCount && Nose_SmoothPathList[Nose_SmoothPathList.length-1][0] != InitialNose_SmoothPathList[InitialNose_SmoothPathList.length-1][0] || Nose_SmoothPathList[Nose_SmoothPathList.length-1][1] != InitialNose_SmoothPathList[InitialNose_SmoothPathList.length-1][1]){
          otp = 1;
          try {
          for (int pathNum = 1; pathNum < Nose_SmoothPathList.length; pathNum++) {
            int _posX = Nose_SmoothPathList[pathNum][0];
            int _posY = Nose_SmoothPathList[pathNum][1];
            int _timePerStep = Nose_SmoothPathList[pathNum][2];
            int isNoseMoving = Nose_SmoothPathList[pathNum][3];

            // TODO: fix this
            Thread.sleep(1);

            // -- Send properties to OverlayWidgetsService
            sendCursorDataToService(_posX, _posY, Cursor_ZPos, isNoseMoving);
            sendMouthDataToService((int)Mouth_TopYPos, (int)Mouth_BottomYPos, mouthIs);
          }
        }catch(InterruptedException e){
          e.printStackTrace();
        }
          InitialNose_SmoothPathList = Nose_SmoothPathList;
        }else if (Nose_SmoothPathList[0][3] == 0 && otp == 1){
          otp = 0;
          sendCursorDataToService(Nose_SmoothPathList[0][0], Nose_SmoothPathList[0][1], Nose_SmoothPathList[0][2], Nose_SmoothPathList[0][3]);
          sendMouthDataToService((int)Mouth_TopYPos, (int)Mouth_BottomYPos, mouthIs);
        }

        sendCursorSizeToService(cursorSize_Width,cursorSize_Height);
      }
      else
      {
        if (debounceAdjustment==true && counterA < maxCount) {counterA = counterA + 1;}
        else {counterA = 0; debounceAdjustment = false;}
        if (debounceIAD==true && counterIOD < 2) {counterIOD = counterIOD + 1;}
        else {counterIOD = 0; debounceIAD = false;}
      }


    Log.i(
            "Cursor_Coordinates",
            String.format(
                    "MediaPipe Face Mesh nose normalized coordinates (value range: [X:Horizontal, Y:Vertical]): " +
                            "[RxN=%f, RyN=%f] - " +
                            "[Cx=%f, Cy=%f] - " +
                            "[IODx=%f, IODy=%f] - " +
                            "[xA=%f, yA=%f] - " +
                            "[Sx=%f, Sy=%f] - "+
                            "[SCenter_x=%f, SCenter_y=%f] - ",
                    Nose_XPos, Nose_YPos,
                    Cursor_XPos, Cursor_YPos,
                    IncrementOrDecrement_cursorSpeed[0], IncrementOrDecrement_cursorSpeed[1],
                    Cursor_XAdjustment, Cursor_YAdjustment,
                    Screen_TotalWidth, Screen_TotalHeight,
                    Screen_XCenterPos, Screen_YCenterPos)
    );
    Log.i(
            "Cursor_Properties",
            String.format(
                    "(value range: [X:Horizontal, Y:Vertical]): " +
                            "[sX=%f, sY=%f - " +
                            //"[vX=%f, vY=%f] - " +
                            "[aX=%f, aY=%f] - " +
                            "[aXH=%f, aYH=%f] - " +
                            "[dXH=%f, dYH=%f]",
                    IncrementOrDecrement_cursorSpeed[0], IncrementOrDecrement_cursorSpeed[1],
                    //velocityX, velocityY,
                    accelerationX, accelerationY,
                    accelerationXHolder, accelerationYHolder,
                    decelerationXHolder, decelerationYHolder)
    );
    Log.i(
            "EyeGaze",
            String.format(
                    "(value range: [X:Horizontal, Y:Vertical]): " +
                            "[xG=%f, yG=%f, - "+
                            "[rCx=%f, rCy=%f, rCz=%f] --- "+
                            "[TRx=%f, TLx=%f, - "+
                            "[eyeXratio=%f, Xmin_D=%f, Xmax_D=%f, dif=%f --- "+
                            "[RBy=%f, RTy=%f, - "+
                            "[eyeYRatio=%f, Ymin_D=%f, Ymax_D=%f, dif=%f - "+
                            "[irisRight_XPosition=%f, irisRight_YPosition=%f]",
                    eyeRight_XRatio[0]*Screen_TotalWidth, eyeRight_YRatio[0]*Screen_TotalHeight,
                    rightCenterIrisLandmark.getX(), rightCenterIrisLandmark.getY(), rightCenterIrisLandmark.getZ(),
                    RE_TOPTORIGTH1Landmark.getX(), RE_TOPTOLEFT1Landmark.getX(),
                    eyeRight_XRatio[0], eyeRight_XRatio[1], eyeRight_XRatio[2], eyeRight_XRatio[2]-eyeRight_XRatio[1],
                    RE_RIGHTTOBOTTOM1Landmark.getY(), RE_RIGHTTOTOP1Landmark.getY(),
                    eyeRight_YRatio[0], eyeRight_YRatio[1], eyeRight_YRatio[2], eyeRight_YRatio[2]-eyeRight_YRatio[1],
                    irisRight_XPosition, irisRight_YPosition)
    );
  }

  public void getPermissions(){
      Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
      startActivityForResult(intent, 25);
  }

  /*
  -------------------------------------------------------------------------
  --------------------------- ACCELERATION AND THRESHOLD PROPERTY
  -------------------------------------------------------------------------
  */

  //float velocityX = 0.0f, velocityY = 0.0f; // Initial velocity
  //float RawXPos_Nose_OldVelocity, RawYPos_Nose_OldVelocity; // Save values of velocity
  float initialPosX_ForAcc = 0.0f, initialPosY_ForAcc = 0.0f; // Use for threshold
  float accelerationX = 0.0f, accelerationY = 0.0f;  // Initial acceleration
  float accelerationXHolder = cursorSpeedX*0.2f, accelerationYHolder = cursorSpeedY*0.2f; // Maintain all values while moving else reset
  float decelerationXHolder = 0.0f, decelerationYHolder = 0.0f; // Maintain all values while moving else reset
  float accelerationThreshold = cursorThreshold*1.5f; // Threshold to determine when the cursor is at rest
  //float accelerationFactor = 1.1f; // Factor to gradually increase velocity over time
  //float decelerationFactor = 0.87f; // Factor to gradually reduce velocity over time


  // Update the cursor velocity based on user input
  /*void updateCursorVelocity() {
    if(RawXPos_Nose_OldVelocity != 0 && RawYPos_Nose_OldVelocity != 0)
    {
      // Calculate the change in position
      float deltaX = Nose_XPos - RawXPos_Nose_OldVelocity;
      float deltaY = Nose_YPos - RawYPos_Nose_OldVelocity;
      // Calculate the new velocity based on the change in position
      velocityX = deltaX;
      velocityY = deltaY;
    }
    // -- saved values of Cursor_rawPos
    RawXPos_Nose_OldVelocity = Nose_XPos;
    RawYPos_Nose_OldVelocity = Nose_YPos;
  }*/

  // Update the cursor position with acceleration, deceleration, and reset when at rest
  /*void updateCursorWithAccelerationAndDeceleration(float posX, float posY) {
    // Apply value to acceleration
    if (accelerationX == 0.0f)
        accelerationX = posX;
    if (accelerationY == 0.0f)
        accelerationY = posY;

    // Apply acceleration to the velocity
    if (Math.abs(velocityX) > accelerationThreshold)
      accelerationX = accelerationX*accelerationFactor; // Increase the X acceleration by 10%
    if (Math.abs(velocityX) > accelerationThreshold)
      accelerationY = accelerationY*accelerationFactor; // Increase the Y acceleration by 10%

    // Reset acceleration if the cursor is at rest
    if (accelerationX < 3.0f) accelerationX = 0.0f;
    if (accelerationY < 3.0f) accelerationY = 0.0f;

    // Apply deceleration to gradually reduce velocity over time
    if (Math.abs(velocityX) <= accelerationThreshold && accelerationX != 0.0f)
      accelerationX = accelerationX*decelerationFactor;
    if (Math.abs(velocityY) <= accelerationThreshold && accelerationY != 0.0f)
      accelerationY = accelerationY*decelerationFactor;

    // TODO: FIX DECELERATION

    // Hold acceleration in highest value
    if (Math.abs(velocityX) > accelerationThreshold)
      accelerationXHolder = accelerationX;
    if (Math.abs(velocityX) > accelerationThreshold)
      accelerationYHolder = accelerationY;

    /* Hold all values of deceleration
    float sign = Math.signum(accelerationX);
    if (Math.abs(velocityX) <= accelerationThreshold && accelerationX != 0.0f)
    {
      if (sign < 0) // negative value
        decelerationXHolder = decelerationXHolder-accelerationX;
      if (sign > 0) // positive value
        decelerationXHolder = decelerationXHolder+accelerationX;
    }
    if (Math.abs(velocityY) <= accelerationThreshold && accelerationY != 0.0f)
    {
      if (sign < 0) // negative value
        decelerationYHolder = decelerationYHolder-accelerationY;
      if (sign > 0) // positive value
        decelerationYHolder = decelerationYHolder+accelerationY;
    }
  }*/

  // Update the cursor position with acceleration and retain when at rest
  void updateCursorWithAccelerationAndDeceleration(float posX, float posY, float accelerationFactor/* Factor to gradually increase speed over time*/) {
    // Apply acceleration to the velocity
    if (Math.abs(posX - initialPosX_ForAcc) > accelerationThreshold){
      accelerationX = accelerationX+(accelerationXHolder*accelerationFactor); // Increase the X acceleration by 10%

      initialPosX_ForAcc = posX;
    }else{
      accelerationXHolder = cursorSpeedX*0.01f; // reset value of holder based from cursor speed
    }
    if (Math.abs(posY - initialPosY_ForAcc) > accelerationThreshold){
      accelerationY = accelerationY+(accelerationYHolder*accelerationFactor); // Increase the Y acceleration by 10%

      initialPosY_ForAcc = posY;
    }else{
      accelerationYHolder = cursorSpeedY*0.01f;
    }
  }

  private boolean AccessibilityService() {
    String accessibilityServices = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
    return accessibilityServices != null && accessibilityServices.contains(getPackageName());
  }

  private boolean CameraPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
    return false;
  }

  private boolean OverlayPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return Settings.canDrawOverlays(this);
    }
    return false;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == 80) {
      if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
        isPermission = false;
        permissionsNotDone();
      }
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 80) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (!Settings.canDrawOverlays(this)){
          isPermission = false;
          permissionsNotDone();
        }
      }
    }
  }
  public void permissionsNotDone(){
    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_MULTI_PROCESS);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean("permissions", false);
    editor.apply();
  }

  private void checkPermissions(){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (!CameraPermission() || !OverlayPermission()){
        isPermission = false;
        permissionsNotDone();
      }
    }
  }
}

