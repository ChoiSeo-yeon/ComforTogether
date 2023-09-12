package com.example.comfortogether;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;


public class IntroActivity extends AppCompatActivity {

    ImageView intro_iv;
    private static String Tag_log = "OpenCV Test:";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        hide_statusbar_navigationbar();
        if (OpenCVLoader.initDebug()){
            Log.d(Tag_log, "OpenCV init");
        } else{
            Log.d(Tag_log, "OpenCV Not Init");
        }
        intro_iv = findViewById(R.id.intro_iv);

        intro_iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentMain();
            }
        });

        IntroThread introThread = new IntroThread(handler);
        introThread.start();
    }
    public class IntroThread extends Thread {

        private Handler handler;

        public IntroThread(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {

            Message msg = new Message();

            try {
                Thread.sleep(1000);
                msg.what = 1;
                handler.sendEmptyMessage(msg.what);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Intent intent = new Intent(IntroActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }
    };

    void IntentMain(){
        Intent go_main_intent = new Intent(IntroActivity.this,MainActivity.class);
        startActivity(go_main_intent);
        finish();
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
            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
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
        // Status bar, Navigation Bar Hide

        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }
}