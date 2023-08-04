package com.example.comfortogether;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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
}