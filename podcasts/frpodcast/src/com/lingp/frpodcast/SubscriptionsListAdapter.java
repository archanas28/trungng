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

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.TextView;

import com.lingp.frpodcast.provider.FeedData;
import com.lingp.frpodcast.utils.StaticMethods;

/**
 * Adapter for {@link AllSubscriptionsFragment}.
 * 
 * @author trung nguyen
 * 
 */
public class SubscriptionsListAdapter extends SimpleCursorAdapter {
  public static final String[] FROM = new String[] { FeedData.SubscriptionColumns._ID,
      FeedData.SubscriptionColumns.NAME, FeedData.SubscriptionColumns.LASTUPDATE,
      FeedData.SubscriptionColumns.URL, FeedData.SubscriptionColumns.ERROR,
      FeedData.SubscriptionColumns.ICON };
  private static final String COUNT_UNREAD = "COUNT(*) - COUNT(readdate)";

  private static final String COUNT = "COUNT(*)";
  private String COLON;
  private int nameColumnPosition;
  private int lastUpdateColumn;
  private int idPosition;
  private int linkPosition;
  private int errorPosition;
  private int iconPosition;

  public SubscriptionsListAdapter(Activity activity) {
    // important to use FROM here as it is used by the internal Android code
    // (even though the documentation says it can be null!)
    super(activity, R.layout.feedlistitem, null, FROM, null, 0);
  }

  /**
   * Init the column index to be used by this adapter. Should only be called
   * after the cursor is loaded.
   * 
   * @param activity
   */
  public void initColumnIdx(Activity activity) {
    Cursor cursor = getCursor();
    if (cursor != null) {
      nameColumnPosition = cursor.getColumnIndex(FeedData.SubscriptionColumns.NAME);
      lastUpdateColumn = cursor.getColumnIndex(FeedData.SubscriptionColumns.LASTUPDATE);
      idPosition = cursor.getColumnIndex(FeedData.SubscriptionColumns._ID);
      linkPosition = cursor.getColumnIndex(FeedData.SubscriptionColumns.URL);
      errorPosition = cursor.getColumnIndex(FeedData.SubscriptionColumns.ERROR);
      iconPosition = cursor.getColumnIndex(FeedData.SubscriptionColumns.ICON);
    }
    COLON = activity.getString(R.string.colon);
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    TextView textView = ((TextView) view.findViewById(android.R.id.text1));
    textView.setSingleLine();
    Cursor countCursor = context.getContentResolver().query(
        FeedData.ItemColumns.subscriptionItemsContentUri(cursor.getString(idPosition)),
        new String[] { COUNT_UNREAD, COUNT }, null, null, null);
    countCursor.moveToFirst();
    int unreadCount = countCursor.getInt(0);
    countCursor.close();
    long date = cursor.getLong(lastUpdateColumn);
    TextView updateTextView = ((TextView) view.findViewById(android.R.id.text2));
    String relativeTime = StaticMethods.getRelativeTimeSpan(context, date);
    updateTextView.setText(String.format("%s, %d %s",
        date == 0 ? context.getString(R.string.not_updated) : relativeTime, unreadCount,
        context.getString(R.string.unread)));
    if (unreadCount > 0) {
      textView.setTypeface(Typeface.DEFAULT_BOLD);
      updateTextView.setEnabled(true);
    } else {
      textView.setTypeface(Typeface.DEFAULT);
      updateTextView.setEnabled(false);
    }

    byte[] iconBytes = cursor.getBlob(iconPosition);
    if (iconBytes != null && iconBytes.length > 0) {
      Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
      if (bitmap != null) {
        if (bitmap.getHeight() > 16) {
          bitmap = Bitmap.createScaledBitmap(bitmap, 16, 16, false);
        }
        textView.setText(" "
            + (cursor.isNull(nameColumnPosition) ? cursor.getString(linkPosition) : cursor
                .getString(nameColumnPosition)));
      } else {
        textView.setText(cursor.isNull(nameColumnPosition) ? cursor.getString(linkPosition)
            : cursor.getString(nameColumnPosition));
      }
      textView.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(context.getResources(),
          bitmap), null, null, null);
    } else {
      view.setTag(null);
      textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
      textView.setText(cursor.isNull(nameColumnPosition) ? cursor.getString(linkPosition) : cursor
          .getString(nameColumnPosition));
    }
  }
}
