package edu.kaist.cs408.cdms.ui;

import java.util.ArrayList;

import org.restlet.resource.ClientResource;

import android.app.ExpandableListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import edu.kaist.cs408.cdms.R;
import edu.kaist.cs408.cdms.common.FriendsResource;
import edu.kaist.cs408.cdms.common.GroupMsg;
import edu.kaist.cs408.cdms.common.UserMsg;
import edu.kaist.cs408.cdms.util.CdmsUtils;
import edu.kaist.cs408.cdms.util.CommonResources;
import edu.kaist.cs408.cdms.util.UIUtils;
import edu.kaist.cs408.cdms.util.UrlBuilder;

/**
 * Activity to display the list of friends.
 * 
 * @author Trung
 *
 */
public class FriendsActivity extends ExpandableListActivity {

  private static final String TAG = "FriendsActivity";
  
  ExpandableListAdapter mAdapter;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_friends);
    ((TextView) findViewById(R.id.title_text)).setText(getTitle());

    long userId = CdmsUtils.getUserId(FriendsActivity.this);
    if (userId != -1) {
      // set adapter (after getting data from the server)
      // should be done in handler
      mAdapter = new MyExpandableListAdapter(getFriendsForUser(userId));
      if (mAdapter != null) {
        setListAdapter(mAdapter);
        registerForContextMenu(getExpandableListView());
      } else {
        Toast.makeText(FriendsActivity.this, "Cannot get data from server",
            Toast.LENGTH_SHORT).show();
      }
    }
  }

  /**
   * Connects to the server and gets the friends of this user.
   */
  private ArrayList<GroupMsg> getFriendsForUser(long userId) {
    UrlBuilder builder = new UrlBuilder(Constants.RESOURCE_USERS + userId + "/friends");
    ClientResource cr = new ClientResource(builder.toString());
    Log.i(TAG, CdmsUtils.getResponse(builder.toString()));
    FriendsResource resource = cr.wrap(FriendsResource.class);
    ArrayList<GroupMsg> msg = resource.retrieve();
    
    return msg;
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    menu.setHeaderTitle("Sample menu");
    menu.add(0, 0, 0, "test sample menu");
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item
        .getMenuInfo();

    String title = ((TextView) info.targetView).getText().toString();

    int type = ExpandableListView.getPackedPositionType(info.packedPosition);
    if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
      int groupPos = ExpandableListView
          .getPackedPositionGroup(info.packedPosition);
      int childPos = ExpandableListView
          .getPackedPositionChild(info.packedPosition);
      Toast.makeText(this,
          title + ": Child " + childPos + " clicked in group " + groupPos,
          Toast.LENGTH_SHORT).show();
      return true;
    } else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
      int groupPos = ExpandableListView
          .getPackedPositionGroup(info.packedPosition);
      Toast.makeText(this, title + ": Group " + groupPos + " clicked",
          Toast.LENGTH_SHORT).show();
      return true;
    }

    return false;
  }

  /**
   * Adapter for friends list.
   */
  public class MyExpandableListAdapter extends BaseExpandableListAdapter {
    ArrayList<GroupMsg> groups;
    
    public MyExpandableListAdapter(ArrayList<GroupMsg> list) {
      groups = list;
    }
    
    public Object getChild(int groupPosition, int childPosition) {
      GroupMsg group = groups.get(groupPosition);
      return group.getFriends().get(childPosition);
    }

    public long getChildId(int groupPosition, int childPosition) {
      return childPosition;
    }

    public int getChildrenCount(int groupPosition) {
      return groups.get(groupPosition).getFriends().size();
    }

    public TextView getGenericView() {
      // TODO(trung): re-use convert view
      // Layout parameters for the ExpandableListView
      AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
          ViewGroup.LayoutParams.FILL_PARENT, 64);

      TextView textView = new TextView(FriendsActivity.this);
      textView.setLayoutParams(lp);
      // Center the text vertically
      textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
      // Set the text starting position
      textView.setPadding(48, 0, 0, 0);
      return textView;
    }

    public View getChildView(int groupPosition, int childPosition,
        boolean isLastChild, View convertView, ViewGroup parent) {
      TextView textView = getGenericView();
      UserMsg msg = (UserMsg) getChild(groupPosition, childPosition);
      textView.setText(msg.getName());
      return textView;
    }

    public Object getGroup(int groupPosition) {
      return groups.get(groupPosition);
    }

    public int getGroupCount() {
      return groups.size();
    }

    public long getGroupId(int groupPosition) {
      return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded,
        View convertView, ViewGroup parent) {
      TextView textView = getGenericView();
      textView.setText(((GroupMsg) getGroup(groupPosition)).getName());
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
    Intent intent = new Intent(FriendsActivity.this, FriendDetailActivity.class);
    UserMsg msg = (UserMsg) getExpandableListAdapter().getChild(groupPosition,
        childPosition);
    CommonResources.setUserMsg(msg);
    startActivity(intent);
    
    return true;
  }
  
  public void onHomeClick(View v) {
    UIUtils.goHome(this);
  }

  public void onSearchClick(View v) {
    UIUtils.goSearch(this);
  }
}
