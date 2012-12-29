package com.lingp.espodcast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.lingp.espodcast.utils.Strings;

/**
 * The home activity of the application.
 * 
 * @author trung nguyen
 */
public class HomeActivity extends FragmentActivity {
  private SharedPreferences mPrefs;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPrefs = getSharedPreferences(Strings.APPLICATION_SHARED_PREFERENCES, 0);
    boolean firstUse = mPrefs.getBoolean(Strings.PREFERENCE_FIRST_USE, true);
    if (firstUse) {
      showWelcomeDialog();
      mPrefs.edit().putBoolean(Strings.PREFERENCE_FIRST_USE, false).commit();
    }
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
    if (item.getItemId() == R.id.option_about) {
      showWelcomeDialog();
    }
    return SubscriptionsMenuHelper.onOptionsItemSelected(this, item);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    return SubscriptionsMenuHelper.onCreateDialog(this, id);
  }

  private void showWelcomeDialog() {
    final Dialog dialog = new Dialog(this);
    dialog.setContentView(R.layout.dialog_welcome);
    dialog.setTitle(R.string.welcome);
    dialog.setCancelable(true);
    TextView textView = (TextView) dialog.findViewById(R.id.txt_welcome);
    Spanned text = Html.fromHtml(readTextFromRawResource());
    textView.setText(text);
    Button btn = (Button) dialog.findViewById(R.id.btn_see_more);
    btn.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        try {
          startActivity(new Intent(Intent.ACTION_VIEW, Uri
              .parse(Strings.INTENT_PUBLISHER_GOOGLE_PLAY)));
        } catch (ActivityNotFoundException e) {
          e.printStackTrace();
        }
      }
    });
    btn = (Button) dialog.findViewById(R.id.btn_dismiss);
    btn.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        dialog.dismiss();
      }
    });
    dialog.setCancelable(true);
    dialog.setCanceledOnTouchOutside(true);
    dialog.show();
  }

  private String readTextFromRawResource() {
    try {
      InputStream inputStream = getResources().openRawResource(R.raw.info);
      StringBuilder builder = new StringBuilder();
      BufferedReader in = new BufferedReader(new InputStreamReader(inputStream,
          "utf-8"));
      String line;
      while ((line = in.readLine()) != null) {
        builder.append(line);
      }
      in.close();
      return builder.toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "Thank you for using this application :)";
  }
}
