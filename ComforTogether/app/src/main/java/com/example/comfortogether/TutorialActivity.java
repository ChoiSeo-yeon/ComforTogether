package com.example.comfortogether;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.viewpager.widget.ViewPager;

import java.util.List;

public class TutorialActivity extends AppCompatActivity {
    ImageView tutorial_iv;
    RelativeLayout tutorial_rl;
    TextView tutorial_tv;
    TextView tuto_title;

    MediaPlayer tuto_mediaPlayer;
    int tuto_num = 0;
    //MediaPlayer mediaPlayer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hide_statusbar_navigationbar();
        setContentView(R.layout.activity_tutorial);

        tutorial_iv = findViewById(R.id.tutorial_iv);
        tutorial_rl = findViewById(R.id.tutorial_rl);
        tutorial_tv = findViewById(R.id.tutorial_tv);
        tuto_title = findViewById(R.id.tutorial_ti);
        PlaySound(R.raw.tuto_purpose);
        tuto_num = 0;
        int[] tuto_mp3 = {R.raw.tuto_sound0,R.raw.tuto_sound1,R.raw.tuto_sound2,R.raw.tuto_sound3,R.raw.tuto_sound4,R.raw.tuto_sound5,R.raw.tuto_sound6};
        String[] tuto_string = {
                "앱 실행 시 카메라 센서를 사용하기 위해 권한을 받습니다.",
                "핸드폰의 후면 카메라를 통해 주변 정보를 받습니다.",
                "장애물이 전방에 존재할 떄 강한 진동이 울리고 점자블럭을 벗어날 경우 약한 진동이 울립니다",
                "장애물 감지를 진동과 음성으로 동시에 듣고 싶은 경우 앱 화면 맨 아래부분에 디스플레이를 클릭합니다.",
                "음성으로 정보 안내를 시작합니다 라는 음성과 함께 음성 장애물 감지 모드가 실행됩니다.",
                "튜토리얼을 마쳤습니다, 곧 실행을 위해 홈 화면으로 이동합니다"};

        String[] tuto_title_string = {
                "[ 앱 권한 ]",
                "[ 앱 안내 ]",
                "[ 점자블럭 - 진동 ]",
                "[ 장애물 - 소리 ]",
                "[ 장애물 감지 모드 ]",
                "[ 튜토리얼 완료 ]"
        };

        int[] tuto_img = {
                R.drawable.tuto_img,
                R.drawable.tuto_img1,
                R.drawable.tuto_img3,
                R.drawable.tuto_img4,
                R.drawable.tuto_img5,
                R.drawable.app_logo,
        };

        tutorial_rl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(tuto_num < 6){
                    tutorial_iv.setImageResource(tuto_img[tuto_num]);
                    tutorial_tv.setText(tuto_string[tuto_num]);
                    tuto_title.setText(tuto_title_string[tuto_num]);
                    PlaySound(tuto_mp3[tuto_num]);
                    if(tuto_num == 2){
                        PlayVibration(2000,255);
                        //PlayVibration(1000,0);
                        //PlayVibration(2000,150);
                    }
                }else{
                    finish_playactivity();
                }
                tuto_num ++;
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish_playactivity();
    }
    void PlaySound(int sound) {
        if(tuto_mediaPlayer == null){
            tuto_mediaPlayer = MediaPlayer.create(getApplicationContext(), sound);
            tuto_mediaPlayer.start();
        }else{
            tuto_mediaPlayer.stop();
            tuto_mediaPlayer = null;
            PlaySound(sound);
        }
    }
    void PlayVibration(int millisec, int amplitude) {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(VibrationEffect.createOneShot(millisec, amplitude));
    }

    void finish_playactivity(){
        tuto_num = 0;
        if(tuto_mediaPlayer != null){
            tuto_mediaPlayer.stop();
            tuto_mediaPlayer = null;
        }
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