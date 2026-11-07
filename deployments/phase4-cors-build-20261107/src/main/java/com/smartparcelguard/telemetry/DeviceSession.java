package com.smartparcelguard.telemetry;

public final class DeviceSession {

    public static final String TOKEN_HEADER = "X-Device-Token";

    private static final ThreadLocal<AuthenticatedDevice> CURRENT = new ThreadLocal<>();

    private DeviceSession() {
    }

    public static void set(AuthenticatedDevice device) {
        CURRENT.set(device);
    }

    public static AuthenticatedDevice require() {
        AuthenticatedDevice current = CURRENT.get();
        if (current == null) {
            throw new IllegalStateException("Device authentication is missing");
        }
        return current;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
