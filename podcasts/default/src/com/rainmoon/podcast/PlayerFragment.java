package com.rainmoon.podcast;

import java.io.IOException;

import com.rainmoon.podcast.FeedItemActivity.OnPlayButtonClickedListener;
import com.rainmoon.podcast.receiver.NetworkReceiver;
import com.rainmoon.podcast.utils.StaticMethods;

import android.app.ProgressDialog;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.MediaController.MediaPlayerControl;

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
    OnPlayButtonClickedListener {

  private static final String TAG = "PlayerFragment";
  private String mUrl = "";

  private AlwaysOnMediaController mMediaController;
  private PlayerController mPlayer;
  private ProgressDialog mDialog;
  private NetworkReceiver mNetworkReceiver;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPlayer = new PlayerController();
    mMediaController = new AlwaysOnMediaController(getActivity());
    mMediaController.setMediaPlayer(mPlayer);
    mMediaController.setVisibility(View.GONE);
    mNetworkReceiver = new NetworkReceiver();
    getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
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
          mDialog = ProgressDialog.show(getActivity(), "", getActivity()
              .getString(R.string.buffering), true);
          player.prepareAsync();
        }
      } catch (Exception e) {
        Log.e(TAG, "Could not open file " + mUrl + " for playback.", e);
      }

    }
  }

  // only release the player when the activity is destroyed
  @Override
  public void onDestroy() {
    super.onDestroy();
    mPlayer.stopAndRelease();
    mPlayer = null;
  }

  /**
   * A media player controller that controls this player.
   * 
   * @author trung nguyen
   * 
   */
  public class PlayerController implements MediaPlayerControl {
    private MediaPlayer mMediaPlayer;

    public PlayerController() {
      mMediaPlayer = new MediaPlayer();
      mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
          Log.i(TAG, "media prepared");
          int duration = getDuration(); // very stupid!!
          mMediaController.setVisibility(View.VISIBLE);
          // should be startShowing() or sth like that
          mMediaController.show();
          mMediaPlayer.start();
          mDialog.dismiss();
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
      try {
        mMediaPlayer.stop();
        mMediaPlayer.release();
      } catch (IllegalStateException e) {
        e.printStackTrace();
      }
    }
  }
}
