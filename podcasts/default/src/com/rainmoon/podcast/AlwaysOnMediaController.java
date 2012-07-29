package com.rainmoon.podcast;

/*
 * Copyright (C) 2006 The Android Open Source Project
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

import java.util.Formatter;
import java.util.Locale;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * A view containing controls for a MediaPlayer. Typically contains the buttons
 * like "Play/Pause", "Rewind", "Fast Forward" and a progress slider. It takes
 * care of synchronizing the controls with the state of the MediaPlayer.
 * <p>
 * The way to use this class is to instantiate it programatically. The
 * MediaController will create a default set of controls and put them in a frame
 * as a regular view in your application.
 * <p>
 * TODO
 * 1. One instance when connection is lost while playing, there is no updating
 * of time and progress
 */
public class AlwaysOnMediaController extends FrameLayout {

  private MediaPlayerControl mPlayer;
  private Context mContext;
  private View mRoot;
  private ProgressBar mProgress;
  private TextView mEndTime, mCurrentTime;
  private boolean mDragging;
  private static final int SHOW_PROGRESS = 2;
  private boolean mUseFastForward;
  private boolean mFromXml;
  StringBuilder mFormatBuilder;
  Formatter mFormatter;
  private ImageButton mPauseButton;
  private ImageButton mFfwdButton;
  private ImageButton mRewButton;

  @Override
  public void onFinishInflate() {
    if (mRoot != null)
      initControllerView(mRoot);
  }

  public AlwaysOnMediaController(Context context) {
    super(context);
    mContext = context;
    mUseFastForward = true;
    setFocusable(true);
    setFocusableInTouchMode(true);
    setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
    //requestFocus();
    View v = makeControllerView();
    FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT);
    addView(v, frameParams);
  }

  /**
   * Create the view that holds the widgets that control playback.
   * 
   * @return The controller view.
   */
  protected View makeControllerView() {
    LayoutInflater inflate = (LayoutInflater) mContext
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    mRoot = inflate.inflate(R.layout.media_controller, null);
    initControllerView(mRoot);
    return mRoot;
  }

  private void initControllerView(View v) {
    mPauseButton = (ImageButton) v.findViewById(R.id.btn_play);
    if (mPauseButton != null) {
      mPauseButton.requestFocus();
      mPauseButton.setOnClickListener(mPauseListener);
    }

    mFfwdButton = (ImageButton) v.findViewById(R.id.btn_fw);
    if (mFfwdButton != null) {
      mFfwdButton.setOnClickListener(mFfwdListener);
      if (!mFromXml) {
        mFfwdButton.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
      }
    }

    mRewButton = (ImageButton) v.findViewById(R.id.btn_rw);
    if (mRewButton != null) {
      mRewButton.setOnClickListener(mRewListener);
      if (!mFromXml) {
        mRewButton.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
      }
    }

    mProgress = (ProgressBar) v.findViewById(R.id.mc_progressbar);
    if (mProgress != null) {
      if (mProgress instanceof SeekBar) {
        SeekBar seeker = (SeekBar) mProgress;
        seeker.setOnSeekBarChangeListener(mSeekListener);
      }
      mProgress.setMax(1000);
    }

    mEndTime = (TextView) v.findViewById(R.id.time);
    mCurrentTime = (TextView) v.findViewById(R.id.time_current);
    mFormatBuilder = new StringBuilder();
    mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
  }

  public void setMediaPlayer(MediaPlayerControl player) {
    mPlayer = player;
  }

  /**
   * Show the controller on screen.
   */
  public void show() {
    updatePausePlay();
    mHandler.sendEmptyMessage(SHOW_PROGRESS);
  }

  /**
   * Disable pause or seek buttons if the stream cannot be paused or seeked.
   * This requires the control interface to be a MediaPlayerControlExt
   */
  private void disableUnsupportedButtons() {
    try {
      if (mPauseButton != null && !mPlayer.canPause()) {
        mPauseButton.setEnabled(false);
      }
      if (mRewButton != null && !mPlayer.canSeekBackward()) {
        mRewButton.setEnabled(false);
      }
      if (mFfwdButton != null && !mPlayer.canSeekForward()) {
        mFfwdButton.setEnabled(false);
      }
    } catch (IncompatibleClassChangeError ex) {
      // We were given an old version of the interface, that doesn't have
      // the canPause/canSeekXYZ methods. This is OK, it just means we
      // assume the media can be paused and seeked, and so we don't disable
      // the buttons.
    }
  }

  private Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      if (!mDragging && mPlayer.isPlaying()) {
        int pos = setProgress();
        msg = obtainMessage(SHOW_PROGRESS);
        // update progress bar every second
        sendMessageDelayed(msg, 1000 - (pos % 1000));
      }
    }
  };

  private String stringForTime(int timeMs) {
    int totalSeconds = timeMs / 1000;

    int seconds = totalSeconds % 60;
    int minutes = (totalSeconds / 60) % 60;
    int hours = totalSeconds / 3600;

    mFormatBuilder.setLength(0);
    if (hours > 0) {
      return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds)
          .toString();
    } else {
      return mFormatter.format("%02d:%02d", minutes, seconds).toString();
    }
  }

  private int setProgress() {
    if (mPlayer == null || mDragging) {
      return 0;
    }
    int position = mPlayer.getCurrentPosition();
    int duration = mPlayer.getDuration();
    if (mProgress != null) {
      if (duration > 0) {
        // use long to avoid overflow
        long pos = 1000L * position / duration;
        mProgress.setProgress((int) pos);
      }
      int percent = mPlayer.getBufferPercentage();
      mProgress.setSecondaryProgress(percent * 10);
    }

    if (mEndTime != null)
      mEndTime.setText(stringForTime(duration));
    if (mCurrentTime != null)
      mCurrentTime.setText(stringForTime(position));

    return position;
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    int keyCode = event.getKeyCode();
    final boolean uniqueDown = event.getRepeatCount() == 0
        && event.getAction() == KeyEvent.ACTION_DOWN;
    if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
        || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        || keyCode == KeyEvent.KEYCODE_SPACE) {
      if (uniqueDown) {
        doPauseResume();
        if (mPauseButton != null) {
          mPauseButton.requestFocus();
        }
      }
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
      if (uniqueDown && !mPlayer.isPlaying()) {
        mPlayer.start();
        updatePausePlay();
      }
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
        || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
      if (uniqueDown && mPlayer.isPlaying()) {
        mPlayer.pause();
        updatePausePlay();
      }
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        || keyCode == KeyEvent.KEYCODE_VOLUME_UP
        || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
      // don't show the controls for volume adjustment
      return super.dispatchKeyEvent(event);
    } else if (keyCode == KeyEvent.KEYCODE_BACK
        || keyCode == KeyEvent.KEYCODE_MENU) {
      return true;
    }

    return super.dispatchKeyEvent(event);
  }

  private View.OnClickListener mPauseListener = new View.OnClickListener() {
    public void onClick(View v) {
      doPauseResume();
    }
  };

  private void updatePausePlay() {
    if (mRoot == null || mPauseButton == null)
      return;

    if (mPlayer.isPlaying()) {
      mPauseButton.setImageResource(android.R.drawable.ic_media_pause);
    } else {
      mPauseButton.setImageResource(android.R.drawable.ic_media_play);
    }
  }

  private void doPauseResume() {
    if (mPlayer.isPlaying()) {
      mPlayer.pause();
    } else {
      mPlayer.start();
    }
    updatePausePlay();
  }

  // There are two scenarios that can trigger the seekbar listener to trigger:
  //
  // The first is the user using the touchpad to adjust the posititon of the
  // seekbar's thumb. In this case onStartTrackingTouch is called followed by
  // a number of onProgressChanged notifications, concluded by
  // onStopTrackingTouch.
  // We're setting the field "mDragging" to true for the duration of the
  // dragging
  // session to avoid jumps in the position in case of ongoing playback.
  //
  // The second scenario involves the user operating the scroll ball, in this
  // case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
  // we will simply apply the updated position without suspending regular
  // updates.
  private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
    public void onStartTrackingTouch(SeekBar bar) {
      mDragging = true;
      // By removing these pending progress messages we make sure
      // that a) we won't update the progress while the user adjusts
      // the seekbar and b) once the user is done dragging the thumb
      // we will post one of these messages to the queue again and
      // this ensures that there will be exactly one message queued up.
      mHandler.removeMessages(SHOW_PROGRESS);
    }

    public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
      if (!fromuser) {
        // We're not interested in programmatically generated changes to
        // the progress bar's position.
        return;
      }

      long duration = mPlayer.getDuration();
      long newposition = (duration * progress) / 1000L;
      mPlayer.seekTo((int) newposition);
      if (mCurrentTime != null)
        mCurrentTime.setText(stringForTime((int) newposition));
    }

    public void onStopTrackingTouch(SeekBar bar) {
      mDragging = false;
      setProgress();
      updatePausePlay();
      // Ensure that progress is properly updated in the future,
      // the call to show() does not guarantee this because it is a
      // no-op if we are already showing.
      mHandler.sendEmptyMessage(SHOW_PROGRESS);
    }
  };

  @Override
  public void setEnabled(boolean enabled) {
    if (mPauseButton != null) {
      mPauseButton.setEnabled(enabled);
    }
    if (mFfwdButton != null) {
      mFfwdButton.setEnabled(enabled);
    }
    if (mRewButton != null) {
      mRewButton.setEnabled(enabled);
    }
    if (mProgress != null) {
      mProgress.setEnabled(enabled);
    }
    disableUnsupportedButtons();
    super.setEnabled(enabled);
  }

  private View.OnClickListener mRewListener = new View.OnClickListener() {
    public void onClick(View v) {
      int pos = mPlayer.getCurrentPosition();
      pos -= 5000; // milliseconds
      mPlayer.seekTo(pos);
      setProgress();
    }
  };

  private View.OnClickListener mFfwdListener = new View.OnClickListener() {
    public void onClick(View v) {
      int pos = mPlayer.getCurrentPosition();
      pos += 15000; // milliseconds
      mPlayer.seekTo(pos);
      setProgress();
    }
  };
}