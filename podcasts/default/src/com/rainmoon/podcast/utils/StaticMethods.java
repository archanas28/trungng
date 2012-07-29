package com.rainmoon.podcast.utils;

import android.content.ContentValues;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.widget.Toast;

import com.rainmoon.podcast.R;
import com.rainmoon.podcast.R.string;
import com.rainmoon.podcast.provider.FeedData;

/**
 * Provides helper static methods.
 * 
 * @author trung nguyen
 * 
 */
public class StaticMethods {
  public static final boolean POSTGINGERBREAD = !Build.VERSION.RELEASE
      .startsWith("1") && !Build.VERSION.RELEASE.startsWith("2");

  public static final ContentValues getReadContentValues() {
    ContentValues values = new ContentValues();

    values.put(FeedData.ItemColumns.READDATE, System.currentTimeMillis());
    return values;
  }

  public static final ContentValues getUnreadContentValues() {
    ContentValues values = new ContentValues();

    values.putNull(FeedData.ItemColumns.READDATE);
    return values;
  }

  public static final boolean checkConnection(Context context) {
    ConnectivityManager connMgr = (ConnectivityManager) context
        .getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    if (networkInfo == null || !networkInfo.isConnected()) {
      Toast.makeText(context, R.string.no_connection, Toast.LENGTH_SHORT)
          .show();
      return false;
    }
    return true;
  }
}
