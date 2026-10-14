package com.taskmanager.security;

import com.taskmanager.domain.User;

public final class AuthContext {

    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void setUser(User user) {
        CURRENT_USER.set(user);
    }

    public static User getUser() {
        return CURRENT_USER.get();
    }

    public static Long getUserId() {
        User user = CURRENT_USER.get();
        return user == null ? null : user.getId();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
