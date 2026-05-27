package org.zkaleejoo.grave;

import org.bukkit.Particle;

import java.util.Map;
import java.util.Locale;

final class ParticleNameResolver {

    private static final Map<String, String> LEGACY_ALIASES = Map.of(
            "SMOKE_NORMAL", "SMOKE",
            "SMOKE_LARGE", "LARGE_SMOKE",
            "SPELL_WITCH", "WITCH");

    private ParticleNameResolver() {
    }

    static Particle resolve(String configuredName, Particle fallback) {
        String normalizedName = normalize(configuredName);
        if (normalizedName == null) {
            return fallback;
        }

        String particleName = LEGACY_ALIASES.getOrDefault(normalizedName, normalizedName);
        try {
            return Particle.valueOf(particleName);
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    static boolean isKnown(String configuredName) {
        String normalizedName = normalize(configuredName);
        if (normalizedName == null) {
            return false;
        }

        String particleName = LEGACY_ALIASES.getOrDefault(normalizedName, normalizedName);
        try {
            Particle.valueOf(particleName);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static String normalize(String configuredName) {
        if (configuredName == null || configuredName.isBlank()) {
            return null;
        }

        String normalizedName = configuredName.trim();
        int namespaceSeparator = normalizedName.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < normalizedName.length()) {
            normalizedName = normalizedName.substring(namespaceSeparator + 1);
        }

        return normalizedName
                .replace('-', '_')
                .replace('.', '_')
                .toUpperCase(Locale.ROOT);
    }
}
