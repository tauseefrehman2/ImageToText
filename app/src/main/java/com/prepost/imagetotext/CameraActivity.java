package com.prepost.imagetotext;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.prepost.imagetotext.Models.ExtractedModel;
import com.prepost.imagetotext.database.Database;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.view.CameraView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import static com.prepost.imagetotext.MainActivity.EXTRACTED_TEXT_CODE;
import static com.prepost.imagetotext.MainActivity.SET_EXTRACTED_TEXT;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "CameraActivity";

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    private CameraView mCameraView;
    private CameraSelector cameraSelector;

    private FrameLayout adContainerView;
    private ImageView capturedImage_iv;
    private ProgressBar progressBar;
    private Uri photoURI;

    private final int UPDATE_APP_REQUEST_CODE = 1112;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        init();
        loadAds();
        callInAppUpdate();

        //Request for Permission
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        callInAppUpdate();
    }

    private void init() {
        mCameraView = findViewById(R.id.camera_cv);
        mCameraView.setFlash(ImageCapture.FLASH_MODE_AUTO);

        findViewById(R.id.camera_capture_iv).setOnClickListener(this);
        findViewById(R.id.camera_rotate_iv).setOnClickListener(this);
        capturedImage_iv = findViewById(R.id.camera_captured_iv);
        capturedImage_iv.setOnClickListener(this);
        progressBar = findViewById(R.id.camera_pb);

        FirebaseApp.initializeApp(this);
    }

    private void startCamera() {
        ImageCapture.Builder builder = new ImageCapture.Builder();

        //Rotation
        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                int rotation;

                // Monitors orientation values to determine the target rotation value
                if (orientation >= 45 && orientation < 135) {
                    rotation = Surface.ROTATION_270;
                } else if (orientation >= 135 && orientation < 225) {
                    rotation = Surface.ROTATION_180;
                } else if (orientation >= 225 && orientation < 315) {
                    rotation = Surface.ROTATION_90;
                } else {
                    rotation = Surface.ROTATION_0;
                }
                builder.setTargetRotation(rotation);
            }
        };

        orientationEventListener.enable();

        //HD display of image
        HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder);

        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            hdrImageCaptureExtender.enableExtension(cameraSelector);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mCameraView.bindToLifecycle(CameraActivity.this);
    }

    //Lets Add All Permission
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS)
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        return true;
    }

    //On Request Permission Result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                callToast("Permission Granted");
                startCamera();
            } else {
                callToast("Permission Not Granted");
                this.finish();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) finish();
        if (data == null) return;
        if (requestCode == UPDATE_APP_REQUEST_CODE) {
            Toast.makeText(this, "Downloading start", Toast.LENGTH_SHORT).show();
            if (resultCode != RESULT_OK) {
                Log.d(TAG, "onActivityResult: Update flow failed" + resultCode);
            }
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    detectTextFromImage(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }

    }

    private void callToast(CharSequence message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.camera_rotate_iv:
                if (ActivityCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (mCameraView.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT)) {
                    mCameraView.toggleCamera();
                } else {
                    return;
                }
                break;
            case R.id.camera_capture_iv:
                captureImage();
                break;
            case R.id.camera_captured_iv:
                openEditingActivity();
                break;
        }
    }

    //This method is called when user wants to capture image
    private void captureImage() {

        runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));

        SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        File file = new File(getApplicationContext().getExternalCacheDir(), File.separator + getResources().getString(R.string.app_name) + ".png");
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(file);
            fOut.flush();
            fOut.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        file.setReadable(true, false);

        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        mCameraView.setCaptureMode(CameraView.CaptureMode.IMAGE);
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
        mCameraView.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {

                photoURI = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", file);
                runOnUiThread(() -> {

                    openEditingActivity();
                    progressBar.setVisibility(View.INVISIBLE);
                    
//                    Glide.with(CameraActivity.this).load(photoURI)
//                            .circleCrop().into(capturedImage_iv);
//                    Log.d(TAG, "onImageSaved: called");

                    //callToast("Click on Image to Extract Text");
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException error) {
                error.printStackTrace();
            }
        });
    }

    private StringBuilder str_detectedText;

    //Extract text from image
    private void detectTextFromImage(Bitmap bitmap) {
        str_detectedText = new StringBuilder();
        progressBar.setVisibility(View.VISIBLE);

        try {
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
            FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
            detector.processImage(image).addOnSuccessListener(firebaseVisionText -> {
                for (FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks()) {
                    String blockText = block.getText();
                    str_detectedText.append(blockText).append("\n");
                }
                progressBar.setVisibility(View.GONE);
                if (!str_detectedText.toString().trim().isEmpty()) {
                    ExtractedModel model = new ExtractedModel(String.valueOf(str_detectedText));
                    Database.getInstance(this).extractedDAO().insert(model);
                }
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(SET_EXTRACTED_TEXT, String.valueOf(str_detectedText));
                setResult(EXTRACTED_TEXT_CODE, intent);
                finish();
            })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        callToast("Something Wrong");
                    });
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            callToast("Something Wrong");
        }
    }

    private void openEditingActivity() {
        CropImage.activity(photoURI).start(CameraActivity.this);
    }


    private void loadAds() {
        MobileAds.initialize(this, initializationStatus -> {
        });

        adContainerView = findViewById(R.id.camera_ad_container);
        adContainerView.post(this::loadBanner);
    }

    private void loadBanner() {
        // Create an ad request.
        AdView adView = new AdView(this);
        adView.setAdUnitId(getString(R.string.banner_ad));
        adContainerView.removeAllViews();
        adContainerView.addView(adView);

        AdSize adSize = getAdSize();
        adView.setAdSize(adSize);

        AdRequest adRequest = new AdRequest.Builder().build();

        // Start loading the ad in the background.
        adView.loadAd(adRequest);
    }

    private AdSize getAdSize() {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);

        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }

    // private final int UPDATE_REQUEST_CODE = 1612;
    private void callInAppUpdate() {
        // Creates instance of the manager.
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(this);

        // Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    // For a flexible update, use AppUpdateType.FLEXIBLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {

                try {
                    appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.FLEXIBLE
                            , CameraActivity.this, UPDATE_APP_REQUEST_CODE);
                } catch (IntentSender.SendIntentException exception) {
                    Log.d(TAG, "callInAppupdate: " + exception.getMessage());
                }
            }
        });
    }
}