package com.lingp.itpodcast.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.ContentValues;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.format.DateUtils;
import android.widget.Toast;

import com.lingp.itpodcast.R;
import com.lingp.itpodcast.provider.FeedData;

/**
 * Provides helper static methods.
 * 
 * @author trung nguyen
 * 
 */
public class StaticMethods {
  public static final boolean POSTGINGERBREAD = !Build.VERSION.RELEASE.startsWith("1")
      && !Build.VERSION.RELEASE.startsWith("2");

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
      Toast.makeText(context, R.string.no_connection, Toast.LENGTH_SHORT).show();
      return false;
    }
    return true;
  }

  /**
   * Reads an input stream and returns its content as bytes.
   * 
   * @param inputStream
   * @return
   * @throws IOException
   */
  public static byte[] getBytes(InputStream inputStream) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int n;
    while ((n = inputStream.read(buffer)) > 0) {
      output.write(buffer, 0, n);
    }
    byte[] result = output.toByteArray();
    output.close();
    inputStream.close();
    return result;
  }

  /**
   * Executes a request at {@code request} and returns the response from the
   * server.
   * 
   * @param request
   *          an encoded url
   * @return the response from the server; an empty string is returned if an
   *         error occurs
   */
  public static String getResponse(String request) throws IOException {
    String response = "";
    HttpClient httpclient = new DefaultHttpClient();
    HttpGet httpget = new HttpGet(request);
    ResponseHandler<String> responseHandler = new BasicResponseHandler();
    response = httpclient.execute(httpget, responseHandler);
    httpclient.getConnectionManager().shutdown();
    return response;
  }

  /**
   * Returns our custom format for relative time span.
   * 
   * @param context
   * @param time
   * 
   * @return
   */
  public static String getRelativeTimeSpan(Context context, long time) {
    String relativeTime = DateUtils.getRelativeDateTimeString(context, time,
        DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE | DateUtils.FORMAT_ABBREV_MONTH).toString();
    if (relativeTime.startsWith("0 mins ago"))
      return context.getString(R.string.just_now);
    int pos = relativeTime.indexOf(",");
    if (pos > 0)
      return relativeTime.substring(0, pos);
    else
      return relativeTime;
  }
}
