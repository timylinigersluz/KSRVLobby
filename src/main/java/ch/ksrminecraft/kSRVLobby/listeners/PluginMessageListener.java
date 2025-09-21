package ch.ksrminecraft.kSRVLobby.listeners;

import ch.ksrminecraft.kSRVLobby.KSRVLobby;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;

import java.util.UUID;

public class PluginMessageListener {

    private final KSRVLobby plugin;

    public PluginMessageListener(KSRVLobby plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        // Nur unser Channel
        if (!KSRVLobby.IDENTIFIER.equals(event.getIdentifier())) {
            return;
        }
        plugin.debug("PME: after successful identifier check");

        // Nachricht nicht weiterleiten
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        plugin.debug("PME: setResult=handled");

        // Quelle prüfen
        if (!(event.getSource() instanceof ServerConnection serverConn)) {
            plugin.debug("PME: source is not ServerConnection -> ignore");
            return;
        }

        final String backend = serverConn.getServerInfo().getName();
        plugin.debug("PME: source=ServerConnection backend=" + backend);

        byte[] data = event.getData();
        if (data.length == 0) {
            plugin.debug("PME: received empty message -> ignore");
            return;
        }

        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(data);

            String sub;
            try {
                sub = in.readUTF();
            } catch (Exception ex) {
                plugin.getLogger().warn("[KSRVLobby] PME: invalid/too short message from " + backend);
                return;
            }

            plugin.debug("PME: subChannel=" + sub);

            switch (sub) {
                case "WORLD_UPDATE": {
                    if (data.length < 10) { // sehr grobe Längenprüfung
                        plugin.getLogger().warn("[KSRVLobby] WORLD_UPDATE: payload too short from " + backend);
                        return;
                    }

                    String uuidStr = in.readUTF();
                    String world = in.readUTF();
                    plugin.debug("WORLD_UPDATE: read uuid=" + uuidStr + " world=" + world);

                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        plugin.getConfigManager().updatePlayerWorld(uuid, world);
                        String check = plugin.getConfigManager().getWorldForPlayer(uuid);
                        plugin.debug("WORLD_UPDATE: cache set OK -> " + uuid + " -> " + check);
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warn("[KSRVLobby] WORLD_UPDATE: invalid UUID=" + uuidStr);
                    }
                    break;
                }

                case "ECHO_START": {
                    Player player = serverConn.getPlayer();
                    plugin.debug("ECHO_START: from=" + player.getUsername() + " backend=" + backend);

                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("ECHO_REPLY");
                    out.writeUTF("Hallo " + player.getUsername() + ", Echo erfolgreich empfangen!");
                    byte[] reply = out.toByteArray();

                    serverConn.sendPluginMessage(KSRVLobby.IDENTIFIER, reply);
                    plugin.getLogger().info("[KSRVLobby] ECHO_REPLY an " + player.getUsername() + " gesendet.");
                    plugin.debug("ECHO_START: reply sent bytes=" + reply.length);
                    break;
                }

                default:
                    plugin.debug("PME: unknown subChannel=" + sub + " (ignore)");
            }

        } catch (Exception e) {
            plugin.getLogger().error("[KSRVLobby] PME: exception while processing message from " + backend, e);
        }

        plugin.debug("PME: end");
    }
}
