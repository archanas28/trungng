package com.lingp.jppodcast.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.lingp.jppodcast.R;

/**
 * 
 * This BroadcastReceiver intercepts the
 * android.net.ConnectivityManager.CONNECTIVITY_ACTION, which indicates a
 * connection change. It checks whether there is connection available.
 * If there is not connection, it displays a toast informing user.
 * 
 */
public class NetworkReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    ConnectivityManager connMgr = (ConnectivityManager) context
        .getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    if (networkInfo == null || !networkInfo.isConnected()) {
      Toast.makeText(context, R.string.no_connection, Toast.LENGTH_SHORT)
          .show();
    }
  }
}