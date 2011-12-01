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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import edu.kaist.cs408.cdms.R;
import edu.kaist.cs408.cdms.common.FileMsg;
import edu.kaist.cs408.cdms.common.FilesResource;
import edu.kaist.cs408.cdms.common.HasCourseResource;
import edu.kaist.cs408.cdms.common.NotificationMsg;
import edu.kaist.cs408.cdms.common.NotificationsResource;
import edu.kaist.cs408.cdms.util.CdmsUtils;
import edu.kaist.cs408.cdms.util.CommonResources;
import edu.kaist.cs408.cdms.util.UIUtils;
import edu.kaist.cs408.cdms.util.UrlBuilder;

/**
 * Activity that displays the list of files in a course.
 */
public class CourseDetailActivity extends ListActivity {

  private static final String TAG = "CourseDetailActivity";
  private FileMsg mCourse;
  private FileListAdapter mAdapter;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_course_detail);

    mCourse = CommonResources.getFileMsg();
    updateCourseInfo(mCourse);
    if (hasCourse(CdmsUtils.getUserId(CourseDetailActivity.this), mCourse.getId())) {
      // TODO(trung): use handler
      mAdapter = new FileListAdapter(CourseDetailActivity.this, R.layout.item_course_file,
          getFiles(mCourse.getId()));
      setListAdapter(mAdapter);
      getListView().setOnItemClickListener(new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
          Intent intent = new Intent(CourseDetailActivity.this, FileDetailActivity.class);
          CommonResources.setFileMsg((FileMsg) parent.getItemAtPosition(position));
          startActivity(intent);
        }
      });
    } else {
      // User does not have access to this course
      findViewById(R.id.btn_subscribe_course).setVisibility(View.VISIBLE);
      findViewById(R.id.txt_no_permission).setVisibility(View.VISIBLE);
    }
  }

  /**
   * Checks if user {@code userId} owns or subscribes to course with given
   *  {@code courseid}.
   * 
   * @return
   */
  private static boolean hasCourse(long userId, long courseId) {
    ClientResource cr = new ClientResource(Constants.RESOURCE_USERS + userId
        + "/hascourse/" + courseId);
    HasCourseResource resource = cr.wrap(HasCourseResource.class);
    return resource.check();
  }
  
  /**
   * Shows the course information.
   * 
   * @param file
   */
  private void updateCourseInfo(FileMsg file) {
    TextView view = (TextView) findViewById(R.id.txt_course_name);
    view.setText(file.getName());
    view = (TextView) findViewById(R.id.txt_course_time);
    view.setText("Created on " + CdmsUtils.formatDate(file.getLastUpdated()));
    view = (TextView) findViewById(R.id.txt_course_desc);
    view.setText(file.getDescription());
  }
  
  /**
   * Connects to the server and gets the files of this course.
   */
  private ArrayList<FileMsg> getFiles(long courseId) {
    UrlBuilder builder = new UrlBuilder(Constants.RESOURCE_COURSES + courseId + "/files");
    ClientResource cr = new ClientResource(builder.toString());
    Log.i(TAG, CdmsUtils.getResponse(builder.toString()));
    FilesResource resource = cr.wrap(FilesResource.class);
    ArrayList<FileMsg> list = resource.retrieve();
    
    return list;
  }
  
  /**
   * Adapter for the comment list.
   */
  private static class FileListAdapter extends ArrayAdapter<FileMsg> {
    
    private LayoutInflater mInflater;
    private int mLayoutResourceId;
    
    public FileListAdapter(Context context, int layoutResourceId,
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
      TextView v = (TextView) convertView.findViewById(
          R.id.txt_item_course_file_name);
      v.setText(msg.getName());
      v = (TextView) convertView.findViewById(R.id.txt_item_course_file_desc);
      v.setText(msg.getDescription());
      
      return convertView;
    }
  }
  
  /**
   * Handles "subscribe" action.
   * 
   * @param v
   */
  public void onSubscribeClick(View v) {
    // creates a notification for this request
    final NotificationMsg notification = new NotificationMsg();
    notification.setFileId(mCourse.getId());
    final Long userId = CdmsUtils.getUserId(CourseDetailActivity.this);
    notification.setSenderId(userId);
    notification.setType((short) NotificationHelper.TYPE_SUBSCRIBE_COURSE);

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
    Toast.makeText(CourseDetailActivity.this, "A request has been sent to subscribe to this course",
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
}
