package org.shield.webserver.membership;

import org.shield.webserver.connection.User;

import javax.persistence.Entity;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Entity
public class RequestWrapper2 {
    @NotNull(message = "Please provide a valid user")
    private User user;
    @NotNull(message = "Please provide a valid index number")
    private int index;

    public User getUser() {
        return user;
    }

    public int getIndex() {
        return index;
    }
}
