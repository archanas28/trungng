package edu.kaist.cs408.cdms.ui;

/**
 * Helper for dealing with notifications.
 * 
 * @author Trung
 */
public class NotificationHelper {

  public static final int TYPE_ADD_FRIEND = 0;
  public static final int TYPE_SUBSCRIBE_COURSE = 1;
  public static final int TYPE_COMMENT_FILE = 2;

  public static String getContent(Short type) {
    switch (type) {
    case TYPE_ADD_FRIEND:
      return " wants to add you as a friend.";
    case TYPE_SUBSCRIBE_COURSE:
      return " wants to subcribe to your course.";
    case TYPE_COMMENT_FILE:
      return " commented on your file.";
    default:
      return "";
    }
  }

  public static String getNotificationContent(Short type) {
    switch (type) {
    case TYPE_ADD_FRIEND:
      return "You have a new friend request.";
    case TYPE_SUBSCRIBE_COURSE:
      return "You have a new course subscription request.";
    case TYPE_COMMENT_FILE:
      return "You have a new file comment.";
    default:
      return "";
    }
  }
}
