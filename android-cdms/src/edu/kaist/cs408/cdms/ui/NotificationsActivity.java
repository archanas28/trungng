package edu.kaist.cs408.cdms.ui;

import java.util.ArrayList;

import org.restlet.resource.ClientResource;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import edu.kaist.cs408.cdms.R;
import edu.kaist.cs408.cdms.common.FriendResponseResource;
import edu.kaist.cs408.cdms.common.NotificationMsg;
import edu.kaist.cs408.cdms.common.NotificationResponseMsg;
import edu.kaist.cs408.cdms.common.NotificationsResource;
import edu.kaist.cs408.cdms.common.SubscriptionResponseResource;
import edu.kaist.cs408.cdms.util.CdmsUtils;
import edu.kaist.cs408.cdms.util.UIUtils;

public class NotificationsActivity extends ListActivity {
  
  private static final String TAG = "NotificationsActivity";
  private NotificationAdapter mAdapter;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_notifications);
    ((TextView) findViewById(R.id.title_text)).setText(getTitle());
    
    // TODO(trung): replace with Intent
    long userId = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, 0).getLong(
        Constants.SHARED_USER_ID, Constants.INVALID_ID);
    if (userId != Constants.INVALID_ID) {
      mAdapter = new NotificationAdapter(NotificationsActivity.this,
          R.layout.item_notification, getNotifications(userId));
      setListAdapter(mAdapter);
    } else {
      UIUtils.goLogin(NotificationsActivity.this);
    }
  }

  /**
   * Gets the list of notifications for this user from the server.
   * @param userId
   * @return
   */
  private ArrayList<NotificationMsg> getNotifications(long userId) {
    ClientResource cr = new ClientResource(Constants.RESOURCE_USERS  + userId + "/notifications");
    Log.i(TAG, CdmsUtils.getResponse(Constants.RESOURCE_USERS  + userId + "/notifications"));
    NotificationsResource resource = cr.wrap(NotificationsResource.class);
    ArrayList<NotificationMsg> list = resource.retrieve();
    
    return list;
  }
  
  /** Handle "home" title-bar action. */
  public void onHomeClick(View v) {
    UIUtils.goHome(this);
  }

  /** Handle "refresh" title-bar action. */
  public void onRefreshClick(View v) {
  }

  /** Handle "search" title-bar action. */
  public void onSearchClick(View v) {
    UIUtils.goSearch(this);
  }
  
  /**
   * Adapter for displaying notifications.
   */
  private static class NotificationAdapter extends ArrayAdapter<NotificationMsg> {
    
    private LayoutInflater mInflater;
    private int mLayoutResourceId;
    
    public NotificationAdapter(Context context, int layoutResourceId,
        ArrayList<NotificationMsg> msgs) {
      super(context, layoutResourceId, msgs);
      mInflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
      mLayoutResourceId = layoutResourceId;
    }
    
    /**
     * Shows buttons for {@code v} depending on its type of notification.
     */
    private void attachButtons(View v, NotificationMsg msg) {
      TextView view;
      int type = msg.getType();
      if (type == NotificationHelper.TYPE_ADD_FRIEND
          || type == NotificationHelper.TYPE_SUBSCRIBE_COURSE) {
        // accept button
        view = (TextView) v.findViewById(R.id.btn_notification_first);
        view.setText(R.string.btn_accept);
        view.setVisibility(View.VISIBLE);
        view.setTag(msg);
        // ignore button
        view = (TextView) v.findViewById(R.id.btn_notification_second);
        view.setText(R.string.btn_ignore);
        view.setVisibility(View.VISIBLE);
        view.setTag(msg);
      }
    }
    
    @Override
    public long getItemId(int position) {
      return getItem(position).getId();
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = mInflater.inflate(mLayoutResourceId, null);
      }
      NotificationMsg msg = getItem(position);
      TextView view = (TextView) convertView.findViewById(R.id.txt_notification_sender);
      view.setText(msg.getSenderName());
      view = (TextView) convertView.findViewById(R.id.txt_notification_content);
      view.setText(NotificationHelper.getContent(msg.getType()));
      view = (TextView) convertView.findViewById(R.id.txt_notification_time);
      view.setText(CdmsUtils.formatDate(msg.getTime()));
      attachButtons(convertView, msg);
      
      return convertView;
    }
  }

  public void buttonClick(View v) {
    NotificationMsg msg = (NotificationMsg) v.getTag();
    mAdapter.remove(msg);
    mAdapter.notifyDataSetChanged();
    Toast.makeText(NotificationsActivity.this, "Your response was sent to your friend.",
        Toast.LENGTH_SHORT).show();
    // send user action to server
    final NotificationResponseMsg resp = new NotificationResponseMsg();
    resp.setNotificationId(msg.getId());
    resp.setSenderId(msg.getSenderId());
    resp.setReceiverId(msg.getReceiverId());
    resp.setAccepted(v.getId() == R.id.btn_notification_first);
    if (msg.getType() == NotificationHelper.TYPE_ADD_FRIEND) {
      new Runnable() {
        @Override
        public void run() {
          ClientResource cr = new ClientResource(Constants.RESOURCE_FRIEND_REQUEST);
          FriendResponseResource resource = cr.wrap(FriendResponseResource.class);
          resource.accept(resp);
        }
      }.run();
      
    } else if (msg.getType() == NotificationHelper.TYPE_SUBSCRIBE_COURSE) {
      resp.setFileId(msg.getFileId());
      new Runnable() {
        @Override
        public void run() {
          ClientResource cr = new ClientResource(Constants.RESOURCE_COURSE_SUBSCRIPTION);
          SubscriptionResponseResource resource = cr.wrap(SubscriptionResponseResource.class);
          resource.accept(resp);
        }
      }.run();
    }
  }
}
