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

import java.util.Date;

import org.restlet.resource.ClientResource;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import edu.kaist.cs408.cdms.R;
import edu.kaist.cs408.cdms.common.FileMsg;
import edu.kaist.cs408.cdms.common.FileResource;
import edu.kaist.cs408.cdms.util.CdmsUtils;
import edu.kaist.cs408.cdms.util.CommonResources;
import edu.kaist.cs408.cdms.util.UIUtils;
import edu.kaist.cs408.cdms.util.UrlBuilder;

/**
 * Activity that displays the list of file for users.
 */
public class EditFileInfoActivity extends Activity {

  private static final String RESOURCE_URL = "http://143.248.140.78:8080/cdms-server/files/";
  private static final String TAG = "EditFileInfoActivity";
  private FileMsg mFileMsg;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_edit_file_info);
    ((TextView) findViewById(R.id.title_text)).setText(getTitle());

    mFileMsg = CommonResources.getFileMsg();
    setFileInfo(mFileMsg);
  }
  
  private void setFileInfo(FileMsg file) {
    EditText view = (EditText) findViewById(R.id.input_file_name);
    view.setText(mFileMsg.getName());
    view = (EditText) findViewById(R.id.input_file_desc);
    view.setText(mFileMsg.getDescription());
    view = (EditText) findViewById(R.id.input_file_tags);
    view.setText(mFileMsg.getTags());
  }

  /**
   * Handle "save" click action.
   * 
   * @param v
   */
  public void onSaveClick(View v) {
    EditText view = (EditText) findViewById(R.id.input_file_name);
    mFileMsg.setName(view.getText().toString());
    view = (EditText) findViewById(R.id.input_file_desc);
    mFileMsg.setDescription(view.getText().toString());
    view = (EditText) findViewById(R.id.input_file_tags);
    mFileMsg.setTags(view.getText().toString());
    mFileMsg.setLastUpdated(new Date(System.currentTimeMillis()));
    
    new Runnable() {
      @Override
      public void run() {
        UrlBuilder builder = new UrlBuilder(RESOURCE_URL + mFileMsg.getId());
        ClientResource cr = new ClientResource(builder.toString());
        Log.i(TAG, CdmsUtils.getResponse(builder.toString()));
        FileResource resource = cr.wrap(FileResource.class);
        resource.store(mFileMsg);
        Toast.makeText(EditFileInfoActivity.this, "File infomration saved",
            Toast.LENGTH_SHORT).show();
      }
    }.run();
    
    back();
  }

  /** Handle "cancel" click action. */
  public void onCancelClick(View v) {
    back();
  }

  /** Goes back to the previous activity */
  private void back() {
    Intent intent = new Intent(EditFileInfoActivity.this, FileDetailActivity.class);
    startActivity(intent);
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
