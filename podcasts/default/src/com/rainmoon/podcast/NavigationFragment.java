package com.rainmoon.podcast;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.rainmoon.podcast.provider.FeedData;

/**
 * The navigation fragment contains navigation to main activities of the
 * application.
 * 
 * @author trung nguyen
 * 
 */
public class NavigationFragment extends ListFragment {

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    // we don't want the default onCreateView that returns only a ListView
    // as we want to supply the empty text
    return inflater.inflate(R.layout.fragment_navigation, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    // onCreateView() calls this method so the view and activity is ready
    super.onActivityCreated(savedInstanceState);
    TextView header = (TextView) getActivity().getLayoutInflater().inflate(R.layout.header, null);
    header.setText(R.string.home);
    getListView().addHeaderView(header);

    List<String> navigation = new ArrayList<String>();
    final String all = getResources().getString(R.string.all_items);
    final String favorites = getResources().getString(R.string.favorites);
    final String recentlyViewed = getResources().getString(R.string.recently_viewed);
    final String subscriptions = getResources().getString(R.string.subscriptions);
    // TODO: add explore for 2nd release
    navigation.add(all);
    navigation.add(favorites);
    navigation.add(recentlyViewed);
    navigation.add(subscriptions);
    // have to setListAdapter() here
    setListAdapter(new ArrayAdapter<String>(getActivity(), R.layout.navigation_item, navigation));

    getListView().setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> list, View view, int arg2, long arg3) {
        try {
          TextView item = (TextView) view;
          Intent intent = null;
          if (item.getText().equals(subscriptions)) {
            intent = new Intent(getActivity(), AllSubscriptionsActivity.class);
          } else {
            if (item.getText().equals(all)) {
              // this intent starts the ItemsListActivity with all items
              intent = new Intent(Intent.ACTION_VIEW, FeedData.ItemColumns.ALL_ITEMS_CONTENT_URI);
            } else if (item.getText().equals(favorites)) {
              // this intent starts the ItemsListAcitivty with favorite items
              intent = new Intent(Intent.ACTION_VIEW, FeedData.ItemColumns.FAVORITES_CONTENT_URI);
            } else if (item.getText().equals(recentlyViewed)) {
              intent = new Intent(Intent.ACTION_VIEW,
                  FeedData.ItemColumns.RECENTLY_VIEWED_CONTENT_URI);
            }
            if (intent != null) {
              intent.putExtra(SingleSubscriptionActivity.EXTRA_SHOWFEEDINFO, true);
              intent.putExtra(SingleSubscriptionActivity.EXTRA_AUTORELOAD, true);
            }  
          }
          if (intent != null) {
            startActivity(intent);
          }
        } catch (ClassCastException e) {
          Log.w("NavigationFragment", "List item is not text view");
        }
      }
    });
  }
}
