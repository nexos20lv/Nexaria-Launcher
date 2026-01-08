package com.nexaria.launcher.model;

public class User {
    private final String id;
    private final String username;
    private final String accessToken;

    public User(String id, String username, String accessToken) {
        this.id = id;
        this.username = username;
        this.accessToken = accessToken;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getAccessToken() { return accessToken; }
}
