package com.example.comfortogether;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class MainActivity extends AppCompatActivity {

    LinearLayout go_play_btn;
    LinearLayout go_tuto_btn;

    MediaPlayer main_mediaPlayer;
    boolean is_playing;
    private static final int REQUEST_CAMERA_PERMISSION = 1234;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PlaySound(R.raw.app_start_sound);
        hide_statusbar_navigationbar();

        // 카메라 권한 체크
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
        go_play_btn = findViewById(R.id.go_play_btn);
        go_tuto_btn = findViewById(R.id.go_tuto_btn);



        //sy 황성민 ttl 위쪽누르면 실행 아래면 튜토리얼
        // 1) 어플을 실행하려면 정 중앙을 기준으로 윗부분을, 2) 튜토리얼을 실행하시려면 정중앙을 기준으로 아랫부분을 눌러주세요
        //main_sound1, mainsound2

        go_play_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlaySound(R.raw.play_btnclick_sound); //sy 황성민 ttl 실행버튼눌렀을때 ttl
                try {
                    Thread.sleep(3500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Intent go_main_intent = new Intent(MainActivity.this,PlayActivity.class);
                startActivity(go_main_intent);
                //finish();
            }
        });

        go_tuto_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlaySound(R.raw.tuto_btnclick_sound);
                try {
                    Thread.sleep(3500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Intent go_tuto_intent = new Intent(MainActivity.this,TutorialActivity.class);
                startActivity(go_tuto_intent);
                //finish();
            }
        });

        PlaySound(R.raw.app_start_sound);
    }
    void PlaySound(int main_sound) {
        if(main_mediaPlayer == null){
            main_mediaPlayer = MediaPlayer.create(getApplicationContext(), main_sound);
            main_mediaPlayer.start();
            is_playing = true;
        }else{
            main_mediaPlayer.stop();
            main_mediaPlayer = null;
            is_playing = false;
            PlaySound(main_sound);
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