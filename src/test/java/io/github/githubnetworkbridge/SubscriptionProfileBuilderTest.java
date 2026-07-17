package io.github.githubnetworkbridge;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriptionProfileBuilderTest {
    @Test
    void generatesOneLocalPortAndOneMinecraftGitHubGroup() throws Exception {
        String source = """
                mixed-port: 7897
                allow-lan: true
                tun:
                  enable: true
                proxies:
                  - name: Node A
                    type: socks5
                    server: 127.0.0.1
                    port: 1080
                  - name: Node B
                    type: socks5
                    server: 127.0.0.1
                    port: 1081
                rules:
                  - MATCH,DIRECT
                """;

        Map<?, ?> result = parse(SubscriptionProfileBuilder.build(source, 17897, "Node B"));
        assertEquals(17897, result.get("mixed-port"));
        assertEquals(false, result.get("allow-lan"));
        assertEquals("127.0.0.1", result.get("bind-address"));
        assertFalse(result.containsKey("socks-port"));
        assertFalse(result.containsKey("redir-port"));
        assertEquals(false, ((Map<?, ?>) result.get("tun")).get("enable"));
        assertEquals(List.of("MATCH,Minecraft GitHub Bridge"), result.get("rules"));

        List<?> groups = (List<?>) result.get("proxy-groups");
        Map<?, ?> group = (Map<?, ?>) groups.getFirst();
        assertEquals("Minecraft GitHub Bridge", group.get("name"));
        assertEquals("select", group.get("type"));
        assertEquals(List.of("Node B", "Node A"), group.get("proxies"));
        assertEquals(List.of("Node A", "Node B"),
                SubscriptionProfileBuilder.availableProxyNames(source));
    }

    @Test
    void acceptsProviderBasedSubscriptions() throws Exception {
        String source = """
                proxy-providers:
                  provider-a:
                    type: http
                    url: https://provider.example/subscription
                    path: ./provider-a.yaml
                """;
        Map<?, ?> result = parse(SubscriptionProfileBuilder.build(source, 17897, ""));
        List<?> groups = (List<?>) result.get("proxy-groups");
        Map<?, ?> group = (Map<?, ?>) groups.getFirst();
        assertNotNull(group);
        assertTrue(((List<?>) group.get("use")).contains("provider-a"));
    }

    @SuppressWarnings("unchecked")
    private static Map<?, ?> parse(String yaml) {
        Object value = new Yaml().load(yaml);
        return (Map<?, ?>) value;
    }
}
