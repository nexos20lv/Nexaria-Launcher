package com.nexaria.launcher.model;

public class User {
    private final String id;
    private final String username;
    private final String accessToken;
    private final String uuid;
    private final boolean emailVerified;
    private final double money;
    private final String roleName;
    private final String roleColor;
    private final boolean banned;
    private final String createdAt;

    public User(String id, String username, String accessToken, String uuid, 
                boolean emailVerified, double money, String roleName, 
                String roleColor, boolean banned, String createdAt) {
        this.id = id;
        this.username = username;
        this.accessToken = accessToken;
        this.uuid = uuid;
        this.emailVerified = emailVerified;
        this.money = money;
        this.roleName = roleName;
        this.roleColor = roleColor;
        this.banned = banned;
        this.createdAt = createdAt;
    }

    // Constructor pour compatibilité
    public User(String id, String username, String accessToken) {
        this(id, username, accessToken, null, false, 0.0, null, null, false, null);
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getAccessToken() { return accessToken; }
    public String getUuid() { return uuid; }
    public boolean isEmailVerified() { return emailVerified; }
    public double getMoney() { return money; }
    public String getRoleName() { return roleName; }
    public String getRoleColor() { return roleColor; }
    public boolean isBanned() { return banned; }
    public String getCreatedAt() { return createdAt; }
}
