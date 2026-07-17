package io.github.githubnetworkbridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BridgeConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createsPrivateConfigurationWithoutOpeningAProxyPort() throws Exception {
        Path file = temporaryDirectory.resolve(BridgeConfig.FILE_NAME);
        BridgeConfig config = BridgeConfig.load(file);

        assertTrue(Files.isRegularFile(file));
        assertTrue(config.enabled());
        assertFalse(config.configured());
        assertFalse(config.shouldBridge());
        assertTrue(config.matches("api.github.com"));
        assertTrue(config.matches("subdomain.github.com"));
        assertFalse(config.matches("notgithub.com"));
        assertFalse(config.matches("mc.hypixel.net"));
    }

    @Test
    void savesAndLoadsACompleteSubscriptionConfiguration() throws Exception {
        Path file = temporaryDirectory.resolve(BridgeConfig.FILE_NAME);
        BridgeConfig config = BridgeConfig.load(file).withProfile(
                "Minecraft Test", "Private subscription",
                "https://subscription.example/clash.yaml?token=private",
                "clash.meta", 20, 180, false, false, true)
                .withSelectedProxy("Node B");
        config.save(file);

        BridgeConfig saved = BridgeConfig.load(file);
        assertTrue(saved.shouldBridge());
        assertEquals("Minecraft Test", saved.profileName());
        assertEquals("Private subscription", saved.profileDescription());
        assertEquals("https://subscription.example/clash.yaml?token=private", saved.subscriptionUrl());
        assertEquals(20, saved.subscriptionTimeoutSeconds());
        assertEquals(180, saved.subscriptionUpdateMinutes());
        assertEquals("Node B", saved.selectedProxyName());
    }

    @Test
    void rejectsNonHttpSubscriptionUrls() throws Exception {
        BridgeConfig config = BridgeConfig.load(temporaryDirectory.resolve(BridgeConfig.FILE_NAME));
        assertThrows(Exception.class, () -> config.withQuickSubscription("socks5://127.0.0.1:1080"));
        assertThrows(Exception.class, () -> config.withQuickSubscription("not-a-url"));
    }
}
