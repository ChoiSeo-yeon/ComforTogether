package com.example.comfortogether;



import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayActivity extends AppCompatActivity {
    private int CAMARA = 10;
    ImageButton close_play_btn;
    Button sound_btn;
    Button vibration_btn;
    Button ml_brn;
    Button sound_onoff_btn;

    MediaPlayer mediaPlayer;
    private static final int REQUEST_CAMERA_PERMISSION = 1234;
    private TextureView mTextureView;
    private ResultView resultView;
    private ImageView lineImgView;

    private CameraDevice mCamera;
    private Size mPreviewSize;
    private CameraCaptureSession mCameraSession;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private Module mModule;
    private Bitmap mBitmap;
    private float mImgScaleX, mImgScaleY, mIvScaleX = 1, mIvScaleY = 1, mStartX, mStartY;

    private LineDetecter mlineDetecter;
    public boolean sound_onoff = false;
    private static Context acontext;

    public static String assetFilePath(Context context, String assetName) throws IOException {

        acontext = context;
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        acontext = getApplicationContext();
        mediaPlayer = new MediaPlayer();
        close_play_btn = findViewById(R.id.close_play_btn);
        sound_btn = findViewById(R.id.sound_btn);
        vibration_btn = findViewById(R.id.vibration_btn);
        sound_onoff_btn = findViewById(R.id.sound_onoff_btn);
        ml_brn = findViewById(R.id.ml_brn);
        resultView = findViewById(R.id.rView);
        lineImgView = findViewById(R.id.lineImage);

        // 카메라 권한 체크
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }

        // yolo 모델 불러오기
        try {
            String str;
            List<String> classes = new ArrayList<>();

            mModule = LiteModuleLoader.load(PlayActivity.assetFilePath(getApplicationContext(), "best.torchscript.ptl"));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getAssets().open("coco.txt")));

            while ((str = bufferedReader.readLine()) != null)
                classes.add(str);

            PrePostProcessor.mClasses = new String[classes.size()];
            classes.toArray(PrePostProcessor.mClasses);
        } catch (Exception e) {
            Log.e("Object detection", "Error:", e);
        }

        mlineDetecter = new LineDetecter();

        initTextureView();


        class MLRunnable implements Runnable {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mBitmap = mTextureView.getBitmap();

                    mImgScaleX = (float) mBitmap.getWidth() / PrePostProcessor.mInputWidth;
                    mImgScaleY = (float) mBitmap.getHeight() / PrePostProcessor.mInputHeight;

                    mIvScaleX = (mBitmap.getWidth() > mBitmap.getHeight() ? (float) mTextureView.getWidth() / mBitmap.getWidth() : (float) mTextureView.getHeight() / mBitmap.getHeight());
                    mIvScaleY = (mBitmap.getHeight() > mBitmap.getWidth() ? (float) mTextureView.getHeight() / mBitmap.getHeight() : (float) mTextureView.getWidth() / mBitmap.getWidth());

                    mStartX = (mTextureView.getWidth() - mIvScaleX * mBitmap.getWidth()) / 2;
                    mStartY = (mTextureView.getHeight() - mIvScaleY * mBitmap.getHeight()) / 2;

                    mStartX = (mBitmap.getWidth() - mIvScaleX * mBitmap.getWidth()) / 2;
                    mStartY = (mBitmap.getHeight() - mIvScaleY * mBitmap.getHeight()) / 2;

                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(mBitmap, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true);
                    final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
                    IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
                    final Tensor outputTensor = outputTuple[0].toTensor();
                    final float[] outputs = outputTensor.getDataAsFloatArray();
                    final ArrayList<Result> results = PrePostProcessor.outputsToNMSPredictions(outputs, mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY);

                    if (results.isEmpty()) {
                        resultView.setVisibility(View.INVISIBLE);
                        Log.d("Object Detection", "Detection Done. But, there's no object");
                    } else {
                        runOnUiThread(() -> {
                            resultView.setResults(results);
                            resultView.invalidate();
                            resultView.setVisibility(View.VISIBLE);
                            Log.d("Object Detection", "Thread run done");
                        });
                    }

                    try {
                        Bitmap lineBitmap = mlineDetecter.DetectingLine(mBitmap);
                        lineImgView.setImageBitmap(lineBitmap);
                        lineImgView.setVisibility(View.VISIBLE);
                    } catch (Exception e) {
                        lineImgView.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }
        MLRunnable ml_runnable = new MLRunnable();
        Thread ml_thread = new Thread(ml_runnable);
        ml_thread.start();


    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public void onclick(View view) {
        switch (view.getId()) {
            case R.id.close_play_btn:
                Intent go_main_intent = new Intent(PlayActivity.this, MainActivity.class);
                startActivity(go_main_intent);
                finish();
                break;

            case R.id.sound_btn:
                PlaySound(R.raw.ringtone_1, true);
                break;

            case R.id.vibration_btn:
                PlayVibration(1000, 100);
                break;

            case R.id.ml_brn:
                //PlayML();
                break;

            case R.id.sound_onoff_btn:
                //sy 황성민 ttl
                //sound_onoff = !sound_onoff;
                if (sound_onoff == false) {
                    sound_onoff = true;
                    //sy PlaySound(); // "음성 장애물 감지 모드가 활성화 되었습니다."
                    PlaySound(R.raw.play_sound1, true);
                } else {
                    sound_onoff = false;
                    //sy PlaySound(); // "음성 장애물 감지 모드가 비 활성화 되었습니다."
                    PlaySound(R.raw.play_sound2, true);
                }
                Log.d("sound", "sount_onoff:" + sound_onoff);
                break;

            default:
                break;
        }
    }

    private void initTextureView() {
        mTextureView = (TextureView) findViewById(R.id.textureView);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                Log.e("cklee", "MMM onSurfaceTextureAvailable");
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
                Log.e("cklee", "MMM onSurfaceTextureSizeChanged");
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                Log.e("cklee", "MMM onSurfaceTextureDestroyed");
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                // 화면 갱신시마다 불림
//                Log.e("cklee", "MMM onSurfaceTextureUpdated");
            }
        });
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdArray = manager.getCameraIdList();
            Log.e("cklee", "MMM cameraIds = " + Arrays.deepToString(cameraIdArray));

            // test 로 0 번 camera 를 사용
            String oneCameraId = cameraIdArray[0];

            CameraCharacteristics cameraCharacter = manager.getCameraCharacteristics(oneCameraId);
            Log.e("cklee", "MMM camraCharacter = " + cameraCharacter);

            StreamConfigurationMap map = cameraCharacter.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizesForStream = map.getOutputSizes(SurfaceTexture.class);
            Log.e("cklee", "MMM sizesForStream = " + Arrays.deepToString(sizesForStream));

            // 가장 큰 사이즈부터 들어있다
            mPreviewSize = sizesForStream[0];

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            manager.openCamera(oneCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    mCamera = cameraDevice;
                    showCameraPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    mCamera.close();
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int errorCode) {
                    Log.e("cklee", "MMM errorCode = " + errorCode);
                    mCamera.close();
                    mCamera = null;
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e("cklee", "MMM openCamera ", e);
        }
    }

    private void showCameraPreview() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface textureViewSurface = new Surface(texture);

            mCaptureRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(textureViewSurface);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            mCamera.createCaptureSession(Arrays.asList(textureViewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCameraSession = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e("cklee", "MMM onConfigureFailed");
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e("cklee", "MMM showCameraPreview ", e);
        }
    }

    private void updatePreview() {
        try {
            mCameraSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            Log.e("cklee", "MMM updatePreview", e);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            openCamera();
        }
    }

    void PlaySound(int sound, boolean explanation) {

        Log.d("label string", "PlaySound " + sound_onoff);

        if(sound_onoff || explanation){
            if (mediaPlayer == null) {
                Log.d("label string", "mediaPlayer == null ");
                mediaPlayer = MediaPlayer.create(acontext, sound);
                mediaPlayer.start();
            } else {
                Log.d("label string", "else ");
                mediaPlayer.stop();
                mediaPlayer = null;
                PlaySound(sound, explanation || sound_onoff);
            }
        }
    }

    void PlayVibration(int millisec, int amplitude) {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(VibrationEffect.createOneShot(millisec, amplitude));
    }

    void tts_label(String tts) {
        switch (tts) {
            case "bus":
                PlaySound(R.raw.label_bus, false);
                break;
            case "car":
                PlaySound(R.raw.label_car, false);
                break;

            case "skateboard":
                PlaySound(R.raw.label_skateboard, false);
                break;

            case "sports ball":
                PlaySound(R.raw.label_sports_ball, false);
                break;

            case "person":
                Log.d("label string", "person");
                PlaySound(R.raw.label_person, false);
                break;

            case "traffic light":
                PlaySound(R.raw.label_traffic, false);
                break;

            case "truck":
                PlaySound(R.raw.label_truck, false);
                break;

        }
    }
}



    /*
    private void PlayML() {
        mBitmap = mTextureView.getBitmap();

        mImgScaleX = (float)mBitmap.getWidth() / PrePostProcessor.mInputWidth;
        mImgScaleY = (float)mBitmap.getHeight() / PrePostProcessor.mInputHeight;

        mIvScaleX = (mBitmap.getWidth() > mBitmap.getHeight() ? (float)mTextureView.getWidth() / mBitmap.getWidth() : (float)mTextureView.getHeight() / mBitmap.getHeight());
        mIvScaleY  = (mBitmap.getHeight() > mBitmap.getWidth() ? (float)mTextureView.getHeight() / mBitmap.getHeight() : (float)mTextureView.getWidth() / mBitmap.getWidth());

        mStartX = (mTextureView.getWidth() - mIvScaleX * mBitmap.getWidth())/2;
        mStartY = (mTextureView.getHeight() -  mIvScaleY * mBitmap.getHeight())/2;

        mStartX = (mBitmap.getWidth()  - mIvScaleX * mBitmap.getWidth())  / 2;
        mStartY = (mBitmap.getHeight() - mIvScaleY * mBitmap.getHeight()) / 2;

        Thread thread = new Thread(PlayActivity.this);
        thread.start();
    }


    @Override
    public void run() {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(mBitmap, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true);
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
        IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
        final Tensor outputTensor = outputTuple[0].toTensor();
        final float[] outputs = outputTensor.getDataAsFloatArray();
        final ArrayList<Result> results = PrePostProcessor.outputsToNMSPredictions(outputs, mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY);

        runOnUiThread(() -> {
            resultView.setResults(results);
            resultView.invalidate();
            resultView.setVisibility(View.VISIBLE);
            System.out.println("Thread run done");
        });
<<<<<<< Updated upstream
    }
     */

