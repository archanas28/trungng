/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.kaist.cs408.cdms.common;

import java.util.ArrayList;
import org.restlet.resource.Get;

/**
 *
 * @author Trung
 */
public interface FriendsResource {
    @Get
    public ArrayList<GroupMsg> retrieve();
}
