package com.smartparcelguard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spg")
public class SmartParcelGuardProperties {

    private final Simulation simulation = new Simulation();
    private final Storage storage = new Storage();
    private final Device device = new Device();
    private final Jwt jwt = new Jwt();

    public Simulation getSimulation() {
        return simulation;
    }

    public Storage getStorage() {
        return storage;
    }

    public Device getDevice() {
        return device;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public static class Simulation {

        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Storage {

        private String imageDir = "../storage/images";

        public String getImageDir() {
            return imageDir;
        }

        public void setImageDir(String imageDir) {
            this.imageDir = imageDir;
        }
    }

    public static class Device {

        private int offlineAfterSeconds = 180;

        public int getOfflineAfterSeconds() {
            return offlineAfterSeconds;
        }

        public void setOfflineAfterSeconds(int offlineAfterSeconds) {
            this.offlineAfterSeconds = offlineAfterSeconds;
        }
    }

    public static class Jwt {

        private String secret;
        private int expirationMinutes = 120;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public int getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(int expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }
    }
}
