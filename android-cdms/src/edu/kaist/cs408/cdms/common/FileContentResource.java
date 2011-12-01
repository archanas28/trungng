/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.kaist.cs408.cdms.common;

import java.io.Serializable;
import org.restlet.resource.Get;

/**
 * Interface for retrieving and storing the content of a file.
 * 
 * @author Trung
 */
public interface FileContentResource extends Serializable {
    
    @Get
    public byte[] retrieve();
}
