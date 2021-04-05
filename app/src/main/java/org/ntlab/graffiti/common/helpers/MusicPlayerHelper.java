package org.ntlab.graffiti.common.helpers;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

/**
 * Helper to play the music.
 * @author a-hongo
 */
public class MusicPlayerHelper {
    private static final String TAG = MusicPlayerHelper.class.getSimpleName();
    private MediaPlayer mediaPlayer;

    /**
     * Set up the music.
     *
     * @param context the application's context.
     * @param filePath the filepath of music.
     * @param isLoop is loop or is not loop.
     */
    private boolean setupMusic(Context context, String filePath, Boolean isLoop) throws IOException {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileCheck;
    }

    /** Play the music.
     *
     * @param context the application's context.
     * @param filePath the filepath of music.
     * @param isLoop is loop or is not loop.
     */
    public void playMusic(Context context, String filePath, Boolean isLoop) throws IOException {

        if (mediaPlayer == null) {
            // audio ファイルを読出し
            if (setupMusic(context, filePath, isLoop)) {
                Log.d(TAG, "read audio file");
            } else {
                Log.e(TAG, "Error: read audio file");
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
                Log.d(TAG, "end of audio");
                stopMusic();
            }
        });
    }

    /** Stop the music. */
    public void stopMusic() {
        if(mediaPlayer != null) {
            mediaPlayer.stop(); // 再生終了
            mediaPlayer.reset(); // リセット
            mediaPlayer.release(); // リソースの解放
            mediaPlayer = null;
        }
    }
}
