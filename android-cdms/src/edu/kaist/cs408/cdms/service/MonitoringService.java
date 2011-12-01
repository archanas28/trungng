/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.kaist.cs408.cdms.service;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.restlet.resource.ClientResource;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import edu.kaist.cs408.cdms.common.NotificationMsg;
import edu.kaist.cs408.cdms.common.NotificationsResource;
import edu.kaist.cs408.cdms.ui.Constants;
import edu.kaist.cs408.cdms.ui.NotificationHelper;
import edu.kaist.cs408.cdms.util.CdmsUtils;

/**
 * Background {@link Service} that checks for notifications from server.
 */
public class MonitoringService extends Service {

  private static final String TAG = "SyncService";
  private static final long UPDATE_INTERVAL = 30000;
  private Timer timer = new Timer();

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    Log.d(TAG, "Monitoring service is created");
    timer.scheduleAtFixedRate(new TimerTask() {
      public void run() {
        checkNotification();
      }
    }, 0, UPDATE_INTERVAL);
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "Monitoring service is destroyed");
    if (timer != null) {
      timer.cancel();
    }
  }

  /**
   * Checks for new notification and shows the notification.
   * 
   * TODO(trung): new notification should be set from the server to 1 after returning
   * so that don't get repeated notification.
   */
  private void checkNotification() {
    Log.i(TAG, "Checking notification...");
    long userId = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, 0).getLong(
        Constants.SHARED_USER_ID, Constants.INVALID_ID);
    ClientResource cr = new ClientResource(Constants.RESOURCE_USERS
        + userId + "/newnotifications");
    NotificationsResource resource = cr.wrap(NotificationsResource.class);
    ArrayList<NotificationMsg> list = resource.retrieve();
    for (NotificationMsg notification : list) {
      CdmsUtils.notify(MonitoringService.this, NotificationHelper
          .getNotificationContent(notification.getType()));
    }
  }
}
