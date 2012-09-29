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

package com.lingp.espodcast.musicplayer;

import java.util.Calendar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.lingp.espodcast.FeedItemActivity2;
import com.lingp.espodcast.R;
import com.lingp.espodcast.utils.Strings;

/**
 * Service that handles media playback. This is the Service through which we
 * perform all the media handling in our application. Upon initialization, it
 * starts a {@link MusicRetriever} to scan the user's media. Then, it waits for
 * Intents (which come from our main activity, {@link CuteMediaPlayer}, which
 * signal the service to perform specific operations: Play, Pause, Rewind, Skip,
 * etc.
 * 
 * TODO music service must register MusicIntentReceiver to handle MediaButton
 * and AudioNoisy
 */
public class MusicService extends Service implements OnPreparedListener, OnErrorListener,
    MusicFocusable {

  final static String TAG = "MusicService";

  // The volume we set the media player to when we lose audio focus, but are
  // allowed to reduce the volume instead of stopping playback.
  public static final float DUCK_VOLUME = 0.1f;

  // indicates the state our service:
  enum State {
    Stopped, // media player is stopped and not prepared to play
    Preparing, // media player is preparing...
    Playing, // playback active (media player ready!). (but the media player may
             // actually be paused in this state if we don't have audio focus.
             // But we stay in this state so that we know we have to resume
             // playback once we get focus back)
    Paused
    // playback paused (media player ready!)
  };

  enum PauseReason {
    UserRequest, // paused by user request
    FocusLoss, // paused because of audio focus loss
  };

  enum AudioFocus {
    NoFocusNoDuck, // we don't have audio focus, and can't duck
    NoFocusCanDuck, // we don't have focus, but can play at a low volume
                    // ("ducking")
    Focused
    // we have full audio focus
  }

  AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;
  State mState;
  // why did we pause? (only relevant if mState == State.Paused)
  PauseReason mPauseReason = PauseReason.UserRequest;
  // our media player
  MediaPlayer mPlayer = null;
  // If not available, this will be null. Always check for null before using!
  AudioFocusHelper mAudioFocusHelper = null;

  String mUrl = null;
  // title of the song we are currently playing
  String mSongTitle = "";

  WifiLock mWifiLock;
  final int NOTIFICATION_ID = 1;

  AudioManager mAudioManager;
  NotificationManager mNotificationManager;
  Notification mNotification = null;
  private final IBinder mBinder = new LocalBinder();

  public class LocalBinder extends Binder {
    MusicService getService() {
      return MusicService.this;
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  /**
   * Makes sure the media player exists and has been reset. This will create the
   * media player if needed, or reset the existing media player if one already
   * exists.
   */
  void createMediaPlayerIfNeeded() {
    if (mPlayer == null) {
      mPlayer = new MediaPlayer();
      mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
      mPlayer.setOnPreparedListener(this);
      mPlayer.setOnErrorListener(this);
    } else
      mPlayer.reset();
  }

  /**
   * Returns the internal media player running in this service. TODO provide
   * method for query state instead of directly exposing the player
   * 
   * @return
   */
  MediaPlayer getMediaPlayer() {
    return mPlayer;
  }

  @Override
  public void onCreate() {
    mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(
        WifiManager.WIFI_MODE_FULL, "mylock");
    mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
    // create the Audio Focus Helper, if the Audio Focus feature is available
    // (SDK 8 or above)
    if (android.os.Build.VERSION.SDK_INT >= 8)
      mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
    else
      mAudioFocus = AudioFocus.Focused;
  }

  /**
   * Called when we receive an Intent. When we receive an intent sent to us via
   * startService(), this is the method that gets called. So here we react
   * appropriately depending on the Intent's action, which specifies what is
   * being requested of us.
   */
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    String action = intent.getAction();
    mUrl = intent.getStringExtra(Strings.PLAY_URL);
    if (action.equals(Strings.ACTION_TOGGLE_PLAYBACK))
      processTogglePlaybackRequest(intent);
    else if (action.equals(Strings.ACTION_PLAY))
      processPlayRequest(intent);
    else if (action.equals(Strings.ACTION_PAUSE))
      processPauseRequest();
    else if (action.equals(Strings.ACTION_STOP))
      processStopRequest();
    else if (action.equals(Strings.ACTION_SEEK))
      processSeekRequest(intent);

    return START_NOT_STICKY; // Means we started the service, but don't want it
                             // to
                             // restart in case it's killed.
  }

  void processPlayRequest(Intent intent) {
    Log.i(TAG, "Playing from URL/path: " + mUrl);
    // try to get audio focus
    if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
        && mAudioFocusHelper.requestFocus())
      mAudioFocus = AudioFocus.Focused;

    if (mState == State.Paused) { // resume playback
      mState = State.Playing;
      setUpAsForeground(mSongTitle + " (playing)");
      configAndStartMediaPlayer();
    } else { // start play
      mState = State.Stopped;
      relaxResources(false); // release everything except MediaPlayer
      try {
        // set the source of the media player to a manual URL or path
        createMediaPlayerIfNeeded();
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setDataSource(mUrl);
        mState = State.Preparing;
        setUpAsForeground(mUrl);
        mPlayer.prepareAsync();
        mWifiLock.acquire();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  void processTogglePlaybackRequest(Intent intent) {
    if (mState == State.Paused || mState == State.Stopped) {
      processPlayRequest(intent);
    } else {
      processPauseRequest();
    }
  }

  void processPauseRequest() {
    if (mState == State.Playing) {
      // Pause media player and cancel the 'foreground service' state.
      mState = State.Paused;
      mPlayer.pause();
      relaxResources(false); // while paused, we always retain the MediaPlayer
      // do not give up audio focus
    }
  }

  void processSeekRequest(Intent intent) {
    if (mState == State.Playing || mState == State.Paused) {
      int change = intent.getIntExtra(Strings.SEEK_CHANGE, 0);
      int newposition = mPlayer.getCurrentPosition() + change;
      mPlayer.seekTo(newposition);
    }
  }

  void processStopRequest() {
    processStopRequest(false);
  }

  void processStopRequest(boolean force) {
    if (mState == State.Playing || mState == State.Paused || force) {
      mState = State.Stopped;
      // let go of all resources...
      relaxResources(true);
      giveUpAudioFocus();
      stopSelf();
    }
  }

  /**
   * Releases resources used by the service for playback. This includes the
   * "foreground service" status and notification, the wake locks and possibly
   * the MediaPlayer.
   * 
   * @param releaseMediaPlayer
   *          Indicates whether the Media Player should also be released or not
   */
  void relaxResources(boolean releaseMediaPlayer) {
    // stop being a foreground service
    stopForeground(true);

    // stop and release the Media Player, if it's available
    if (releaseMediaPlayer && mPlayer != null) {
      mPlayer.reset();
      mPlayer.release();
      mPlayer = null;
    }

    // we can also release the Wifi lock, if we're holding it
    if (mWifiLock.isHeld())
      mWifiLock.release();
  }

  void giveUpAudioFocus() {
    if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
        && mAudioFocusHelper.abandonFocus())
      mAudioFocus = AudioFocus.NoFocusNoDuck;
  }

  /**
   * Reconfigures MediaPlayer according to audio focus settings and
   * starts/restarts it. This method starts/restarts the MediaPlayer respecting
   * the current audio focus state. So if we have focus, it will play normally;
   * if we don't have focus, it will either leave the MediaPlayer paused or set
   * it to a low volume, depending on what is allowed by the current focus
   * settings. This method assumes mPlayer != null, so if you are calling it,
   * you have to do so from a context where you are sure this is the case.
   */
  void configAndStartMediaPlayer() {
    if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
      if (mPlayer.isPlaying())
        mPlayer.pause();
      return;
    } else if (mAudioFocus == AudioFocus.NoFocusCanDuck)
      mPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME); // we'll be relatively quiet
    else
      mPlayer.setVolume(1.0f, 1.0f); // we can be loud

    if (!mPlayer.isPlaying()) {
      mPlayer.start();
      updateListenCount(mPlayer.getDuration());
    }
  }

  /** Called when media player is done preparing. */
  public void onPrepared(MediaPlayer player) {
    // The media player is done preparing. That means we can start playing!
    mState = State.Playing;
    updateNotification(mSongTitle + " (playing)");
    configAndStartMediaPlayer();
  }

  /** Updates the notification. */
  void updateNotification(String text) {
    PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(
        getApplicationContext(), FeedItemActivity2.class), PendingIntent.FLAG_UPDATE_CURRENT);
    mNotification.setLatestEventInfo(getApplicationContext(), getText(R.string.app_name), text, pi);
    mNotificationManager.notify(NOTIFICATION_ID, mNotification);
  }

  /**
   * Configures service as a foreground service. A foreground service is a
   * service that's doing something the user is actively aware of (such as
   * playing music), and must appear to the user as a notification. That's why
   * we create the notification here.
   */
  void setUpAsForeground(String text) {
    PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(
        getApplicationContext(), CuteMediaPlayer.class), PendingIntent.FLAG_UPDATE_CURRENT);
    mNotification = new Notification();
    mNotification.tickerText = text;
    mNotification.icon = R.drawable.ic_stat_playing;
    mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
    mNotification.setLatestEventInfo(getApplicationContext(), getString(R.string.app_name), text,
        pi);
    startForeground(NOTIFICATION_ID, mNotification);
  }

  /**
   * Called when there's an error playing media. When this happens, the media
   * player goes to the Error state. We warn the user about the error and reset
   * the media player.
   */
  public boolean onError(MediaPlayer mp, int what, int extra) {
    Toast.makeText(getApplicationContext(), "Media player error! Resetting.", Toast.LENGTH_SHORT)
        .show();

    mState = State.Stopped;
    relaxResources(true);
    giveUpAudioFocus();
    return true; // true indicates we handled the error
  }

  public void onGainedAudioFocus() {
    mAudioFocus = AudioFocus.Focused;

    // restart media player with new focus settings
    if (mState == State.Playing)
      configAndStartMediaPlayer();
  }

  public void onLostAudioFocus(boolean canDuck) {
    mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

    // start/restart/pause media player with new focus settings
    if (mPlayer != null && mPlayer.isPlaying())
      configAndStartMediaPlayer();
  }

  /**
   * Updates the listen count.
   * 
   * @param duration
   */
  private void updateListenCount(int duration) {
    SharedPreferences prefs = getSharedPreferences(Strings.APPLICATION_SHARED_PREFERENCES, 0);
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
    if (calendar.get(Calendar.DAY_OF_MONTH) == 1 && !prefs.getBoolean(Strings.MONTH_RESETED, false)) {
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
  public void onDestroy() {
    // Service is being killed, so make sure we release our resources
    mState = State.Stopped;
    relaxResources(true);
    giveUpAudioFocus();
    Log.i(TAG, "is being desotryed");
  }
}
