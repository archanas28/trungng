package com.lingp.krpodcast;

import com.lingp.krpodcast.R;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

/**
 * The home activity of the application.
 * 
 * @author trung nguyen
 * 
 */
public class HomeActivity extends FragmentActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_home);
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
