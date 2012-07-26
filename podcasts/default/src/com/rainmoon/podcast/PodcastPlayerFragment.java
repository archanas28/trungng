package com.rainmoon.podcast;

import java.io.IOException;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;

/**
 * Fragment showing the Podcast player.
 * 
 * TODO(TRUNG): add this fragment to FeedItemActivity
 * 
 * @author trung nguyen
 * 
 */
public class PodcastPlayerFragment extends Fragment implements
    OnPlayButtonClickedListener {

  private static final String TAG = "PodcastPlayerFragment";
  private String mUrl = "";

  private MediaController mMediaController;
  private PodcastPlayer mPlayer;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPlayer = new PodcastPlayer();
    //mMediaController = new AlwaysOnMediaController(getActivity());
    mMediaController = new MediaController(getActivity());
    mMediaController.setMediaPlayer(mPlayer);
  }

  public class AlwaysOnMediaController extends MediaController {
    public AlwaysOnMediaController(Context context) {
      super(context);
    }

    @Override
    public void hide() {
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
      return false;
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    // TODO(trung): create view programmatically
    View view = new LinearLayout(getActivity());
    LayoutParams layout = new LayoutParams(LayoutParams.FILL_PARENT,
        LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(layout);
    mMediaController.setAnchorView(view);
    // View playerController = inflater.inflate(R.layout.fragment_podcast,
    // container, false);
    // mMediaController.setAnchorView(playerController
    // .findViewById(R.id.podcast_player));
    return view;
  }

  @Override
  public void onPlayClicked(String url) {
    mUrl = url;
    try {
      MediaPlayer player = mPlayer.getMediaPlayer();
      if (player != null) {
        player.reset();
        player.setDataSource(mUrl);
        player.prepareAsync();
      }
    } catch (IOException e) {
      Log.e(TAG, "Could not open file " + mUrl + " for playback.", e);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "Could not open file " + mUrl + " for playback.", e);
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
   * A media player control to associate with the MediaController
   * 
   * @author trung nguyen
   * 
   */
  public class PodcastPlayer implements MediaPlayerControl {
    private MediaPlayer mMediaPlayer;

    public PodcastPlayer() {
      mMediaPlayer = new MediaPlayer();
      mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
          Log.i(TAG, "media prepared");
          int duration = getDuration(); // very stupid!!
          // can only call show() once preparation is done (because it calls
          // getDuration() which will be in invalid state)
          mMediaController.show(0);
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
      mMediaPlayer.start();
    }

    public void stopAndRelease() {
      mMediaPlayer.stop();
      mMediaPlayer.release();
    }

    // TODO(trung): get the touch even from Activity
    public boolean onTouchEvent(MotionEvent event) {
      mMediaController.show(0);
      return false;
    }
  }
}
