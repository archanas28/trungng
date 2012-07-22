package com.rainmoon.podcast;

import com.rainmoon.podcast.provider.FeedData;

import android.content.ContentValues;
import android.os.Build;

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

}
