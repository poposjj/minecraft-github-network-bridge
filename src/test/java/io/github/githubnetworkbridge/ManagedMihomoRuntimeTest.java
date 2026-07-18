package io.github.githubnetworkbridge;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedMihomoRuntimeTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void choosesAnotherLocalPortWhenClashUsesThePreferredPort() throws Exception {
        byte[] subscription = """
                proxies:
                  - name: Test Node
                    type: socks5
                    server: 127.0.0.1
                    port: 9
                """.getBytes(StandardCharsets.UTF_8);
        HttpServer subscriptionServer = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);
        subscriptionServer.createContext("/subscription", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/yaml");
            exchange.sendResponseHeaders(200, subscription.length);
            exchange.getResponseBody().write(subscription);
            exchange.close();
        });
        subscriptionServer.start();

        ManagedMihomoRuntime runtime = new ManagedMihomoRuntime(
                temporaryDirectory, LoggerFactory.getLogger("port-conflict-test"), null);
        try (ServerSocket occupied = new ServerSocket(0, 50,
                java.net.InetAddress.getLoopbackAddress())) {
            int preferredPort = occupied.getLocalPort();
            BridgeConfig config = BridgeConfig.load(
                            temporaryDirectory.resolve(BridgeConfig.FILE_NAME))
                    .withProfile("Clash coexistence", "", "http://127.0.0.1:"
                                    + subscriptionServer.getAddress().getPort() + "/subscription",
                            "clash.meta", 10, 360, false, false, false, true)
                    .withLocalProxyPort(preferredPort);

            BridgeConfig active = runtime.ensureAvailable(config, false);
            assertNotEquals(preferredPort, active.localProxyPort());
            assertTrue(runtime.isEndpointReady(active.localProxyPort()));
            runtime.stop();
            assertFalse(runtime.isEndpointReady(active.localProxyPort()));
        } finally {
            runtime.stop();
            subscriptionServer.stop(0);
        }
    }
}
