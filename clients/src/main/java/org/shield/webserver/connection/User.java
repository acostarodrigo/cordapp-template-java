package org.shield.webserver.connection;

import javax.persistence.Entity;
import javax.validation.constraints.NotEmpty;

@Entity
public class User {
    public User() {
    }

    public User(@NotEmpty(message = "Please provide a valid server name") String serverName, @NotEmpty(message = "Please provide a valid username") String username, @NotEmpty(message = "Please provide a valid password") String password) {
        this.serverName = serverName;
        this.username = username;
        this.password = password;
    }

    @NotEmpty(message = "Please provide a valid server name")
    private String serverName;
    @NotEmpty(message = "Please provide a valid username")
    private String username;
    @NotEmpty(message = "Please provide a valid password")
    private String password;

    public String getServerName() {
        return serverName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
