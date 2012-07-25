package com.rainmoon.podcast;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
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
    Fragment playerFragment = getSupportFragmentManager().findFragmentById(
        R.id.frag_player);
    
    try {
      OnFeedItemChangeListener listener = (OnFeedItemChangeListener) playerFragment;
      listener.onUrlChange("http://www.nhaccuatui.com/nghe?L=NJZ8I0EKsoJV");
    } catch (ClassCastException e) {
      Log.e("HomeActivity", "Fragment does not implement required interface");
    }
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
