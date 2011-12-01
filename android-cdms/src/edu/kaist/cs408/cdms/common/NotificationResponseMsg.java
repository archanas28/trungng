/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.kaist.cs408.cdms.common;

import java.io.Serializable;

/**
 * Message to communicate the acceptance/rejection event of a notification.
 */
public class NotificationResponseMsg implements Serializable {

    /**
     * Default constructor
     */
    public NotificationResponseMsg() {}

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }
    
    private Long notificationId;
    private Long senderId;
    private Long receiverId;
    private Long fileId;
    private Boolean accepted;
}
