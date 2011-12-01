package edu.kaist.cs408.cdms.ui;

import java.util.ArrayList;
import java.util.HashSet;

import org.restlet.resource.ClientResource;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.CursorAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.kaist.cs408.cdms.R;
import edu.kaist.cs408.cdms.common.FileMsg;
import edu.kaist.cs408.cdms.common.FilesResource;
import edu.kaist.cs408.cdms.common.FriendsResource;
import edu.kaist.cs408.cdms.common.GroupMsg;
import edu.kaist.cs408.cdms.common.IsFriendResource;
import edu.kaist.cs408.cdms.common.NotificationMsg;
import edu.kaist.cs408.cdms.common.NotificationsResource;
import edu.kaist.cs408.cdms.common.UserMsg;
import edu.kaist.cs408.cdms.util.CdmsUtils;
import edu.kaist.cs408.cdms.util.CommonResources;
import edu.kaist.cs408.cdms.util.UIUtils;

/**
 * Activity that displays the list of file for users.
 */
public class FriendDetailActivity extends ExpandableListActivity {
  
  //private static final String TAG = "FriendDetailActivity";
  private static final String RESOURCE_URL = "http://143.248.140.78:8080/cdms-server/users/";
  private FriendDetailListAdapter mAdapter;
  private UserMsg mUserMsg;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_friend_detail);
    ((TextView) findViewById(R.id.title_text)).setText(getTitle());
    
    mUserMsg = CommonResources.getUserMsg();
    updateFriendInfo(mUserMsg);
    if (!hasFriend(CdmsUtils.getUserId(FriendDetailActivity.this), mUserMsg.getId())) {
      findViewById(R.id.btn_add_friend).setVisibility(View.VISIBLE);
    }
    // TODO(trung): do in handler
    mAdapter = getAdapter(mUserMsg);
    setListAdapter(mAdapter);
  }

  /**
   * Checks if the user with {@code userId} has friend {@code friendId}.
   * 
   * @param userId
   * @param friendId
   * @return
   */
  private boolean hasFriend(long userId, long friendId) {
    ClientResource cr = new ClientResource(Constants.RESOURCE_USERS + userId
        + "/hasfriend/" + friendId);
    IsFriendResource resource = cr.wrap(IsFriendResource.class);
    return resource.check();
  }
  
  /**
   * Converts and returns the list of group to friends.
   * 
   * @param groups
   * @return
   */
  private ArrayList<UserMsg> getFriends(ArrayList<GroupMsg> groups) {
    HashSet<UserMsg> set = new HashSet<UserMsg>();
    for (GroupMsg group : groups) {
      set.addAll(group.getFriends());
    }
    ArrayList<UserMsg> list = new ArrayList<UserMsg>();
    list.addAll(set);
    
    return list;
  }
  
  /**
   * Builds the adapter for the list.
   * 
   * @param adapter
   * @param user
   */
  private FriendDetailListAdapter getAdapter(UserMsg user) {
    // get friends
    ClientResource cr = new ClientResource(RESOURCE_URL + user.getId() + "/friends");
    FriendsResource resource = cr.wrap(FriendsResource.class);
    ArrayList<UserMsg> friends = getFriends(resource.retrieve());
    // get courses
    cr = new ClientResource(RESOURCE_URL + user.getId() + "/courses");
    FilesResource rsc = cr.wrap(FilesResource.class);
    ArrayList<FileMsg> courses = rsc.retrieve();
    return new FriendDetailListAdapter(friends, courses);
  }
  
  /**
   * Fills in the friend info section.
   * 
   * @param msg
   */
  private void updateFriendInfo(UserMsg msg) {
    TextView v = (TextView) findViewById(R.id.txt_friend_name);
    v.setText(msg.getName());
  }
  
  /**
   * Adapter for the friends and courses of a user.
   */
  public class FriendDetailListAdapter extends BaseExpandableListAdapter {
    static final int GROUP_SIZE = 2;
    final String[] GROUP_NAME = { "Friends", "Courses" };
    ArrayList<UserMsg> mFriends;
    ArrayList<FileMsg> mCourses;
    
    public FriendDetailListAdapter(ArrayList<UserMsg> friends,
        ArrayList<FileMsg> courses) {
      mFriends = friends;
      mCourses = courses;
    }
    
    public Object getChild(int groupPosition, int childPosition) {
      if (groupPosition == 0) {
        return mFriends.get(childPosition);
      };
      
      return mCourses.get(childPosition);
    }

    public long getChildId(int groupPosition, int childPosition) {
      return childPosition;
    }

    public int getChildrenCount(int groupPosition) {
      if (groupPosition == 0) {
        return mFriends.size();
      }
      
      return mCourses.size();
    }

    public TextView getGenericView() {
      // TODO(trung): re-use convert view
      // Layout parameters for the ExpandableListView
      AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
          ViewGroup.LayoutParams.FILL_PARENT, 64);

      TextView textView = new TextView(FriendDetailActivity.this);
      textView.setLayoutParams(lp);
      // Center the text vertically
      textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
      // Set the text starting position
      textView.setPadding(52, 0, 0, 0);
      return textView;
    }

    public View getChildView(int groupPosition, int childPosition,
        boolean isLastChild, View convertView, ViewGroup parent) {
      TextView textView = getGenericView();
      Object o = getChild(groupPosition, childPosition);
      if (groupPosition == 0) {
        UserMsg msg = (UserMsg) o;
        textView.setText(msg.getName());
      } else {
        FileMsg msg = (FileMsg) o;
        textView.setText(msg.getName());
      }
      
      return textView;
    }

    public Object getGroup(int groupPosition) {
      if (groupPosition == 0) {
        return mFriends;
      }
      
      return mCourses;
    }

    public int getGroupCount() {
      return GROUP_SIZE;
    }

    public long getGroupId(int groupPosition) {
      return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded,
        View convertView, ViewGroup parent) {
      TextView textView = getGenericView();
      textView.setText(GROUP_NAME[groupPosition]);
      return textView;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
      return true;
    }

    public boolean hasStableIds() {
      return true;
    }
  }
  
  @Override
  public boolean onChildClick(ExpandableListView parent, View v,
      int groupPosition, int childPosition, long id) {
    Intent intent;
    if (groupPosition == 0) {
      intent = new Intent(FriendDetailActivity.this, FriendDetailActivity.class);
      UserMsg msg = (UserMsg) getExpandableListAdapter().getChild(groupPosition,
          childPosition);
      CommonResources.setUserMsg(msg);
    } else {
      intent = new Intent(FriendDetailActivity.this, CourseDetailActivity.class);
      FileMsg msg = (FileMsg) getExpandableListAdapter().getChild(groupPosition,
          childPosition);
      CommonResources.setFileMsg(msg);
    }
    
    startActivity(intent);
    return true;
  }
  
  /**
   * Handles "add friend" action.
   */
  public void onAddClick(View v) {
    // creates a notification for this request
    final NotificationMsg notification = new NotificationMsg();
    final Long userId = CdmsUtils.getUserId(FriendDetailActivity.this);
    notification.setSenderId(userId);
    notification.setReceiverId(mUserMsg.getId());
    notification.setType((short) NotificationHelper.TYPE_ADD_FRIEND);

    // sending request to the server
    new Runnable() {
      @Override
      public void run() {
        ClientResource cr = new ClientResource(
            Constants.RESOURCE_USERS + userId + "/notifications");
        NotificationsResource resource = cr.wrap(NotificationsResource.class);
        resource.store(notification);
      }
    }.run();
    Toast.makeText(FriendDetailActivity.this, "A request has been sent to add this friend.",
        Toast.LENGTH_SHORT).show();
  }
  
  /** Handle "home" title-bar action. */
  public void onHomeClick(View v) {
    UIUtils.goHome(this);
  }

  /** Handle "search" title-bar action. */
  public void onSearchClick(View v) {
    UIUtils.goSearch(this);
  }

  /**
   * {@link CursorAdapter} that renders a {@link SearchQuery}.
   */
  private class SearchAdapter extends CursorAdapter {
    public SearchAdapter(Context context) {
      super(context, null);
    }

    /** {@inheritDoc} */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
      return null;
    }

    /** {@inheritDoc} */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
      // ((TextView) view.findViewById(R.id.session_title)).setText(cursor
      // .getString(SearchQuery.TITLE));
      //
      // final String snippet = cursor.getString(SearchQuery.SEARCH_SNIPPET);
      // final Spannable styledSnippet = buildStyledSnippet(snippet);
      // ((TextView) view.findViewById(R.id.session_subtitle))
      // .setText(styledSnippet);
      //
      // final boolean starred = cursor.getInt(SearchQuery.STARRED) != 0;
      // final CheckBox starButton = (CheckBox) view
      // .findViewById(R.id.star_button);
      // starButton.setVisibility(starred ? View.VISIBLE : View.INVISIBLE);
      // starButton.setChecked(starred);
    }
  }
}
