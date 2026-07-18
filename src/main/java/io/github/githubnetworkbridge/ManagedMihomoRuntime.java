package io.github.githubnetworkbridge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

final class ManagedMihomoRuntime {
    private static final String RESOURCE = "/managed-runtime/mihomo-windows-amd64-compatible.exe";
    private static final String EMBEDDED_SHA256 = "a3799f2d75c623a7c6d307e1faf88269e24dd746c59df3e9f1c84d5cfbff6c92";
    private static final int MAX_SUBSCRIPTION_BYTES = 16 * 1024 * 1024;
    private static final int[] COMMON_LOCAL_PROXY_PORTS = {7897, 7890};
    private static final String WINDOWS_DOWNLOAD_SCRIPT = """
            $ErrorActionPreference='Stop'
            Add-Type -AssemblyName System.Net.Http
            $handler=New-Object System.Net.Http.HttpClientHandler
            $handler.UseProxy=$true
            $handler.Proxy=New-Object System.Net.WebProxy($env:MGB_PROXY)
            $client=New-Object System.Net.Http.HttpClient($handler)
            $client.Timeout=[TimeSpan]::FromSeconds([int]$env:MGB_TIMEOUT)
            $client.DefaultRequestHeaders.UserAgent.ParseAdd($env:MGB_USER_AGENT)
            $response=$client.GetAsync($env:MGB_URL).GetAwaiter().GetResult()
            if(-not $response.IsSuccessStatusCode){exit 22}
            $bytes=$response.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult()
            [IO.File]::WriteAllBytes($env:MGB_OUTPUT,$bytes)
            $client.Dispose()
            $handler.Dispose()
            """;

    private final Logger logger;
    private final ProxySelector bootstrapProxySelector;
    private final Path runtimeDirectory;
    private final Path executable;
    private final Path rawSubscription;
    private final Path generatedConfig;
    private final Path sourceHash;
    private final Path lastUpdate;
    private Process ownedProcess;
    private int ownedPort = -1;

    ManagedMihomoRuntime(Path configDirectory, Logger logger, ProxySelector bootstrapProxySelector) {
        this.logger = logger;
        this.bootstrapProxySelector = bootstrapProxySelector;
        runtimeDirectory = configDirectory.resolve("minecraft-github-network-bridge").resolve("runtime");
        executable = runtimeDirectory.resolve("mihomo.exe");
        rawSubscription = runtimeDirectory.resolve("subscription-source.yaml");
        generatedConfig = runtimeDirectory.resolve("minecraft-github-only.yaml");
        sourceHash = runtimeDirectory.resolve("subscription-source.sha256");
        lastUpdate = runtimeDirectory.resolve("last-update.txt");
    }

    synchronized BridgeConfig ensureAvailable(BridgeConfig config, boolean forceRefresh) throws IOException {
        if (!config.shouldBridge()) {
            stop();
            return config;
        }
        ensureSupportedPlatform();
        Files.createDirectories(runtimeDirectory);
        extractExecutable();

        if (ownedProcess != null && ownedProcess.isAlive()) {
            stop();
        }
        waitForPortRelease(config.localProxyPort(), Duration.ofSeconds(3));
        BridgeConfig activeConfig = config;
        if (portReady(config.localProxyPort(), Duration.ofMillis(250))) {
            int availablePort = reserveAvailablePort();
            activeConfig = config.withLocalProxyPort(availablePort);
            logger.info("端口 {} 已被其他程序占用，已自动切换到 127.0.0.1:{}",
                    config.localProxyPort(), availablePort);
        }

        boolean sourceMatches = Files.isRegularFile(sourceHash)
                && Files.readString(sourceHash).trim().equals(hash(activeConfig.subscriptionUrl()));
        boolean refresh = forceRefresh || !sourceMatches || !Files.isRegularFile(rawSubscription)
                || (activeConfig.subscriptionAutoUpdate()
                && cacheExpired(activeConfig.subscriptionUpdateMinutes()));
        if (refresh) {
            try {
                String yaml = downloadSubscription(activeConfig);
                writePrivate(rawSubscription, yaml);
                writePrivate(sourceHash, hash(activeConfig.subscriptionUrl()) + System.lineSeparator());
                writePrivate(lastUpdate, Long.toString(System.currentTimeMillis()) + System.lineSeparator());
            } catch (IOException exception) {
                if (!sourceMatches || !Files.isRegularFile(rawSubscription)) {
                    throw exception;
                }
                logger.warn("订阅更新失败，继续使用现有私人缓存：{}",
                        exception.getMessage());
            }
        }
        String yaml = Files.readString(rawSubscription, StandardCharsets.UTF_8);
        String generated = SubscriptionProfileBuilder.build(
                yaml, activeConfig.localProxyPort(), activeConfig.selectedProxyName());
        writePrivate(generatedConfig, generated);
        start(activeConfig);
        return activeConfig;
    }

    synchronized boolean isAlive() {
        return ownedProcess != null && ownedProcess.isAlive();
    }

    synchronized List<String> availableProxyNames() throws IOException {
        if (!Files.isRegularFile(rawSubscription)) {
            return List.of();
        }
        return SubscriptionProfileBuilder.availableProxyNames(
                Files.readString(rawSubscription, StandardCharsets.UTF_8));
    }

    Map<String, Integer> measureProxyLatencies(Duration timeout) throws IOException {
        String yaml;
        List<String> proxyNames;
        synchronized (this) {
            if (!Files.isRegularFile(rawSubscription)) {
                return Map.of();
            }
            Files.createDirectories(runtimeDirectory);
            extractExecutable();
            yaml = Files.readString(rawSubscription, StandardCharsets.UTF_8);
            proxyNames = SubscriptionProfileBuilder.availableProxyNames(yaml);
        }
        if (proxyNames.isEmpty()) {
            return Map.of();
        }

        Path latencyDirectory = runtimeDirectory.resolve("latency-test");
        Path configPath = latencyDirectory.resolve("latency-test.private.yaml");
        Path logPath = latencyDirectory.resolve("latency-test.private.log");
        Files.createDirectories(latencyDirectory);
        int mixedPort;
        int controllerPort;
        try (ServerSocket mixedReservation = reserveLoopbackPort();
             ServerSocket controllerReservation = reserveLoopbackPort()) {
            mixedPort = mixedReservation.getLocalPort();
            controllerPort = controllerReservation.getLocalPort();
        }
        String secret = UUID.randomUUID().toString();
        writePrivate(configPath, SubscriptionProfileBuilder.buildLatencyProfile(
                yaml, mixedPort, controllerPort, secret));

        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    executable.toString(), "-d", latencyDirectory.toString(),
                    "-f", configPath.toString());
            builder.directory(latencyDirectory.toFile());
            builder.redirectErrorStream(true);
            builder.redirectOutput(logPath.toFile());
            process = builder.start();

            Instant deadline = Instant.now().plusSeconds(15);
            while (!portReady(controllerPort, Duration.ofMillis(250))) {
                if (!process.isAlive()) {
                    throw new IOException("临时节点测速服务启动失败");
                }
                if (Instant.now().isAfter(deadline)) {
                    throw new IOException("临时节点测速服务启动超时");
                }
                sleepForLatencyStartup();
            }

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .proxy(new DirectProxySelector())
                    .build();
            int workers = Math.min(8, proxyNames.size());
            ExecutorService executor = Executors.newFixedThreadPool(workers, runnable -> {
                Thread thread = new Thread(runnable, "minecraft-github-latency");
                thread.setDaemon(true);
                return thread;
            });
            List<CompletableFuture<Integer>> measurements = proxyNames.stream()
                    .map(endpoint -> CompletableFuture.supplyAsync(
                            () -> measureProxyWithMihomo(
                                    client, controllerPort, secret, endpoint, timeout), executor))
                    .toList();
            Map<String, Integer> result = new LinkedHashMap<>();
            try {
                for (int index = 0; index < proxyNames.size(); index++) {
                    result.put(proxyNames.get(index), measurements.get(index).join());
                }
            } finally {
                executor.shutdownNow();
            }
            return Map.copyOf(result);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroy();
                try {
                    if (!process.waitFor(3, TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    process.destroyForcibly();
                }
            }
            deleteTemporary(configPath);
            deleteTemporary(logPath);
        }
    }

    boolean isEndpointReady(int port) {
        return portReady(port, Duration.ofMillis(500));
    }

    synchronized void stop() {
        if (ownedProcess == null) {
            return;
        }
        Process process = ownedProcess;
        int port = ownedPort;
        ownedProcess = null;
        ownedPort = -1;
        if (process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
        if (port > 0) {
            waitForPortRelease(port, Duration.ofSeconds(3));
        }
    }

    private String downloadSubscription(BridgeConfig config) throws IOException {
        URI uri = URI.create(config.subscriptionUrl());
        IOException directFailure;
        try {
            return downloadSubscription(config, uri, new DirectProxySelector());
        } catch (IOException exception) {
            directFailure = exception;
        }

        if (!config.subscriptionUseSystemProxy() && !config.subscriptionUseKernelProxy()) {
            throw directFailure;
        }

        Set<ProxySelector> fallbacks = new LinkedHashSet<>();
        if (config.subscriptionUseSystemProxy() && bootstrapProxySelector != null
                && !(bootstrapProxySelector instanceof DirectProxySelector)
                && !(bootstrapProxySelector instanceof GitHubProxySelector)) {
            fallbacks.add(bootstrapProxySelector);
        }
        if (config.subscriptionUseKernelProxy()) {
            for (int port : COMMON_LOCAL_PROXY_PORTS) {
                if (portReady(port, Duration.ofMillis(250))) {
                    fallbacks.add(ProxySelector.of(
                            InetSocketAddress.createUnresolved("127.0.0.1", port)));
                }
            }
        }

        IOException fallbackFailure = null;
        for (ProxySelector selector : fallbacks) {
            try {
                String downloaded = downloadSubscription(config, uri, selector);
                logger.info("已通过当前电脑的网络工具更新订阅");
                return downloaded;
            } catch (IOException exception) {
                fallbackFailure = exception;
            }
        }
        if (config.subscriptionUseKernelProxy()) {
            for (int port : COMMON_LOCAL_PROXY_PORTS) {
                if (!portReady(port, Duration.ofMillis(250))) {
                    continue;
                }
                try {
                    String downloaded = downloadWithWindowsNetworkTool(config, uri, port);
                    logger.info("已通过本地代理内核更新订阅");
                    return downloaded;
                } catch (IOException exception) {
                    fallbackFailure = exception;
                }
            }
        }
        if (fallbackFailure != null) {
            directFailure.addSuppressed(fallbackFailure);
        }
        throw new IOException(directFailure.getMessage()
                + "；当前电脑的网络工具也无法下载订阅", directFailure);
    }

    private String downloadSubscription(BridgeConfig config, URI uri,
                                        ProxySelector proxySelector) throws IOException {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.subscriptionTimeoutSeconds()))
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .proxy(proxySelector);
        if (config.subscriptionAllowInsecure()) {
            installInsecureTls(clientBuilder);
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(config.subscriptionTimeoutSeconds()))
                .header("User-Agent", config.subscriptionUserAgent())
                .header("Accept", "application/yaml,text/yaml,text/plain,*/*")
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = clientBuilder.build()
                    .send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("订阅服务器返回 HTTP " + response.statusCode());
            }
            if (response.body().length == 0 || response.body().length > MAX_SUBSCRIPTION_BYTES) {
                throw new IOException("订阅内容为空或超过 16 MB");
            }
            return new String(response.body(), StandardCharsets.UTF_8);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("订阅下载被中断", exception);
        }
    }

    private String downloadWithWindowsNetworkTool(BridgeConfig config, URI uri, int proxyPort)
            throws IOException {
        Path output = runtimeDirectory.resolve("subscription-download.tmp");
        Files.deleteIfExists(output);
        ProcessBuilder builder = new ProcessBuilder(
                "powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive",
                "-ExecutionPolicy", "Bypass", "-Command", WINDOWS_DOWNLOAD_SCRIPT);
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        builder.environment().put("MGB_URL", uri.toString());
        builder.environment().put("MGB_USER_AGENT", config.subscriptionUserAgent());
        builder.environment().put("MGB_TIMEOUT",
                Integer.toString(config.subscriptionTimeoutSeconds()));
        builder.environment().put("MGB_PROXY", "http://127.0.0.1:" + proxyPort);
        builder.environment().put("MGB_OUTPUT", output.toAbsolutePath().toString());

        Process process = builder.start();
        try {
            boolean completed = process.waitFor(config.subscriptionTimeoutSeconds() + 10L,
                    TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new IOException("Windows 订阅下载超时");
            }
            if (process.exitValue() != 0 || !Files.isRegularFile(output)) {
                throw new IOException("Windows 网络工具无法下载订阅");
            }
            byte[] bytes = Files.readAllBytes(output);
            if (bytes.length == 0 || bytes.length > MAX_SUBSCRIPTION_BYTES) {
                throw new IOException("订阅内容为空或超过 16 MB");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("订阅下载被中断", exception);
        } finally {
            Files.deleteIfExists(output);
        }
    }

    private void start(BridgeConfig config) throws IOException {
        Path log = runtimeDirectory.resolve("mihomo.log");
        ProcessBuilder builder = new ProcessBuilder(
                executable.toString(), "-d", runtimeDirectory.toString(),
                "-f", generatedConfig.toString());
        builder.directory(runtimeDirectory.toFile());
        builder.redirectErrorStream(true);
        builder.redirectOutput(log.toFile());
        ownedProcess = builder.start();
        ownedPort = config.localProxyPort();

        Instant deadline = Instant.now().plus(config.startupTimeout());
        while (Instant.now().isBefore(deadline)) {
            if (!ownedProcess.isAlive()) {
                int exit = ownedProcess.exitValue();
                ownedProcess = null;
                ownedPort = -1;
                throw new IOException("内置连接服务已退出，退出代码：" + exit);
            }
            if (portReady(config.localProxyPort(), Duration.ofMillis(250))) {
                logger.info("我的世界 GitHub 内置连接已就绪：127.0.0.1:{}",
                        config.localProxyPort());
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("内置连接服务启动过程被中断", exception);
            }
        }
        stop();
        throw new IOException("内置连接服务未能在 "
                + config.startupTimeout().toSeconds() + " 秒内启动");
    }

    private void extractExecutable() throws IOException {
        if (Files.isRegularFile(executable) && hash(executable).equals(EMBEDDED_SHA256)) {
            return;
        }
        Path temporary = executable.resolveSibling("mihomo.exe.tmp");
        try (InputStream input = ManagedMihomoRuntime.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IOException("Mod JAR 中缺少内置 Windows 连接程序");
            }
            Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
        }
        if (!hash(temporary).equals(EMBEDDED_SHA256)) {
            Files.deleteIfExists(temporary);
            throw new IOException("内置连接程序完整性校验失败");
        }
        Files.move(temporary, executable, StandardCopyOption.REPLACE_EXISTING);
    }

    private boolean cacheExpired(int updateMinutes) throws IOException {
        if (!Files.isRegularFile(lastUpdate)) {
            return true;
        }
        try {
            long timestamp = Long.parseLong(Files.readString(lastUpdate).trim());
            return Instant.ofEpochMilli(timestamp).plus(Duration.ofMinutes(updateMinutes))
                    .isBefore(Instant.now());
        } catch (NumberFormatException exception) {
            return true;
        }
    }

    private static void ensureSupportedPlatform() throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (!os.contains("windows") || !(arch.equals("amd64") || arch.equals("x86_64"))) {
            throw new IOException("当前版本仅内置 Windows x64 连接程序");
        }
    }

    private static void installInsecureTls(HttpClient.Builder builder) throws IOException {
        try {
            TrustManager[] trustManagers = {new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }};
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustManagers, null);
            SSLParameters parameters = new SSLParameters();
            parameters.setEndpointIdentificationAlgorithm("");
            builder.sslContext(context).sslParameters(parameters);
        } catch (GeneralSecurityException exception) {
            throw new IOException("无法启用无效证书兼容选项", exception);
        }
    }

    private static boolean portReady(int port, Duration timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port),
                    Math.toIntExact(timeout.toMillis()));
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private static int measureProxyWithMihomo(HttpClient client, int controllerPort,
                                              String secret, String proxyName,
                                              Duration timeout) {
        try {
            String encodedName = URLEncoder.encode(proxyName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            String target = URLEncoder.encode("https://api.github.com/rate_limit",
                    StandardCharsets.UTF_8);
            URI uri = URI.create("http://127.0.0.1:" + controllerPort
                    + "/proxies/" + encodedName + "/delay?url=" + target
                    + "&timeout=" + timeout.toMillis());
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(timeout.plusSeconds(2))
                    .header("Authorization", "Bearer " + secret)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return -1;
            }
            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            return body.has("delay") ? body.get("delay").getAsInt() : -1;
        } catch (Exception exception) {
            return -1;
        }
    }

    private static ServerSocket reserveLoopbackPort() throws IOException {
        return new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
    }

    private static int reserveAvailablePort() throws IOException {
        try (ServerSocket reservation = reserveLoopbackPort()) {
            return reservation.getLocalPort();
        }
    }

    private static void waitForPortRelease(int port, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (portReady(port, Duration.ofMillis(100)) && Instant.now().isBefore(deadline)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void sleepForLatencyStartup() throws IOException {
        try {
            Thread.sleep(100);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("节点测速启动过程被中断", exception);
        }
    }

    private static void deleteTemporary(Path path) {
        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                Files.deleteIfExists(path);
                return;
            } catch (IOException exception) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        path.toFile().deleteOnExit();
    }

    private static void writePrivate(Path path, String value) throws IOException {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temporary, value, StandardCharsets.UTF_8);
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String hash(String value) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IOException("当前 Java 环境无法使用 SHA-256", exception);
        }
    }

    private static String hash(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (GeneralSecurityException exception) {
            throw new IOException("当前 Java 环境无法使用 SHA-256", exception);
        }
    }
}
