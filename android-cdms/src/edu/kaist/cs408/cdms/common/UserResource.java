/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.kaist.cs408.cdms.common;

import java.io.Serializable;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

/**
 *
 * @author Trung
 */
public interface UserResource extends Serializable {
    @Get
    public UserMsg retrieve();

    @Put
    public void store(UserMsg info);
}
