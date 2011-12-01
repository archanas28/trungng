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

import java.util.ArrayList;

import org.restlet.resource.ClientResource;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import edu.kaist.cs408.cdms.R;
import edu.kaist.cs408.cdms.common.FileMsg;
import edu.kaist.cs408.cdms.common.FilesResource;
import edu.kaist.cs408.cdms.util.CommonResources;
import edu.kaist.cs408.cdms.util.UIUtils;
import edu.kaist.cs408.cdms.util.UrlBuilder;

/**
 * Activity that displays the list of file for users.
 * 
 * TODO(trung): perhaps we should use the same activity for subscribed courses
 * as for owned courses.
 * TODO(trung): adds subscribe/unsubscribe button
 */
public class SubscribedCoursesActivity extends ListActivity {
  private static final String RESOURCE_URL = "http://143.248.140.78:8080/cdms-server/users/";
  
  ListAdapter mAdapter;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_courses);
    ((TextView) findViewById(R.id.title_text)).setText(getTitle());

    long userId = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, 0).getLong(
        Constants.SHARED_USER_ID, Constants.INVALID_ID);
    if (userId != Constants.INVALID_ID) {
      // set adapter (after getting data from the server)
      // should be done in handler
      mAdapter = new CourseListAdapter(SubscribedCoursesActivity.this,
          R.layout.item_course, getCourses(userId));
      if (mAdapter != null) {
        setListAdapter(mAdapter);
        registerForContextMenu(getListView());
        // handle click item action
        getListView().setOnItemClickListener(new OnItemClickListener() {
          @Override
          public void onItemClick(AdapterView<?> parent, View view, int position,
              long id) {
            CommonResources.setFileMsg((FileMsg) parent.getItemAtPosition(position));
            Intent intent = new Intent(SubscribedCoursesActivity.this,
                CourseDetailActivity.class);
            startActivity(intent);
          }
        });
      } else {
        Toast.makeText(SubscribedCoursesActivity.this, "Cannot get data from server",
            Toast.LENGTH_SHORT).show();
      }
    } else {
      UIUtils.goLogin(SubscribedCoursesActivity.this);
    }
  }

  /**
   * Connects to the server and gets the courses of this user.
   */
  private ArrayList<FileMsg> getCourses(long userId) {
    UrlBuilder builder = new UrlBuilder(RESOURCE_URL + userId + "/subscribed");
    ClientResource cr = new ClientResource(builder.toString());
    FilesResource resource = cr.wrap(FilesResource.class);
    ArrayList<FileMsg> list = resource.retrieve();
    return list;
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    menu.setHeaderTitle("Sample menu");
    menu.add(0, 0, 0, "test sample menu");
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    return false;
  }

  /**
   * Adapter for the comment list.
   */
  private static class CourseListAdapter extends ArrayAdapter<FileMsg> {
    
    private LayoutInflater mInflater;
    private int mLayoutResourceId;
    
    public CourseListAdapter(Context context, int layoutResourceId,
        ArrayList<FileMsg> list) {
      super(context, layoutResourceId, list);
      mLayoutResourceId = layoutResourceId;
      mInflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = mInflater.inflate(mLayoutResourceId, null);
      }
      // inflate content of the view
      FileMsg msg = getItem(position);
      TextView v = (TextView) convertView.findViewById(R.id.txt_item_course_name);
      v.setText(msg.getName());
      v = (TextView) convertView.findViewById(R.id.txt_item_course_desc);
      v.setText(msg.getDescription());
      
      return convertView;
    }
  }
  
  public void onHomeClick(View v) {
    UIUtils.goHome(this);
  }

  public void onSearchClick(View v) {
    UIUtils.goSearch(this);
  }
}
