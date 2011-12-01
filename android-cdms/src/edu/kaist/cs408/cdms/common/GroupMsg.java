/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.kaist.cs408.cdms.common;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Message that represents group for passing between components.
 * 
 * @author Trung
 */
public class GroupMsg implements Serializable {
    // default constructor
    public GroupMsg() {}

    public ArrayList<UserMsg> getFriends() {
        return friends;
    }

    public void setFriends(ArrayList<UserMsg> friends) {
        this.friends = friends;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private Long id;
    private String name;
    private ArrayList<UserMsg> friends;
}
