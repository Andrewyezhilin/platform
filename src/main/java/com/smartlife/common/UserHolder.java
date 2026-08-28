package com.smartlife.common;

import com.smartlife.dto.UserDTO;

/**
 * 基于 ThreadLocal 的登录用户上下文。
 */
public final class UserHolder {

    private static final ThreadLocal<UserDTO> TL = new ThreadLocal<>();

    private UserHolder() {
    }

    public static void saveUser(UserDTO user) {
        TL.set(user);
    }

    public static UserDTO getUser() {
        return TL.get();
    }

    public static void removeUser() {
        TL.remove();
    }
}
