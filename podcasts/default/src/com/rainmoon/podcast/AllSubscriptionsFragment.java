package com.rainmoon.podcast;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.rainmoon.podcast.provider.FeedData;
import com.rainmoon.podcast.service.RefreshService;

/**
 * Fragment showing the list of all subscriptions.
 *  TODO(trung): currently update
 * of data is handled via broadcasting Intent...
 * 
 * @author trung nguyen
 * 
 */
public class AllSubscriptionsFragment extends ListFragment {

  private static final int CONTEXTMENU_EDIT_ID = 1;
  private static final int CONTEXTMENU_UNSUBSCRIBE = 2;

  private AllSubscriptionsListAdapter mListAdapter;
  private Context mContext;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = getActivity();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    return inflater.inflate(R.layout.main, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    ListView lv = getListView();
    lv.setOnCreateContextMenuListener(new MyContextMenuListener());
    TextView header = (TextView) getActivity().getLayoutInflater().inflate(
        R.layout.header, null);
    header.setText(R.string.subscriptions);
    getListView().addHeaderView(header);

    mListAdapter = new AllSubscriptionsListAdapter((Activity) mContext);
    setListAdapter(mListAdapter);
    if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(
        Strings.SETTINGS_REFRESHENABLED, false)) {
      // starts the service independent of this activity
      mContext.startService(new Intent(mContext, RefreshService.class));
      mContext.sendBroadcast(new Intent(Strings.ACTION_REFRESHFEEDS));
    } else {
      mContext.stopService(new Intent(mContext, RefreshService.class));
    }
  }

  final class MyContextMenuListener implements OnCreateContextMenuListener {

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
        ContextMenuInfo menuInfo) {
      menu.setHeaderTitle(((TextView) ((AdapterView.AdapterContextMenuInfo) menuInfo).targetView
          .findViewById(android.R.id.text1)).getText());
      menu.add(Menu.NONE, CONTEXTMENU_EDIT_ID, Menu.NONE,
          R.string.contextmenu_edit);
      menu.add(Menu.NONE, CONTEXTMENU_UNSUBSCRIBE, Menu.NONE,
          R.string.contextmenu_unsubscribe);
    }

  }

  @Override
  public boolean onContextItemSelected(final MenuItem item) {
    super.onContextItemSelected(item);

    switch (item.getItemId()) {
    case CONTEXTMENU_EDIT_ID: {
      startActivity(new Intent(Intent.ACTION_EDIT)
          .setData(FeedData.FeedColumns
              .CONTENT_URI(((AdapterView.AdapterContextMenuInfo) item
                  .getMenuInfo()).id)));
      break;
    }
      // case CONTEXTMENU_REFRESH_ID: {
      // final String id = Long
      // .toString(((AdapterView.AdapterContextMenuInfo)
      // item.getMenuInfo()).id);
      // refresh(id);
      // break;
      // }
    case CONTEXTMENU_UNSUBSCRIBE: {
      String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item
          .getMenuInfo()).id);
      unsubscribe(item, id);
      break;
    }
      // case CONTEXTMENU_MARKASREAD_ID: {
      // new Thread() {
      // public void run() {
      // String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item
      // .getMenuInfo()).id);
      //
      // if (getActivity().getContentResolver().update(
      // FeedData.EntryColumns.CONTENT_URI(id),
      // getReadContentValues(),
      // new StringBuilder(FeedData.EntryColumns.READDATE).append(
      // Strings.DB_ISNULL).toString(), null) > 0) {
      // getActivity().getContentResolver().notifyChange(
      // FeedData.FeedColumns.CONTENT_URI(id), null);
      // }
      // }
      // }.start();
      // break;
      // }
      // case CONTEXTMENU_MARKASUNREAD_ID: {
      // new Thread() {
      // public void run() {
      // String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item
      // .getMenuInfo()).id);
      //
      // if (getActivity().getContentResolver().update(
      // FeedData.EntryColumns.CONTENT_URI(id), getUnreadContentValues(),
      // null, null) > 0) {
      // getActivity().getContentResolver().notifyChange(
      // FeedData.FeedColumns.CONTENT_URI(id), null);
      // ;
      // }
      // }
      // }.start();
      // break;
      // }
      // case CONTEXTMENU_DELETEREAD_ID: {
      // new Thread() {
      // public void run() {
      // String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item
      // .getMenuInfo()).id);
      //
      // Uri uri = FeedData.EntryColumns.CONTENT_URI(id);
      //
      // String selection = Strings.READDATE_GREATERZERO + Strings.DB_AND
      // + " (" + Strings.DB_EXCUDEFAVORITE + ")";
      //
      // FeedData.deletePicturesOfFeed(mContext, uri, selection);
      // if (getActivity().getContentResolver().delete(uri, selection, null) >
      // 0) {
      // getActivity().getContentResolver().notifyChange(
      // FeedData.FeedColumns.CONTENT_URI(id), null);
      // }
      // }
      // }.start();
      // break;
      // }
      // case CONTEXTMENU_RESETUPDATEDATE_ID: {
      // ContentValues values = new ContentValues();
      //
      // values.put(FeedData.FeedColumns.LASTUPDATE, 0);
      // values.put(FeedData.FeedColumns.REALLASTUPDATE, 0);
      // getActivity().getContentResolver().update(
      // FeedData.FeedColumns.CONTENT_URI(Long
      // .toString(((AdapterView.AdapterContextMenuInfo) item
      // .getMenuInfo()).id)), values, null, null);
      // break;
      // }
    }
    return true;
  }

  /**
   * Handler for Delete item.
   * 
   * @param item
   * @param id
   */
  private void unsubscribe(final MenuItem item, String id) {
    Cursor cursor = getActivity().getContentResolver().query(
        FeedData.FeedColumns.CONTENT_URI(id),
        new String[] { FeedData.FeedColumns.NAME }, null, null, null);
    cursor.moveToFirst();

    Builder builder = new AlertDialog.Builder(mContext);

    builder.setIcon(android.R.drawable.ic_dialog_alert);
    builder.setTitle(cursor.getString(0));
    builder.setMessage(R.string.question_deletefeed);
    builder.setPositiveButton(android.R.string.yes,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            new Thread() {
              public void run() {
                getActivity().getContentResolver().delete(
                    FeedData.FeedColumns.CONTENT_URI(Long
                        .toString(((AdapterView.AdapterContextMenuInfo) item
                            .getMenuInfo()).id)), null, null);
                getActivity().sendBroadcast(
                    new Intent(Strings.ACTION_UPDATEWIDGET));
              }
            }.start();
          }
        });
    builder.setNegativeButton(android.R.string.no, null);
    cursor.close();
    builder.show();
  }

  /**
   * Refresh the content when menu item Refresh is selected.
   * 
   * @param id
   */
  @SuppressWarnings("unused")
  private void refresh(String id) {
    ConnectivityManager connectivityManager = (ConnectivityManager) mContext
        .getSystemService(Context.CONNECTIVITY_SERVICE);

    final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

    if (networkInfo != null
        && networkInfo.getState() == NetworkInfo.State.CONNECTED) {
      // since we have acquired the networkInfo, we use it for
      // basic checks
      final Intent intent = new Intent(Strings.ACTION_REFRESHFEEDS).putExtra(
          Strings.FEEDID, id);

      final Thread thread = new Thread() {
        public void run() {
          mContext.sendBroadcast(intent);
        }
      };

      if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI
          || PreferenceManager.getDefaultSharedPreferences(mContext)
              .getBoolean(Strings.SETTINGS_OVERRIDEWIFIONLY, false)) {
        intent.putExtra(Strings.SETTINGS_OVERRIDEWIFIONLY, true);
        thread.start();
      } else {
        Cursor cursor = mContext.getContentResolver().query(
            FeedData.FeedColumns.CONTENT_URI(id),
            new String[] { FeedData.FeedColumns.WIFIONLY }, null, null, null);

        cursor.moveToFirst();

        if (cursor.isNull(0) || cursor.getInt(0) == 0) {
          thread.start();
        } else {
          Builder builder = new AlertDialog.Builder(mContext);

          builder.setIcon(android.R.drawable.ic_dialog_alert);
          builder.setTitle(R.string.dialog_hint);
          builder.setMessage(R.string.question_refreshwowifi);
          builder.setPositiveButton(android.R.string.yes,
              new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                  intent.putExtra(Strings.SETTINGS_OVERRIDEWIFIONLY, true);
                  thread.start();
                }
              });
          builder.setNeutralButton(R.string.button_alwaysokforall,
              new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                  PreferenceManager.getDefaultSharedPreferences(mContext)
                      .edit()
                      .putBoolean(Strings.SETTINGS_OVERRIDEWIFIONLY, true)
                      .commit();
                  intent.putExtra(Strings.SETTINGS_OVERRIDEWIFIONLY, true);
                  thread.start();
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
  public void onListItemClick(ListView listView, View view, int position,
      long id) {
    super.onListItemClick(listView, view, position, id);
    Intent intent = new Intent(Intent.ACTION_VIEW,
        FeedData.EntryColumns.CONTENT_URI(Long.toString(id)));

    intent.putExtra(FeedData.FeedColumns._ID, id);
    startActivity(intent);
  }

}
