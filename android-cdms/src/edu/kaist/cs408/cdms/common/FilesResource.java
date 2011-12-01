/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.kaist.cs408.cdms.common;

import java.io.Serializable;
import java.util.ArrayList;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

/**
 * Interface for the files resource that can be passed between server and client.
 * 
 * @author Trung
 */
public interface FilesResource extends Serializable {
    @Get
    public ArrayList<FileMsg> retrieve();

    @Put
    public void store(FileMsg msg);
}
