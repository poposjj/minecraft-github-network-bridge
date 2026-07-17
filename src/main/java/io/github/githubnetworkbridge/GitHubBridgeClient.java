package io.github.githubnetworkbridge;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.WeakHashMap;

public final class GitHubBridgeClient implements ClientModInitializer {
    private static final Map<Screen, ButtonWidget> OPTIONS_BUTTONS = new WeakHashMap<>();

    @Override
    public void onInitializeClient() {
        BridgeRuntime.INSTANCE.initialize();
        registerCommands();
        registerOptionsButton();
    }

    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommandManager.literal("githubbridge")
                    .executes(context -> status(context.getSource()))
                    .then(ClientCommandManager.literal("status")
                            .executes(context -> status(context.getSource())))
                    .then(ClientCommandManager.literal("config")
                            .executes(context -> openConfig()))
                    .then(ClientCommandManager.literal("proxies")
                            .executes(context -> openProxyGroup()))
                    .then(ClientCommandManager.literal("reload")
                            .executes(context -> reload(context.getSource())))
                    .then(ClientCommandManager.literal("refresh")
                            .executes(context -> reload(context.getSource())))
                    .then(ClientCommandManager.literal("test")
                            .executes(context -> test(context.getSource())))
                    .then(setCommand());
            dispatcher.register(root);
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> setCommand() {
        return ClientCommandManager.literal("set")
                .then(ClientCommandManager.literal("enabled")
                        .then(ClientCommandManager.literal("on")
                                .executes(context -> setEnabled(context.getSource(), true)))
                        .then(ClientCommandManager.literal("off")
                                .executes(context -> setEnabled(context.getSource(), false))))
                .then(ClientCommandManager.literal("subscription")
                        .then(ClientCommandManager.argument("url", StringArgumentType.greedyString())
                                .executes(context -> setSubscription(context.getSource(),
                                        StringArgumentType.getString(context, "url")))))
                .then(ClientCommandManager.literal("proxy")
                        .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(context -> setProxy(context.getSource(),
                                        StringArgumentType.getString(context, "name")))));
    }

    private static void registerOptionsButton() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof OptionsScreen)) {
                return;
            }
            ButtonWidget previous = OPTIONS_BUTTONS.remove(screen);
            if (previous != null) {
                Screens.getButtons(screen).remove(previous);
            }
            int doneLeft = width / 2 - 100;
            int buttonWidth = Math.min(150, Math.max(40, doneLeft - 16));
            Text label = buttonWidth >= 100
                    ? Text.translatable("github_network_bridge.options_button")
                    : Text.literal("MC 网络");
            ButtonWidget button = ButtonWidget.builder(label,
                            ignored -> client.setScreen(new BridgeConfigScreen(screen)))
                    .dimensions(8, height - 29, buttonWidth, 20)
                    .build();
            OPTIONS_BUTTONS.put(screen, button);
            Screens.getButtons(screen).add(button);
        });
    }

    private static int openConfig() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new BridgeConfigScreen(client.currentScreen));
        return 1;
    }

    private static int openProxyGroup() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new BridgeConfigScreen(client.currentScreen, true));
        return 1;
    }

    private static int status(FabricClientCommandSource source) {
        BridgeRuntime runtime = BridgeRuntime.INSTANCE;
        BridgeConfig config = runtime.config();
        if (config == null) {
            source.sendError(Text.literal("我的世界 GitHub 连接尚未初始化"));
            return 0;
        }
        source.sendFeedback(Text.literal("我的世界 GitHub："
                + (config.enabled() ? "已开启" : "已关闭"))
                .formatted(config.enabled() ? Formatting.GREEN : Formatting.YELLOW));
        source.sendFeedback(Text.literal("配置："
                + (config.configured() ? config.profileName() : "尚未创建")));
        source.sendFeedback(Text.literal("当前线路："
                + (config.selectedProxyName().isBlank() ? "未选择（使用第一条）" : config.selectedProxyName())));
        source.sendFeedback(Text.literal("仅代理 GitHub：是 | 连接="
                + (runtime.endpointReady() ? "就绪" : "离线")
                + " | 内置服务=" + (runtime.builtInConnectionRunning() ? "运行中" : "已停止")));
        source.sendFeedback(Text.literal("私人配置文件：" + runtime.configPath()));
        return 1;
    }

    private static int reload(FabricClientCommandSource source) {
        try {
            BridgeConfig loaded = BridgeRuntime.INSTANCE.reload();
            source.sendFeedback(Text.literal("正在刷新我的世界 GitHub 配置："
                    + loaded.profileName()).formatted(Formatting.GREEN));
            return 1;
        } catch (Exception exception) {
            return error(source, "刷新失败", exception);
        }
    }

    private static int setEnabled(FabricClientCommandSource source, boolean enabled) {
        try {
            BridgeConfig current = BridgeRuntime.INSTANCE.config();
            BridgeRuntime.INSTANCE.saveAndApply(current.withEnabled(enabled));
            source.sendFeedback(Text.literal("我的世界 GitHub 已" + (enabled ? "开启" : "关闭"))
                    .formatted(Formatting.GREEN));
            return 1;
        } catch (Exception exception) {
            return error(source, "无法修改开关状态", exception);
        }
    }

    private static int setSubscription(FabricClientCommandSource source, String url) {
        try {
            BridgeConfig current = BridgeRuntime.INSTANCE.config();
            BridgeRuntime.INSTANCE.saveAndApply(current.withQuickSubscription(url));
            source.sendFeedback(Text.literal("订阅已保存，正在准备内置连接")
                    .formatted(Formatting.GREEN));
            return 1;
        } catch (Exception exception) {
            return error(source, "无法保存订阅", exception);
        }
    }

    private static int setProxy(FabricClientCommandSource source, String proxyName) {
        try {
            BridgeRuntime runtime = BridgeRuntime.INSTANCE;
            if (!runtime.availableProxyNames().contains(proxyName)) {
                source.sendError(Text.literal("当前订阅中没有找到这条线路"));
                return 0;
            }
            runtime.saveAndApply(runtime.config().withSelectedProxy(proxyName), false);
            source.sendFeedback(Text.literal("已选择线路：" + proxyName)
                    .formatted(Formatting.GREEN));
            return 1;
        } catch (Exception exception) {
            return error(source, "无法选择线路", exception);
        }
    }

    private static int test(FabricClientCommandSource source) {
        source.sendFeedback(Text.literal("正在准备我的世界 GitHub 连接..."));
        BridgeRuntime.INSTANCE.test().whenComplete((result, error) ->
                MinecraftClient.getInstance().execute(() -> {
                    if (error != null) {
                        source.sendError(Text.literal("我的世界 GitHub 测试失败：" + rootMessage(error)));
                        return;
                    }
                    Formatting color = result.statusCode() >= 200 && result.statusCode() < 400
                            ? Formatting.GREEN : Formatting.YELLOW;
                    source.sendFeedback(Text.literal("我的世界 GitHub 测试：HTTP " + result.statusCode()
                            + "，耗时 " + result.elapsedMillis() + " ms").formatted(color));
                }));
        return 1;
    }

    private static int error(FabricClientCommandSource source, String prefix, Throwable error) {
        source.sendError(Text.literal(prefix + "：" + rootMessage(error)));
        return 0;
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
