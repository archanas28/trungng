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

package com.rainmoon.podcast;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

import com.rainmoon.podcast.provider.FeedData;
import com.rainmoon.podcast.service.FetcherService;

@SuppressWarnings("deprecation")
public class MainTabActivity extends TabActivity {
  private static final int DIALOG_LICENSEAGREEMENT = 0;

  private boolean tabsAdded;

  private static final String TAG_NORMAL = "normal";

  private static final String TAG_ALL = "all";

  private static final String TAG_FAVORITE = "favorite";

  public static MainTabActivity INSTANCE;
  private Menu menu;
  private boolean hasContent = false;
  
  public static final boolean POSTGINGERBREAD = !Build.VERSION.RELEASE
      .startsWith("1") && !Build.VERSION.RELEASE.startsWith("2");

  private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      setProgressBarIndeterminateVisibility(true);
    }
  };

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.tabs);
    INSTANCE = this;
    setContent();
  }

  @Override
  protected void onResume() {
    super.onResume();
    setProgressBarIndeterminateVisibility(isCurrentlyRefreshing());
    registerReceiver(refreshReceiver, new IntentFilter(
        "com.rainmoon.podcast.REFRESH"));
  }

  @Override
  protected void onPause() {
    unregisterReceiver(refreshReceiver);
    super.onPause();
  }

  private boolean isCurrentlyRefreshing() {
    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
    for (RunningServiceInfo service : manager
        .getRunningServices(Integer.MAX_VALUE)) {
      if (FetcherService.class.getName().equals(service.service.getClassName())) {
        return true;
      }
    }
    return false;
  }

  private void setContent() {
    TabHost tabHost = getTabHost();
    hasContent = true;
    tabHost.addTab(tabHost.newTabSpec(TAG_NORMAL)
        .setIndicator(getString(R.string.overview))
        .setContent(new Intent(this, RssOverviewActivity.class)));
    if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
        Strings.SETTINGS_SHOWTABS, false)) {
      tabHost.addTab(tabHost
          .newTabSpec(TAG_ALL)
          .setIndicator(getString(R.string.all))
          .setContent(
              new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.CONTENT_URI)
                  .putExtra(EntriesListActivity.EXTRA_SHOWFEEDINFO, true)));

      tabHost.addTab(tabHost
          .newTabSpec(TAG_FAVORITE)
          .setIndicator(getString(R.string.favorites),
              getResources().getDrawable(android.R.drawable.star_big_on))
          .setContent(
              new Intent(Intent.ACTION_VIEW,
                  FeedData.EntryColumns.FAVORITES_CONTENT_URI).putExtra(
                  EntriesListActivity.EXTRA_SHOWFEEDINFO, true).putExtra(
                  EntriesListActivity.EXTRA_AUTORELOAD, true)));
      tabsAdded = true;
    }
    getTabWidget().setVisibility(View.VISIBLE);
    if (POSTGINGERBREAD) {
      /* Change the menu also on ICS when tab is changed */
      tabHost.setOnTabChangedListener(new OnTabChangeListener() {
        public void onTabChanged(String tabId) {
          if (menu != null) {
            menu.clear();
            onCreateOptionsMenu(menu);
          }
        }
      });
      if (menu != null) {
        menu.clear();
        onCreateOptionsMenu(menu);
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    this.menu = menu;
    // to get the activity associated with selected tab and its menu
    Activity activity = getCurrentActivity();

    if (hasContent && activity != null) {
      return activity.onCreateOptionsMenu(menu);
    } else {
      menu.add(Strings.EMPTY); // to let the menu be available
      return true;
    }
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    Activity activity = getCurrentActivity();

    if (hasContent && activity != null) {
      return activity.onMenuItemSelected(featureId, item);
    } else {
      return super.onMenuItemSelected(featureId, item);
    }
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    Activity activity = getCurrentActivity();

    if (hasContent && activity != null) {
      return activity.onPrepareOptionsMenu(menu);
    } else {
      return super.onPrepareOptionsMenu(menu);
    }
  }

  public void setTabWidgetVisible(boolean visible) {
    if (visible) {
      if (!tabsAdded) {
        TabHost tabHost = getTabHost();

        tabHost.addTab(tabHost
            .newTabSpec(TAG_ALL)
            .setIndicator(getString(R.string.all))
            .setContent(
                new Intent(Intent.ACTION_VIEW,
                    FeedData.EntryColumns.CONTENT_URI).putExtra(
                    EntriesListActivity.EXTRA_SHOWFEEDINFO, true)));
        tabHost.addTab(tabHost
            .newTabSpec(TAG_FAVORITE)
            .setIndicator(getString(R.string.favorites),
                getResources().getDrawable(android.R.drawable.star_big_on))
            .setContent(
                new Intent(Intent.ACTION_VIEW,
                    FeedData.EntryColumns.FAVORITES_CONTENT_URI).putExtra(
                    EntriesListActivity.EXTRA_SHOWFEEDINFO, true)));
        tabsAdded = true;
      }
      getTabWidget().setVisibility(View.VISIBLE);
    } else {
      getTabWidget().setVisibility(View.GONE);
    }

  }
}
