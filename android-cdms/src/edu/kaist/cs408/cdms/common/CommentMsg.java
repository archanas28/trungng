/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.kaist.cs408.cdms.common;

import java.io.Serializable;
import java.util.Date;

/**
 * Message that represents the content of a file.
 *
 * <p> This is used for downloading the file.
 * 
 * @author Trung
 */
public class CommentMsg implements Serializable {

    // Default constructor
    public CommentMsg() {}

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getPostedTime() {
        return postedTime;
    }

    public void setPostedTime(Date postedTime) {
        this.postedTime = postedTime;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String s) {
        this.userName = s;
    }

    public String getPostedTimeString() {
        return postedTimeString;
    }

    public void setPostedTimeString(String postedTimeString) {
        this.postedTimeString = postedTimeString;
    }
    
    private Long id;
    private Date postedTime;
    private String postedTimeString;
    private String content;
    private Long userId;
    private Long fileId;
    private String userName;
}
