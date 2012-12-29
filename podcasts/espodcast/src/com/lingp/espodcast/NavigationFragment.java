package com.lingp.espodcast;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.lingp.espodcast.provider.FeedData;
import com.lingp.espodcast.utils.Strings;

/**
 * The navigation fragment contains navigation to main activities of the
 * application.
 * 
 * @author trung nguyen
 */
public class NavigationFragment extends ListFragment {

  private SharedPreferences mPrefs;
  private static final int MEDIUM_ACTIVITY = 50;
  private static final int HIGH_ACTIVITY = 100;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    return inflater.inflate(R.layout.fragment_navigation, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    // onCreateView() calls this method so the view and activity is ready
    super.onActivityCreated(savedInstanceState);

    List<String> navigation = new ArrayList<String>();
    final String all = getResources().getString(R.string.all_items);
    final String favorites = getResources().getString(R.string.favorites);
    final String recentlyViewed = getResources().getString(
        R.string.recently_viewed);
    final String subscriptions = getResources().getString(
        R.string.subscriptions);
    final String summary = getResources().getString(R.string.listen_summary);
    final String random = getResources().getString(R.string.random);

    navigation.add(subscriptions);
    navigation.add(all);
    navigation.add(favorites);
    navigation.add(recentlyViewed);
    navigation.add(random);
    navigation.add(summary);
    // have to setListAdapter() here
    setListAdapter(new ArrayAdapter<String>(getActivity(),
        R.layout.navigation_item, navigation));

    getListView().setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> list, View view, int arg2,
          long arg3) {
        TextView item = (TextView) view;
        Intent intent = null;
        String text = item.getText().toString();
        if (text.equals(subscriptions)) {
          intent = new Intent(getActivity(), AllSubscriptionsActivity.class);
        } else if (text.equals(summary)) {
          showSummaryDialog();
        } else if (text.equals(random)) {
          playRandomItem();
        } else {
          intent = new Intent(getActivity().getApplicationContext(),
              SingleSubscriptionActivity.class);
          if (text.equals(all)) {
            intent.setData(FeedData.ItemColumns.ALL_ITEMS_CONTENT_URI);
            intent.putExtra(Strings.NO_CONTENT_MSG,
                getString(R.string.no_content_all_items));
          } else if (text.equals(favorites)) {
            intent.setData(FeedData.ItemColumns.FAVORITES_CONTENT_URI);
            intent.putExtra(Strings.NO_CONTENT_MSG,
                getString(R.string.no_content_favorites));
          } else if (text.equals(recentlyViewed)) {
            intent.setData(FeedData.ItemColumns.RECENTLY_VIEWED_CONTENT_URI);
            intent.putExtra(Strings.NO_CONTENT_MSG,
                getString(R.string.no_content_recent));
          }
        }
        if (intent != null) {
          startActivity(intent);
        }
      }
    });
  }

  private void showSummaryDialog() {
    Context context = getActivity();
    mPrefs = context.getSharedPreferences(
        Strings.APPLICATION_SHARED_PREFERENCES, 0);
    Dialog dialog = new Dialog(context);
    dialog.setTitle(R.string.title_summary);
    dialog.setContentView(R.layout.dialog_summary);
    setWeekSummary(context,
        (TextView) dialog.findViewById(R.id.cnt_week_items),
        (TextView) dialog.findViewById(R.id.cnt_week_hours));
    setMonthSummary(context,
        (TextView) dialog.findViewById(R.id.cnt_month_items),
        (TextView) dialog.findViewById(R.id.cnt_month_hours));
    setTotalSummary(context,
        (TextView) dialog.findViewById(R.id.cnt_total_items),
        (TextView) dialog.findViewById(R.id.cnt_total_hours));
    setAverageSummary(context,
        (TextView) dialog.findViewById(R.id.cnt_average_items),
        (TextView) dialog.findViewById(R.id.cnt_average_hours));
    dialog.setCanceledOnTouchOutside(true);

    dialog.show();
  }

  private void playRandomItem() {
    Intent intent = new Intent(getActivity(), FeedItemActivity2.class);
    // get random play url
    long id = 1;
    String[] projection = new String[] { FeedData.ItemColumns._ID,
        FeedData.ItemColumns.ENCLOSURE };
    String selection = "enclosure != ?";
    String[] selectionArgs = { "" };
    Cursor cursor = getActivity().getContentResolver().query(
        FeedData.ItemColumns.ALL_ITEMS_CONTENT_URI, projection, selection,
        selectionArgs, null);
    if (cursor != null && cursor.getCount() > 0) {
      int randomPosition = (int) (cursor.getCount() * Math.random());
      if (cursor.moveToPosition(randomPosition)) {
        id = cursor.getInt(cursor.getColumnIndex(FeedData.ItemColumns._ID));
      }
      intent.setData(ContentUris.withAppendedId(
          FeedData.ItemColumns.ALL_ITEMS_CONTENT_URI, id));
      Log.i("NavigationFragment", "Playing " + intent.getDataString());
      startActivity(intent);
    } else {
      Toast.makeText(getActivity(), R.string.randomize_no_item,
          Toast.LENGTH_SHORT).show();
    }
  }

  /**
   * Average items and hours listened daily
   */
  private void setAverageSummary(Context context, TextView itemview,
      TextView hourview) {
    long currentTime = new Date().getTime();
    long startTime = mPrefs.getLong(Strings.LISTEN_START_TIME, currentTime);
    float elapsedDays = ((float) currentTime - startTime)
        / DateUtils.DAY_IN_MILLIS + 1;
    int totalItems = mPrefs.getInt(Strings.LISTEN_TOTAL_ITEMS, 0);
    float itemsPerDay = totalItems / elapsedDays;
    long totalMillis = mPrefs.getLong(Strings.LISTEN_TOTAL_MILLIS, 0);
    float totalHours = ((float) totalMillis) / DateUtils.HOUR_IN_MILLIS;
    float hoursPerDay = totalHours / elapsedDays;
    itemview.setText(String.format("%.1f ", itemsPerDay));
    hourview.setText(String.format("%.1f ", hoursPerDay));
    itemview.setTextColor(getColorView(context, itemsPerDay));
    hourview.setTextColor(getColorView(context, hoursPerDay * 2));
  }

  private void setMonthSummary(Context context, TextView itemview,
      TextView hourview) {
    int totalItems = mPrefs.getInt(Strings.LISTEN_MONTH_ITEMS, 0);
    long totalMillis = mPrefs.getLong(Strings.LISTEN_MONTH_MILLIS, 0);
    float totalHours = ((float) totalMillis) / DateUtils.HOUR_IN_MILLIS;
    itemview.setText(String.valueOf(totalItems) + " ");
    hourview.setText(String.format("%.1f ", totalHours));
    itemview.setTextColor(getColorView(context, totalItems));
    hourview.setTextColor(getColorView(context, totalHours * 2));
  }

  /**
   * Total items and hours listened so far.
   */
  private void setTotalSummary(Context context, TextView itemview,
      TextView hourview) {
    int totalItems = mPrefs.getInt(Strings.LISTEN_TOTAL_ITEMS, 0);
    long totalMillis = mPrefs.getLong(Strings.LISTEN_TOTAL_MILLIS, 0);
    float totalHours = ((float) totalMillis) / DateUtils.HOUR_IN_MILLIS;
    itemview.setText(String.valueOf(totalItems) + " ");
    hourview.setText(String.format("%.1f ", totalHours));
    itemview.setTextColor(getColorView(context, totalItems));
    hourview.setTextColor(getColorView(context, totalHours * 2));
  }

  private void setWeekSummary(Context context, TextView itemview,
      TextView hourview) {
    int totalItems = mPrefs.getInt(Strings.LISTEN_WEEK_ITEMS, 0);
    long totalMillis = mPrefs.getLong(Strings.LISTEN_WEEK_MILLIS, 0);
    float totalHours = ((float) totalMillis) / DateUtils.HOUR_IN_MILLIS;
    itemview.setText(String.valueOf(totalItems) + " ");
    hourview.setText(String.format("%.1f ", totalHours));
    itemview.setTextColor(getColorView(context, totalItems));
    hourview.setTextColor(getColorView(context, totalHours * 2));
  }

  private int getColorView(Context context, float value) {
    if (value >= HIGH_ACTIVITY) {
      return context.getResources().getColor(R.color.color_magenta);
    } else if (value >= MEDIUM_ACTIVITY) {
      return context.getResources().getColor(R.color.color_blue);
    } else {
      return context.getResources().getColor(R.color.color_black);
    }
  }
}
