package com.lingp.jppodcast;

import java.util.Calendar;

import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
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

import com.lingp.jppodcast.R;
import com.lingp.jppodcast.FeedItemActivity.OnConfigurationChangedListener;
import com.lingp.jppodcast.FeedItemActivity2.OnPlayButtonClickedListener;
import com.lingp.jppodcast.receiver.NetworkReceiver;
import com.lingp.jppodcast.utils.StaticMethods;
import com.lingp.jppodcast.utils.Strings;

/**
 * Fragment showing the Podcast player.
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
  private WifiLock mWifiLock;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    FragmentActivity holderActivity = getActivity();
    mWifiLock = ((WifiManager) holderActivity
        .getSystemService(Context.WIFI_SERVICE)).createWifiLock(
        WifiManager.WIFI_MODE_FULL, "mylock");
    mWifiLock.acquire();
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

  /**
   * Updates the listen count.
   * 
   * @param duration
   */
  private void updateListenCount(int duration) {
    SharedPreferences prefs = getActivity().getSharedPreferences(
        Strings.APPLICATION_SHARED_PREFERENCES, 0);
    Editor prefEditor = prefs.edit();
    long startTime = prefs.getLong(Strings.LISTEN_START_TIME, -1);
    Calendar calendar = Calendar.getInstance();
    long currentTime = calendar.getTimeInMillis();
    if (startTime == -1) { // first time
      prefEditor.putLong(Strings.LISTEN_START_TIME, currentTime);
      prefEditor.putBoolean(Strings.WEEK_RESETED, true);
      prefEditor.putBoolean(Strings.MONTH_RESETED, true);
    }
    // reset counter for the week and month as needed
    if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
        && !prefs.getBoolean(Strings.WEEK_RESETED, false)) {
      prefEditor.putBoolean(Strings.WEEK_RESETED, true);
      prefEditor.putLong(Strings.LISTEN_WEEK_ITEMS, 0);
      prefEditor.putLong(Strings.LISTEN_WEEK_MILLIS, 0);
    }
    if (calendar.get(Calendar.DAY_OF_MONTH) == 1
        && !prefs.getBoolean(Strings.MONTH_RESETED, false)) {
      prefEditor.putBoolean(Strings.MONTH_RESETED, true);
      prefEditor.putLong(Strings.LISTEN_MONTH_ITEMS, 0);
      prefEditor.putLong(Strings.LISTEN_MONTH_MILLIS, 0);
    }
    
    int totalItems = prefs.getInt(Strings.LISTEN_TOTAL_ITEMS, 0) + 1;
    long totalMillis = prefs.getLong(Strings.LISTEN_TOTAL_MILLIS, 0) + duration;
    int weekItems = prefs.getInt(Strings.LISTEN_WEEK_ITEMS, 0) + 1;
    long weekMillis = prefs.getLong(Strings.LISTEN_WEEK_MILLIS, 0) + duration;
    int monthItems = prefs.getInt(Strings.LISTEN_MONTH_ITEMS, 0) + 1;
    long monthMillis = prefs.getLong(Strings.LISTEN_MONTH_MILLIS, 0) + duration;
    prefEditor.putInt(Strings.LISTEN_TOTAL_ITEMS, totalItems);
    prefEditor.putLong(Strings.LISTEN_TOTAL_MILLIS, totalMillis);
    prefEditor.putInt(Strings.LISTEN_WEEK_ITEMS, weekItems);
    prefEditor.putLong(Strings.LISTEN_WEEK_MILLIS, weekMillis);
    prefEditor.putInt(Strings.LISTEN_MONTH_ITEMS, monthItems);
    prefEditor.putLong(Strings.LISTEN_MONTH_MILLIS, monthMillis);
    prefEditor.commit();
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

  boolean mIsChangingConfiguration = false;

  @Override
  public void onStop() {
    mIsChangingConfiguration = getActivity().isChangingConfigurations();
    Log.i(TAG, "mConfig=" + mIsChangingConfiguration);
    super.onStop();
  }

  // only release the player when the activity is destroyed
  @Override
  public void onDestroy() {
    super.onDestroy();
    if (!mIsChangingConfiguration && mPlayer != null) {
      mPlayer.stopAndRelease();
      mWifiLock.release();
    }
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
      Context context = getActivity().getApplicationContext();
      mMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
      mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
          Log.i(TAG, "media prepared");
          int duration = getDuration(); // very stupid!!
          Log.i(TAG, "duration = " + duration);
          // should be startShowing() or sth like that
          mController.setVisibility(View.VISIBLE);
          mController.show();
          mMediaPlayer.start();
          updateListenCount(duration);
        }
      });
      mMediaPlayer.setOnErrorListener(new OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
          Log.e(TAG, "Player error:" + what + " " + extra);
          Toast.makeText(getActivity(), getText(R.string.player_error_msg),
              Toast.LENGTH_SHORT).show();
          try {
            if (mUrl != null) {
              onPlayClicked(mUrl);
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
