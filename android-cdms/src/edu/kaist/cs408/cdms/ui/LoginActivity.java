package edu.kaist.cs408.cdms.ui;

import org.restlet.resource.ClientResource;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import edu.kaist.cs408.cdms.R;
import edu.kaist.cs408.cdms.common.UserMsg;
import edu.kaist.cs408.cdms.common.UserResource;
import edu.kaist.cs408.cdms.util.UrlBuilder;

/**
 * Log-in activity.
 */
public class LoginActivity extends Activity {
  
  private static final String RESOURCE_URL = "http://143.248.140.78:8080/cdms-server/users/info";
  private static final String PARAM_USERNAME = "username";
  private static final String PARAM_PASSWORD = "password";
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
  }
  
  public void onLoginClick(View v) {
    TextView username, password;
    username = (TextView) findViewById(R.id.input_username);
    password = (TextView) findViewById(R.id.input_password);
    if (username.getText().length() > 0 && password.getText().length() > 0) {
      UrlBuilder builder = new UrlBuilder(RESOURCE_URL);
      builder.appendParam(PARAM_USERNAME, username.getText().toString());
      builder.appendParam(PARAM_PASSWORD, password.getText().toString());
      ClientResource cr = new ClientResource(builder.toString());
      UserResource resource = cr.wrap(UserResource.class);
      UserMsg msg = resource.retrieve();
      if (msg != null) {
        Editor editor = getSharedPreferences(
            Constants.SHARED_PREFERENCES_FILE, 0).edit();
        editor.putLong(Constants.SHARED_USER_ID, msg.getId());
        editor.commit();
        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
      }
    }
  }
  
  public void onClearClick(View v) {
    TextView username, password;
    username = (TextView) findViewById(R.id.input_username);
    password = (TextView) findViewById(R.id.input_password);
    username.setText("");
    password.setText("");
  }
  
  public void onSignupClick(View v) {
    Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
    startActivity(intent);
  }
}
