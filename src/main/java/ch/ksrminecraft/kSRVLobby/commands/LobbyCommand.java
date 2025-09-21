package ch.ksrminecraft.kSRVLobby.commands;

import ch.ksrminecraft.kSRVLobby.KSRVLobby;
import ch.ksrminecraft.kSRVLobby.utils.ConfigManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class LobbyCommand implements SimpleCommand {

    private final KSRVLobby plugin;

    public LobbyCommand(KSRVLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("Nur Spieler können diesen Befehl nutzen."));
            return;
        }

        final UUID uuid = player.getUniqueId();
        final String name = player.getUsername();
        final ConfigManager cfg = plugin.getConfigManager();

        plugin.debug("/lobby begin player=" + name + " uuid=" + uuid);

        // Cache dump loggen (nur bei Debug)
        plugin.debug("/lobby cache dump START");
        cfg.getAllPlayerWorlds().forEach((u, w) ->
                plugin.debug("   -> " + u + " -> " + w)
        );
        plugin.debug("/lobby cache dump END");

        String currentServer = player.getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse("unknown");
        plugin.debug("/lobby currentServer=" + currentServer);

        String currentWorld = cfg.getWorldForPlayer(uuid); // loggt intern bei Debug
        plugin.debug("/lobby currentWorld(from cache)=" + currentWorld);

        List<String> blockedList = cfg.getBlockedWorldsForServer(currentServer);
        plugin.debug("/lobby blockedList=" + blockedList);

        boolean blocked = cfg.isWorldBlocked(currentServer, currentWorld); // loggt intern bei Debug/Wichtige Fälle
        plugin.debug("/lobby decision blocked=" + blocked);

        if (blocked) {
            player.sendMessage(Component.text("§cDu kannst dich in dieser Welt nicht mit /hub oder /lobby teleportieren!"));
            plugin.getLogger().info("[KSRVLobby] /lobby -> DENY (player=" + name + ")");
            return;
        }

        Optional<String> targetLobby = cfg.getLobbyServer();
        if (targetLobby.isPresent()) {
            String target = targetLobby.get();
            plugin.debug("/lobby targetLobby=" + target);

            plugin.getServer().getServer(target).ifPresentOrElse(serverInfo -> {
                player.createConnectionRequest(serverInfo).fireAndForget();
                plugin.getLogger().info("[KSRVLobby] /lobby -> CONNECT player=" + name + " -> " + target);
            }, () -> {
                plugin.getLogger().warn("[KSRVLobby] /lobby -> targetLobby not found: " + target);
                player.sendMessage(Component.text("§cLobby-Server \"" + target + "\" ist nicht verfügbar."));
            });
        } else {
            plugin.getLogger().warn("[KSRVLobby] /lobby -> no lobby-server configured");
            player.sendMessage(Component.text("§cKein Lobby-Server in der Config definiert."));
        }
    }
}
