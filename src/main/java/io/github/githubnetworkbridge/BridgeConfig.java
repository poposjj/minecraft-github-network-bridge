package io.github.githubnetworkbridge;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public record BridgeConfig(
        boolean enabled,
        String profileName,
        String profileDescription,
        String subscriptionUrl,
        String selectedProxyName,
        String subscriptionUserAgent,
        int subscriptionTimeoutSeconds,
        int subscriptionUpdateMinutes,
        boolean subscriptionUseSystemProxy,
        boolean subscriptionUseKernelProxy,
        boolean subscriptionAllowInsecure,
        boolean subscriptionAutoUpdate,
        int localProxyPort,
        Set<String> githubDomains,
        Duration startupTimeout,
        URI testUrl,
        Duration connectTimeout) {

    public static final String FILE_NAME = "minecraft-github-network-bridge.properties";
    public static final String LEGACY_FILE_NAME = "github-network-bridge.properties";
    public static final String DEFAULT_USER_AGENT = "clash.meta";
    public static final int DEFAULT_LOCAL_PORT = 17897;
    private static final String DEFAULT_DOMAINS = String.join(",",
            "github.com", "api.github.com", "codeload.github.com",
            "raw.githubusercontent.com", "objects.githubusercontent.com",
            "release-assets.githubusercontent.com", "githubusercontent.com",
            "githubassets.com", "github.io");

    public static BridgeConfig load(Path path) throws IOException {
        if (Files.notExists(path)) {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, defaultFile(), StandardCharsets.UTF_8);
        }

        Properties values = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            values.load(reader);
        }

        Set<String> domains = Arrays.stream(value(values, "githubDomains", DEFAULT_DOMAINS).split(","))
                .map(String::trim)
                .map(domain -> domain.toLowerCase(Locale.ROOT))
                .filter(domain -> !domain.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (domains.isEmpty()) {
            throw new IOException("githubDomains 至少需要包含一个域名");
        }

        String subscriptionUrl = value(values, "subscriptionUrl", "");
        validateSubscriptionUrl(subscriptionUrl, true);
        boolean legacyUseCurrentProxy = bool(values, "subscriptionUseCurrentProxy", true);
        return new BridgeConfig(
                bool(values, "enabled", true),
                value(values, "profileName", "Minecraft GitHub"),
                value(values, "profileDescription", ""),
                subscriptionUrl,
                value(values, "selectedProxyName", ""),
                value(values, "subscriptionUserAgent", DEFAULT_USER_AGENT),
                integer(values, "subscriptionTimeoutSeconds", 15, 3, 120),
                integer(values, "subscriptionUpdateMinutes", 360, 5, 10080),
                bool(values, "subscriptionUseSystemProxy", legacyUseCurrentProxy),
                bool(values, "subscriptionUseKernelProxy", legacyUseCurrentProxy),
                bool(values, "subscriptionAllowInsecure", false),
                bool(values, "subscriptionAutoUpdate", true),
                integer(values, "localProxyPort", DEFAULT_LOCAL_PORT, 1024, 65535),
                Set.copyOf(domains),
                Duration.ofSeconds(integer(values, "startupTimeoutSeconds", 20, 3, 120)),
                URI.create(value(values, "testUrl", "https://api.github.com/rate_limit")),
                Duration.ofSeconds(integer(values, "connectTimeoutSeconds", 10, 3, 120)));
    }

    public void save(Path path) throws IOException {
        Properties values = new Properties();
        values.setProperty("enabled", Boolean.toString(enabled));
        values.setProperty("profileName", profileName);
        values.setProperty("profileDescription", profileDescription);
        values.setProperty("subscriptionUrl", subscriptionUrl);
        values.setProperty("selectedProxyName", selectedProxyName);
        values.setProperty("subscriptionUserAgent", subscriptionUserAgent);
        values.setProperty("subscriptionTimeoutSeconds", Integer.toString(subscriptionTimeoutSeconds));
        values.setProperty("subscriptionUpdateMinutes", Integer.toString(subscriptionUpdateMinutes));
        values.setProperty("subscriptionUseSystemProxy", Boolean.toString(subscriptionUseSystemProxy));
        values.setProperty("subscriptionUseKernelProxy", Boolean.toString(subscriptionUseKernelProxy));
        values.setProperty("subscriptionUseCurrentProxy",
                Boolean.toString(subscriptionUseSystemProxy || subscriptionUseKernelProxy));
        values.setProperty("subscriptionAllowInsecure", Boolean.toString(subscriptionAllowInsecure));
        values.setProperty("subscriptionAutoUpdate", Boolean.toString(subscriptionAutoUpdate));
        values.setProperty("localProxyPort", Integer.toString(localProxyPort));
        values.setProperty("githubDomains", String.join(",", githubDomains));
        values.setProperty("startupTimeoutSeconds", Long.toString(startupTimeout.toSeconds()));
        values.setProperty("testUrl", testUrl.toString());
        values.setProperty("connectTimeoutSeconds", Long.toString(connectTimeout.toSeconds()));

        Files.createDirectories(path.getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            values.store(writer, "Private Minecraft GitHub Network Bridge configuration. Do not publish.");
        }
        try {
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public boolean configured() {
        return !subscriptionUrl.isBlank();
    }

    public boolean shouldBridge() {
        return enabled && configured();
    }

    public boolean matches(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return githubDomains.stream().anyMatch(domain ->
                normalized.equals(domain) || normalized.endsWith("." + domain));
    }

    public BridgeConfig withEnabled(boolean newEnabled) {
        return copy(newEnabled, profileName, profileDescription, subscriptionUrl, selectedProxyName,
                subscriptionUserAgent, subscriptionTimeoutSeconds, subscriptionUpdateMinutes,
                subscriptionUseSystemProxy, subscriptionUseKernelProxy,
                subscriptionAllowInsecure, subscriptionAutoUpdate);
    }

    public BridgeConfig withQuickSubscription(String url) throws IOException {
        validateSubscriptionUrl(url, false);
        String name = profileName.isBlank() ? "Minecraft GitHub" : profileName;
        return copy(true, name, profileDescription, url.trim(), "", DEFAULT_USER_AGENT,
                15, 360, true, true, false, true);
    }

    public BridgeConfig withProfile(String name, String description, String url, String userAgent,
                                    int timeoutSeconds, int updateMinutes,
                                     boolean useSystemProxy, boolean useKernelProxy,
                                     boolean allowInsecure,
                                    boolean autoUpdate) throws IOException {
        validateSubscriptionUrl(url, false);
        if (name == null || name.isBlank()) {
            throw new IOException("配置名称不能为空");
        }
        if (timeoutSeconds < 3 || timeoutSeconds > 120) {
            throw new IOException("请求超时必须在 3 到 120 秒之间");
        }
        if (updateMinutes < 5 || updateMinutes > 10080) {
            throw new IOException("更新间隔必须在 5 到 10080 分钟之间");
        }
        String normalizedUrl = url.trim();
        String selected = normalizedUrl.equals(subscriptionUrl) ? selectedProxyName : "";
        return copy(true, name.trim(), description == null ? "" : description.trim(), normalizedUrl,
                selected,
                userAgent == null || userAgent.isBlank() ? DEFAULT_USER_AGENT : userAgent.trim(),
                timeoutSeconds, updateMinutes, useSystemProxy, useKernelProxy,
                allowInsecure, autoUpdate);
    }

    public BridgeConfig withSelectedProxy(String proxyName) {
        return copy(enabled, profileName, profileDescription, subscriptionUrl,
                proxyName == null ? "" : proxyName.trim(), subscriptionUserAgent,
                subscriptionTimeoutSeconds, subscriptionUpdateMinutes,
                subscriptionUseSystemProxy, subscriptionUseKernelProxy,
                subscriptionAllowInsecure, subscriptionAutoUpdate);
    }

    public BridgeConfig withGeneralSettings(boolean newEnabled, int timeoutSeconds,
                                            int updateMinutes) throws IOException {
        if (timeoutSeconds < 3 || timeoutSeconds > 120) {
            throw new IOException("请求超时必须在 3 到 120 秒之间");
        }
        if (updateMinutes < 5 || updateMinutes > 10080) {
            throw new IOException("更新间隔必须在 5 到 10080 分钟之间");
        }
        return copy(newEnabled, profileName, profileDescription, subscriptionUrl,
                selectedProxyName, subscriptionUserAgent, timeoutSeconds, updateMinutes,
                subscriptionUseSystemProxy, subscriptionUseKernelProxy,
                subscriptionAllowInsecure, subscriptionAutoUpdate);
    }

    public BridgeConfig withLocalProxyPort(int port) throws IOException {
        if (port < 1024 || port > 65535) {
            throw new IOException("本地端口必须在 1024 到 65535 之间");
        }
        return new BridgeConfig(enabled, profileName, profileDescription, subscriptionUrl,
                selectedProxyName, subscriptionUserAgent, subscriptionTimeoutSeconds,
                subscriptionUpdateMinutes, subscriptionUseSystemProxy,
                subscriptionUseKernelProxy,
                subscriptionAllowInsecure, subscriptionAutoUpdate, port, githubDomains,
                startupTimeout, testUrl, connectTimeout);
    }

    private BridgeConfig copy(boolean newEnabled, String newName, String newDescription,
                              String newUrl, String newSelectedProxy, String newUserAgent, int newTimeout,
                               int newUpdateMinutes, boolean newUseSystemProxy,
                               boolean newUseKernelProxy,
                              boolean newAllowInsecure, boolean newAutoUpdate) {
        return new BridgeConfig(newEnabled, newName, newDescription, newUrl, newSelectedProxy, newUserAgent,
                newTimeout, newUpdateMinutes, newUseSystemProxy, newUseKernelProxy,
                newAllowInsecure,
                newAutoUpdate, localProxyPort, githubDomains, startupTimeout,
                testUrl, connectTimeout);
    }

    private static void validateSubscriptionUrl(String raw, boolean allowBlank) throws IOException {
        if (raw == null || raw.isBlank()) {
            if (allowBlank) {
                return;
            }
            throw new IOException("订阅链接不能为空");
        }
        try {
            URI uri = URI.create(raw.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null) {
                throw new IOException("订阅链接必须以 http:// 或 https:// 开头");
            }
        } catch (IllegalArgumentException exception) {
            throw new IOException("订阅链接格式无效", exception);
        }
    }

    private static String value(Properties values, String key, String fallback) {
        String value = values.getProperty(key);
        return value == null ? fallback : value.trim();
    }

    private static boolean bool(Properties values, String key, boolean fallback) throws IOException {
        String raw = value(values, key, Boolean.toString(fallback));
        if (!raw.equalsIgnoreCase("true") && !raw.equalsIgnoreCase("false")) {
            throw new IOException(key + " 必须是 true 或 false");
        }
        return Boolean.parseBoolean(raw);
    }

    private static int integer(Properties values, String key, int fallback, int min, int max) throws IOException {
        try {
            int parsed = Integer.parseInt(value(values, key, Integer.toString(fallback)));
            if (parsed < min || parsed > max) {
                throw new IOException(key + " 必须在 " + min + " 到 " + max + " 之间");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IOException(key + " 必须是整数", exception);
        }
    }

    private static String defaultFile() {
        return """
                # Minecraft GitHub Network Bridge private configuration.
                # Paste a Clash/Mihomo YAML subscription in the in-game settings.
                enabled=true
                profileName=Minecraft GitHub
                profileDescription=
                subscriptionUrl=
                selectedProxyName=
                subscriptionUserAgent=clash.meta
                subscriptionTimeoutSeconds=15
                subscriptionUpdateMinutes=360
                subscriptionUseSystemProxy=true
                subscriptionUseKernelProxy=true
                subscriptionUseCurrentProxy=true
                subscriptionAllowInsecure=false
                subscriptionAutoUpdate=true
                localProxyPort=17897
                githubDomains=%s
                startupTimeoutSeconds=20
                testUrl=https://api.github.com/rate_limit
                connectTimeoutSeconds=10
                """.formatted(DEFAULT_DOMAINS);
    }
}
