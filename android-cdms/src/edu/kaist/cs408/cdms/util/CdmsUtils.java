package edu.kaist.cs408.cdms.util;

import java.util.Date;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.format.DateFormat;
import edu.kaist.cs408.cdms.R;
import edu.kaist.cs408.cdms.ui.Constants;
import edu.kaist.cs408.cdms.ui.NotificationsActivity;

public class CdmsUtils {

  public static final int NOTIFICATION_FILE_DOWNLOAD_COMPLETED = 1;

  /**
   * Returns a pretty string representation for a date.
   * 
   * @param date
   * @return
   */
  public static String formatDate(Date date) {
    return DateFormat.format("MMM dd, 'at' h:mmaa", date).toString();
  }
  
  /**
   * Gets the id of the logged in user.
   * 
   * @param context
   * @return
   */
  public static long getUserId(Context context) {
    return context.getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, 0).getLong(
        Constants.SHARED_USER_ID, Constants.INVALID_ID);    
  }
  
  /**
   * Returns the response from the given {@code request}.
   * 
   * @param url
   * @return
   */
  public static String getResponse(String request) {
    String response = "";
    try {
      HttpClient httpclient = new DefaultHttpClient();
      HttpGet httpget = new HttpGet(request);
      ResponseHandler<String> responseHandler = new BasicResponseHandler();
      response = httpclient.execute(httpget, responseHandler);
      httpclient.getConnectionManager().shutdown();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return response;
  }

  /**
   * Notifies user with the given text message.
   */
  public static void notifyFileDownload(Context context, Uri uri) {
    NotificationManager mNotificationManager = (NotificationManager) context
        .getSystemService(Context.NOTIFICATION_SERVICE);
    Notification notification = new Notification(R.drawable.ic_launcher,
        "File download completed", System.currentTimeMillis());
    Intent intent = new Intent("android.intent.action.VIEW");
    intent.setData(uri);
    PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent,
        0);
    notification.setLatestEventInfo(context, "File download completed.", "",
        contentIntent);

    mNotificationManager.notify(NOTIFICATION_FILE_DOWNLOAD_COMPLETED,
        notification);
  }

  public static void notify(Context context, String msg) {
    NotificationManager mNotificationManager = (NotificationManager) context
        .getSystemService(Context.NOTIFICATION_SERVICE);
    Notification notification = new Notification(R.drawable.ic_launcher, msg,
        System.currentTimeMillis());
    Intent intent = new Intent(context, NotificationsActivity.class);
    PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);
    notification.setLatestEventInfo(context, msg, "", contentIntent);

    mNotificationManager.notify(NOTIFICATION_FILE_DOWNLOAD_COMPLETED,
        notification);
  }
}
