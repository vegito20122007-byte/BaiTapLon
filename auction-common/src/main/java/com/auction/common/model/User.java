package com.auction.common.model;

import com.auction.common.enums.UserRole;

/**
 * Lớp User trừu tượng kế thừa từ Entity.
 */
public abstract class User extends Entity {
    private static final long serialVersionUID = 1L;

    protected String username;
    protected String password;
    protected String fullName;
    protected String email;
    protected UserRole role;

    public User() {
        super();
    }

    public User(Long id, String username, String password, String fullName, String email, UserRole role) {
        super(id);
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    @Override
    public String getSummary() {
        return String.format("[%s] %s (%s)", role, fullName, username);
    }
}
