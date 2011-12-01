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
 * Interface for retrieving and storing the content of a file.
 * 
 * @author Trung
 */
public interface CommentsResource extends Serializable {
    
    @Get
    public ArrayList<CommentMsg> retrieve();

    @Put
    public void store(CommentMsg msg);
}
