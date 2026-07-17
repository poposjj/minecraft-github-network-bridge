package io.github.githubnetworkbridge;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public final class BridgeRuntime {
    public static final BridgeRuntime INSTANCE = new BridgeRuntime();
    public static final Logger LOGGER = LoggerFactory.getLogger("MinecraftGitHubNetworkBridge");

    private final AtomicReference<BridgeConfig> config = new AtomicReference<>();
    private final ExecutorService runtimeExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "minecraft-github-connection");
        thread.setDaemon(true);
        return thread;
    });
    private Path configPath;
    private ManagedMihomoRuntime managedRuntime;
    private GitHubProxySelector installedSelector;
    private CompletableFuture<Void> lastPreparation = CompletableFuture.completedFuture(null);

    private BridgeRuntime() {
    }

    public synchronized void initialize() {
        if (installedSelector != null) {
            return;
        }
        Path configDirectory = FabricLoader.getInstance().getConfigDir();
        configPath = configDirectory.resolve(BridgeConfig.FILE_NAME);
        try {
            BridgeConfig loaded = BridgeConfig.load(configPath);
            config.set(loaded);
            ProxySelector originalSelector = ProxySelector.getDefault();
            managedRuntime = new ManagedMihomoRuntime(configDirectory, LOGGER, originalSelector);
            installedSelector = new GitHubProxySelector(originalSelector, config);
            ProxySelector.setDefault(installedSelector);
            try {
                BridgeConfig active = managedRuntime.ensureAvailable(loaded, false);
                persistRuntimePortIfChanged(loaded, active);
                config.set(active);
            } catch (IOException exception) {
                LOGGER.warn("我的世界 GitHub 内置连接尚未就绪：{}",
                        exception.getMessage());
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                runtimeExecutor.shutdownNow();
                managedRuntime.stop();
            }, "minecraft-github-bridge-shutdown"));
            LOGGER.info("我的世界 GitHub 网络桥接已加载；配置文件={}", configPath);
        } catch (Exception exception) {
            LOGGER.error("我的世界 GitHub 网络桥接初始化失败", exception);
        }
    }

    public synchronized BridgeConfig reload() throws IOException {
        BridgeConfig loaded = BridgeConfig.load(configPath);
        applyLoaded(loaded, true);
        return loaded;
    }

    public synchronized BridgeConfig saveAndApply(BridgeConfig updated) throws IOException {
        return saveAndApply(updated, true);
    }

    public synchronized BridgeConfig saveAndApply(BridgeConfig updated,
                                                   boolean refreshSubscription) throws IOException {
        updated.save(configPath);
        BridgeConfig loaded = BridgeConfig.load(configPath);
        applyLoaded(loaded, refreshSubscription);
        return loaded;
    }

    public BridgeConfig config() {
        return config.get();
    }

    public Path configPath() {
        return configPath;
    }

    public boolean builtInConnectionRunning() {
        return managedRuntime != null && managedRuntime.isAlive();
    }

    public boolean endpointReady() {
        BridgeConfig current = config.get();
        return current != null && managedRuntime != null
                && managedRuntime.isEndpointReady(current.localProxyPort());
    }

    public List<String> availableProxyNames() {
        if (managedRuntime == null) {
            return List.of();
        }
        try {
            return managedRuntime.availableProxyNames();
        } catch (IOException exception) {
            LOGGER.warn("无法读取代理组缓存：{}", exception.getMessage());
            return List.of();
        }
    }

    public synchronized CompletableFuture<List<String>> refreshProxyNames() {
        BridgeConfig current = config.get();
        if (current == null || !current.configured()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("请先创建订阅配置"));
        }
        applyLoaded(current, true);
        return lastPreparation.thenApply(ignored -> availableProxyNames());
    }

    public synchronized CompletableFuture<List<String>> proxyNamesWhenReady() {
        return lastPreparation.thenApply(ignored -> availableProxyNames());
    }

    public synchronized CompletableFuture<Map<String, Integer>> measureProxyLatencies() {
        if (managedRuntime == null || availableProxyNames().isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("请先刷新代理组"));
        }
        return lastPreparation.thenApplyAsync(ignored -> {
            try {
                return managedRuntime.measureProxyLatencies(Duration.ofSeconds(5));
            } catch (IOException exception) {
                throw new CompletionException(exception);
            }
        }, runtimeExecutor);
    }

    public CompletableFuture<TestResult> test() {
        BridgeConfig current = config.get();
        if (current == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("网络桥接尚未初始化"));
        }
        if (!current.configured()) {
            return CompletableFuture.failedFuture(new IllegalStateException("请先创建订阅配置"));
        }
        CompletableFuture<Void> preparation;
        synchronized (this) {
            preparation = lastPreparation;
        }
        return preparation.thenCompose(ignored -> {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(current.connectTimeout())
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(current.testUrl())
                    .timeout(current.connectTimeout())
                    .header("User-Agent", "Minecraft-GitHub-Network-Bridge/1.1")
                    .GET()
                    .build();
            long started = System.nanoTime();
            return client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenApply(response -> new TestResult(
                            response.statusCode(),
                            (System.nanoTime() - started) / 1_000_000,
                            current.testUrl().toString()));
        });
    }

    private void applyLoaded(BridgeConfig loaded, boolean forceRefresh) {
        config.set(loaded);
        lastPreparation = lastPreparation.handle((ignored, error) -> null)
                .thenRunAsync(() -> {
                    try {
                        BridgeConfig active = managedRuntime.ensureAvailable(loaded, forceRefresh);
                        persistRuntimePortIfChanged(loaded, active);
                        config.set(active);
                    } catch (IOException exception) {
                        throw new CompletionException(exception);
                    }
                }, runtimeExecutor);
    }

    private void persistRuntimePortIfChanged(BridgeConfig requested, BridgeConfig active)
            throws IOException {
        if (requested.localProxyPort() != active.localProxyPort()) {
            active.save(configPath);
        }
    }

    public record TestResult(int statusCode, long elapsedMillis, String url) {
    }
}
