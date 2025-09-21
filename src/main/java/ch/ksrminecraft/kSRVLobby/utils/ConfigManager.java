package ch.ksrminecraft.kSRVLobby.utils;

import ch.ksrminecraft.kSRVLobby.KSRVLobby;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class ConfigManager {

    private final KSRVLobby plugin;
    private ConfigurationNode root;

    // Cache: PlayerUUID -> world
    private final Map<UUID, String> playerWorldCache = new HashMap<>();

    // Debug-Flag aus config.yml
    private boolean debugEnabled = false;

    public ConfigManager(KSRVLobby plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        try {
            File file = new File("plugins/KSRVLobby/config.yml");
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                plugin.getLogger().info("[KSRVLobby] CFG: create default config.yml...");

                String defaultConfig =
                        "debug: true\n" +
                                "lobby-server: KSR-Lobby\n" +
                                "\n" +
                                "lobby-commands:\n" +
                                "  - hub\n" +
                                "  - lobby\n" +
                                "\n" +
                                "blocked-worlds:\n" +
                                "  servername1:\n" +
                                "    - \"worldname\"\n" +
                                "  servername2:\n" +
                                "    - \"*\"\n";

                Files.writeString(file.toPath(), defaultConfig, StandardCharsets.UTF_8);
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(file).build();
            root = loader.load();

            // Debug-Flag lesen
            debugEnabled = root.node("debug").getBoolean(false);

            plugin.getLogger().info("[KSRVLobby] CFG: loaded. lobbyServer="
                    + getLobbyServer().orElse("<none>")
                    + " lobbyCmds=" + getLobbyCommands()
                    + " debug=" + debugEnabled);

        } catch (IOException e) {
            plugin.getLogger().error("[KSRVLobby] CFG: load FAILED", e);
        }
    }

    // Debug-Helfer
    private void debug(String msg) {
        if (debugEnabled) {
            plugin.getLogger().info("[KSRLobby-DEBUG] " + msg);
        }
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public List<String> getLobbyCommands() {
        try {
            return root.node("lobby-commands").getList(String.class, Arrays.asList("hub", "lobby"));
        } catch (Exception e) {
            return Arrays.asList("hub", "lobby");
        }
    }

    public Optional<String> getLobbyServer() {
        return Optional.ofNullable(root.node("lobby-server").getString());
    }

    public void updatePlayerWorld(UUID uuid, String world) {
        playerWorldCache.put(uuid, world);
        debug("cache.put " + uuid + " -> " + world);
    }

    public String getWorldForPlayer(UUID uuid) {
        String w = playerWorldCache.getOrDefault(uuid, "unknown");
        debug("cache.get " + uuid + " -> " + w);
        return w;
    }

    public List<String> getBlockedWorldsForServer(String server) {
        try {
            List<String> list = root.node("blocked-worlds", server).getList(String.class, Collections.emptyList());
            return list == null ? Collections.emptyList() : list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public boolean isWorldBlocked(String server, String world) {
        debug("isWorldBlocked? server=" + server + " world=" + world);

        // Sicherheits-Default: unbekannte Welt → blockieren
        if (world == null || world.equalsIgnoreCase("unknown")) {
            plugin.getLogger().info("[KSRVLobby] CFG: world unknown -> BLOCK");
            return true;
        }

        List<String> blocked = getBlockedWorldsForServer(server);
        debug("blocked-list for server=" + server + " -> " + blocked);

        if (blocked.isEmpty()) {
            debug("no blocked worlds -> ALLOW");
            return false;
        }

        // 1) Alles blockieren
        if (blocked.contains("*")) {
            plugin.getLogger().info("[KSRVLobby] CFG: wildcard * matched -> BLOCK");
            return true;
        }

        // 2) Exakt blockieren
        if (blocked.contains(world)) {
            plugin.getLogger().info("[KSRVLobby] CFG: exact match -> BLOCK (" + world + ")");
            return true;
        }

        // 3) Prefix-Match (z. B. "1vs*")
        for (String entry : blocked) {
            if (entry.endsWith("*") && !entry.equals("*")) {
                String prefix = entry.substring(0, entry.length() - 1);
                if (world.startsWith(prefix)) {
                    plugin.getLogger().info("[KSRVLobby] CFG: prefix match " + entry + " -> BLOCK");
                    return true;
                }
            }
        }

        // 4) Generischer Glob-Match (z. B. "*vs*")
        for (String entry : blocked) {
            if (entry.contains("*") && !entry.equals("*")) {
                String regex = entry.replace("*", ".*");
                if (world.matches(regex)) {
                    plugin.getLogger().info("[KSRVLobby] CFG: glob match " + entry + " -> BLOCK");
                    return true;
                }
            }
        }

        debug("no match -> ALLOW");
        return false;
    }

    public Map<UUID, String> getAllPlayerWorlds() {
        return Collections.unmodifiableMap(playerWorldCache);
    }
}
