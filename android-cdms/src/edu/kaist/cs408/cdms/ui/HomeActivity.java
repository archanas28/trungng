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

package edu.kaist.cs408.cdms.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import edu.kaist.cs408.cdms.R;
import edu.kaist.cs408.cdms.service.MonitoringService;
import edu.kaist.cs408.cdms.util.CdmsUtils;
import edu.kaist.cs408.cdms.util.UIUtils;

/**
 * Front-door {@link Activity} that displays high-level features the application
 *  offers to users.
 * 
 * TODO(trung): pre-fetch data for CommonResources.
 */
public class HomeActivity extends Activity {

  @SuppressWarnings("unused")
  private static final String TAG = "HomeActivity";

  /** State held between configuration changes. */
  private long mUserId;

  // private Handler mMessageHandler = new Handler();
  // private NotifyingAsyncQueryHandler mQueryHandler;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mUserId = CdmsUtils.getUserId(HomeActivity.this);
    if (mUserId == -1) {
      startActivity(new Intent(HomeActivity.this, LoginActivity.class));
    } else {
      setContentView(R.layout.activity_home);
    }
    
    // start service for notification checking
    startService(new Intent(HomeActivity.this, MonitoringService.class));
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_home, menu);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    if (item.getItemId() == R.id.menu_logout) {
      Editor editor = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, 0)
          .edit();
      editor.putLong(Constants.SHARED_USER_ID, -1L);
      editor.commit();
      stopService(new Intent(HomeActivity.this, MonitoringService.class));
      startActivity(new Intent(HomeActivity.this, LoginActivity.class));
    }
    return true;
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  /** Handle "search" title-bar action. */
  public void onSearchClick(View v) {
    UIUtils.goSearch(this);
  }

  /** Handle "friends" action. */
  public void onFriendsClick(View v) {
    startActivity(new Intent(this, FriendsActivity.class));
  }

  /** Handle "courses" action. */
  public void onCoursesClick(View v) {
    startActivity(new Intent(this, CoursesActivity.class));
  }

  /** Handle "courses" action. */
  public void onSubscribedClick(View v) {
    startActivity(new Intent(this, SubscribedCoursesActivity.class));
  }

  /** Handle "notifications" action. */
  public void onNotificationsClick(View v) {
    startActivity(new Intent(this, NotificationsActivity.class));
  }
}
