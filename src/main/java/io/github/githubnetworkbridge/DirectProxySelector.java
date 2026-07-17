package io.github.githubnetworkbridge;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

final class DirectProxySelector extends ProxySelector {
    @Override
    public List<Proxy> select(URI uri) {
        return List.of(Proxy.NO_PROXY);
    }

    @Override
    public void connectFailed(URI uri, SocketAddress address, IOException exception) {
    }
}
