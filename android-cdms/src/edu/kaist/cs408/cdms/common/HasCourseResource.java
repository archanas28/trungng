/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.kaist.cs408.cdms.common;

import org.restlet.resource.Get;

/**
 * Resource for checking if a user owns or subscribes to a course.
 * 
 * @author Trung
 */
public interface HasCourseResource {
    @Get
    public Boolean check();
}
