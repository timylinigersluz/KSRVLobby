package ch.ksrminecraft.kSRVLobby.commands;

import ch.ksrminecraft.kSRVLobby.KSRVLobby;
import ch.ksrminecraft.kSRVLobby.utils.ConfigManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;

public class ReloadLobbyConfigCommand implements SimpleCommand {

    private final KSRVLobby plugin;

    public ReloadLobbyConfigCommand(KSRVLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        // Permission-Check
        if (!source.hasPermission("ksrlobby.reloadconfig")) {
            source.sendMessage(Component.text("§cKeine Berechtigung."));
            return;
        }

        try {
            plugin.getConfigManager().loadConfig();

            // Commands neu registrieren
            ConfigManager cfg = plugin.getConfigManager();

            // Alte Commands deregistrieren
            for (String cmd : cfg.getLobbyCommands()) {
                plugin.getServer().getCommandManager().unregister(cmd);
                plugin.debug("Lobby-Command deregistriert: /" + cmd);
            }

            // Neue Commands registrieren
            LobbyCommand lobbyCmd = new LobbyCommand(plugin);
            for (String cmd : cfg.getLobbyCommands()) {
                plugin.getServer().getCommandManager().register(cmd, lobbyCmd);
                plugin.debug("Lobby-Command registriert: /" + cmd);
            }

            source.sendMessage(Component.text("§a[KSRLobby] Config & Commands neu geladen."));
            plugin.getLogger().info("[KSRVLobby] Config & Commands erfolgreich neu geladen.");
        } catch (Exception e) {
            source.sendMessage(Component.text("§c[KSRLobby] Fehler beim Neuladen der Config."));
            plugin.getLogger().error("[KSRVLobby] Fehler beim Neuladen der Config", e);
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("ksrlobby.reloadconfig");
    }
}
