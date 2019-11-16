package org.shield.webserver.connection;

import javax.persistence.Entity;
import javax.validation.constraints.NotEmpty;

@Entity
public class User {
    @NotEmpty(message = "Please provide a valid username")
    private String username;
    @NotEmpty(message = "Please provide a valid password")
    private String password;

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
