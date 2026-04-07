package com.axiom.antivpn.common.util;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public interface IpUtil {

    Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");

    Pattern IPV6 = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|" +
            "^::([0-9a-fA-F]{1,4}:){0,6}[0-9a-fA-F]{1,4}$|" +
            "^([0-9a-fA-F]{1,4}:){1,7}:$|" +
            "^([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}$");

    static boolean isValidIp(@NotNull String ip) {
        return IPV4.matcher(ip).matches() || IPV6.matcher(ip).matches();
    }

    static boolean isLocalIp(@NotNull String ip) {
        return ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")
                || ip.equals("::1") || ip.startsWith("192.168.")
                || ip.startsWith("10.") || ip.startsWith("172.");
    }
}
