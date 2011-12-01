/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.kaist.cs408.cdms.common;

import java.io.Serializable;
import java.util.Date;

/**
 * The file message to be communicated between server and clients.
 *
 * <p> It is important to notice that this message only contains information
 * of a file. To get the file content, use {@link FileContentMsg}.
 * 
 * @author Trung
 */
public class FileMsg implements Serializable {
    /**
     * Default constructor
     */
    public FileMsg() {}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
    
    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public UserMsg getOwner() {
        return owner;
    }

    public void setOwner(UserMsg owner) {
        this.owner = owner;
    }

    public String getLastUpdatedString() {
        return lastUpdatedString;
    }

    public void setLastUpdatedString(String lastUpdatedString) {
        this.lastUpdatedString = lastUpdatedString;
    }
    
    private String description;
    private String name;
    private Integer size;
    private Long id;
    private Date lastUpdated;
    private String lastUpdatedString;
    private String tags;
    private Long parentId;
    private UserMsg owner;
}
