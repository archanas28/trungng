package com.rainmoon.podcast;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Activity for showing list of all subscriptions. Note that we must extend
 * FragmentActivity instead of Activity.
 * 
 * TODO(trung): make this activity work properly
 * 
 */
public class AllSubscriptionsActivity extends FragmentActivity {

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    setContentView(R.layout.activity_all_subscriptions);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.all_subscriptions, menu);
    return true;
  }

  public boolean onOptionsItemSelected(final MenuItem item) {
    super.onOptionsItemSelected(item);
    return SubscriptionsMenuHelper.onOptionsItemSelected(this, item);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    return SubscriptionsMenuHelper.onCreateDialog(this, id);
  }
}
