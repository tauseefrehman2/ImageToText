package com.prepost.imagetotext;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.prepost.imagetotext.Models.ExtractedModel;
import com.prepost.imagetotext.database.Database;

import java.io.FileNotFoundException;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import static com.prepost.imagetotext.HistoryActivity.SET_HISTORY_CONTENT;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    public static final int EXTRACTED_TEXT_CODE = 123;
    public static final int HISTORY_TEXT_CODE = 321;
    public static final int UPDATE_REQUEST_CODE = 321;
    public static final int REQUEST_STORAGE_PERMISSION = 101;
    private final int OPEN_GALLERY_REQUEST_CODE = 102;


    public static String SET_EXTRACTED_TEXT;
    public static final String TEXT_TO_SPEAK_UTTERANCE_ID = "111";

    private FrameLayout adContainerView;
    private EditText content_et;
    private ProgressBar mLoading_pb;
    private RelativeLayout mControls_rl;
    private SeekBar mSpeed_sb;
    private SeekBar mPitch_sb;

    private ImageButton mStopVoice_btn;

    private TextToSpeech mTextToSpeech;

    //on create option menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    //on option item selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_menu_share:
                shareAppContent();
                break;
            case R.id.main_menu_feedback:
                callInAppReview();
                break;
            case R.id.main_menu_exist:
                closeAppConfirmation();
                break;
            case R.id.main_menu_sound:
                letsSpeak();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    //on create
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel("MyNotifications",
                    "MyNotifications"
                    , NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager manager = this.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        FirebaseMessaging.getInstance().subscribeToTopic("general")
                .addOnCompleteListener(task -> {
                    String msg = "Successfully";
                    if (!task.isSuccessful()) {
                        msg = "Failed";
                    }
                    Log.d(TAG, msg);
                    //Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();

                })
                .addOnFailureListener(e -> {

                });

        init();
        callInAppUpdate();
    }

    //widget initialization
    private void init() {
        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        loadAds();

        content_et = findViewById(R.id.main_content_et);
        mLoading_pb = findViewById(R.id.main_progressbar);
        findViewById(R.id.main_bottom_clear_ll).setOnClickListener(this);
        findViewById(R.id.main_bottom_history_ll).setOnClickListener(this);
        findViewById(R.id.main_bottom_camera_ll).setOnClickListener(this);
        findViewById(R.id.main_bottom_copy_ll).setOnClickListener(this);
        findViewById(R.id.main_bottom_send_ll).setOnClickListener(this);
        mControls_rl = findViewById(R.id.main_sound_control_rl);
        mControls_rl.setOnClickListener(this);

        mSpeed_sb = findViewById(R.id.main_speed_sb);
        mPitch_sb = findViewById(R.id.main_pitch_sb);

        ImageButton mHideControls_btn = findViewById(R.id.main_hideControls_ib);
        mStopVoice_btn = findViewById(R.id.main_stopVoice_ib);
        mHideControls_btn.setOnClickListener(this);
        mStopVoice_btn.setOnClickListener(this);

        initializeTextToSpeech();
    }

    private void initializeTextToSpeech() {
        mTextToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = mTextToSpeech.setLanguage(Locale.ENGLISH);

                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Language Not Supported", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d(TAG, "init: Initialization Failed");
            }
        });
    }

    private boolean isPlaying = false;

    private void letsSpeak() {
        //set visibility of controls to visible
        String text = content_et.getText().toString();

        float pitch = (float) mPitch_sb.getProgress() / 50;
        if (pitch < 0.1) pitch = 0.1f;
        float speed = (float) mSpeed_sb.getProgress() / 50;
        if (speed < 0.1) speed = 0.1f;

        mTextToSpeech.setPitch(pitch);
        mTextToSpeech.setSpeechRate(speed);

        if (!text.isEmpty()) {
            mTextToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "102");
            mControls_rl.setVisibility(View.VISIBLE);
        } else
            mTextToSpeech.speak("There is no Text", TextToSpeech.QUEUE_FLUSH, null, "102");
    }

    //On Activity for result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) return;
        if (requestCode == EXTRACTED_TEXT_CODE) {
            String text = data != null ? data.getStringExtra(SET_EXTRACTED_TEXT) : null;
            content_et.setText(text);
        }
        if (requestCode == HISTORY_TEXT_CODE) {
            String text = data != null ? data.getStringExtra(SET_HISTORY_CONTENT) : null;
            content_et.setText(text);
        }
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Log.d(TAG, "onActivityResult: Update flow failed" + resultCode);
            }
        }

        //this code is for load image from gallery and display in image view
        if (requestCode == OPEN_GALLERY_REQUEST_CODE && resultCode == RESULT_OK && null != data) {

            Uri source = data.getData();
            try {
                //tempBitmap is Immutable bitmap,
                //cannot be passed to Canvas constructor
                Bitmap tempBitmap = BitmapFactory.decodeStream(
                        getContentResolver().openInputStream(source));
                DetectTextFromImage(tempBitmap);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //Check is storage permission granted
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                OpenGallery();
            } else {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //call toast
    private void callToast(CharSequence message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    //load ads
    private void loadAds() {
        MobileAds.initialize(this, initializationStatus -> {
        });

        adContainerView = findViewById(R.id.main_ad_container);
        adContainerView.post(this::loadBanner);
    }

    //load banner ads
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

    //get ad size
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


    //On click method
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_bottom_clear_ll:
                clearEditText();
                break;
            case R.id.main_bottom_history_ll:
                Intent historyIntent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivityForResult(historyIntent, HISTORY_TEXT_CODE);
                break;
            case R.id.main_bottom_camera_ll:
                CameraOrGalleryDialog();
                break;
            case R.id.main_bottom_copy_ll:
                setTextToClipboard(content_et.getText().toString());
                break;
            case R.id.main_bottom_send_ll:
                shareAppContent(content_et.getText().toString());
                break;
            case R.id.main_hideControls_ib:
                mControls_rl.setVisibility(View.GONE);
                break;
            case R.id.main_stopVoice_ib:
                stopTextToSpeech();
                break;
        }
    }

    //Clear Edit text on red button clicked
    private void clearEditText() {
        stopTextToSpeech();
        if (content_et.getText().toString().isEmpty()) return;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Confirmation").setMessage("Do you wants to clear the text?");

        dialogBuilder.setPositiveButton("Yes", (dialog, which) -> {
            dialog.dismiss();
            content_et.setText("");
            callToast("Text cleared");
        }).setNegativeButton("No", (dialog, which) -> {
            dialog.dismiss();
        });
        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    //Clear Edit text on red button clicked
    private void closeAppConfirmation() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Confirmation").setMessage("Do you wants to close the app?");

        dialogBuilder.setPositiveButton("Yes", (dialog, which) -> {
            dialog.dismiss();
            this.finish();
        }).setNegativeButton("No", (dialog, which) -> {
            dialog.dismiss();
        });
        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    //Set text to clip board
    private void setTextToClipboard(String text) {
        stopTextToSpeech();
        if (text.isEmpty()) {
            callToast("There is no text to copy");
            return;
        }
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                this.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
        assert clipboard != null;
        clipboard.setPrimaryClip(clip);
        callToast("Text Copy to Clipboard");
    }

    //share content of apps to other apps
    private void shareAppContent(String text) {
        stopTextToSpeech();
        if (text.isEmpty()) {
            callToast("There is not text to send");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        String shareSub = "Extracted Text";
        intent.putExtra(Intent.EXTRA_SUBJECT, shareSub);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, "Send Text Using"));
    }

    //on destroy
    @Override
    protected void onDestroy() {
        stopTextToSpeech();
        super.onDestroy();
    }

    //on resume method
    @Override
    protected void onResume() {
        super.onResume();
        callInAppUpdate();
    }

    //In App Review Method
    private void callInAppReview() {
        ReviewManager manager = ReviewManagerFactory.create(this);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // We can get the ReviewInfo object
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = manager.launchReviewFlow(this, reviewInfo);
                flow.addOnCompleteListener(task2 -> {

                });
            } else {
                callToast("Something Wrong");
            }
        }).addOnFailureListener(e -> {
            callToast("Something Wrong");
        });
    }

    //In app update method
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
                            , MainActivity.this, UPDATE_REQUEST_CODE);
                } catch (IntentSender.SendIntentException exception) {
                    Log.d(TAG, "callInAppupdate: " + exception.getMessage());
                }
            }
        });
    }

    //Share apps method
    private void shareAppContent() {
        stopTextToSpeech();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        String shareBody = "https://play.google.com/store/apps/developer?id=prepostseo.com";
        String shareSub = "App link";
        intent.putExtra(Intent.EXTRA_SUBJECT, shareSub);
        intent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(intent, "Share App Using"));
    }

    //stop text to speech method
    private void stopTextToSpeech() {
        if (mTextToSpeech != null) {
            mTextToSpeech.stop();
        }
    }

    //Check is storage permission granted or not
    private void IsStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                    && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            } else {
                OpenGallery();
            }
        } else {
            OpenGallery();
        }
    }

    //Open gallery method
    private void OpenGallery() {
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, OPEN_GALLERY_REQUEST_CODE);
    }

    //Show Dialog of choose gallery or camera
    private void CameraOrGalleryDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setCancelable(false);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.diag_choose_option, null);
        dialogBuilder.setView(dialogView);

        LinearLayout camera_ll = dialogView.findViewById(R.id.diag_camera_ll);
        LinearLayout gallery_ll = dialogView.findViewById(R.id.diag_gallery_ll);
        TextView cancel_tv = dialogView.findViewById(R.id.diag_cancel_tv);

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        //Choose camera option
        camera_ll.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivityForResult(intent, EXTRACTED_TEXT_CODE);
            alertDialog.dismiss();
        });

        //Choose Gallery Option
        gallery_ll.setOnClickListener(v -> {
            IsStoragePermissionGranted();
            alertDialog.dismiss();
        });

        cancel_tv.setOnClickListener(view -> {
            alertDialog.dismiss();
        });
        alertDialog.show();
    }

    //Extract text from image
    private void DetectTextFromImage(Bitmap bitmap) {
        StringBuilder str_detectedText = new StringBuilder();
        mLoading_pb.setVisibility(View.VISIBLE);

        try {
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
            FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
            detector.processImage(image).addOnSuccessListener(firebaseVisionText -> {
                for (FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks()) {
                    String blockText = block.getText();
                    str_detectedText.append(blockText).append("\n");
                }
                if (!str_detectedText.toString().trim().isEmpty()) {
                    ExtractedModel model = new ExtractedModel(String.valueOf(str_detectedText));
                    Database.getInstance(this).extractedDAO().insert(model);
                }
                content_et.setText(str_detectedText.toString());
                mLoading_pb.setVisibility(View.GONE);
            })
                    .addOnFailureListener(e -> {
                        mLoading_pb.setVisibility(View.GONE);
                        callToast("Something Wrong");
                    });
        } catch (Exception e) {
            mLoading_pb.setVisibility(View.GONE);
            callToast("Something Wrong");
        }
    }
}