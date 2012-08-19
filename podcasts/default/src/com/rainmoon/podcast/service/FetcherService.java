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

package com.rainmoon.podcast.service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import org.xml.sax.SAXException;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Xml;

import com.rainmoon.podcast.handler.RSSHandler;
import com.rainmoon.podcast.provider.FeedData;
import com.rainmoon.podcast.utils.BASE64;
import com.rainmoon.podcast.utils.StaticMethods;
import com.rainmoon.podcast.utils.Strings;

public final class FetcherService extends IntentService {
  private static final int FETCHMODE_UNKNOWN = 0;
  private static final int FETCHMODE_DIRECT = 1;
  private static final int FETCHMODE_REENCODE = 2;
  private static final String KEY_USERAGENT = "User-agent";
  private static final String VALUE_USERAGENT = "Mozilla/5.0";
  private static final String CHARSET = "charset=";
  private static final String CONTENT_TYPE_TEXT_HTML = "text/html";
  private static final String LINK_RSS = "<link rel=\"alternate\" ";
  private static final String LINK_RSS_SLOPPY = "<link rel=alternate ";
  private static final String HREF = "href=\"";
  private static final String HTML_BODY = "<body";
  private static final String ENCODING = "encoding=\"";
  private static final String SERVICENAME = "FetcherService";
  private static final String TAG = "FetcherService";

  private static SharedPreferences preferences = null;

  public FetcherService() {
    super(SERVICENAME);
    HttpURLConnection.setFollowRedirects(true);
    Log.i("FetcherService", "staring fetcher service");
  }

  @Override
  public void onHandleIntent(Intent intent) {
    Log.i(TAG, "handle intent " + intent.getStringExtra(Strings.FEEDID));
    if (preferences == null) {
      try {
        preferences = PreferenceManager.getDefaultSharedPreferences(createPackageContext(
            Strings.PACKAGE, 0));
      } catch (NameNotFoundException e) {
        preferences = PreferenceManager.getDefaultSharedPreferences(FetcherService.this);
      }
    }

    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
    if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED
        && intent != null) {
      refreshFeedsStatic(FetcherService.this, intent.getStringExtra(Strings.FEEDID), networkInfo,
          intent.getBooleanExtra(Strings.SETTINGS_OVERRIDEWIFIONLY, false));
    }
  }

  int refreshFeedsStatic(Context context, String feedId, NetworkInfo networkInfo,
      boolean overrideWifiOnly) {
    //TODO(trung): feed == null & not wifi
    // display message that only feeds with override-wifi only setting will be refreshed
    String selection = null;
    if (!overrideWifiOnly && networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
      // "IS NOT 1" does not work on 2.1
      selection = new StringBuilder(FeedData.SubscriptionColumns.WIFIONLY).append("=0 or ")
          .append(FeedData.SubscriptionColumns.WIFIONLY).append(" IS NULL").toString();
    }
    Cursor cursor = context.getContentResolver().query(
        feedId == null ? FeedData.SubscriptionColumns.CONTENT_URI
            : FeedData.SubscriptionColumns.subscriptionContentUri(feedId), null, selection, null,
        null); // no managed query here
    int urlColumn = cursor.getColumnIndex(FeedData.SubscriptionColumns.URL);
    int idColumn = cursor.getColumnIndex(FeedData.SubscriptionColumns._ID);
    int lastUpdateColumn = cursor.getColumnIndex(FeedData.SubscriptionColumns.REALLASTUPDATE);
    int titleColumn = cursor.getColumnIndex(FeedData.SubscriptionColumns.NAME);
    int fetchmodeColumn = cursor.getColumnIndex(FeedData.SubscriptionColumns.FETCHMODE);
    int iconColumn = cursor.getColumnIndex(FeedData.SubscriptionColumns.ICON);
    int result = 0;
    RSSHandler handler = new RSSHandler(context);
    handler.setFetchImages(preferences.getBoolean(Strings.SETTINGS_FETCHPICTURES, false));
    while (cursor.moveToNext()) {
      String id = cursor.getString(idColumn);
      HttpURLConnection connection = null;
      try {
        String feedUrlString = cursor.getString(urlColumn);
        URL feedUrl = new URL(feedUrlString);
        handler.init(new Date(cursor.getLong(lastUpdateColumn)), id, cursor.getString(titleColumn),
            feedUrlString);
        int fetchMode = cursor.getInt(fetchmodeColumn);
        if (fetchMode == FETCHMODE_UNKNOWN) {
          fetchMode = setFetchMode(feedUrlString, id);
        }
        setFeedIcon(cursor.getBlob(iconColumn), id,
            new StringBuilder(feedUrl.getProtocol()).append(Strings.PROTOCOL_SEPARATOR)
                .append(feedUrl.getHost()).append(Strings.FILE_FAVICON).toString());
        connection = setupConnection(feedUrl, 0);
        if (fetchMode == FETCHMODE_REENCODE) {
          fetchContentReEncode(handler, connection.getInputStream(), connection.getContentType());
        } else {
          fetchContentDirect(handler, connection.getInputStream(), connection.getContentType());
        }
      } catch (Exception e) {
        Log.e(TAG, "Fetch error", e);
        if (!handler.isDone() && !handler.isCancelled()) {
          ContentValues values = new ContentValues();
          // resets the fetchmode to determine it again later
          values.put(FeedData.SubscriptionColumns.FETCHMODE, 0);
          values.put(FeedData.SubscriptionColumns.ERROR, e.getMessage());
          context.getContentResolver().update(
              FeedData.SubscriptionColumns.subscriptionContentUri(id), values, null, null);
        }
      } finally {
        if (connection != null) {
          connection.disconnect();
        }
      }
      result += handler.getNewCount();
    }
    cursor.close();

    return result;
  }

  private void fetchContentReEncode(RSSHandler handler, InputStream inputStream, String contentType)
      throws IOException, SAXException, UnsupportedEncodingException {
    ByteArrayOutputStream ouputStream = new ByteArrayOutputStream();
    byte[] byteBuffer = new byte[4096];
    int n;
    while ((n = inputStream.read(byteBuffer)) > 0) {
      ouputStream.write(byteBuffer, 0, n);
    }
    String xmlText = ouputStream.toString();
    int start = xmlText != null ? xmlText.indexOf(ENCODING) : -1;
    if (start > -1) {
      Xml.parse(
          new StringReader(new String(ouputStream.toByteArray(), xmlText.substring(start + 10,
              xmlText.indexOf('"', start + 11)))), handler);
    } else {
      // use content type
      if (contentType != null) {
        int index = contentType.indexOf(CHARSET);
        if (index > -1) {
          int index2 = contentType.indexOf(';', index);
          try {
            StringReader reader = new StringReader(new String(ouputStream.toByteArray(),
                index2 > -1 ? contentType.substring(index + 8, index2)
                    : contentType.substring(index + 8)));
            handler.setReader(reader);
            Xml.parse(reader, handler);
          } catch (Exception e) {
            e.printStackTrace();
          }
        } else {
          StringReader reader = new StringReader(new String(ouputStream.toByteArray()));
          handler.setReader(reader);
          Xml.parse(reader, handler);
        }
      }
    }
  }

  private void fetchContentDirect(RSSHandler handler, InputStream inputStream, String contentType)
      throws IOException, SAXException, UnsupportedEncodingException {
    if (contentType != null) {
      int index = contentType.indexOf(CHARSET);
      int index2 = contentType.indexOf(';', index);
      handler.setInputStream(inputStream);
      Xml.parse(inputStream, Xml.findEncodingByName(index2 > -1 ? contentType.substring(index + 8,
          index2) : contentType.substring(index + 8)), handler);
    } else {
      InputStreamReader reader = new InputStreamReader(inputStream);
      handler.setReader(reader);
      Xml.parse(reader, handler);
    }
  }

  /**
   * Sets the fetch mode for a feed url (the first time we see a feed).
   * 
   * @param feedUrl
   * @param id
   * @return
   * @throws IOException
   */
  int setFetchMode(String feedUrl, String id) throws IOException {
    int fetchMode = 0;
    HttpURLConnection connection = setupConnection(new URL(feedUrl), 0);
    String contentType = connection.getContentType();
    if (contentType != null && contentType.startsWith(CONTENT_TYPE_TEXT_HTML)) {
      // this reader consumes the input stream
      BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line = null;
      int pos = -1, posStart = -1;
      while ((line = reader.readLine()) != null && line.indexOf(HTML_BODY) == -1) {
        pos = line.indexOf(LINK_RSS);
        if (pos < 0)
          pos = line.indexOf(LINK_RSS_SLOPPY);
        if (pos < 0)
          continue;
        posStart = line.indexOf(HREF, pos);
        if (posStart > -1) {
          String url = line.substring(posStart + 6, line.indexOf('"', posStart + 10)).replace(
              Strings.AMP_SG, Strings.AMP);
          if (url.startsWith(Strings.SLASH)) {
            int index = feedUrl.indexOf('/', 8);
            if (index > -1) {
              url = feedUrl.substring(0, index) + url;
            } else {
              url = feedUrl + url;
            }
          } else if (!url.startsWith(Strings.HTTP) && !url.startsWith(Strings.HTTPS)) {
            url = new StringBuilder(feedUrl).append('/').append(url).toString();
          }
          ContentValues values = new ContentValues();
          values.put(FeedData.SubscriptionColumns.URL, url);
          getContentResolver().update(FeedData.SubscriptionColumns.subscriptionContentUri(id),
              values, null, null);
          connection.disconnect();
          connection = setupConnection(new URL(url), 0);
          contentType = connection.getContentType();
          break;
        }
      }
      if (posStart == -1) { // this indicates a badly configured feed
        connection.disconnect();
        connection = setupConnection(new URL(feedUrl), 0);
        contentType = connection.getContentType();
      }
    }

    if (contentType != null) {
      int index = contentType.indexOf(CHARSET);
      if (index > -1) {
        int index2 = contentType.indexOf(';', index);
        try {
          Xml.findEncodingByName(index2 > -1 ? contentType.substring(index + 8, index2)
              : contentType.substring(index + 8));
          fetchMode = FETCHMODE_DIRECT;
        } catch (UnsupportedEncodingException usee) {
          fetchMode = FETCHMODE_REENCODE;
        }
      } else {
        fetchMode = FETCHMODE_REENCODE;
      }

    } else {
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
          connection.getInputStream()));
      char[] chars = new char[20];
      int length = bufferedReader.read(chars);
      String xmlDescription = new String(chars, 0, length);
      connection.disconnect();
      connection = setupConnection(connection.getURL(), 0);
      int start = xmlDescription != null ? xmlDescription.indexOf(ENCODING) : -1;
      if (start > -1) {
        try {
          Xml.findEncodingByName(xmlDescription.substring(start + 10,
              xmlDescription.indexOf('"', start + 11)));
          fetchMode = FETCHMODE_DIRECT;
        } catch (UnsupportedEncodingException usee) {
          fetchMode = FETCHMODE_REENCODE;
        }
      } else {
        // absolutely no encoding
        // information found
        fetchMode = FETCHMODE_DIRECT;
      }
    }

    ContentValues values = new ContentValues();
    values.put(FeedData.SubscriptionColumns.FETCHMODE, fetchMode);
    getContentResolver().update(FeedData.SubscriptionColumns.subscriptionContentUri(id), values,
        null, null);
    return fetchMode;
  }

  /**
   * Set feed icon if available.
   * 
   * @param iconBytes
   * @param feedId
   * @param url
   */
  void setFeedIcon(byte[] iconBytes, String feedId, String url) {
    if (iconBytes == null) {
      HttpURLConnection connection = null;
      try {
        connection = setupConnection(new URL(url), 0);
        iconBytes = StaticMethods.getBytes(connection.getInputStream());
        ContentValues values = new ContentValues();
        values.put(FeedData.SubscriptionColumns.ICON, iconBytes);
        getContentResolver().update(FeedData.SubscriptionColumns.subscriptionContentUri(feedId),
            values, null, null);
      } catch (Exception e) {
        ContentValues values = new ContentValues();
        values.put(FeedData.SubscriptionColumns.ICON, new byte[0]);
        getContentResolver().update(FeedData.SubscriptionColumns.subscriptionContentUri(feedId),
            values, null, null);
      } finally {
        if (connection != null)
          connection.disconnect();
      }
    }
  }

  HttpURLConnection setupConnection(URL url, int cycle) throws IOException {
    Log.i(TAG, "setting up connection to " + url + "\n cycle=" + cycle);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoInput(true);
    connection.setDoOutput(false);
    connection.setRequestProperty(KEY_USERAGENT, VALUE_USERAGENT);
    connection.setConnectTimeout(30000);
    connection.setReadTimeout(30000);
    connection.setUseCaches(false);
    if (url.getUserInfo() != null) {
      connection.setRequestProperty("Authorization",
          "Basic " + BASE64.encode(url.getUserInfo().getBytes()));
    }
    // Workaround for android issue 7786
    connection.setRequestProperty("connection", "close");
    connection.connect();
    String location = connection.getHeaderField("Location");
    if (location != null
        && (url.getProtocol().equals(Strings._HTTP) && location.startsWith(Strings.HTTPS) || url
            .getProtocol().equals(Strings._HTTPS) && location.startsWith(Strings.HTTP))) {
      // if location != null, the system-automatic redirect has failed which
      // indicates a protocol change
      connection.disconnect();
      if (cycle < 5) {
        return setupConnection(new URL(location), cycle + 1);
      } else {
        throw new IOException("Too many redirects");
      }
    }
    return connection;
  }
}
