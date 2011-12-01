package edu.kaist.cs408.cdms.util;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import edu.kaist.cs408.cdms.common.FileMsg;
import edu.kaist.cs408.cdms.common.UserMsg;
import edu.kaist.cs408.cdms.ui.Constants;
import edu.kaist.cs408.cdms.ui.FileDetailActivity;

/**
 * Class that allow sharing and passing of common resources among activity.
 * 
 * @author Trung
 */
public final class CommonResources {
  
  private static FileMsg mFileMsg;
  private static UserMsg mUserMsg;
  
  /**
   * Sets the file message to be used by {@link FileDetailActivity}.
   * 
   * <p> This is the only message because only one activity can be running at a time.
   * 
   * @param msg
   */
  public static void setFileMsg(FileMsg msg) {
    mFileMsg = msg;
  }

  /**
   * Gets the file message to be used by {@link FileDetailActivity}.
   * 
   * <p> This is the only message because only one activity can be running at a time.
   * 
   * @param msg
   */
  public static FileMsg getFileMsg() {
    return mFileMsg;
  }
  
  /**
   * Gets the logged in user id.
   * 
   * @param context
   * @return
   */
  public static long getUserId(Context context) {
    return context.getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, 0).getLong(
        Constants.SHARED_USER_ID, Constants.INVALID_ID);    
  }

  /**
   * Sets the shared instance of {@link UserMsg} between activities.
   * 
   * @param msg
   */
  public static void setUserMsg(UserMsg msg) {
    mUserMsg = msg;
  }
  
  /**
   * Returns the shared instance of {@link UserMsg} between activities..
   * 
   * @return
   */
  public static UserMsg getUserMsg() {
    return mUserMsg;
  }
}
