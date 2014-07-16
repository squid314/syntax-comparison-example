package com.squid314.examples.collections;

import java.util.List;

/** Super simple user object. */
public class User {
    /** user's login name */
    private String username;
    /** is the user an admin? */
    private boolean admin;
    /** what role does this use serve? */
    private Role role;
    /** to which groups does this user belong? */
    private List<Group> groups;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }
}
