# KSRVLobby

KSRVLobby ist ein **Velocity-Plugin** für das KSR Minecraft-Netzwerk.  
Es stellt zentrale **/hub** und **/lobby** Befehle bereit und verhindert, dass Spieler aus bestimmten Welten den Lobby-Teleport nutzen können.

Zusätzlich kommuniziert das Plugin über den PluginMessage-Channel `ksr:lobby` mit den Paper-Servern (Client-Plugin: `KSRLobbyClient`), um die aktuellen Welten der Spieler an den Proxy zu übermitteln.

---

## Features
- Befehle `/hub` und `/lobby` (konfigurierbar in der `config.yml`)
- Abgleich mit einer **Blacklist** von Welten pro Server (`blocked-worlds`)
- Kommunikation zwischen Proxy und Paper über **PluginMessages**
- Automatische Synchronisation der aktuellen Welt bei **Join** und **WorldChange**
- Konfigurierbarer **Lobby-Server** als Ziel
- Debug-Modus für detaillierte Logs

---

## Installation
1. **Build**:
   ```bash
   mvn package
   ```
   → erzeugt `KSRVLobby-x.y.z.jar` im `target/`-Ordner.

2. **Deploy**:
    - Kopiere die JAR in den `plugins/` Ordner deines Velocity-Proxys.
    - Starte den Proxy neu.

3. **Config**:  
   Nach dem ersten Start findest du eine `config.yml` unter:
   ```
   plugins/KSRVLobby/config.yml
   ```

---

## Beispiel `config.yml`

```yaml
debug: true
lobby-server: KSR-Lobby

lobby-commands:
  - hub
  - lobby

blocked-worlds:
  KSR-BEDWARS:
    - "*vs*"
  KSR-SMP:
    - "nether"
    - "the_end"
```

### Erklärung
- **debug:** `true` → sehr viele Logs für Debugging
- **lobby-server:** Name des Zielservers (wie in `velocity.toml` definiert)
- **lobby-commands:** Liste der Befehle, die registriert werden
- **blocked-worlds:** Map von `ServerName` → Liste blockierter Welten
    - `*` blockiert alle Welten
    - `prefix*` blockiert alle Welten, die mit dem Prefix beginnen
    - `*suffix` blockiert alle Welten, die mit dem Suffix enden
    - `*vs*` blockiert alle Welten, die `vs` enthalten

---

## Commands & Permissions

### `/hub` und `/lobby`
- Teleportieren den Spieler zum definierten Lobby-Server.
- Werden **verweigert**, wenn die aktuelle Welt in der `blocked-worlds`-Liste steht.

### `/reloadlobbyconfig`
- Lädt die `config.yml` neu und registriert die Commands neu.
- Permission:
  ```
  ksrlobby.reloadconfig
  ```

---

## Kommunikation mit Paper
Das Proxy-Plugin empfängt Welt-Updates von den Paper-Servern.  
Dafür ist das Client-Plugin **KSRLobbyClient** nötig, das auf jedem Paper-Server installiert wird.

Beispiele:
- Beim Spieler-Join wird ein `WORLD_UPDATE` gesendet.
- Beim Weltwechsel (`PlayerChangedWorldEvent`) ebenfalls.

Der Proxy cached diese Informationen pro Spieler, um bei `/hub` oder `/lobby` zu prüfen, ob ein Teleport erlaubt ist.

---

## Debug-Modus
Wenn `debug: true` gesetzt ist, schreibt das Plugin sehr detaillierte Logs in die Proxy-Konsole, z. B.:

```
[ksrvlobby]: [KSRLobby-DEBUG] PME: subChannel=WORLD_UPDATE
[ksrvlobby]: [KSRLobby-DEBUG] cache.put <UUID> -> 1vs1
[ksrvlobby]: [KSRLobby-DEBUG] /lobby decision blocked=true
```

Dies hilft beim Troubleshooting.

---

## Lizenz
Dieses Projekt wurde für das KSR Minecraft Netzwerk entwickelt.  
Verwendung und Anpassungen sind erlaubt, solange die ursprüngliche Herkunft (`ksrminecraft`) erkennbar bleibt.
