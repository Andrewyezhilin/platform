package com.smartlife.utils;

/**
 * 常用格式校验。
 */
public final class RegexUtils {

    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
    private static final String CODE_REGEX = "^\\d{6}$";

    private RegexUtils() {
    }

    public static boolean isPhoneInvalid(String phone) {
        return phone == null || !phone.matches(PHONE_REGEX);
    }

    public static boolean isCodeInvalid(String code) {
        return code == null || !code.matches(CODE_REGEX);
    }
}
