package com.rainmoon.podcast;

import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.Toast;

import com.rainmoon.podcast.FeedItemActivity.OnConfigurationChangedListener;
import com.rainmoon.podcast.FeedItemActivity.OnPlayButtonClickedListener;
import com.rainmoon.podcast.receiver.NetworkReceiver;
import com.rainmoon.podcast.utils.StaticMethods;

/**
 * Fragment showing the Podcast player.
 * 
 * TODO(TRUNG): handle media playback button; see
 * http://developer.android.com/training/managing-audio/volume-playback.html
 * TODO(trung): make this a service (play in background)
 * 
 * @author trung nguyen
 * 
 */
public class PlayerFragment extends Fragment implements
    OnPlayButtonClickedListener, OnConfigurationChangedListener {

  /**
   * Interface for communication with the activity to get the saved media player
   * instance of this player (when screen orientation changed).
   * 
   * @author trung nguyen
   */
  public interface OnPlayerFragmentListener {
    public PlayerController getLastMediaPlayerController();
  }

  private static final String TAG = "PlayerFragment";
  private String mUrl = "";

  private AlwaysOnMediaController mMediaController;
  private PlayerController mPlayer;
  private NetworkReceiver mNetworkReceiver;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    FragmentActivity holderActivity = getActivity();
    mMediaController = new AlwaysOnMediaController(holderActivity);
    mPlayer = ((OnPlayerFragmentListener) holderActivity)
        .getLastMediaPlayerController();
    if (mPlayer == null) {
      mPlayer = new PlayerController();
      mMediaController.setVisibility(View.GONE);
      mMediaController.setMediaPlayer(mPlayer);
    } else {
      // this is due to screen orientation changed
      mMediaController.setMediaPlayer(mPlayer);
      if (mPlayer.isPlaying()) {
        mMediaController.setVisibility(View.VISIBLE);
        mMediaController.show();
      } else {
        mMediaController.setVisibility(View.GONE);
      }
    }
    mPlayer.setController(mMediaController);
    holderActivity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    mNetworkReceiver = new NetworkReceiver();
  }

  @Override
  public void onResume() {
    super.onResume();
    IntentFilter filter = new IntentFilter(
        ConnectivityManager.CONNECTIVITY_ACTION);
    getActivity().registerReceiver(mNetworkReceiver, filter);
  }

  @Override
  public void onPause() {
    super.onPause();
    getActivity().unregisterReceiver(mNetworkReceiver);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    LinearLayout view = (LinearLayout) inflater.inflate(
        R.layout.fragment_podcast, container, false);
    view.addView(mMediaController);
    return view;
  }

  @Override
  public void onPlayClicked(String url) {
    if (StaticMethods.checkConnection(getActivity())) {
      mUrl = url;
      try {
        MediaPlayer player = mPlayer.getMediaPlayer();
        if (player != null) {
          player.reset();
          player.setDataSource(mUrl);
          player.prepareAsync();
          Toast.makeText(getActivity(), R.string.preparing, Toast.LENGTH_LONG)
              .show();
        }
      } catch (Exception e) {
        // TODO(trung): display could not open message
        Log.e(TAG, "Could not open file " + mUrl + " for playback.", e);
      }

    }
  }

  @Override
  public void onStop() {
    boolean isChangingConfiguration = getActivity().isChangingConfigurations();
    Log.i(TAG, "mConfig=" + isChangingConfiguration);
    if (!isChangingConfiguration) {
      mPlayer.stopAndRelease();
    }
    super.onStop();
  }

  // only release the player when the activity is destroyed
  @Override
  public void onDestroy() {
    // mPlayer.stopAndRelease();
    super.onDestroy();
  }

  @Override
  public Object onRetainNonInstanceObject() {
    Log.i(TAG, "onRetainNonInstanceObject() called");
    return mPlayer;
  }

  /**
   * A media player controller that controls this player.
   * 
   * @author trung nguyen
   * 
   */
  public class PlayerController implements MediaPlayerControl {
    private MediaPlayer mMediaPlayer;
    private AlwaysOnMediaController mController;
    
    public PlayerController() {
      mMediaPlayer = new MediaPlayer();
      mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      mMediaPlayer.setWakeMode(getActivity(), PowerManager.PARTIAL_WAKE_LOCK);

      mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
          Log.i(TAG, "media prepared");
          int duration = getDuration(); // very stupid!!
          // should be startShowing() or sth like that
          mController.setVisibility(View.VISIBLE);
          Log.i(TAG, "onPrepared: should be visible: " + mMediaController);
          mController.show();
          mMediaPlayer.start();
        }
      });
      mMediaPlayer.setOnErrorListener(new OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
          Log.e(TAG, "Listener error: " + what + " " + extra);
          mp.reset();
          try {
            if (mUrl != null) {
              mp.reset();
              mp.setDataSource(mUrl);
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
          return true;
        }
      });
    }

    public void setController(AlwaysOnMediaController controller) {
      mController = controller;
    }
    
    /** Returns the internal media player */
    public MediaPlayer getMediaPlayer() {
      return mMediaPlayer;
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
      try {
        return mMediaPlayer.isPlaying();
      } catch (IllegalStateException e) {
        e.printStackTrace();
      } catch (NullPointerException e) {
        e.printStackTrace();
      }
      return false;
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
      try {
        mMediaPlayer.start();
      } catch (IllegalStateException e) {
        e.printStackTrace();
      }
    }

    public void stopAndRelease() {
      Log.i(TAG, "stopAndRelease() called");
      try {
        mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = null;
        Log.i(TAG, "mMediaPlayer set to null");
      } catch (IllegalStateException e) {
        e.printStackTrace();
      }
    }
  }
}
