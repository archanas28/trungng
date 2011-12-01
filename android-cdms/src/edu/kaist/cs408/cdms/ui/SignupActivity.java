package edu.kaist.cs408.cdms.ui;

import org.restlet.resource.ClientResource;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import edu.kaist.cs408.cdms.R;
import edu.kaist.cs408.cdms.common.UserMsg;
import edu.kaist.cs408.cdms.common.UserResource;

/**
 * Sign-up activity.
 */
public class SignupActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_signup);
  }
  
  public void onSubmitClick(View v) {
    UserMsg msg = new UserMsg();
    msg.setEmail(((TextView) findViewById(R.id.input_email)).getText().toString());
    msg.setName(((TextView) findViewById(R.id.input_name)).getText().toString());
    msg.setPassword(((TextView) findViewById(R.id.input_password)).getText().toString());
    msg.setUsername(((TextView) findViewById(R.id.input_username)).getText().toString());
    ClientResource cr = new ClientResource(Constants.RESOURCE_USERS + "info");
    UserResource resource = cr.wrap(UserResource.class);
    resource.store(msg);
    Toast.makeText(SignupActivity.this, "Your account has been successfully created! Please log in by the account.",
        Toast.LENGTH_LONG).show();
    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
    startActivity(intent);
  }
  
  public void onClearClick(View v) {
    ((TextView) findViewById(R.id.input_name)).setText("");
    ((TextView) findViewById(R.id.input_username)).setText("");
    ((TextView) findViewById(R.id.input_password)).setText("");
    ((TextView) findViewById(R.id.input_password2)).setText("");
    ((TextView) findViewById(R.id.input_email)).setText("");
  }
}
