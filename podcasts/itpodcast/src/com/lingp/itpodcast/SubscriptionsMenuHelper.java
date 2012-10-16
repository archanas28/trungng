package com.lingp.itpodcast;

import java.io.File;
import java.io.FilenameFilter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.lingp.itpodcast.R;
import com.lingp.itpodcast.preference.PreferencesActivityCompatability;
import com.lingp.itpodcast.preference.PreferencesActivityV11;
import com.lingp.itpodcast.preference.PrefsFragment;
import com.lingp.itpodcast.provider.FeedData;
import com.lingp.itpodcast.provider.OPML;
import com.lingp.itpodcast.utils.StaticMethods;
import com.lingp.itpodcast.utils.Strings;

/**
 * Common class to handle menu options for activities that show a list of
 * subscriptions.
 * 
 * @author trung nguyen
 * 
 */
public final class SubscriptionsMenuHelper {
  private static final int DIALOG_ERROR_FEEDIMPORT = 3;
  private static final int DIALOG_ERROR_FEEDEXPORT = 4;
  private static final int DIALOG_ERROR_INVALIDIMPORTFILE = 5;
  private static final int DIALOG_ERROR_EXTERNALSTORAGENOTAVAILABLE = 6;

  static boolean onOptionsItemSelected(final Context context, final MenuItem item) {
    switch (item.getItemId()) {
    case R.id.option_addfeed: {
      Intent intent = new Intent(context, EditSubscriptionActivity.class);
      intent.setData(FeedData.SubscriptionColumns.CONTENT_URI);
      intent.putExtra(EditSubscriptionActivity.ADD_FEED, true);
      context.startActivity(intent);
      break;
    }
    case R.id.option_refresh:
      if (StaticMethods.checkConnection(context)) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
            .getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
          new Thread() {
            public void run() {
              context.sendBroadcast(new Intent(Strings.ACTION_REFRESHFEEDS).putExtra(
                  Strings.SETTINGS_OVERRIDEWIFIONLY,
                  PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                      Strings.SETTINGS_OVERRIDEWIFIONLY, false)));
            }
          }.start();
          Toast.makeText(context, R.string.refreshing, Toast.LENGTH_SHORT).show();
        } else {
          Dialog dialog = new Dialog(context);
          dialog.setTitle(R.string.title_tip);
          dialog.setContentView(R.layout.message);
          TextView msg = (TextView) dialog.findViewById(R.id.msg);
          msg.setText(R.string.tip_msg1);
          dialog.setCanceledOnTouchOutside(true);
          dialog.show();
        }
      }
      break;

    case R.id.option_settings: {
      if (Build.VERSION.SDK_INT < 11) {
        context.startActivity(new Intent(context, PreferencesActivityCompatability.class));
      } else {
        Intent intent = new Intent(context, PreferencesActivityV11.class);
        // do not show header because currently there is only 1
        intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, PrefsFragment.class.getName());
        context.startActivity(intent);
      }

      break;
    }
    case R.id.option_import: {
      if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
          || Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(R.string.select_file);

        try {
          final String[] fileNames = Environment.getExternalStorageDirectory().list(
              new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                  return new File(dir, filename).isFile();
                }
              });
          builder.setItems(fileNames, new DialogInterface.OnClickListener() {
            @SuppressWarnings("deprecation")
            public void onClick(DialogInterface dialog, int which) {
              try {
                OPML.importFromFile(new StringBuilder(Environment.getExternalStorageDirectory()
                    .toString()).append(File.separator).append(fileNames[which]).toString(),
                    context);
              } catch (Exception e) {
                ((Activity) context).showDialog(DIALOG_ERROR_FEEDIMPORT);
              }
            }
          });
          builder.show();
        } catch (Exception e) {
          ((Activity) context).showDialog(DIALOG_ERROR_FEEDIMPORT);
        }
      } else {
        ((Activity) context).showDialog(DIALOG_ERROR_EXTERNALSTORAGENOTAVAILABLE);
      }

      break;
    }
    case R.id.option_export: {
      if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
          || Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
        try {
          String filename = new StringBuilder(Environment.getExternalStorageDirectory().toString())
              .append("/podcast").append(System.currentTimeMillis()).append(".opml").toString();

          OPML.exportToFile(filename, context);
          Toast.makeText(context,
              String.format(context.getString(R.string.message_exportedto), filename),
              Toast.LENGTH_LONG).show();
        } catch (Exception e) {
          ((Activity) context).showDialog(DIALOG_ERROR_FEEDEXPORT);
        }
      } else {
        ((Activity) context).showDialog(DIALOG_ERROR_EXTERNALSTORAGENOTAVAILABLE);
      }
      break;
    }
    }
    return true;
  }

  static Dialog onCreateDialog(final Context context, int id) {
    Dialog dialog;

    switch (id) {
    case DIALOG_ERROR_FEEDIMPORT: {
      dialog = createErrorDialog(context, R.string.error_feedimport);
      break;
    }
    case DIALOG_ERROR_FEEDEXPORT: {
      dialog = createErrorDialog(context, R.string.error_feedexport);
      break;
    }
    case DIALOG_ERROR_INVALIDIMPORTFILE: {
      dialog = createErrorDialog(context, R.string.error_invalidimportfile);
      break;
    }
    case DIALOG_ERROR_EXTERNALSTORAGENOTAVAILABLE: {
      dialog = createErrorDialog(context, R.string.error_externalstoragenotavailable);
      break;
    }
    default:
      dialog = null;
    }
    return dialog;
  }

  private static Dialog createErrorDialog(Context context, int messageId) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);

    builder.setMessage(messageId);
    builder.setTitle(R.string.error);
    builder.setIcon(android.R.drawable.ic_dialog_alert);
    builder.setPositiveButton(android.R.string.ok, null);
    return builder.create();
  }
}
