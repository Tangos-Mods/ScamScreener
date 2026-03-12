package eu.tango.scamscreener.profiler.web;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

final class TangoWebApiConfigResolver {
    private static final String DEFAULT_HOST = "0.0.0.0";
    private static final int DEFAULT_PORT = 8080;

    private TangoWebApiConfigResolver() {
    }

    static URI resolveProfilerUri(String modId, String staticPrefix, String pageName) {
        Properties properties = loadProperties();
        String host = firstNonBlank(
            properties.getProperty("host"),
            System.getProperty("tango.webapi.host"),
            System.getenv("TANGO_WEBAPI_HOST"),
            DEFAULT_HOST
        );
        int port = parseInt(
            firstNonBlank(
                properties.getProperty("port"),
                System.getProperty("tango.webapi.port"),
                System.getenv("TANGO_WEBAPI_PORT"),
                String.valueOf(DEFAULT_PORT)
            ),
            DEFAULT_PORT
        );

        return buildProfilerUri(host, port, modId, staticPrefix, pageName);
    }

    static URI buildProfilerUri(String host, int port, String modId, String staticPrefix, String pageName) {
        String path = "/" + normalizeSegment(modId) + "/" + normalizeSegment(staticPrefix) + "/" + normalizeSegment(pageName);
        try {
            return new URI("http", null, normalizeBrowserHost(host), port, path, null, null);
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Could not build TangoWebAPI profiler URL.", exception);
        }
    }

    static String normalizeBrowserHost(String host) {
        if (host == null || host.isBlank()) {
            return "127.0.0.1";
        }

        String normalized = host.trim();
        if ("0.0.0.0".equals(normalized) || "::".equals(normalized) || "[::]".equals(normalized) || "*".equals(normalized)) {
            return "127.0.0.1";
        }

        return normalized;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        Path configPath = resolveConfigPath();
        if (!Files.isRegularFile(configPath) || !Files.isReadable(configPath)) {
            return properties;
        }

        try (InputStream inputStream = Files.newInputStream(configPath)) {
            properties.load(inputStream);
        } catch (IOException ignored) {
            return new Properties();
        }

        return properties;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        return "";
    }

    private static int parseInt(String rawValue, int fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }

        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String normalizeSegment(String segment) {
        if (segment == null || segment.isBlank()) {
            return "";
        }

        String normalized = segment.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static Path resolveConfigPath() {
        try {
            return FabricLoader.getInstance().getConfigDir().resolve("tango-webapi.properties");
        } catch (Throwable ignored) {
            return Paths.get("config", "tango-webapi.properties");
        }
    }
}
