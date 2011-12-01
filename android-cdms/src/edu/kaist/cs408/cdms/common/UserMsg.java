/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.kaist.cs408.cdms.common;

import java.io.Serializable;

/**
 * A shared entity between components.
 * 
 * @author Trung
 */
public class UserMsg implements Serializable {
    private static final long serialVersionUID = 1L;

    public UserMsg() {}
    
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private String name;
    private String username;
    private String password;
    private String email;
    private Long id;
}
