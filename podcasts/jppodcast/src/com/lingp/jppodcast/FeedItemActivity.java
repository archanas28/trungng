/**
 * Sparse rss
 * 
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.lingp.jppodcast;

import java.text.DateFormat;
import java.util.Date;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.lingp.jppodcast.R;
import com.lingp.jppodcast.PlayerFragment.OnPlayerFragmentListener;
import com.lingp.jppodcast.PlayerFragment.PlayerController;
import com.lingp.jppodcast.provider.FeedData;
import com.lingp.jppodcast.utils.StaticMethods;
import com.lingp.jppodcast.utils.Strings;

/**
 * Activity for showing a single feed item.
 * 
 * TODO(trung): show ProgressBar while buffering...
 * 
 * @author trung nguyen
 * 
 */
@Deprecated
public class FeedItemActivity extends FragmentActivity implements
    OnPlayerFragmentListener {

  /**
   * The mPlayClickedListener that is used to request a media player to play
   * from an external button.
   */
  public interface OnPlayButtonClickedListener {

    /**
     * This event is called when a play button is clicked.
     * 
     * @param url
     *          the url of the audio in this feed item
     */
    public void onPlayClicked(String url);
  }

  /**
   * The listenr that saves media player when the screen orientation changes.
   */
  public interface OnConfigurationChangedListener {

    /**
     * Returns an object that should be retained when the configuration changes.
     * More specifically, this method can be used to return a value object
     * (e.g., heavy resource or content downloaded from the internet). This
     * allows the activity to use the object when for example, screen
     * orientation changes, instead of creating a new one.
     * 
     * @return
     */
    public Object onRetainNonInstanceObject();
  }

  private static final String TEXT_HTML = "text/html";
  private static final String UTF8 = "utf-8";

  private static final String CSS = "<head><style type=\"text/css\">body {max-width: 100%}\nimg {max-width: 100%; height: auto;}\npre {white-space: pre-wrap;}</style></head>";
  private static final String FONTSIZE_START = "<font size=\"+";
  private static final String FONTSIZE_MIDDLE = "\">";
  private static final String FONTSIZE_END = "</font>";
  private static final String BODY_START = "<body>";
  private static final String BODY_END = "</body>";

  private static final String IMAGE_ENCLOSURE = "[@]image/";
  private static final String TEXTPLAIN = "text/plain";
  private static final String TAG = "FeedItemActivity";

  private int mTitleColumnIdx;
  private int mDateColumnIdx;
  private int mAbstractColumnIdx;
  private int mLinkColumnIdx;
  private int mFeedIdColumnIdx;
  private int mFavoriteColumnIdx;
  private int mReadDateColumnIdx;
  private int mEnclosureColumnIdx;

  private String _id;
  private String link;
  private Uri uri;
  private int feedId;
  boolean favorite;
  private boolean canShowIcon;
  private byte[] iconBytes;

  private WebView webView;
  private TextView urlButton;
  private TextView playButton;
  private OnPlayButtonClickedListener mPlayClickedListener;
  private OnConfigurationChangedListener mConfigChangeListener;
  private PlayerController mPlayerController;
  private Fragment mPlayerFragment;

  int scrollX;
  int scrollY;

  private SharedPreferences preferences;
  private boolean localPictures;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPlayerController = (PlayerController) getLastCustomNonConfigurationInstance();
    setContentView(R.layout.activity_feeditem);
    uri = getIntent().getData();
    iconBytes = getIntent()
        .getByteArrayExtra(FeedData.SubscriptionColumns.ICON);
    feedId = 0;
    setupFeedColumnIndice();

    // set up play button
    mPlayerFragment = getSupportFragmentManager().findFragmentById(
        R.id.frag_player);
    mPlayClickedListener = (OnPlayButtonClickedListener) mPlayerFragment;
    mConfigChangeListener = (OnConfigurationChangedListener) mPlayerFragment;

    playButton = (TextView) findViewById(R.id.btn_play);
    urlButton = (TextView) findViewById(R.id.btn_view);

    webView = (WebView) findViewById(R.id.content);
    OnKeyListener onKeyEventListener = new OnKeyListener() {
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
          if (keyCode == 92 || keyCode == 94) {
            scrollUp();
            return true;
          } else if (keyCode == 93 || keyCode == 95) {
            scrollDown();
            return true;
          }
        }
        return false;
      }
    };
    webView.setOnKeyListener(onKeyEventListener);
    preferences = PreferenceManager.getDefaultSharedPreferences(this);
    scrollX = 0;
    scrollY = 0;
  }

  private void setupFeedColumnIndice() {
    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
    mTitleColumnIdx = cursor.getColumnIndex(FeedData.ItemColumns.TITLE);
    mDateColumnIdx = cursor.getColumnIndex(FeedData.ItemColumns.DATE);
    mAbstractColumnIdx = cursor.getColumnIndex(FeedData.ItemColumns.ABSTRACT);
    mLinkColumnIdx = cursor.getColumnIndex(FeedData.ItemColumns.LINK);
    mFeedIdColumnIdx = cursor.getColumnIndex(FeedData.ItemColumns.FEED_ID);
    mFavoriteColumnIdx = cursor.getColumnIndex(FeedData.ItemColumns.FAVORITE);
    mReadDateColumnIdx = cursor.getColumnIndex(FeedData.ItemColumns.READDATE);
    mEnclosureColumnIdx = cursor.getColumnIndex(FeedData.ItemColumns.ENCLOSURE);
    cursor.close();
  }

  @Override
  protected void onResume() {
    super.onResume();
    uri = getIntent().getData();
    reload();
  }

  @Override
  protected void onPause() {
    super.onPause();
    scrollX = webView.getScrollX();
    scrollY = webView.getScrollY();
  }

  @Override
  public Object onRetainCustomNonConfigurationInstance() {
    super.onRetainCustomNonConfigurationInstance();
    mPlayerController = (PlayerController) mConfigChangeListener
        .onRetainNonInstanceObject();
    return mPlayerController;
  }

  @Override
  public PlayerController getLastMediaPlayerController() {
    return mPlayerController;
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
  }

  private void reload() {
    if (_id != null && _id.equals(uri.getLastPathSegment())) {
      return;
    }
    _id = uri.getLastPathSegment();
    ContentValues values = new ContentValues();
    values.put(FeedData.ItemColumns.READDATE, System.currentTimeMillis());
    Cursor entryCursor = getContentResolver()
        .query(uri, null, null, null, null);
    if (entryCursor.moveToFirst()) {
      String abstractText = entryCursor.getString(mAbstractColumnIdx);
      if (entryCursor.isNull(mReadDateColumnIdx)) {
        getContentResolver().update(
            uri,
            values,
            new StringBuilder(FeedData.ItemColumns.READDATE).append(
                Strings.DB_ISNULL).toString(), null);
      }
      if (abstractText == null) {
        String link = entryCursor.getString(mLinkColumnIdx);
        entryCursor.close();
        finish();
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
      } else {
        ((TextView) findViewById(R.id.item_title)).setText(entryCursor
            .getString(mTitleColumnIdx));
        int _feedId = entryCursor.getInt(mFeedIdColumnIdx);
        if (feedId != _feedId) {
          if (feedId != 0) {
            iconBytes = null; // triggers re-fetch of the icon
          }
          feedId = _feedId;
        }
        drawIcon();

        long date = entryCursor.getLong(mDateColumnIdx);
        ((TextView) findViewById(R.id.entry_date)).setText(DateFormat
            .getDateTimeInstance().format(new Date(date)));
        drawFavoriteButton(entryCursor);

        // loadData does not recognize the encoding without correct html-header
        localPictures = abstractText.indexOf(Strings.IMAGEID_REPLACEMENT) > -1;
        if (localPictures) {
          abstractText = abstractText.replace(Strings.IMAGEID_REPLACEMENT, _id
              + Strings.IMAGEFILE_IDSEPARATOR);
        }
        if (preferences.getBoolean(Strings.SETTINGS_DISABLEPICTURES, false)) {
          abstractText = abstractText.replaceAll(Strings.HTML_IMG_REGEX,
              Strings.EMPTY);
          webView.getSettings().setLoadsImagesAutomatically(false);
        } else {
          webView.getSettings().setLoadsImagesAutomatically(true);
        }

        int fontsize = Integer.parseInt(preferences.getString(
            Strings.SETTINGS_FONTSIZE, Strings.ONE));
        if (fontsize > 0) {
          webView.loadDataWithBaseURL(null, new StringBuilder(FONTSIZE_START)
              .append(fontsize).append(FONTSIZE_MIDDLE).append(abstractText)
              .append(FONTSIZE_END).toString(), TEXT_HTML, UTF8, null);
        } else {
          webView.loadDataWithBaseURL(null,
              new StringBuilder(CSS).append(BODY_START).append(abstractText)
                  .append(BODY_END).toString(), TEXT_HTML, UTF8, null);
        }
        setupButtons(entryCursor);
        webView.scrollTo(scrollX, scrollY); // resets the scrolling
      }
    }
    entryCursor.close();
  }

  private void setupButtons(Cursor entryCursor) {
    link = entryCursor.getString(mLinkColumnIdx);
    if (link != null && link.length() > 0) {
      urlButton.setEnabled(true);
      urlButton.setOnClickListener(new OnClickListener() {
        public void onClick(View view) {
          startActivityForResult(
              new Intent(Intent.ACTION_VIEW, Uri.parse(link)), 0);
        }
      });
    } else {
      urlButton.setEnabled(false);
    }

    final String enclosure = entryCursor.getString(mEnclosureColumnIdx);
    if (enclosure != null && enclosure.length() > 6
        && enclosure.indexOf(IMAGE_ENCLOSURE) == -1) {
      playButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          if (preferences.getBoolean(Strings.SETTINGS_ENCLOSUREWARNINGSENABLED,
              true)) {
            final int position1 = enclosure
                .indexOf(Strings.ENCLOSURE_SEPARATOR);
            final int position2 = enclosure.indexOf(
                Strings.ENCLOSURE_SEPARATOR, position1 + 3);
            final Uri uri = Uri.parse(enclosure.substring(0, position1));
            Builder builder = new AlertDialog.Builder(FeedItemActivity.this);
            builder.setTitle(R.string.question_areyousure);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            if (position2 + 4 > enclosure.length()) {
              builder.setMessage(getString(R.string.question_playenclosure,
                  uri,
                  position2 + 4 > enclosure.length() ? Strings.QUESTIONMARKS
                      : enclosure.substring(position2 + 3)));
            } else {
              try {
                builder.setMessage(getString(
                    R.string.question_playenclosure,
                    uri,
                    (Integer.parseInt(enclosure.substring(position2 + 3)) / 1024f)
                        + getString(R.string.kb)));
              } catch (Exception e) {
                builder.setMessage(getString(R.string.question_playenclosure,
                    uri, enclosure.substring(position2 + 3)));
              }
            }
            builder.setCancelable(true);
            builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                    mPlayClickedListener.onPlayClicked(uri.toString());
                  }
                });
            builder.setNeutralButton(R.string.button_alwaysokforall,
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                    preferences
                        .edit()
                        .putBoolean(Strings.SETTINGS_ENCLOSUREWARNINGSENABLED,
                            false).commit();
                    mPlayClickedListener.onPlayClicked(uri.toString());
                  }
                });
            builder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                  }
                });
            builder.show();
          } else {
            mPlayClickedListener.onPlayClicked(uri.toString());
          }
        }
      });
    } else {
      playButton.setEnabled(false);
    }
  }

  // draw the favorite button
  private void drawFavoriteButton(Cursor cursor) {
    final ImageView imageView = (ImageView) findViewById(android.R.id.icon);
    favorite = cursor.getInt(mFavoriteColumnIdx) == 1;
    imageView.setImageResource(favorite ? android.R.drawable.star_on
        : android.R.drawable.star_off);
    imageView.setOnClickListener(new OnClickListener() {
      public void onClick(View view) {
        favorite = !favorite;
        imageView.setImageResource(favorite ? android.R.drawable.star_on
            : android.R.drawable.star_off);
        ContentValues values = new ContentValues();
        values.put(FeedData.ItemColumns.FAVORITE, favorite ? 1 : 0);
        getContentResolver().update(uri, values, null, null);
      }
    });
  }

  private void drawIcon() {
    if (canShowIcon) {
      if (iconBytes != null && iconBytes.length > 0) {
        drawIconBytes();
      } else {
        Cursor iconCursor = getContentResolver().query(
            FeedData.SubscriptionColumns.subscriptionContentUri(Integer
                .toString(feedId)),
            new String[] { FeedData.SubscriptionColumns._ID,
                FeedData.SubscriptionColumns.ICON }, null, null, null);
        if (iconCursor.moveToFirst()) {
          iconBytes = iconCursor.getBlob(1);
          if (iconBytes != null && iconBytes.length > 0) {
            drawIconBytes();
          }
        }
        iconCursor.close();
      }
    }
  }

  private void drawIconBytes() {
    if (StaticMethods.POSTGINGERBREAD) {
      CompatibilityHelper.setActionBarDrawable(
          this,
          new BitmapDrawable(getResources(), BitmapFactory.decodeByteArray(
              iconBytes, 0, iconBytes.length)));
    } else {
      setFeatureDrawable(
          Window.FEATURE_LEFT_ICON,
          new BitmapDrawable(getResources(), BitmapFactory.decodeByteArray(
              iconBytes, 0, iconBytes.length)));
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.entry, menu);
    return true;
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    switch (item.getItemId()) {
    case R.id.menu_copytoclipboard: {
      if (link != null) {
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setText(link);
      }
      break;
    }
    case R.id.menu_delete: {
      getContentResolver().delete(uri, null, null);
      if (localPictures) {
        FeedData.deletePicturesOfEntry(_id);
      }
      finish();
      break;
    }
    case R.id.menu_share: {
      if (link != null) {
        startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_TEXT, link).setType(TEXTPLAIN),
            getString(R.string.contextmenu_share)));
      }
      break;
    }
    }
    return true;
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
      if (keyCode == 92 || keyCode == 94) {
        scrollUp();
        return true;
      } else if (keyCode == 93 || keyCode == 95) {
        scrollDown();
        return true;
      }
    }
    return super.onKeyDown(keyCode, event);
  }

  private void scrollUp() {
    if (webView != null) {
      webView.pageUp(false);
    }
  }

  private void scrollDown() {
    if (webView != null) {
      webView.pageDown(false);
    }
  }

  /**
   * Works around android issue 6191
   */
  @Override
  public void unregisterReceiver(BroadcastReceiver receiver) {
    try {
      super.unregisterReceiver(receiver);
    } catch (Exception e) {
      // do nothing
    }
  }
}
