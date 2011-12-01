/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.kaist.cs408.cdms.common;

import java.io.Serializable;
import org.restlet.resource.Post;

/**
 *
 * @author Trung
 */
public interface FriendResponseResource extends Serializable {

    @Post
    public void accept(NotificationResponseMsg msg);
}
