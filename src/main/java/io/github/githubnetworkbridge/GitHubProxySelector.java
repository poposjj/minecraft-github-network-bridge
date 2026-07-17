package io.github.githubnetworkbridge;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class GitHubProxySelector extends ProxySelector {
    private final ProxySelector delegate;
    private final AtomicReference<BridgeConfig> config;

    public GitHubProxySelector(ProxySelector delegate, AtomicReference<BridgeConfig> config) {
        this.delegate = delegate;
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public List<Proxy> select(URI uri) {
        Objects.requireNonNull(uri, "uri");
        BridgeConfig current = config.get();
        if (!current.enabled()) {
            return delegated(uri);
        }
        if (!current.configured() || !current.matches(uri.getHost())) {
            return List.of(Proxy.NO_PROXY);
        }
        return List.of(new Proxy(Proxy.Type.HTTP,
                InetSocketAddress.createUnresolved("127.0.0.1", current.localProxyPort())));
    }

    @Override
    public void connectFailed(URI uri, SocketAddress address, IOException exception) {
        if (delegate != null) {
            delegate.connectFailed(uri, address, exception);
        }
    }

    private List<Proxy> delegated(URI uri) {
        if (delegate == null || delegate == this) {
            return List.of(Proxy.NO_PROXY);
        }
        List<Proxy> proxies = delegate.select(uri);
        return proxies == null || proxies.isEmpty() ? List.of(Proxy.NO_PROXY) : proxies;
    }
}
