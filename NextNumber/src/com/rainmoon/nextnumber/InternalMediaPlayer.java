package com.rainmoon.nextnumber;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;

/**
 * Internal media player responsible for all phases of a media player.
 * 
 * @author trung
 */
public class InternalMediaPlayer {

  private MediaPlayer mPlayer;
  private Context mContext;

  /**
   * Constructor.
   * <p>
   * This creates and initializes the media player.
   */
  public InternalMediaPlayer(Context context) {
    mContext = context;
    mPlayer = new MediaPlayer();

    mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    mPlayer.setOnPreparedListener(new OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer player) {
        player.start();
      }
    });
    mPlayer.setOnCompletionListener(new OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer player) {
        player.stop();
        player.reset();
      }
    });
  }

  /**
   * Plays the sound ay given resource id.
   * 
   * @param resourceId
   */
  public void playSound(int resourceId) {
    mPlayer.reset();
    AssetFileDescriptor fd = null;
    fd = mContext.getResources().openRawResourceFd(resourceId);
    try {
      mPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(),
          fd.getLength());
      fd.close();
      mPlayer.prepareAsync();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Stops and releases the media player.
   */
  public void destroy() {
    if (mPlayer != null) {
      mPlayer.release();
      mPlayer = null;
    }
  }

}
