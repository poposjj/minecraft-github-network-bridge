package io.github.githubnetworkbridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GitHubProxySelectorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void proxiesOnlyGitHubAndAlwaysKeepsMinecraftTrafficDirect() throws Exception {
        BridgeConfig config = BridgeConfig.load(temporaryDirectory.resolve(BridgeConfig.FILE_NAME))
                .withQuickSubscription("https://subscription.example/clash.yaml");
        ProxySelector delegate = new FixedSelector(new Proxy(Proxy.Type.HTTP,
                java.net.InetSocketAddress.createUnresolved("system-proxy", 8080)));
        GitHubProxySelector selector = new GitHubProxySelector(delegate, new AtomicReference<>(config));

        assertEquals(Proxy.Type.HTTP,
                selector.select(URI.create("https://api.github.com/repos")).getFirst().type());
        assertEquals(Proxy.Type.HTTP,
                selector.select(URI.create("https://raw.githubusercontent.com/file")).getFirst().type());
        assertEquals(Proxy.NO_PROXY,
                selector.select(URI.create("https://hypixel.net/")).getFirst());
        assertEquals(Proxy.NO_PROXY,
                selector.select(URI.create("https://sessionserver.mojang.com/")).getFirst());
    }

    @Test
    void disabledBridgeReturnsControlToTheOriginalSelector() throws Exception {
        BridgeConfig config = BridgeConfig.load(temporaryDirectory.resolve(BridgeConfig.FILE_NAME))
                .withQuickSubscription("https://subscription.example/clash.yaml")
                .withEnabled(false);
        Proxy expected = new Proxy(Proxy.Type.HTTP,
                java.net.InetSocketAddress.createUnresolved("original", 3128));
        GitHubProxySelector selector = new GitHubProxySelector(
                new FixedSelector(expected), new AtomicReference<>(config));
        assertEquals(expected, selector.select(URI.create("https://api.github.com/")).getFirst());
    }

    private static final class FixedSelector extends ProxySelector {
        private final Proxy proxy;

        private FixedSelector(Proxy proxy) {
            this.proxy = proxy;
        }

        @Override
        public List<Proxy> select(URI uri) {
            return List.of(proxy);
        }

        @Override
        public void connectFailed(URI uri, java.net.SocketAddress address, java.io.IOException exception) {
        }
    }
}
