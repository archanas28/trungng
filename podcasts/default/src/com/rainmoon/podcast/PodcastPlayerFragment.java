package com.rainmoon.podcast;

import java.io.IOException;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;

/**
 * Fragment showing the Podcast player.
 * 
 * TODO(TRUNG): add this fragment to FeedItemActivity TODO(trung): need one way
 * for this fragment to communicate with the hosting activity (e.g., to change
 * the mUrl of the file being played).
 * 
 * @author trung nguyen
 * 
 */
public class PodcastPlayerFragment extends Fragment implements
    MediaPlayerControl, OnFeedItemChangeListener {

  private static final String TAG = "PodcastPlayerFragment";
  private String mUrl = "";

  private MediaController mMediaController;
  private MediaPlayer mMediaPlayer;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mMediaPlayer = new MediaPlayer();
    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mp) {
        mMediaController.show(10000);
        mMediaPlayer.start();
      }
    });

    mMediaController = new MediaController(getActivity());
    mMediaController.setMediaPlayer(PodcastPlayerFragment.this);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    View playerController = inflater.inflate(R.layout.fragment_podcast,
        container, false);
    mMediaController.setAnchorView(playerController
        .findViewById(R.id.podcast_player));
    mMediaController.setVisibility(View.VISIBLE);
    return playerController;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    //TODO(trung): how to make this visible with the fragment??
    //mMediaController.show();
  }

  @Override
  public boolean canPause() {
    return true;
  }

  @Override
  public boolean canSeekBackward() {
    return true;
  }

  @Override
  public boolean canSeekForward() {
    return true;
  }

  @Override
  public int getBufferPercentage() {
    return (mMediaPlayer.getCurrentPosition() * 100)
        / mMediaPlayer.getDuration();
  }

  @Override
  public int getCurrentPosition() {
    return mMediaPlayer.getCurrentPosition();
  }

  @Override
  public int getDuration() {
    return mMediaPlayer.getDuration();
  }

  @Override
  public boolean isPlaying() {
    return mMediaPlayer.isPlaying();
  }

  @Override
  public void pause() {
    if (mMediaPlayer.isPlaying()) {
      mMediaPlayer.pause();
    }
  }

  @Override
  public void seekTo(int pos) {
    mMediaPlayer.seekTo(pos);
  }

  @Override
  public void start() {
    // this option is to start preparing the media only when user
    // clicks the play button
    // TODO(trung): can start prepare in onUrlChange(), however, it will be
    // more expensive
    mMediaPlayer.prepareAsync();
  }

  // TODO(trung): get the touch even from Activity
  public boolean onTouchEvent(MotionEvent event) {
    mMediaController.show();

    return false;
  }

  // only release the player when the activity is destroyed
  public void onDestroy() {
    super.onDestroy();
    mMediaPlayer.stop();
    mMediaPlayer.release();
    mMediaPlayer = null;
  }

  @Override
  public void onUrlChange(String url) {
    mUrl = url;
    try {
      mMediaPlayer.setDataSource(mUrl);
    } catch (IOException e) {
      Log.e(TAG, "Could not open file " + mUrl + " for playback.", e);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "Could not open file " + mUrl + " for playback.", e);
    }
  }
}
