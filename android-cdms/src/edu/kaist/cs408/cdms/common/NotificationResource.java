/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.kaist.cs408.cdms.common;

import org.restlet.resource.Get;
import org.restlet.resource.Put;

/**
 *
 * @author Trung
 */
public interface NotificationResource {
    @Get
    public NotificationMsg retrieve();

    @Put
    public void store(NotificationMsg msg);
}
