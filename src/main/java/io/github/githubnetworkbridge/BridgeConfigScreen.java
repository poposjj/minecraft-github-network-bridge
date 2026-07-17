package io.github.githubnetworkbridge;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BridgeConfigScreen extends Screen {
    private enum Tab {
        SETTINGS,
        PROXY_GROUP
    }

    private enum SettingsPage {
        QUICK,
        CREATE,
        GENERAL
    }

    private final Screen parent;
    private Tab tab = Tab.SETTINGS;
    private SettingsPage settingsPage;
    private boolean enabled;
    private String profileName;
    private String profileDescription;
    private String subscriptionUrl;
    private String selectedProxyName;
    private String userAgent;
    private String timeoutSeconds;
    private String updateMinutes;
    private boolean useCurrentProxy;
    private boolean allowInsecure;
    private boolean autoUpdate;
    private List<String> proxyNames;
    private Map<String, Integer> proxyLatencies = Map.of();
    private int proxyPage;
    private Text status = Text.empty();

    private TextFieldWidget quickUrlField;
    private TextFieldWidget nameField;
    private TextFieldWidget descriptionField;
    private TextFieldWidget subscriptionField;
    private TextFieldWidget userAgentField;
    private TextFieldWidget timeoutField;
    private TextFieldWidget updateField;

    public BridgeConfigScreen(Screen parent) {
        this(parent, false);
    }

    public BridgeConfigScreen(Screen parent, boolean openProxyGroup) {
        super(Text.translatable("github_network_bridge.config.title"));
        this.parent = parent;
        BridgeConfig config = BridgeRuntime.INSTANCE.config();
        if (config == null) {
            throw new IllegalStateException("我的世界 GitHub 网络桥接尚未初始化");
        }
        enabled = config.enabled();
        profileName = config.profileName();
        profileDescription = config.profileDescription();
        subscriptionUrl = config.subscriptionUrl();
        selectedProxyName = config.selectedProxyName();
        userAgent = config.subscriptionUserAgent();
        timeoutSeconds = Integer.toString(config.subscriptionTimeoutSeconds());
        updateMinutes = Integer.toString(config.subscriptionUpdateMinutes());
        useCurrentProxy = config.subscriptionUseCurrentProxy();
        allowInsecure = config.subscriptionAllowInsecure();
        autoUpdate = config.subscriptionAutoUpdate();
        proxyNames = new ArrayList<>(BridgeRuntime.INSTANCE.availableProxyNames());
        settingsPage = config.configured() ? SettingsPage.GENERAL : SettingsPage.QUICK;
        tab = openProxyGroup ? Tab.PROXY_GROUP : Tab.SETTINGS;
    }

    @Override
    protected void init() {
        resetFields();
        int contentWidth = Math.min(380, width - 20);
        int left = (width - contentWidth) / 2;
        int gap = 5;
        int tabWidth = (contentWidth - gap) / 2;
        addDrawableChild(tabButton(Tab.SETTINGS, left, 31, tabWidth,
                Text.translatable("github_network_bridge.config.settings")));
        addDrawableChild(tabButton(Tab.PROXY_GROUP, left + tabWidth + gap, 31, tabWidth,
                Text.translatable("github_network_bridge.config.proxy_group")));

        if (tab == Tab.SETTINGS) {
            initSettings(left, contentWidth);
        } else {
            initProxyGroup(left, contentWidth);
        }

        int footerY = height - 28;
        int footerWidth = Math.min(115, (contentWidth - gap * 2) / 3);
        int footerLeft = (width - (footerWidth * 3 + gap * 2)) / 2;
        boolean quickSubscriptionPage = tab == Tab.SETTINGS && settingsPage == SettingsPage.QUICK;
        Text primaryActionLabel = Text.translatable(quickSubscriptionPage
                ? "github_network_bridge.config.refresh_proxies"
                : "github_network_bridge.config.test");
        addDrawableChild(ButtonWidget.builder(primaryActionLabel, button -> {
                    if (quickSubscriptionPage) {
                        refreshProxyGroup();
                    } else {
                        testConnection();
                    }
                })
                .dimensions(footerLeft, footerY, footerWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(footerLeft + footerWidth + gap, footerY, footerWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> saveAndClose())
                .dimensions(footerLeft + (footerWidth + gap) * 2, footerY, footerWidth, 20).build());
    }

    private void initSettings(int left, int width) {
        int gap = 4;
        int buttonWidth = (width - gap * 2) / 3;
        addDrawableChild(settingsPageButton(SettingsPage.QUICK, left, 58, buttonWidth,
                Text.translatable("github_network_bridge.config.quick")));
        addDrawableChild(settingsPageButton(SettingsPage.CREATE, left + buttonWidth + gap, 58,
                buttonWidth, Text.translatable("github_network_bridge.config.create")));
        addDrawableChild(settingsPageButton(SettingsPage.GENERAL, left + (buttonWidth + gap) * 2, 58,
                buttonWidth, Text.translatable("github_network_bridge.config.general")));

        switch (settingsPage) {
            case QUICK -> initQuick(left, width);
            case CREATE -> initCreate(left, width);
            case GENERAL -> initGeneral(left, width);
        }
    }

    private void initQuick(int left, int width) {
        quickUrlField = field(left, 108, width,
                Text.translatable("github_network_bridge.config.subscription_url"), subscriptionUrl, 4096);
        quickUrlField.setPlaceholder(Text.translatable("github_network_bridge.config.paste_subscription"));
    }

    private void initCreate(int left, int width) {
        int rowGap = Math.min(32, Math.max(24, (height - 132) / 4));
        int y = 96;
        nameField = field(left, y, width,
                Text.translatable("github_network_bridge.config.profile_name"), profileName, 100);
        y += rowGap;
        descriptionField = field(left, y, width,
                Text.translatable("github_network_bridge.config.description"), profileDescription, 300);
        y += rowGap;
        subscriptionField = field(left, y, width,
                Text.translatable("github_network_bridge.config.subscription_url"), subscriptionUrl, 4096);
        y += rowGap;
        userAgentField = field(left, y, width,
                Text.translatable("github_network_bridge.config.user_agent"), userAgent, 200);
    }

    private void initGeneral(int left, int width) {
        int half = (width - 5) / 2;
        int y = 88;
        addDrawableChild(toggle(left, y, half, enabledText(), button -> {
            enabled = !enabled;
            button.setMessage(enabledText());
        }));
        addDrawableChild(toggle(left + half + 5, y, half, autoUpdateText(), button -> {
            autoUpdate = !autoUpdate;
            button.setMessage(autoUpdateText());
        }));
        y += 26;
        addDrawableChild(toggle(left, y, half, useCurrentProxyText(), button -> {
            useCurrentProxy = !useCurrentProxy;
            button.setMessage(useCurrentProxyText());
        }));
        addDrawableChild(toggle(left + half + 5, y, half, allowInsecureText(), button -> {
            allowInsecure = !allowInsecure;
            button.setMessage(allowInsecureText());
        }));

        int fieldsY = y + 36;
        timeoutField = field(left, fieldsY, half,
                Text.translatable("github_network_bridge.config.timeout"), timeoutSeconds, 3);
        timeoutField.setTextPredicate(BridgeConfigScreen::digitsOnly);
        updateField = field(left + half + 5, fieldsY, half,
                Text.translatable("github_network_bridge.config.interval"), updateMinutes, 5);
        updateField.setTextPredicate(BridgeConfigScreen::digitsOnly);
    }

    private void initProxyGroup(int left, int width) {
        ButtonWidget latencyButton = ButtonWidget.builder(Text.literal("↻"),
                        button -> refreshProxyLatencies())
                .dimensions(left + width - 20, 58, 20, 20).build();
        latencyButton.setTooltip(Tooltip.of(
                Text.translatable("github_network_bridge.config.refresh_latency")));
        addDrawableChild(latencyButton);

        int listTop = 86;
        int listBottom = height - 64;
        int pageSize = Math.max(1, (listBottom - listTop) / 23);
        int pageCount = Math.max(1, (proxyNames.size() + pageSize - 1) / pageSize);
        proxyPage = Math.max(0, Math.min(proxyPage, pageCount - 1));
        int start = proxyPage * pageSize;
        int end = Math.min(proxyNames.size(), start + pageSize);
        for (int index = start; index < end; index++) {
            String proxyName = proxyNames.get(index);
            boolean selected = proxyName.equals(selectedProxyName);
            String prefix = selected ? "[*] " : "";
            Integer latency = proxyLatencies.get(proxyName);
            String suffix = latency == null ? "" : latency < 0 ? "  [--]" : "  [" + latency + " ms]";
            int nameWidth = Math.max(20, width - textRenderer.getWidth(suffix) - 12);
            String label = textRenderer.trimToWidth(prefix + proxyName, nameWidth) + suffix;
            addDrawableChild(ButtonWidget.builder(Text.literal(label),
                            button -> selectProxy(proxyName))
                    .dimensions(left, listTop + (index - start) * 23, width, 20).build());
        }

        if (pageCount > 1) {
            int y = 58;
            ButtonWidget previous = ButtonWidget.builder(Text.literal("<"), button -> {
                proxyPage--;
                clearAndInit();
            }).dimensions(left, y, 28, 20).build();
            previous.active = proxyPage > 0;
            addDrawableChild(previous);
            ButtonWidget next = ButtonWidget.builder(Text.literal(">"), button -> {
                proxyPage++;
                clearAndInit();
            }).dimensions(left + 33, y, 28, 20).build();
            next.active = proxyPage + 1 < pageCount;
            addDrawableChild(next);
        }
    }

    private ButtonWidget tabButton(Tab target, int x, int y, int width, Text label) {
        ButtonWidget button = ButtonWidget.builder(label, ignored -> switchTab(target))
                .dimensions(x, y, width, 20).build();
        button.active = tab != target;
        return button;
    }

    private ButtonWidget settingsPageButton(SettingsPage target, int x, int y, int width, Text label) {
        ButtonWidget button = ButtonWidget.builder(label, ignored -> switchSettingsPage(target))
                .dimensions(x, y, width, 20).build();
        button.active = settingsPage != target;
        return button;
    }

    private ButtonWidget toggle(int x, int y, int width, Text label, ButtonWidget.PressAction action) {
        return ButtonWidget.builder(label, action).dimensions(x, y, width, 20).build();
    }

    private TextFieldWidget field(int x, int y, int width, Text label, String value, int maxLength) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, width, 20, label);
        field.setMaxLength(maxLength);
        field.setText(value == null ? "" : value);
        field.setPlaceholder(label);
        addDrawableChild(field);
        return field;
    }

    private void switchTab(Tab target) {
        captureFields();
        tab = target;
        clearAndInit();
    }

    private void switchSettingsPage(SettingsPage target) {
        captureFields();
        settingsPage = target;
        clearAndInit();
    }

    private void captureFields() {
        if (quickUrlField != null) subscriptionUrl = quickUrlField.getText();
        if (nameField != null) profileName = nameField.getText();
        if (descriptionField != null) profileDescription = descriptionField.getText();
        if (subscriptionField != null) subscriptionUrl = subscriptionField.getText();
        if (userAgentField != null) userAgent = userAgentField.getText();
        if (timeoutField != null) timeoutSeconds = timeoutField.getText();
        if (updateField != null) updateMinutes = updateField.getText();
    }

    private BridgeConfig configuredState() throws Exception {
        captureFields();
        BridgeConfig current = BridgeRuntime.INSTANCE.config();
        BridgeConfig updated;
        if (settingsPage == SettingsPage.QUICK) {
            updated = current.withQuickSubscription(subscriptionUrl);
            selectedProxyName = "";
        } else {
            updated = current.withProfile(profileName, profileDescription, subscriptionUrl, userAgent,
                    parseInteger(timeoutSeconds, "请求超时"), parseInteger(updateMinutes, "更新间隔"),
                    useCurrentProxy, allowInsecure, autoUpdate);
            if (!selectedProxyName.isBlank()) {
                updated = updated.withSelectedProxy(selectedProxyName);
            }
        }
        return updated.withEnabled(enabled);
    }

    private BridgeConfig save(boolean refreshSubscription) throws Exception {
        return BridgeRuntime.INSTANCE.saveAndApply(configuredState(), refreshSubscription);
    }

    private void refreshProxyGroup() {
        try {
            save(true);
            status = Text.translatable("github_network_bridge.config.loading_proxies")
                    .formatted(Formatting.YELLOW);
            BridgeRuntime.INSTANCE.proxyNamesWhenReady().whenComplete((names, error) ->
                    client.execute(() -> {
                        if (error != null) {
                            status = Text.literal(message(error)).formatted(Formatting.RED);
                            return;
                        }
                        proxyNames = new ArrayList<>(names);
                        proxyLatencies = Map.of();
                        proxyPage = 0;
                        settingsPage = SettingsPage.GENERAL;
                        tab = Tab.PROXY_GROUP;
                        status = Text.translatable("github_network_bridge.config.proxy_count", names.size())
                                .formatted(Formatting.GREEN);
                        clearAndInit();
                    }));
        } catch (Exception exception) {
            status = Text.literal(message(exception)).formatted(Formatting.RED);
        }
    }

    private void refreshProxyLatencies() {
        status = Text.translatable("github_network_bridge.config.loading_latency")
                .formatted(Formatting.YELLOW);
        BridgeRuntime.INSTANCE.measureProxyLatencies().whenComplete((latencies, error) ->
                client.execute(() -> {
                    if (error != null) {
                        status = Text.literal(message(error)).formatted(Formatting.RED);
                        return;
                    }
                    proxyLatencies = latencies;
                    status = Text.translatable("github_network_bridge.config.latency_done")
                            .formatted(Formatting.GREEN);
                    clearAndInit();
                }));
    }

    private void selectProxy(String proxyName) {
        try {
            selectedProxyName = proxyName;
            settingsPage = SettingsPage.GENERAL;
            BridgeConfig updated = BridgeRuntime.INSTANCE.config().withSelectedProxy(proxyName);
            BridgeRuntime.INSTANCE.saveAndApply(updated, false);
            status = Text.translatable("github_network_bridge.config.proxy_selected", proxyName)
                    .formatted(Formatting.GREEN);
            clearAndInit();
        } catch (Exception exception) {
            status = Text.literal(message(exception)).formatted(Formatting.RED);
        }
    }

    private void saveAndClose() {
        try {
            save(false);
            close();
        } catch (Exception exception) {
            status = Text.literal(message(exception)).formatted(Formatting.RED);
        }
    }

    private void testConnection() {
        try {
            save(false);
            status = Text.translatable("github_network_bridge.config.preparing")
                    .formatted(Formatting.YELLOW);
            BridgeRuntime.INSTANCE.test().whenComplete((result, error) ->
                    client.execute(() -> {
                        if (error != null) {
                            status = Text.literal(message(error)).formatted(Formatting.RED);
                        } else {
                            status = Text.translatable("github_network_bridge.config.test_result",
                                            result.statusCode(), result.elapsedMillis())
                                    .formatted(result.statusCode() >= 200 && result.statusCode() < 400
                                            ? Formatting.GREEN : Formatting.YELLOW);
                        }
                    }));
        } catch (Exception exception) {
            status = Text.literal(message(exception)).formatted(Formatting.RED);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, Colors.WHITE);
        drawLabels(context);
        if (tab == Tab.PROXY_GROUP && proxyNames.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("github_network_bridge.config.no_proxies"),
                    width / 2, 98, Colors.LIGHT_GRAY);
        }
        if (!status.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, status, width / 2, height - 42, Colors.WHITE);
        }
    }

    private void drawLabels(DrawContext context) {
        if (tab != Tab.SETTINGS) {
            return;
        }
        if (settingsPage == SettingsPage.QUICK) {
            label(context, Text.translatable("github_network_bridge.config.subscription_url"), quickUrlField);
        } else if (settingsPage == SettingsPage.CREATE) {
            label(context, Text.translatable("github_network_bridge.config.profile_name"), nameField);
            label(context, Text.translatable("github_network_bridge.config.description"), descriptionField);
            label(context, Text.translatable("github_network_bridge.config.subscription_url"), subscriptionField);
            label(context, Text.translatable("github_network_bridge.config.user_agent"), userAgentField);
        } else {
            label(context, Text.translatable("github_network_bridge.config.timeout"), timeoutField);
            label(context, Text.translatable("github_network_bridge.config.interval"), updateField);
        }
    }

    private void label(DrawContext context, Text text, TextFieldWidget field) {
        if (field != null) {
            context.drawTextWithShadow(textRenderer, text, field.getX(), field.getY() - 10, Colors.LIGHT_GRAY);
        }
    }

    private Text enabledText() {
        return Text.translatable(enabled
                ? "github_network_bridge.config.enabled" : "github_network_bridge.config.disabled");
    }

    private Text autoUpdateText() {
        return Text.translatable(autoUpdate
                ? "github_network_bridge.config.auto_update_on" : "github_network_bridge.config.auto_update_off");
    }

    private Text useCurrentProxyText() {
        return Text.translatable(useCurrentProxy
                ? "github_network_bridge.config.current_line_on" : "github_network_bridge.config.current_line_off");
    }

    private Text allowInsecureText() {
        return Text.translatable(allowInsecure
                ? "github_network_bridge.config.insecure_on" : "github_network_bridge.config.insecure_off");
    }

    private void resetFields() {
        quickUrlField = null;
        nameField = null;
        descriptionField = null;
        subscriptionField = null;
        userAgentField = null;
        timeoutField = null;
        updateField = null;
    }

    private static boolean digitsOnly(String value) {
        return value.isEmpty() || value.chars().allMatch(Character::isDigit);
    }

    private static int parseInteger(String value, String label) throws IOException {
        if (value == null || value.isBlank() || !digitsOnly(value)) {
            throw new IOException(label + "必须填写整数");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IOException(label + "数值过大", exception);
        }
    }

    private static String message(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
