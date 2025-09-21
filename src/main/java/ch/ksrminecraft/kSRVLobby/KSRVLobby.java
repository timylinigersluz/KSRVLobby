package ch.ksrminecraft.kSRVLobby;

import ch.ksrminecraft.kSRVLobby.commands.LobbyCommand;
import ch.ksrminecraft.kSRVLobby.listeners.PluginMessageListener;
import ch.ksrminecraft.kSRVLobby.utils.ConfigManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import ch.ksrminecraft.kSRVLobby.commands.ReloadLobbyConfigCommand;
import org.slf4j.Logger;

import javax.inject.Inject;

@Plugin(
        id = "ksrvlobby",
        name = "KSRVLobby",
        version = "1.0.0",
        description = "Custom /hub & /lobby mit Welt-Blockierung",
        authors = {"ksrminecraft tecs"}
)
public class KSRVLobby {

    private final ProxyServer server;
    private final Logger logger;
    private ConfigManager configManager;

    // Channel Identifier zentral in der Main-Klasse
    public static final MinecraftChannelIdentifier IDENTIFIER =
            MinecraftChannelIdentifier.from("ksr:lobby");

    @Inject
    public KSRVLobby(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();

        // Commands dynamisch registrieren
        LobbyCommand lobbyCmd = new LobbyCommand(this);
        for (String cmd : configManager.getLobbyCommands()) {
            server.getCommandManager().register(cmd, lobbyCmd);
            logger.info("[KSRVLobby] Lobby-Command registriert: /" + cmd);
        }

        // Channel korrekt registrieren
        server.getChannelRegistrar().register(IDENTIFIER);
        logger.info("[KSRVLobby] Channel registriert: " + IDENTIFIER.getId());

        // Listener registrieren
        server.getEventManager().register(this, new PluginMessageListener(this));

        // Reload-Befehl registrieren
        server.getCommandManager().register("reloadlobbyconfig", new ReloadLobbyConfigCommand(this));
        logger.info("[KSRVLobby] Command /reloadlobbyconfig registriert.");


        logger.info("[KSRVLobby] Plugin erfolgreich geladen!");
    }

    public void debug(String msg) {
        if (configManager != null && configManager.isDebugEnabled()) {
            logger.info("[KSRLobby-DEBUG] " + msg);
        }
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
