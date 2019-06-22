package com.google.ar.core.examples.java.common.helpers;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class MusicPlayerHelper {
    private MediaPlayer mediaPlayer;

    private boolean musicSetup(Context context, String filePath, Boolean isLoop) throws IOException {
        boolean fileCheck = false;

        // インタンスを生成
        mediaPlayer = new MediaPlayer();

        // assetsから mp3, ogg ファイルを読み込み
        try(AssetFileDescriptor afdescripter = context.getAssets().openFd(filePath);)
        {
            // MediaPlayerに読み込んだ音楽ファイルを指定
            mediaPlayer.setDataSource(afdescripter.getFileDescriptor(),
                    afdescripter.getStartOffset(),
                    afdescripter.getLength());
            if(isLoop) {
                mediaPlayer.setLooping(isLoop);
            }
            // 音量調整を端末のボタンに任せる
            mediaPlayer.setVolume((float) AudioManager.STREAM_MUSIC,(float)AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();
            fileCheck = true;
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return fileCheck;
    }

    public void musicPlay(Context context, String filePath, Boolean isLoop) throws IOException {

        if (mediaPlayer == null) {
            // audio ファイルを読出し
            if (musicSetup(context, filePath, isLoop)) {
//                Toast.makeText(context, "Rread audio file", Toast.LENGTH_SHORT).show();
            } else {
//                Toast.makeText(context, "Error: read audio file", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            // 繰り返し再生する場合
            mediaPlayer.stop();
            mediaPlayer.reset();
            // リソースの解放
            mediaPlayer.release();
        }

        // 再生する
        mediaPlayer.start();

        // 終了を検知するリスナー
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
//                Toast.makeText(context, "end of audio", Toast.LENGTH_SHORT).show();
//                Log.d("debug", "end of audio");
                musicStop();
            }
        });
    }

    public void musicStop() {
        // 再生終了
        mediaPlayer.stop();
        // リセット
        mediaPlayer.reset();
        // リソースの解放
        mediaPlayer.release();

        mediaPlayer = null;
    }
}
