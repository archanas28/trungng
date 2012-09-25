/*   
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rainmoon.podcast.musicplayer;

import java.util.Formatter;
import java.util.Locale;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.rainmoon.podcast.FeedItemActivity2;
import com.rainmoon.podcast.FeedItemActivity2.OnPlayButtonClickedListener;
import com.rainmoon.podcast.R;
import com.rainmoon.podcast.utils.StaticMethods;
import com.rainmoon.podcast.utils.Strings;

/**
 * Main activity: shows media player buttons. This activity shows the media
 * player buttons and lets the user click them. No media handling is done here
 * -- everything is done by passing Intents to our {@link MusicService}.
 * */
public class CuteMediaPlayer extends Fragment implements OnPlayButtonClickedListener {

  private static final int SHOW_PROGRESS = 2;

  private View mMediaController;

  private MusicService mBoundService;
  private boolean mIsBound;

  private View mControlerLayout;
  private SeekBar mSeekBar;
  private TextView mEndTime, mCurrentTime;
  private boolean mDragging = false;

  private Handler mHandler;
  StringBuilder mFormatBuilder;
  Formatter mFormatter;
  private ImageButton mPauseButton;
  private ImageButton mFfwdButton;
  private ImageButton mRewButton;
  private ImageButton mStopButton;
  private static final String TAG = "CuteMediaPlayer";

  private ServiceConnection mConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder service) {
      mBoundService = ((MusicService.LocalBinder) service).getService();
      Log.i(TAG, "boundService " + mBoundService + " received");
    }

    public void onServiceDisconnected(ComponentName className) {
      mBoundService = null;
      Log.i(TAG, "bound service disconnected");
    }
  };

  void doBindService() {
    mIsBound = getActivity().bindService(new Intent(this.getActivity(), MusicService.class),
        mConnection, Context.BIND_AUTO_CREATE);
  }

  void doUnbindService() {
    if (mIsBound) {
      // Detach our existing connection.
      getActivity().unbindService(mConnection);
      mIsBound = false;
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    LinearLayout view = (LinearLayout) inflater
        .inflate(R.layout.fragment_podcast, container, false);
    mMediaController = makeControllerView();
    view.addView(mMediaController);
    mMediaController.setVisibility(View.VISIBLE);
    mHandler = new ProgressUpdateHandler();
    mHandler.sendEmptyMessage(SHOW_PROGRESS);
    doBindService();
    return view;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    doUnbindService();
  }

  @Override
  public void onPlayClicked(String url) {
    if (StaticMethods.checkConnection(getActivity())) {
      Intent intent = new Intent(Strings.ACTION_PLAY);
      intent.putExtra(Strings.PLAY_URL, url);
      getActivity().startService(intent);
      doBindService();
      if (mPauseButton != null)
        mPauseButton.setImageResource(android.R.drawable.ic_media_pause);
      updatePausePlay(null);
      Toast.makeText(getActivity(), R.string.preparing, Toast.LENGTH_LONG).show();
    }
  }

  public MediaPlayer getMediaPlayer() {
    if (mIsBound && mBoundService != null) {
      return mBoundService.getMediaPlayer();
    }
    return null;
  }

  /**
   * Create the view that holds the widgets that control playback.
   * 
   * @return The controller view.
   */
  protected View makeControllerView() {
    LayoutInflater inflate = (LayoutInflater) getActivity().getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
    mControlerLayout = inflate.inflate(R.layout.media_controller, null);
    mPauseButton = (ImageButton) mControlerLayout.findViewById(R.id.btn_play);
    mPauseButton.setOnClickListener(mPauseListener);
    mFfwdButton = (ImageButton) mControlerLayout.findViewById(R.id.btn_fw);
    mFfwdButton.setOnClickListener(mFfwdListener);
    mRewButton = (ImageButton) mControlerLayout.findViewById(R.id.btn_rw);
    mRewButton.setOnClickListener(mRewListener);
    mStopButton = (ImageButton) mControlerLayout.findViewById(R.id.btn_stop);
    mStopButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        getActivity().startService(new Intent(Strings.ACTION_STOP));
        resetMediaController();
      }
    });
    mSeekBar = (SeekBar) mControlerLayout.findViewById(R.id.mc_seekbar);
    mSeekBar.setOnSeekBarChangeListener(mSeekListener);
    mSeekBar.setMax(1000);

    mFormatBuilder = new StringBuilder();
    mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    mCurrentTime = (TextView) mControlerLayout.findViewById(R.id.time_current);
    mCurrentTime.setText(stringForTime(0));
    mEndTime = (TextView) mControlerLayout.findViewById(R.id.time);
    mEndTime.setText(stringForTime(0));
    return mControlerLayout;
  }

  private class ProgressUpdateHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      try {
        MediaPlayer player = getMediaPlayer();
        if (!mDragging && player != null) {
          int pos = setProgress(player);
        }
        msg = obtainMessage(SHOW_PROGRESS);
        sendMessageDelayed(msg, 1000);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private String stringForTime(int timeMs) {
    int totalSeconds = timeMs / 1000;

    int seconds = totalSeconds % 60;
    int minutes = (totalSeconds / 60) % 60;
    int hours = totalSeconds / 3600;

    mFormatBuilder.setLength(0);
    if (hours > 0) {
      return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
    } else {
      return mFormatter.format("%02d:%02d", minutes, seconds).toString();
    }
  }

  private void resetMediaController() {
    try {
      if (mSeekBar != null) {
        mSeekBar.setProgress(0);
        mSeekBar.setSecondaryProgress(0);
      }
      mCurrentTime.setText(stringForTime(0));
      mEndTime.setText(stringForTime(0));
      mPauseButton.setImageResource(R.drawable.ic_media_play);
    } catch (NullPointerException e) {
      // this should never occur but we catch error to make sure the app does
      // not
      // crash
    }
  }

  private int setProgress(MediaPlayer player) {
    if (player == null) {
      player = getMediaPlayer();
    }
    if (player != null && player.isPlaying()) {
      int position = player.getCurrentPosition();
      int duration = player.getDuration();
      if (mSeekBar != null) {
        if (duration > 0) {
          // use long to avoid overflow
          long pos = 1000L * position / duration;
          mSeekBar.setProgress((int) pos);
        }
        int percent = (player.getCurrentPosition() * 100) / player.getDuration();
        mSeekBar.setSecondaryProgress(percent * 10);
      }

      if (mEndTime != null)
        mEndTime.setText(stringForTime(duration));
      if (mCurrentTime != null)
        mCurrentTime.setText(stringForTime(position));
      return position;
    }
    return 0;
  }

  // TODO(trung): perhaps define interface to communicate with activity for
  // event
  // need only update interface accordingly
  public boolean dispatchKeyEvent(KeyEvent event) {
    MediaPlayer player = getMediaPlayer();
    int keyCode = event.getKeyCode();
    final boolean uniqueDown = event.getRepeatCount() == 0
        && event.getAction() == KeyEvent.ACTION_DOWN;
    if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        || keyCode == KeyEvent.KEYCODE_SPACE) {
      if (uniqueDown) {
        doPauseResume();
        if (mPauseButton != null) {
          mPauseButton.requestFocus();
        }
      }
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
      if (uniqueDown && !player.isPlaying()) {
        // TODO start service for play
        updatePausePlay(player);
      }
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
      if (uniqueDown && player.isPlaying()) {
        // TODO start service for pause
        updatePausePlay(player);
      }
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP
        || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
      // don't show the controls for volume adjustment
      return false;
    }

    return false;
  }

  private void updatePausePlay(MediaPlayer player) {
    if (player == null) {
      player = getMediaPlayer();
    }
    if (player != null) {
      if (player.isPlaying()) {
        mPauseButton.setImageResource(R.drawable.ic_media_pause);
      } else {
        mPauseButton.setImageResource(R.drawable.ic_media_play);
      }
    }
  }

  private void doPauseResume() {
    try {
      FeedItemActivity2 context = (FeedItemActivity2) getActivity();
      String playUrl = context.getPlayUrl();
      if (playUrl == null) {
        Toast.makeText(context, R.string.no_media, Toast.LENGTH_SHORT).show();
        return;
      }
      MediaPlayer player = getMediaPlayer();
      if (player == null) {
        onPlayClicked(context.getPlayUrl());
      } else if (player.isPlaying()) {
        context.startService(new Intent(Strings.ACTION_PAUSE));
        mPauseButton.setImageResource(R.drawable.ic_media_play);
      } else {
        Intent intent = new Intent(Strings.ACTION_PLAY);
        intent.putExtra(Strings.PLAY_URL, context.getPlayUrl());
        context.startService(new Intent(Strings.ACTION_PLAY));
        mPauseButton.setImageResource(R.drawable.ic_media_pause);
      }
    } catch (Exception e) {
      Log.e(TAG, "error in doPauseResume()");
      e.printStackTrace();
    }
  }

  private void doSeek(int change) {
    Intent intent = new Intent(Strings.ACTION_SEEK);
    intent.putExtra(Strings.SEEK_CHANGE, change);
    getActivity().startService(intent);
    setProgress(null);
  }

  private View.OnClickListener mPauseListener = new View.OnClickListener() {
    public void onClick(View v) {
      doPauseResume();
    }
  };

  private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
    public void onStartTrackingTouch(SeekBar bar) {
      mDragging = true;
      mHandler.removeMessages(SHOW_PROGRESS);
    }

    @Override
    public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
      if (!fromuser) {
        return;
      }
      MediaPlayer player = getMediaPlayer();
      if (player != null) {
        long duration = player.getDuration();
        long newposition = (duration * progress) / 1000L;
        doSeek(((int) newposition) - player.getCurrentPosition());
        if (mCurrentTime != null)
          mCurrentTime.setText(stringForTime((int) newposition));
      }
    }

    public void onStopTrackingTouch(SeekBar bar) {
      mDragging = false;
      setProgress(null);
      updatePausePlay(null);
      mHandler.sendEmptyMessage(SHOW_PROGRESS);
    }
  };

  private View.OnClickListener mRewListener = new View.OnClickListener() {
    public void onClick(View v) {
      doSeek(-5000);
    }
  };

  private View.OnClickListener mFfwdListener = new View.OnClickListener() {
    public void onClick(View v) {
      doSeek(5000);
    }
  };

  // @Override
  // public boolean onKeyDown(int keyCode, KeyEvent event) {
  // switch (keyCode) {
  // case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
  // case KeyEvent.KEYCODE_HEADSETHOOK:
  // startService(new Intent(MusicService.ACTION_TOGGLE_PLAYBACK));
  // return true;
  // }
  // return super.onKeyDown(keyCode, event);
  // }
}
