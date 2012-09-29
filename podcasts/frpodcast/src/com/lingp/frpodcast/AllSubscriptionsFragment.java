package com.lingp.frpodcast;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
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

import com.lingp.frpodcast.provider.FeedData;
import com.lingp.frpodcast.service.RefreshService;
import com.lingp.frpodcast.utils.Strings;

/**
 * Fragment showing the list of all subscriptions.
 * 
 * @author trung nguyen
 * 
 */
public class AllSubscriptionsFragment extends ListFragment implements LoaderCallbacks<Cursor> {

  private static final int CONTEXTMENU_EDIT_ID = 1;
  private static final int CONTEXTMENU_UNSUBSCRIBE = 2;
  private static final String TAG = "AllSubscriptionsFragment";

  private CursorAdapter mAdapter;
  private Context mContext;
  private BroadcastReceiver mDatabaseReadyReceiver;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = getActivity();
    boolean databaseReady = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(
        Strings.PREFERENCE_DATABASE_READY, false);
    // listen for the database ready event and refresh (this is needed for first
    // use only)
    if (!databaseReady) {
      mDatabaseReadyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          Log.d(TAG, "Database ready intent received");
          getLoaderManager().restartLoader(0, null, AllSubscriptionsFragment.this);
        }
      };
      LocalBroadcastManager.getInstance(mContext).registerReceiver(mDatabaseReadyReceiver,
          new IntentFilter(Strings.INTENT_DATABASE_READY));
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    return inflater.inflate(R.layout.view_list, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    ListView lv = getListView();
    lv.setOnCreateContextMenuListener(new MyContextMenuListener());
    TextView header = (TextView) getActivity().getLayoutInflater().inflate(R.layout.header, null);
    header.setText(R.string.subscriptions);
    getListView().addHeaderView(header);

    // Create an empty adapter we will use to display the loaded data.
    mAdapter = new SubscriptionsListAdapter((Activity) mContext);
    setListAdapter(mAdapter);

    // Prepare the loader. Either re-connect with an existing one,
    // or start a new one. The third argument is an implementation of the
    // LoaderCallback.
    getLoaderManager().initLoader(0, null, this);
    if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(
        Strings.SETTINGS_REFRESHENABLED, false)) {
      // starts the service independent of this activity
      mContext.startService(new Intent(mContext, RefreshService.class));
      mContext.sendBroadcast(new Intent(Strings.ACTION_REFRESHFEEDS));
    } else {
      mContext.stopService(new Intent(mContext, RefreshService.class));
    }
  }

  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    // Now create and return a CursorLoader that will take care of
    // creating a Cursor for the data being displayed.
    return new CursorLoader(getActivity(), FeedData.SubscriptionColumns.CONTENT_URI,
        SubscriptionsListAdapter.FROM, null, null, null);
  }

  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    // Swap the new cursor in. This is like calling the adapter's constructor
    // using the new cursor.
    mAdapter.swapCursor(data);
    ((SubscriptionsListAdapter) mAdapter).initColumnIdx(getActivity());
    Log.i(TAG, "cursor loaded");
  }

  public void onLoaderReset(Loader<Cursor> loader) {
    // This is called when the last Cursor provided to onLoadFinished()
    // above is about to be closed. We need to make sure we are no longer using
    // it.
    mAdapter.swapCursor(null);
  }

  final class MyContextMenuListener implements OnCreateContextMenuListener {

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
      menu.setHeaderTitle(((TextView) ((AdapterView.AdapterContextMenuInfo) menuInfo).targetView
          .findViewById(android.R.id.text1)).getText());
      menu.add(Menu.NONE, CONTEXTMENU_EDIT_ID, Menu.NONE, R.string.contextmenu_edit);
      menu.add(Menu.NONE, CONTEXTMENU_UNSUBSCRIBE, Menu.NONE, R.string.contextmenu_unsubscribe);
    }

  }

  @Override
  public boolean onContextItemSelected(final MenuItem item) {
    super.onContextItemSelected(item);

    switch (item.getItemId()) {
    case CONTEXTMENU_EDIT_ID: {
      startActivity(new Intent(Intent.ACTION_EDIT).setData(FeedData.SubscriptionColumns
          .subscriptionContentUri(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id)));
      break;
    }
    case CONTEXTMENU_UNSUBSCRIBE: {
      String id = Long.toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id);
      unsubscribe(item, id);
      break;
    }
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
        FeedData.SubscriptionColumns.subscriptionContentUri(id),
        new String[] { FeedData.SubscriptionColumns.NAME }, null, null, null);
    cursor.moveToFirst();

    Builder builder = new AlertDialog.Builder(mContext);

    builder.setIcon(android.R.drawable.ic_dialog_alert);
    builder.setTitle(cursor.getString(0));
    builder.setMessage(R.string.question_deletefeed);
    builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        new Thread() {
          public void run() {
            getActivity().getContentResolver().delete(
                FeedData.SubscriptionColumns.subscriptionContentUri(Long
                    .toString(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id)), null,
                null);
          }
        }.start();
      }
    });
    builder.setNegativeButton(android.R.string.no, null);
    cursor.close();
    builder.show();
  }

  @Override
  public void onListItemClick(ListView listView, View view, int position, long id) {
    super.onListItemClick(listView, view, position, id);
    Intent intent = new Intent(getActivity().getApplicationContext(),
        SingleSubscriptionActivity.class);
    intent.setData(FeedData.ItemColumns.subscriptionItemsContentUri(Long.toString(id))).putExtra(
        FeedData.SubscriptionColumns._ID, id);
    startActivity(intent);
  }

  @Override
  public void onDestroy() {
    if (mDatabaseReadyReceiver != null)
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDatabaseReadyReceiver);
    super.onDestroy();
  }
}
