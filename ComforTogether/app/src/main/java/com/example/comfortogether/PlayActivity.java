package com.example.comfortogether;



import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
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
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class PlayActivity extends AppCompatActivity {
    private int CAMARA = 10;
    ImageButton close_play_btn;
    Button sound_btn;
    Button vibration_btn;
    Button ml_brn;
    LinearLayout sound_onoff_btn;

    MediaPlayer mediaPlayer;
    private static final int REQUEST_CAMERA_PERMISSION = 1234;
    private TextureView mTextureView;
    private ResultView resultView;

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

    public int line_detect_count = 0;
    public int line_not_detect_count = 0;

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

        hide_statusbar_navigationbar();

        acontext = getApplicationContext();
        mediaPlayer = new MediaPlayer();
        close_play_btn = findViewById(R.id.close_play_btn);
        sound_btn = findViewById(R.id.sound_btn);
        vibration_btn = findViewById(R.id.vibration_btn);
        sound_onoff_btn = findViewById(R.id.sound_onoff_btn);
        ml_brn = findViewById(R.id.ml_brn);
        resultView = findViewById(R.id.rView);

        sound_onoff_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sound_onoff == false) {
                    sound_onoff = true;
                    PlaySound(R.raw.play_sound1, true);
                } else {
                    sound_onoff = false;
                    PlaySound(R.raw.play_sound2, true);
                }
                resultView.Sound_swich();
                Log.d("sound", "sount_onoff:" + sound_onoff);
            }
        });

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
                        Log.d("Size", "width" + mBitmap.getWidth());
                        Log.d("Size", "height" + mBitmap.getHeight());
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
                        grayscale(mBitmap);
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                    boolean isDetect = mlineDetecter.DetectingLine(mBitmap);

                    Log.d("Line", "is Detect is " + isDetect);

                    if (!isDetect){
                        line_not_detect_count = 0;
                        if(line_detect_count == 2){
                            PlayVibration(800, 255);
                        }
                        line_detect_count++;
                    }else{
                        line_detect_count = 0;
                        if(line_not_detect_count < 3){
                            PlayVibration(400, 130);
                        }
                        line_not_detect_count++;
                    }

                    Log.d("isDetect","Line Detect : "+ line_detect_count + " , Line Not Detect : "+line_not_detect_count);
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
    ArrayList<Boolean> histo_count = new ArrayList<>();
    public void grayscale(final Bitmap orgBitmap) {
        int width, height;
        width = orgBitmap.getWidth();
        height = orgBitmap.getHeight();
        int histSize = 256;

        Bitmap bmpGrayScale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        try {
            boolean tf = false;
            Mat grayScale = new Mat();
            Utils.bitmapToMat(orgBitmap, grayScale);
            List<Mat> bgrPlanes = new ArrayList<>();
            Core.split(grayScale, bgrPlanes);
            boolean accumlate = false;

            float[] range = {0, 256};
            MatOfFloat histRange = new MatOfFloat(range);
            Mat iHist = new Mat();
            Imgproc.calcHist(bgrPlanes, new MatOfInt(0), new Mat(), iHist, new MatOfInt(histSize), histRange, accumlate);
            int binW = (int) Math.round((double) width / histSize);

            Mat Histimage = new Mat(height, width, CvType.CV_8UC3, new Scalar(0, 0, 0));
            Core.normalize(iHist, iHist, 0, Histimage.rows(), Core.NORM_MINMAX);
            float[] iHistData = new float[(int) (iHist.total() * iHist.channels())];
            iHist.get(0, 0, iHistData);

            Utils.matToBitmap(Histimage, bmpGrayScale);

            Log.d("Histdata", "Histdata" + Histimage);

            SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd HH-mm-ss z");
            Date date = new Date(System.currentTimeMillis());
            saveBitmapToJpeg(bmpGrayScale,formatter.format(date));
            double sum = 0;
            for (int i = 0; i < iHistData.length; i++) {
                sum += iHistData[i];
            }
            double[] persent = new double[256];
            for (int i = 0; i < iHistData.length; i++) {
                persent[i] = (iHistData[i]/sum)*100;
                //Log.d("HISTOGRAM","HIST Date : " + i + " : " + persent[i]);
            }

            for (int i = 0; i < 255; i++) {
                double sum_per = 0;
                for (int j = 0; j < 20; j++) {
                    sum_per += persent[i];
                }
                if(sum_per > 80){
                    Log.d("HISTOGRAM","HERE IS FINISH");
                    tf = true;
                    break;
                }
            }



            if(histo_count.size() > 4){
                for (int i = 1; i < histo_count.size(); i++) {
                    histo_count.set(i-1,histo_count.get(i));
                }
                histo_count.set(4,tf);

                int is = 0;
                for (int i = 0; i < histo_count.size()-1; i++) {
                    if(histo_count.get(i)){
                        is++;
                    }
                }
                if(is>=3){
                    Log.d("화면","돌아감");
                }else{
                    Log.d("화면","안 돌아감");
                }
                Log.d("tf","tf >4 : "+Arrays.toString(histo_count.toArray()));

            }else{
                histo_count.add(tf);
                Log.d("tf","tf <4 : "+Arrays.toString(histo_count.toArray()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void saveBitmapToJpeg(Bitmap bitmap, String name) {

        File storage = getCacheDir();
        String fileName = name + ".jpg";
        File tempFile = new File(storage, fileName);
        try {
            tempFile.createNewFile();
            FileOutputStream out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
        } catch (FileNotFoundException e) {
            Log.e("MyTag","FileNotFoundException : " + e.getMessage());
        } catch (IOException e) {
            Log.e("MyTag","IOException : " + e.getMessage());
        }
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

            //case R.id.sound_onoff_btn:
            //    if (sound_onoff == false) {
            //        sound_onoff = true;
            //        PlaySound(R.raw.play_sound1, true);
            //    } else {
            //        sound_onoff = false;
            //        PlaySound(R.raw.play_sound2, true);
            //    }
            //    resultView.Sound_swich();
            //    Log.d("sound", "sount_onoff:" + sound_onoff);
            //    break;

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

            mPreviewSize = sizesForStream[0];

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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

    void PlayVibration(int millisec, int amplitude) {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(VibrationEffect.createOneShot(millisec, amplitude));
    }

    void tts_label(String tts, boolean sound_is) {
        if(sound_is){
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

    public void hide_statusbar_navigationbar() {
        // Status bar, Navigation Bar Hide
        int currentApiVersion = Build.VERSION.SDK_INT;

        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        // This work only for android 4.4+
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(flags);
            final View decorView = getWindow().getDecorView();
            decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        decorView.setSystemUiVisibility(flags);
                    }
                }
            });
        }

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }
}

