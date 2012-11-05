package com.lingp.jppodcast;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.Policy;
import com.google.android.vending.licensing.ServerManagedPolicy;

/**
 * The home activity of the application.
 * 
 * @author trung nguyen
 * 
 */
public class HomeActivity extends FragmentActivity {
  private static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkdrmcAHE4J0NY2JJa5Ce47vHLkUvohQ2+NRdrvYOPFoevEeu0qIRgRoMkjN6wepI/RhWkYA9StegQw7+ORIbuk7XxnOn5ZhBfwaEQPxu0lhr90f/Wbyy9706Bzae/mtB4IvCEiVuQ6IJTfPcx8SA2Sp5QDReDzGsYah73emjJ76unRuq9ZG4xPUkQE/l07CDTly8hFRLBd8hA0+umsBk2vLMQYpflpS2UucD3k8ujaaU2L4MwCtN6a+D6JwC9FZ7f1sNAeYzQu+LCTHDBg8xdZz0dlu1GmmfShijez+tl9Xh2DoSgGKwWTe9AEqLLZjlzix8ywp9DUNO+9SJ8H+GZwIDAQAB";
  private static final int ID_NO_LICENSE = 1;
  // Generate your own 20 random bytes, and put them here.
  private static final byte[] SALT = new byte[] { -64, 52, 31, -108, -13, -51, 73, -61, 5, 88, -95,
      -45, 78, -110, -36, -13, -11, 32, -64, 89 };

  private LicenseCheckerCallback mLicenseCheckerCallback;
  private LicenseChecker mChecker;
  // A handler on the UI thread.
  private Handler mHandler;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);
    mHandler = new Handler();
    // Try to use more data here. ANDROID_ID is a single point of attack.
    String deviceId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
    // Library calls this when it's done.
    mLicenseCheckerCallback = new MyLicenseCheckerCallback();
    // Construct the LicenseChecker with a policy.
    mChecker = new LicenseChecker(this, new ServerManagedPolicy(this, new AESObfuscator(SALT,
        getPackageName(), deviceId)), BASE64_PUBLIC_KEY);
    mChecker.checkAccess(mLicenseCheckerCallback);
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

  protected Dialog onCreateDialog(int id) {
    if (id != ID_NO_LICENSE)
      return SubscriptionsMenuHelper.onCreateDialog(this, id);

    return new AlertDialog.Builder(this).setTitle(R.string.unlicensed_dialog_title)
        .setMessage(R.string.unlicensed_dialog_body)
        .setPositiveButton(R.string.buy_button, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri
                .parse("http://market.android.com/details?id=" + getPackageName()));
            startActivity(marketIntent);
          }
        }).setNegativeButton(R.string.quit_button, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            finish();
          }
        }).create();
  }

  /**
   * Shows unlicensed message to user and ask them to buy or quit.
   * 
   * @param showRetry
   */
  private void displayDialog() {
    mHandler.post(new Runnable() {
      public void run() {
        showDialog(ID_NO_LICENSE);
      }
    });
  }

  private class MyLicenseCheckerCallback implements LicenseCheckerCallback {
    public void allow(int policyReason) {
      // do nothing
      Log.i(getCallingPackage(), "licensed");
    }

    public void dontAllow(int policyReason) {
      if (isFinishing()) {
        return;
      }
      if (policyReason == Policy.RETRY) {
        // some error due to no network connection or error with the licensing
        // server,
        // let user through this time and will check later when things are ok
        return;
      }
      displayDialog();
    }

    public void applicationError(int errorCode) {
      if (isFinishing()) {
        return;
      }
      // This is a polite way of saying the developer made a mistake
      // while setting up or calling the license checker library.
      Log.e("HomeActivity", "Error while checking license");
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mChecker.onDestroy();
  }
}
