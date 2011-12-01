/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.kaist.cs408.cdms.common;

import java.io.Serializable;
import org.restlet.resource.Get;

/**
 * Interface for getting a course resource.
 */
public interface CourseResource extends Serializable {
    
    @Get
    public FileMsg retrieve();
}
