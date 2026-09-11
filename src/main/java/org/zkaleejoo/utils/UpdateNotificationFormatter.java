package org.zkaleejoo.utils;

import java.util.List;

public final class UpdateNotificationFormatter {

    private UpdateNotificationFormatter() {
    }

    public static List<String> format(String prefix, String updateAvailable, String currentVersion,
            String latestVersion, String downloadUrl) {
        if (latestVersion == null || latestVersion.isBlank()
                || currentVersion == null || currentVersion.equalsIgnoreCase(latestVersion)) {
            return List.of();
        }

        String resolvedLatest = latestVersion.trim();
        String resolvedUrl = safe(downloadUrl).trim();

        String message = safe(prefix) + safe(updateAvailable)
                .replace("{version}", resolvedLatest)
                .replace("{link}", resolvedUrl);

        return List.of(message);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
