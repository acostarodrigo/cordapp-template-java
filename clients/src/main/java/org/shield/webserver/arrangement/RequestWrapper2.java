package org.shield.webserver.arrangement;

import org.shield.webserver.connection.User;

import javax.persistence.Entity;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

@Entity
public class RequestWrapper2 {
    @NotNull(message = "Please provide a valid user")
    private User user;
    @NotEmpty(message = "Please provide a valid arrangement id")
    private String id;
    private int size;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
