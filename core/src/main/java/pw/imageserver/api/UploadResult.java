/*
 * Copyright (c) 2025 leoms1408
 * Licensed under the Apache License, Version 2.0
 */

package pw.imageserver.api;

public sealed interface UploadResult {

    record Success(String link) implements UploadResult {}

    /**
     * @param detail optional, untranslatable extra information (e.g. a short server message), may be null
     */
    record Failure(Reason reason, String detail) implements UploadResult {}

    enum Reason {
        FILE("file"),
        INVALID_KEY("invalidKey"),
        TOO_LARGE("tooLarge"),
        RATE_LIMITED("rateLimited"),
        REJECTED("rejected"),
        SERVER("server"),
        NETWORK("network"),
        TIMEOUT("timeout"),
        INVALID_RESPONSE("invalidResponse"),
        UNKNOWN("unknown");

        private final String translationKey;

        Reason(String key) {
            this.translationKey = "imageserver.errors." + key;
        }

        public String translationKey() {
            return translationKey;
        }
    }
}
