package com.example.comfortogether;

import android.content.Context;
import android.media.MediaPlayer;

public class Play_MediaPlayer {
    private MediaPlayer mediaPlayer;
    private Context context;

    // 생성자
    public Play_MediaPlayer(Context context) {
        this.context = context;
        mediaPlayer = new MediaPlayer();
    }

    // MediaPlayer 초기화
    public void initializeMediaPlayer() {
        mediaPlayer = new MediaPlayer();
    }

    // MediaPlayer에 raw 리소스 파일 설정
    public void setRawResource(int resourceId) throws Exception {
        mediaPlayer = MediaPlayer.create(context, resourceId);
    }

    // 음악 재생
    public void play() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    // 음악 일시정지
    public void pause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    // 음악 정지
    public void stop() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // 음악 재생 중인지 확인
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }
}
