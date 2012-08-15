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

package com.rainmoon.podcast.provider;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.rainmoon.podcast.R;
import com.rainmoon.podcast.utils.Strings;

/**
 * Implementation of the internal database that stores the data and also
 * providing externel access via the interface of content provider.
 * 
 * @author trung nguyen
 * 
 */
public class FeedDataContentProvider extends ContentProvider {
  private static final String FOLDER = Environment
      .getExternalStorageDirectory() + "/podcast/";
  public static final String IMAGEFOLDER = Environment
      .getExternalStorageDirectory() + "/podcast/images/";
  public static final File IMAGEFOLDER_FILE = new File(IMAGEFOLDER);

  private static final String BACKUPOPML = Environment
      .getExternalStorageDirectory() + "/podcast/backup.opml";

  private static final String DATABASE_NAME = "podcast.db";
  private static final int DATABASE_VERSION = 1;
  // store Subscriptions
  protected static final String TABLE_SUBSCRIPTIONS = "feeds";
  // store Feed items
  private static final String TABLE_ITEMS = "entries";

  /** IDS for various URI queries */
  private static final int URI_SUBSCRIPTIONS = 1;
  private static final int URI_SUBSCRIPTION = 2;
  private static final int URI_SUBSCRIPTION_ITEMS = 3;
  private static final int URI_SUBSCRIPTION_ITEM = 4;
  private static final int URI_ALL_ITEMS = 5;
  private static final int URI_ITEM = 6;
  private static final int URI_FAVORITES = 7;
  private static final int URI_FAVORITE = 8;
  private static final int URI_RECENT_ITEMS = 9;
  private static final int URI_RECENT_ITEM = 10;

  private static final String EQUALS_ONE = "=1";

  private static UriMatcher uriMatcher;
  private static final String[] PROJECTION_PRIORITY = new String[] { FeedData.SubscriptionColumns.PRIORITY };

  static {
    uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    /** all subscriptions */
    uriMatcher.addURI(FeedData.AUTHORITY, "feeds", URI_SUBSCRIPTIONS);
    /** a single subscription identified by id */
    uriMatcher.addURI(FeedData.AUTHORITY, "feeds/#", URI_SUBSCRIPTION);
    /** all items of a subscription */
    uriMatcher.addURI(FeedData.AUTHORITY, "feeds/#/entries",
        URI_SUBSCRIPTION_ITEMS);
    /** an item of a subscription */
    uriMatcher.addURI(FeedData.AUTHORITY, "feeds/#/entries/#",
        URI_SUBSCRIPTION_ITEM);
    /** all items */
    // join table (not real physical table)
    uriMatcher.addURI(FeedData.AUTHORITY, "entries", URI_ALL_ITEMS);
    /** an item */
    uriMatcher.addURI(FeedData.AUTHORITY, "entries/#", URI_ITEM);
    /** all favorite items */
    uriMatcher.addURI(FeedData.AUTHORITY, "favorites", URI_FAVORITES);
    /** a favorite item */
    uriMatcher.addURI(FeedData.AUTHORITY, "favorites/#", URI_FAVORITE);
    /** all recently viewed items */
    uriMatcher.addURI(FeedData.AUTHORITY, "recent", URI_RECENT_ITEMS);
    uriMatcher.addURI(FeedData.AUTHORITY, "recent/#", URI_RECENT_ITEM);
  }

  private DatabaseHelper mDatabaseHelper;

  private String[] MAXPRIORITY = new String[] { "MAX("
      + FeedData.SubscriptionColumns.PRIORITY + ")" };

  @Override
  // TODO(trung): this should only be applicable to some of the uris
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    int option = uriMatcher.match(uri);
    String table = null;
    StringBuilder where = new StringBuilder();
    SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();

    switch (option) {
    case URI_SUBSCRIPTION: {
      table = TABLE_SUBSCRIPTIONS;
      final String feedId = uri.getPathSegments().get(1);
      new Thread() {
        public void run() {
          delete(FeedData.ItemColumns.subscriptionItemsContentUri(feedId),
              null, null);
        }
      }.start();
      where.append(FeedData.SubscriptionColumns._ID).append('=').append(feedId);

      /** Update the priorities */
      Cursor priorityCursor = database.query(TABLE_SUBSCRIPTIONS,
          PROJECTION_PRIORITY, FeedData.SubscriptionColumns._ID + "=" + feedId,
          null, null, null, null);
      if (priorityCursor.moveToNext()) {
        database.execSQL("UPDATE " + TABLE_SUBSCRIPTIONS + " SET "
            + FeedData.SubscriptionColumns.PRIORITY + " = "
            + FeedData.SubscriptionColumns.PRIORITY + "-1 WHERE "
            + FeedData.SubscriptionColumns.PRIORITY + " > "
            + priorityCursor.getInt(0));
        priorityCursor.close();
      } else {
        priorityCursor.close();
      }
      break;
    }
    case URI_SUBSCRIPTIONS: {
      table = TABLE_SUBSCRIPTIONS;
      break;
    }
    case URI_SUBSCRIPTION_ITEM: {
      table = TABLE_ITEMS;
      where.append(FeedData.ItemColumns._ID).append('=')
          .append(uri.getPathSegments().get(3));
      break;
    }
    case URI_SUBSCRIPTION_ITEMS: {
      table = TABLE_ITEMS;
      where.append(FeedData.ItemColumns.FEED_ID).append('=')
          .append(uri.getPathSegments().get(1));
      break;
    }
    case URI_ALL_ITEMS: {
      table = TABLE_ITEMS;
      break;
    }
    case URI_FAVORITE:
    case URI_ITEM:
    case URI_RECENT_ITEM: {
      table = TABLE_ITEMS;
      where.append(FeedData.ItemColumns._ID).append('=')
          .append(uri.getPathSegments().get(1));
      break;
    }
    case URI_FAVORITES: {
      table = TABLE_ITEMS;
      where.append(FeedData.ItemColumns.FAVORITE).append(EQUALS_ONE);
      break;
    }
    }

    if (!TextUtils.isEmpty(selection)) {
      if (where.length() > 0) {
        where.append(Strings.DB_AND);
      }
      where.append(selection);
    }

    int count = database.delete(table, where.toString(), selectionArgs);

    if (table == TABLE_SUBSCRIPTIONS) { // == is ok here
      OPML.exportToFile(BACKUPOPML, database);
    }
    if (count > 0) {
      getContext().getContentResolver().notifyChange(uri, null);
    }
    return count;
  }

  @Override
  public String getType(Uri uri) {
    int option = uriMatcher.match(uri);

    switch (option) {
    case URI_SUBSCRIPTIONS:
      return "vnd.android.cursor.dir/vnd.feeddata.feed";
    case URI_SUBSCRIPTION:
      return "vnd.android.cursor.item/vnd.feeddata.feed";
    case URI_FAVORITES:
    case URI_RECENT_ITEMS:
    case URI_ALL_ITEMS:
    case URI_SUBSCRIPTION_ITEMS:
      return "vnd.android.cursor.dir/vnd.feeddata.entry";
    case URI_FAVORITE:
    case URI_RECENT_ITEM:
    case URI_ITEM:
    case URI_SUBSCRIPTION_ITEM:
      return "vnd.android.cursor.item/vnd.feeddata.entry";
    default:
      throw new IllegalArgumentException("Unknown URI: " + uri);
    }
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    long newId = -1;
    int option = uriMatcher.match(uri);
    SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
    switch (option) {
    case URI_SUBSCRIPTIONS:
      Cursor cursor = database.query(TABLE_SUBSCRIPTIONS, MAXPRIORITY, null,
          null, null, null, null, null);

      if (cursor.moveToNext()) {
        values.put(FeedData.SubscriptionColumns.PRIORITY, cursor.getInt(0) + 1);
      } else {
        values.put(FeedData.SubscriptionColumns.PRIORITY, 1);
      }
      cursor.close();
      newId = database.insert(TABLE_SUBSCRIPTIONS, null, values);
      OPML.exportToFile(BACKUPOPML, database);
      break;
    case URI_SUBSCRIPTION_ITEMS:
      values.put(FeedData.ItemColumns.FEED_ID, uri.getPathSegments().get(1));
      newId = database.insert(TABLE_ITEMS, null, values);
      break;
    case URI_ALL_ITEMS:
      newId = database.insert(TABLE_ITEMS, null, values);
      break;
    default:
      break;
    }
    if (newId > -1) {
      getContext().getContentResolver().notifyChange(uri, null);
      return ContentUris.withAppendedId(uri, newId);
    } else {
      throw new SQLException("Could not insert row into " + uri);
    }
  }

  @Override
  public boolean onCreate() {
    try {
      File folder = new File(FOLDER);

      folder.mkdir(); // maybe we use the boolean return value later
    } catch (Exception e) {

    }
    mDatabaseHelper = new DatabaseHelper(getContext(), DATABASE_NAME,
        DATABASE_VERSION);
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
    int option = uriMatcher.match(uri);
    if ((option == URI_SUBSCRIPTION || option == URI_SUBSCRIPTIONS)
        && sortOrder == null) {
      sortOrder = FeedData.FEED_DEFAULTSORTORDER;
    }
    if (option == URI_RECENT_ITEMS) {
      sortOrder = FeedData.ItemColumns.READDATE + " DESC";
    }

    switch (option) {
    case URI_SUBSCRIPTION: {
      queryBuilder.setTables(TABLE_SUBSCRIPTIONS);
      queryBuilder.appendWhere(new StringBuilder(
          FeedData.SubscriptionColumns._ID).append('=').append(
          uri.getPathSegments().get(1)));
      break;
    }
    case URI_SUBSCRIPTIONS: {
      queryBuilder.setTables(TABLE_SUBSCRIPTIONS);
      break;
    }
    case URI_SUBSCRIPTION_ITEM: {
      queryBuilder.setTables(TABLE_ITEMS);
      queryBuilder.appendWhere(new StringBuilder(FeedData.ItemColumns._ID)
          .append('=').append(uri.getPathSegments().get(3)));
      break;
    }
    case URI_SUBSCRIPTION_ITEMS: {
      queryBuilder.setTables(TABLE_ITEMS);
      queryBuilder.appendWhere(new StringBuilder(FeedData.ItemColumns.FEED_ID)
          .append('=').append(uri.getPathSegments().get(1)));
      break;
    }
    case URI_ALL_ITEMS: {
      queryBuilder
          .setTables("entries join (select name, icon, _id as feed_id from feeds) as F on (entries.feedid = F.feed_id)");
      break;
    }
    case URI_RECENT_ITEMS: {
      queryBuilder
          .setTables("entries join (select name, icon, _id as feed_id from feeds) as F on (entries.feedid = F.feed_id)");
      queryBuilder.appendWhere(new StringBuilder(FeedData.ItemColumns.READDATE)
          .append(" NOT NULL"));
      break;
    }
    case URI_FAVORITE:
    case URI_ITEM:
    case URI_RECENT_ITEM: {
      queryBuilder.setTables(TABLE_ITEMS);
      queryBuilder.appendWhere(new StringBuilder(FeedData.ItemColumns._ID)
          .append('=').append(uri.getPathSegments().get(1)));
      break;
    }
    case URI_FAVORITES: {
      queryBuilder
          .setTables("entries join (select name, icon, _id as feed_id from feeds) as F on (entries.feedid = F.feed_id)");
      queryBuilder.appendWhere(new StringBuilder(FeedData.ItemColumns.FAVORITE)
          .append(EQUALS_ONE));
      break;
    }
    }

    SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
    Cursor cursor = queryBuilder.query(database, projection, selection,
        selectionArgs, null, null, sortOrder);
    cursor.setNotificationUri(getContext().getContentResolver(), uri);
    return cursor;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection,
      String[] selectionArgs) {
    int option = uriMatcher.match(uri);
    String table = null;
    StringBuilder where = new StringBuilder();
    SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();

    switch (option) {
    case URI_SUBSCRIPTION: {
      table = TABLE_SUBSCRIPTIONS;
      long feedId = Long.parseLong(uri.getPathSegments().get(1));
      where.append(FeedData.SubscriptionColumns._ID).append('=').append(feedId);
      if (values != null
          && values.containsKey(FeedData.SubscriptionColumns.PRIORITY)) {
        int newPriority = values
            .getAsInteger(FeedData.SubscriptionColumns.PRIORITY);
        Cursor priorityCursor = database.query(TABLE_SUBSCRIPTIONS,
            PROJECTION_PRIORITY, FeedData.SubscriptionColumns._ID + "="
                + feedId, null, null, null, null);
        if (priorityCursor.moveToNext()) {
          int oldPriority = priorityCursor.getInt(0);
          priorityCursor.close();
          if (newPriority > oldPriority) {
            database.execSQL("UPDATE " + TABLE_SUBSCRIPTIONS + " SET "
                + FeedData.SubscriptionColumns.PRIORITY + " = "
                + FeedData.SubscriptionColumns.PRIORITY + "-1 WHERE "
                + FeedData.SubscriptionColumns.PRIORITY + " BETWEEN "
                + (oldPriority + 1) + " AND " + newPriority);
          } else if (newPriority < oldPriority) {
            database.execSQL("UPDATE " + TABLE_SUBSCRIPTIONS + " SET "
                + FeedData.SubscriptionColumns.PRIORITY + " = "
                + FeedData.SubscriptionColumns.PRIORITY + "+1 WHERE "
                + FeedData.SubscriptionColumns.PRIORITY + " BETWEEN "
                + newPriority + " AND " + (oldPriority - 1));
          }
        } else {
          priorityCursor.close();
        }
      }
      break;
    }
    case URI_SUBSCRIPTIONS: {
      table = TABLE_SUBSCRIPTIONS;
      // maybe this should be disabled
      break;
    }
    case URI_SUBSCRIPTION_ITEM: {
      table = TABLE_ITEMS;
      where.append(FeedData.ItemColumns._ID).append('=')
          .append(uri.getPathSegments().get(3));
      break;
    }
    case URI_SUBSCRIPTION_ITEMS: {
      table = TABLE_ITEMS;
      where.append(FeedData.ItemColumns.FEED_ID).append('=')
          .append(uri.getPathSegments().get(1));
      break;
    }
    case URI_ALL_ITEMS: {
      table = TABLE_ITEMS;
      break;
    }
    case URI_FAVORITE:
    case URI_ITEM:
    case URI_RECENT_ITEM: {
      table = TABLE_ITEMS;
      where.append(FeedData.ItemColumns._ID).append('=')
          .append(uri.getPathSegments().get(1));
      break;
    }
    case URI_FAVORITES: {
      table = TABLE_ITEMS;
      where.append(FeedData.ItemColumns.FAVORITE).append(EQUALS_ONE);
      break;
    }
    }

    if (!TextUtils.isEmpty(selection)) {
      if (where.length() > 0) {
        where.append(Strings.DB_AND).append(selection);
      } else {
        where.append(selection);
      }
    }

    int count = 0;
    try {
      count = database.update(table, values, where.toString(), selectionArgs);
      if (table == TABLE_SUBSCRIPTIONS
          && (values.containsKey(FeedData.SubscriptionColumns.NAME)
              || values.containsKey(FeedData.SubscriptionColumns.URL) || values
              .containsKey(FeedData.SubscriptionColumns.PRIORITY))) {
        OPML.exportToFile(BACKUPOPML, database);
      }
      if (count > 0) {
        getContext().getContentResolver().notifyChange(uri, null);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return count;
  }

  /**
   * Database helper.
   * 
   * @author trung nguyen
   * 
   */
  private static class DatabaseHelper extends SQLiteOpenHelper {
    private Context mContext;

    public DatabaseHelper(Context context, String name, int version) {
      super(context, name, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
      database.execSQL(createTable(TABLE_SUBSCRIPTIONS,
          FeedData.SubscriptionColumns.COLUMNS,
          FeedData.SubscriptionColumns.TYPES));
      database.execSQL(createTable(TABLE_ITEMS, FeedData.ItemColumns.COLUMNS,
          FeedData.ItemColumns.TYPES));
      File backupFile = new File(BACKUPOPML);
      if (backupFile.exists()) {
        /** Perform an automated import of the backup */
        OPML.importFromFile(backupFile, database);
      }
      insertDefaultSubscriptions(database);
    }

    // insert default subscriptions from raw file
    private void insertDefaultSubscriptions(final SQLiteDatabase database) {
      new Thread(new Runnable() {
        public void run() {
          try {
            InputStream inputStream = mContext.getResources().openRawResource(
                R.raw.feeds);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                inputStream));
            String url, title;
            while ((url = in.readLine()) != null
                && (title = in.readLine()) != null) {
              ContentValues values = new ContentValues();
              values.put(FeedData.SubscriptionColumns.WIFIONLY, 1);
              values.put(FeedData.SubscriptionColumns.URL, url);
              values.put(FeedData.SubscriptionColumns.ERROR, (String) null);
              values.put(FeedData.SubscriptionColumns.NAME, title);
              database.insert(TABLE_SUBSCRIPTIONS, null, values);
            }
            in.close();
            // mContext.sendBroadcast(new Intent())
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }).start();
    }

    private String createTable(String tableName, String[] columns,
        String[] types) {
      if (tableName == null || columns == null || types == null
          || types.length != columns.length || types.length == 0) {
        throw new IllegalArgumentException(
            "Invalid parameters for creating table " + tableName);
      } else {
        StringBuilder stringBuilder = new StringBuilder("CREATE TABLE ");
        stringBuilder.append(tableName);
        stringBuilder.append(" (");
        for (int n = 0, i = columns.length; n < i; n++) {
          if (n > 0) {
            stringBuilder.append(", ");
          }
          stringBuilder.append(columns[n]).append(' ').append(types[n]);
        }
        return stringBuilder.append(");").toString();
      }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      // nothing to upgrade yet
    }
  }

}
