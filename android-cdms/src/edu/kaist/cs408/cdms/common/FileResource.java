/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.kaist.cs408.cdms.common;

import java.io.Serializable;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

/**
 * Interface for getting a file resource.
 */
public interface FileResource extends Serializable {
    
    @Get
    public FileMsg retrieve();

    @Put
    public void store(FileMsg msg);

    @Post
    public String acceptFile(Representation entity);
}
