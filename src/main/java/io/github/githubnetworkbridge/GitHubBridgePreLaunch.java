package io.github.githubnetworkbridge;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public final class GitHubBridgePreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        BridgeRuntime.INSTANCE.initialize();
    }
}
