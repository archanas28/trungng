/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.kaist.cs408.cdms.common;

import org.restlet.resource.Get;

/**
 * Resource for checking if some user is friend of a particular user.
 * 
 * @author Trung
 */
public interface IsFriendResource {
    @Get
    public Boolean check();
}
