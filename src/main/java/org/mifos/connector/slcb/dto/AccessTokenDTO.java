package org.mifos.connector.slcb.dto;

public class AccessTokenDTO {

    public String username;
    public String password;
    public String token;

    public int expiresIn;

    public AccessTokenDTO() {
    }

    public AccessTokenDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public AccessTokenDTO(String username, String password, int expiresIn) {
        this.username = username;
        this.password = password;
        this.expiresIn = expiresIn;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
