package io.github.githubnetworkbridge;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SubscriptionProfileBuilder {
    private static final String GROUP_NAME = "Minecraft GitHub Bridge";

    private SubscriptionProfileBuilder() {
    }

    static String build(String subscriptionYaml, int port, String selectedProxyName) throws IOException {
        Map<String, Object> root = loadRoot(subscriptionYaml);
        List<String> proxyNames = proxyNames(root.get("proxies"));
        List<String> providerNames = providerNames(root.get("proxy-providers"));
        if (proxyNames.isEmpty() && providerNames.isEmpty()) {
            throw new IOException("订阅中没有找到代理节点，请使用 Clash/Mihomo YAML 订阅链接");
        }
        if (selectedProxyName != null && proxyNames.remove(selectedProxyName)) {
            proxyNames.addFirst(selectedProxyName);
        }

        root.remove("port");
        root.remove("socks-port");
        root.remove("redir-port");
        root.remove("tproxy-port");
        root.remove("listeners");
        root.remove("external-controller-tls");
        root.put("mixed-port", port);
        root.put("allow-lan", false);
        root.put("bind-address", "127.0.0.1");
        root.put("external-controller", "");
        root.put("mode", "rule");
        root.put("log-level", "warning");
        root.put("ipv6", false);
        root.put("tun", Map.of("enable", false));
        root.put("dns", Map.of("enable", false));

        List<Object> groups = new ArrayList<>();
        Object existingGroups = root.get("proxy-groups");
        if (existingGroups instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> map)
                        || !GROUP_NAME.equals(String.valueOf(map.get("name")))) {
                    groups.add(item);
                }
            }
        }

        Map<String, Object> bridgeGroup = new LinkedHashMap<>();
        bridgeGroup.put("name", GROUP_NAME);
        bridgeGroup.put("type", "select");
        if (!proxyNames.isEmpty()) {
            bridgeGroup.put("proxies", proxyNames);
        }
        if (!providerNames.isEmpty()) {
            bridgeGroup.put("use", providerNames);
        }
        groups.addFirst(bridgeGroup);
        root.put("proxy-groups", groups);
        root.put("rules", List.of("MATCH," + GROUP_NAME));
        return dump(root);
    }

    static String buildLatencyProfile(String subscriptionYaml, int mixedPort,
                                      int controllerPort, String secret) throws IOException {
        Map<String, Object> source = loadRoot(subscriptionYaml);
        Object proxies = source.get("proxies");
        List<String> names = proxyNames(proxies);
        if (names.isEmpty()) {
            throw new IOException("订阅中没有可用于测速的直接代理节点");
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("mixed-port", mixedPort);
        root.put("allow-lan", false);
        root.put("bind-address", "127.0.0.1");
        root.put("external-controller", "127.0.0.1:" + controllerPort);
        root.put("secret", secret);
        root.put("mode", "rule");
        root.put("log-level", "warning");
        root.put("ipv6", false);
        root.put("tun", Map.of("enable", false));
        root.put("dns", Map.of("enable", false));
        root.put("proxies", proxies);
        root.put("proxy-groups", List.of(Map.of(
                "name", GROUP_NAME,
                "type", "select",
                "proxies", names)));
        root.put("rules", List.of("MATCH," + GROUP_NAME));
        return dump(root);
    }

    static List<String> availableProxyNames(String subscriptionYaml) throws IOException {
        return List.copyOf(proxyNames(loadRoot(subscriptionYaml).get("proxies")));
    }

    static List<ProxyEndpoint> availableProxyEndpoints(String subscriptionYaml) throws IOException {
        List<ProxyEndpoint> endpoints = new ArrayList<>();
        Object value = loadRoot(subscriptionYaml).get("proxies");
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }
                String name = stringValue(map.get("name"));
                String server = stringValue(map.get("server"));
                int port = integerValue(map.get("port"));
                if (!name.isBlank() && !server.isBlank() && port > 0 && port <= 65535) {
                    endpoints.add(new ProxyEndpoint(name, server, port));
                }
            }
        }
        return List.copyOf(endpoints);
    }

    private static Map<String, Object> loadRoot(String subscriptionYaml) throws IOException {
        Object loaded;
        try {
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(true);
            options.setCodePointLimit(16 * 1024 * 1024);
            loaded = new Yaml(options).load(subscriptionYaml);
        } catch (RuntimeException exception) {
            throw new IOException("订阅返回的内容不是有效的 Clash/Mihomo YAML", exception);
        }
        if (!(loaded instanceof Map<?, ?> source)) {
            throw new IOException("订阅没有返回 Clash/Mihomo 配置");
        }
        return stringKeyMap(source);
    }

    private static List<String> proxyNames(Object value) {
        List<String> names = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map && map.get("name") != null) {
                    String name = String.valueOf(map.get("name")).trim();
                    if (!name.isEmpty()) {
                        names.add(name);
                    }
                }
            }
        }
        return names;
    }

    private static List<String> providerNames(Object value) {
        List<String> names = new ArrayList<>();
        if (value instanceof Map<?, ?> map) {
            for (Object key : map.keySet()) {
                String name = String.valueOf(key).trim();
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private static Map<String, Object> stringKeyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private static String dump(Map<String, Object> root) {
        DumperOptions dumpOptions = new DumperOptions();
        dumpOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumpOptions.setIndent(2);
        dumpOptions.setPrettyFlow(true);
        return new Yaml(dumpOptions).dump(root);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static int integerValue(Object value) {
        try {
            return Integer.parseInt(stringValue(value));
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    record ProxyEndpoint(String name, String server, int port) {
    }
}
