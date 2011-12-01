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

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;

import org.restlet.resource.ClientResource;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import edu.kaist.cs408.cdms.R;
import edu.kaist.cs408.cdms.common.CommentMsg;
import edu.kaist.cs408.cdms.common.CommentsResource;
import edu.kaist.cs408.cdms.common.FileContentResource;
import edu.kaist.cs408.cdms.common.FileMsg;
import edu.kaist.cs408.cdms.util.CdmsUtils;
import edu.kaist.cs408.cdms.util.CommonResources;
import edu.kaist.cs408.cdms.util.FileManagerIntents;
import edu.kaist.cs408.cdms.util.UIUtils;
import edu.kaist.cs408.cdms.util.UrlBuilder;

/**
 * Activity that displays the list of file for users.
 */
public class FileDetailActivity extends ListActivity {

  private static final String RESOURCE_URL = "http://143.248.140.78:8080/cdms-server/files/";
  private static final int PICK_DIRECTORY = 1;
  private static final String TAG = "FileDetailActivity";
  private FileMsg mFileMsg;
  private CommentListAdapter mAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_file_detail);
    ((TextView) findViewById(R.id.title_text)).setText(getTitle());

    mFileMsg = CommonResources.getFileMsg();
    updateFileInfo(mFileMsg);
    // TODO(trung): use handler
    mAdapter = new CommentListAdapter(FileDetailActivity.this,
        getComments(mFileMsg.getId()));
    setListAdapter(mAdapter);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_file, menu);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    int id = item.getItemId();
    switch (id) {
    case R.id.menu_file_download:
      pickDirectory();
      break;
    case R.id.menu_file_edit:
      CommonResources.setFileMsg(mFileMsg);
      Intent intent = new Intent(FileDetailActivity.this, EditFileInfoActivity.class);
      startActivity(intent);
      break;
    }

    return true;
  }

  /**
   * Opens the file manager to pick a directory.
   */
  private void pickDirectory() {
    Intent intent = new Intent(FileManagerIntents.ACTION_PICK_DIRECTORY);
    intent.setData(Uri.parse("file://sdcard"));
    intent.putExtra(FileManagerIntents.EXTRA_TITLE,
        getString(R.string.title_select_location));
    intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT,
        getString(R.string.title_done));

    try {
      startActivityForResult(intent, PICK_DIRECTORY);
    } catch (ActivityNotFoundException e) {
      Toast.makeText(this, "No file manager installed", Toast.LENGTH_SHORT).show();
    }
  }

  /**
   * Downloads file the selected location.
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    switch (requestCode) {
    case PICK_DIRECTORY:
      if (resultCode == RESULT_OK && data != null) {
        String directory = data.getDataString();
        if (directory != null) {
          if (directory.startsWith("file://")) {
            directory = directory.substring(7);
            downloadFile(directory);
          }
        }
      }
      break;
    }
  }

  /**
   * Downloads the file to the selected directory.
   * 
   * @param directory
   */
  private void downloadFile(final String directory) {
    new Runnable () {
      @Override
      public void run() {
        UrlBuilder builder = new UrlBuilder(RESOURCE_URL + mFileMsg.getId() + "/content");
        ClientResource cr = new ClientResource(builder.toString());
        FileContentResource resource = cr.wrap(FileContentResource.class);
        File file = new File(directory, mFileMsg.getName());
        try {
          FileOutputStream os = new FileOutputStream(file);
          os.write(resource.retrieve());
          os.close();
          CdmsUtils.notifyFileDownload(FileDetailActivity.this,
              Uri.parse("file://" + file.getAbsolutePath()));
        } catch (Exception e) {
          Toast.makeText(FileDetailActivity.this, "Error downloading file",
              Toast.LENGTH_SHORT).show();
          Log.e("FileDetailActivity", "error", e);
        }
      }
    }.run();
  }
  
  private void updateFileInfo(FileMsg file) {
    TextView view = (TextView) findViewById(R.id.txt_file_name);
    view.setText(file.getName());
    view = (TextView) findViewById(R.id.txt_file_time);
    view.setText("Uploaded on " + CdmsUtils.formatDate(file.getLastUpdated()));
    view = (TextView) findViewById(R.id.txt_file_desc);
    view.setText(file.getDescription());
  }

  /**
   * Connects to the server and gets the comments of this file.
   */
  private ArrayList<CommentMsg> getComments(long fileId) {
    UrlBuilder builder = new UrlBuilder(RESOURCE_URL + fileId + "/comments");
    ClientResource cr = new ClientResource(builder.toString());
    Log.i(TAG, CdmsUtils.getResponse(builder.toString()));
    CommentsResource resource = cr.wrap(CommentsResource.class);
    ArrayList<CommentMsg> list = resource.retrieve();

    return list;
  }

  /**
   * Adapter for the comment list.
   */
  private static class CommentListAdapter extends ArrayAdapter<CommentMsg> {

    private LayoutInflater mInflater;

    public CommentListAdapter(Context context, ArrayList<CommentMsg> list) {
      super(context, R.layout.item_comment, list);
      mInflater = (LayoutInflater) context
          .getSystemService(LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = mInflater.inflate(R.layout.item_comment, null);
      }
      // inflate content of the view
      CommentMsg msg = getItem(position);
      TextView view = (TextView) convertView.findViewById(R.id.item_commentor);
      view.setText(msg.getUserName());
      view = (TextView) convertView.findViewById(R.id.item_comment_time);
      view.setText(CdmsUtils.formatDate(msg.getPostedTime()));
      view = (TextView) convertView.findViewById(R.id.item_comment);
      view.setText(msg.getContent());

      return convertView;
    }
  }

  /**
   * Handle "comment" click action.
   * 
   * @param v
   */
  public void onCommentClick(View v) {
    CommentMsg msg = new CommentMsg();
    long fileId = CommonResources.getFileMsg().getId();
    TextView comment = ((TextView) findViewById(R.id.txt_comment));
    if (comment.getText().length() > 0) {
      msg.setContent(comment.getText().toString());
      msg.setFileId(fileId);
      msg.setUserId(CommonResources.getUserId(FileDetailActivity.this));
    }
    UrlBuilder builder = new UrlBuilder(RESOURCE_URL + fileId + "/comments");
    ClientResource cr = new ClientResource(builder.toString());
    CommentsResource resource = cr.wrap(CommentsResource.class);
    resource.store(msg);

    // refresh view
    comment.setText("");
    msg.setUserName("You");
    msg.setPostedTime(new Date(System.currentTimeMillis()));
    mAdapter.add(msg);
    mAdapter.notifyDataSetChanged();
  }

  /** Handle "clear" click action. */
  public void onClearClick(View v) {
    ((TextView) findViewById(R.id.txt_comment)).setText("");
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
