package io.github.githubnetworkbridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivateSubscriptionCompatibilityTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    @EnabledIfEnvironmentVariable(named = "MINECRAFT_GITHUB_SUBSCRIPTION_FIXTURE", matches = ".+")
    void convertsAndSelectsAPrivateProxyWithoutPrintingIt() throws Exception {
        Path fixture = Path.of(System.getenv("MINECRAFT_GITHUB_SUBSCRIPTION_FIXTURE"));
        String source = Files.readString(fixture);
        List<String> names = SubscriptionProfileBuilder.availableProxyNames(source);
        String selected = names.get(1);
        String generated = SubscriptionProfileBuilder.build(source, 17897, selected);
        Map<?, ?> result = (Map<?, ?>) new Yaml().load(generated);

        assertEquals(17897, result.get("mixed-port"));
        assertEquals(false, result.get("allow-lan"));
        assertFalse(((Map<?, ?>) result.get("tun")).containsValue(true));
        List<?> groups = (List<?>) result.get("proxy-groups");
        Map<?, ?> bridgeGroup = (Map<?, ?>) groups.getFirst();
        assertEquals("select", bridgeGroup.get("type"));
        assertEquals(selected, ((List<?>) bridgeGroup.get("proxies")).getFirst());
        assertTrue(names.size() > 1);
        assertFalse(SubscriptionProfileBuilder.availableProxyEndpoints(source).isEmpty());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "MINECRAFT_GITHUB_SUBSCRIPTION_FIXTURE", matches = ".+")
    void measuresPrivateProxyEndpointsWithoutPrintingThem() throws Exception {
        Path runtimeDirectory = temporaryDirectory
                .resolve("minecraft-github-network-bridge").resolve("runtime");
        Files.createDirectories(runtimeDirectory);
        Files.copy(Path.of(System.getenv("MINECRAFT_GITHUB_SUBSCRIPTION_FIXTURE")),
                runtimeDirectory.resolve("subscription-source.yaml"));
        ManagedMihomoRuntime runtime = new ManagedMihomoRuntime(
                temporaryDirectory, LoggerFactory.getLogger("private-latency-test"), null);

        Map<String, Integer> latencies = runtime.measureProxyLatencies(Duration.ofSeconds(2));
        assertFalse(latencies.isEmpty());
        assertTrue(latencies.values().stream().anyMatch(value -> value > 0));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "MINECRAFT_GITHUB_TEST_CONFIG", matches = ".+")
    void selectsFastestWorkingProxyOnlyInThePrivateTestConfiguration() throws Exception {
        Path configPath = Path.of(System.getenv("MINECRAFT_GITHUB_TEST_CONFIG"));
        ManagedMihomoRuntime runtime = new ManagedMihomoRuntime(
                configPath.getParent(), LoggerFactory.getLogger("private-selection-test"), null);
        Map<String, Integer> latencies = runtime.measureProxyLatencies(Duration.ofSeconds(5));
        Map.Entry<String, Integer> fastest = latencies.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .min(Map.Entry.comparingByValue())
                .orElseThrow();

        BridgeConfig.load(configPath).withSelectedProxy(fastest.getKey()).save(configPath);
        assertEquals(fastest.getKey(), BridgeConfig.load(configPath).selectedProxyName());
    }
}
