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

package com.lingp.frpodcast;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.lingp.frpodcast.provider.FeedData;
import com.lingp.frpodcast.utils.StaticMethods;
import com.lingp.frpodcast.utils.Strings;

/**
 * Activity for showing list of feed items.
 * 
 * @author trung nguyen
 * 
 */
public class SingleSubscriptionActivity extends ListActivity {

  private static final int CONTEXTMENU_MARKASREAD_ID = 6;
  private static final int CONTEXTMENU_MARKASUNREAD_ID = 7;
  private static final int CONTEXTMENU_DELETE_ID = 8;
  private static final int CONTEXTMENU_COPYURL = 9;

  private static final String[] FEED_PROJECTION = { FeedData.SubscriptionColumns.NAME,
      FeedData.SubscriptionColumns.URL, FeedData.SubscriptionColumns.ICON };

  private Uri uri;
  private long mFeedId;
  private SingleSubscriptionAdapter entriesListAdapter;
  private byte[] iconBytes;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    String title = null;
    iconBytes = null;
    Intent intent = getIntent();
    mFeedId = intent.getLongExtra(FeedData.SubscriptionColumns._ID, 0);
    if (mFeedId > 0) {
      Cursor cursor = getContentResolver().query(
          FeedData.SubscriptionColumns.subscriptionContentUri(mFeedId), FEED_PROJECTION, null,
          null, null);
      if (cursor.moveToFirst()) {
        title = cursor.isNull(0) ? cursor.getString(1) : cursor.getString(0);
        iconBytes = cursor.getBlob(2);
      }
      cursor.close();
    }
    // we cannot insert the icon here because it would be overwritten,
    // but we have to reserve the icon here
    if (!StaticMethods.POSTGINGERBREAD && iconBytes != null && iconBytes.length > 0) {
      if (!requestWindowFeature(Window.FEATURE_LEFT_ICON)) {
        iconBytes = null;
      }
    }

    setContentView(R.layout.activity_items);
    TextView emptyView = (TextView) findViewById(android.R.id.empty);
    if (emptyView != null) {
      emptyView.setText(intent.getStringExtra(Strings.NO_CONTENT_MSG));
    }
    uri = intent.getData();
    entriesListAdapter = new SingleSubscriptionAdapter(this, uri);
    setListAdapter(entriesListAdapter);

    if (title != null) {
      setTitle(title);
    }
    if (iconBytes != null && iconBytes.length > 0) {
      if (StaticMethods.POSTGINGERBREAD) {
        CompatibilityHelper.setActionBarDrawable(this, new BitmapDrawable(getResources(),
            BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length)));
      } else {
        setFeatureDrawable(Window.FEATURE_LEFT_ICON, new BitmapDrawable(getResources(),
            BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length)));
      }
    }

    getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
      public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        menu.setHeaderTitle(((TextView) ((AdapterView.AdapterContextMenuInfo) menuInfo).targetView
            .findViewById(android.R.id.text1)).getText());
        menu.add(0, CONTEXTMENU_MARKASREAD_ID, Menu.NONE, R.string.contextmenu_markasread).setIcon(
            android.R.drawable.ic_menu_manage);
        menu.add(0, CONTEXTMENU_MARKASUNREAD_ID, Menu.NONE, R.string.contextmenu_markasunread)
            .setIcon(android.R.drawable.ic_menu_manage);
        menu.add(0, CONTEXTMENU_DELETE_ID, Menu.NONE, R.string.contextmenu_delete).setIcon(
            android.R.drawable.ic_menu_delete);
        menu.add(0, CONTEXTMENU_COPYURL, Menu.NONE, R.string.contextmenu_copyurl).setIcon(
            android.R.drawable.ic_menu_share);
      }
    });
  }

  @Override
  protected void onListItemClick(ListView listView, View view, int position, long id) {
    TextView textView = (TextView) view.findViewById(android.R.id.text1);

    textView.setTypeface(Typeface.DEFAULT);
    textView.setEnabled(false);
    view.findViewById(android.R.id.text2).setEnabled(false);
    entriesListAdapter.neutralizeReadState();
    Intent intent = new Intent(getApplicationContext(), FeedItemActivity2.class);
    intent.setData(ContentUris.withAppendedId(uri, id))
        .putExtra(Strings.EXTRA_SHOWREAD, entriesListAdapter.isShowRead())
        .putExtra(FeedData.SubscriptionColumns.ICON, iconBytes);
    startActivity(intent);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.entrylist, menu);
    return !uri.equals(FeedData.ItemColumns.RECENTLY_VIEWED_CONTENT_URI)
        && !uri.equals(FeedData.ItemColumns.FAVORITES_CONTENT_URI);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.setGroupVisible(R.id.menu_group_0, entriesListAdapter.getCount() > 0);
    menu.setGroupVisible(R.id.menu_group_1, entriesListAdapter.getCount() > 0);
    menu.setGroupVisible(R.id.menu_group_refresh, mFeedId > 0);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.option_refresh1:
      if (mFeedId > 0 && StaticMethods.checkConnection(this)) {
        refresh(String.valueOf(mFeedId));
      }
      break;
    case R.id.option_markasread:
      new Thread() { // the update process takes some time
        public void run() {
          getContentResolver().update(uri, StaticMethods.getReadContentValues(), null, null);
        }
      }.start();
      entriesListAdapter.markAsRead();
      break;
    case R.id.option_markasunread: {
      new Thread() { // the update process takes some time
        public void run() {
          getContentResolver().update(uri, StaticMethods.getUnreadContentValues(), null, null);
        }
      }.start();
      entriesListAdapter.markAsUnread();
      break;
    }
    case R.id.option_hideread: {
      if (item.isChecked()) {
        item.setChecked(false).setTitle(R.string.option_hideread)
            .setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        entriesListAdapter.showRead(true);
      } else {
        item.setChecked(true).setTitle(R.string.option_showread)
            .setIcon(android.R.drawable.ic_menu_view);
        entriesListAdapter.showRead(false);
      }
      break;
    }
    case R.id.option_deleteread: {
      new Thread() { // the delete process takes some time
        public void run() {
          String selection = Strings.READDATE_GREATERZERO + Strings.DB_AND + " ("
              + Strings.DB_EXCUDEFAVORITE + ")";

          getContentResolver().delete(uri, selection, null);
          FeedData.deletePicturesOfFeed(SingleSubscriptionActivity.this, uri, selection);
          runOnUiThread(new Runnable() {
            public void run() {
              entriesListAdapter.getCursor().requery();
            }
          });
        }
      }.start();
      break;
    }
    case R.id.option_deleteallentries: {
      Builder builder = new AlertDialog.Builder(this);

      builder.setIcon(android.R.drawable.ic_dialog_alert);
      builder.setTitle(R.string.option_deleteallentries);
      builder.setMessage(R.string.question_areyousure);
      builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
          new Thread() {
            public void run() {
              getContentResolver().delete(uri, Strings.DB_EXCUDEFAVORITE, null);
              runOnUiThread(new Runnable() {
                public void run() {
                  entriesListAdapter.getCursor().requery();
                }
              });
            }
          }.start();
        }
      });
      builder.setNegativeButton(android.R.string.no, null);
      builder.show();
      break;
    }
    default:
      break;
    }

    return true;
  }

  /**
   * Refresh the content when menu item Refresh is selected.
   * 
   * @param id
   */
  private void refresh(String id) {
    Log.i("SingleSubscriptionActivity", "refresh: " + id);
    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
    if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED) {
      final Intent intent = new Intent(Strings.ACTION_REFRESHFEEDS).putExtra(Strings.FEEDID, id);
      final Thread thread = new Thread() {
        public void run() {
          Log.i("SingleSubscriptionAct", "sendig intent " + intent.getStringExtra(Strings.FEEDID));
          sendBroadcast(intent);
        }
      };

      if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI
          || PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
              Strings.SETTINGS_OVERRIDEWIFIONLY, false)) {
        intent.putExtra(Strings.SETTINGS_OVERRIDEWIFIONLY, true);
        thread.start();
        Toast.makeText(SingleSubscriptionActivity.this, R.string.refreshing, Toast.LENGTH_SHORT)
            .show();
      } else {
        Cursor cursor = getContentResolver().query(
            FeedData.SubscriptionColumns.subscriptionContentUri(id),
            new String[] { FeedData.SubscriptionColumns.WIFIONLY }, null, null, null);
        cursor.moveToFirst();
        if (cursor.isNull(0) || cursor.getInt(0) == 0) {
          thread.start();
          Toast.makeText(SingleSubscriptionActivity.this, R.string.refreshing, Toast.LENGTH_SHORT)
              .show();

        } else {
          Builder builder = new AlertDialog.Builder(this);
          builder.setIcon(android.R.drawable.ic_dialog_alert);
          builder.setTitle(R.string.dialog_hint);
          builder.setMessage(R.string.question_refreshwowifi);
          builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              intent.putExtra(Strings.SETTINGS_OVERRIDEWIFIONLY, true);
              thread.start();
              Toast.makeText(SingleSubscriptionActivity.this, R.string.refreshing,
                  Toast.LENGTH_SHORT).show();

            }
          });
          builder.setNeutralButton(R.string.button_alwaysokforall,
              new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                  PreferenceManager.getDefaultSharedPreferences(SingleSubscriptionActivity.this)
                      .edit().putBoolean(Strings.SETTINGS_OVERRIDEWIFIONLY, true).commit();
                  intent.putExtra(Strings.SETTINGS_OVERRIDEWIFIONLY, true);
                  thread.start();
                  Toast.makeText(SingleSubscriptionActivity.this, R.string.refreshing,
                      Toast.LENGTH_SHORT).show();
                }
              });
          builder.setNegativeButton(android.R.string.no, null);
          builder.show();
        }
        cursor.close();
      }

    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    long id = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id;
    switch (item.getItemId()) {
    case CONTEXTMENU_MARKASREAD_ID: {
      getContentResolver().update(ContentUris.withAppendedId(uri, id),
          StaticMethods.getReadContentValues(), null, null);
      entriesListAdapter.markAsRead(id);
      break;
    }
    case CONTEXTMENU_MARKASUNREAD_ID: {
      getContentResolver().update(ContentUris.withAppendedId(uri, id),
          StaticMethods.getUnreadContentValues(), null, null);
      entriesListAdapter.markAsUnread(id);
      break;
    }
    case CONTEXTMENU_DELETE_ID: {
      getContentResolver().delete(ContentUris.withAppendedId(uri, id), null, null);
      FeedData.deletePicturesOfEntry(Long.toString(id));
      entriesListAdapter.getCursor().requery(); // he have no other choice
      break;
    }
    case CONTEXTMENU_COPYURL: {
      ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
          .setText(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).targetView.getTag()
              .toString());
      break;
    }
    default:
      break;
    }
    return true;
  }

}
