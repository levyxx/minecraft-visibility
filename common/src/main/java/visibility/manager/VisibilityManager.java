package visibility.manager;

import visibility.compat.VersionCompat;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Manages player visibility state, distance-based filtering, and collision.
 * All version-specific API calls are delegated to {@link VersionCompat}.
 */
public class VisibilityManager {

    private final Map<UUID, Boolean> hidingState = new ConcurrentHashMap<UUID, Boolean>();
    private final Map<UUID, Integer> distanceThreshold = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Long> lastActionTime = new ConcurrentHashMap<UUID, Long>();

    private static final long COOLDOWN_MS = 1000L;

    private final Plugin plugin;
    private final VersionCompat compat;
    private int distanceTaskId = -1;

    public VisibilityManager(Plugin plugin, VersionCompat compat) {
        this.plugin = plugin;
        this.compat = compat;
    }

    // -----------------------------------------------------------------------
    // Cooldown
    // -----------------------------------------------------------------------

    public boolean isOnCooldown(UUID playerId) {
        Long last = lastActionTime.get(playerId);
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < COOLDOWN_MS;
    }

    public void markAction(UUID playerId) {
        lastActionTime.put(playerId, System.currentTimeMillis());
    }

    // -----------------------------------------------------------------------
    // Visibility toggle
    // -----------------------------------------------------------------------

    public boolean isHiding(UUID playerId) {
        Boolean val = hidingState.get(playerId);
        return val != null ? val : false;
    }

    public boolean hidePlayers(Player player) {
        UUID playerId = player.getUniqueId();
        if (isHiding(playerId)) {
            return false;
        }
        hidingState.put(playerId, true);
        applyVisibility(player);
        compat.swapItems(player, true);
        markAction(playerId);
        return true;
    }

    public boolean showPlayers(Player player) {
        UUID playerId = player.getUniqueId();
        if (!isHiding(playerId)) {
            return false;
        }
        hidingState.put(playerId, false);
        applyVisibility(player);
        compat.swapItems(player, false);
        markAction(playerId);
        return true;
    }

    // -----------------------------------------------------------------------
    // Distance threshold
    // -----------------------------------------------------------------------

    public int getDistance(UUID playerId) {
        Integer val = distanceThreshold.get(playerId);
        return val != null ? val : 0;
    }

    public void setDistance(UUID playerId, int distance) {
        if (distance <= 0) {
            distanceThreshold.remove(playerId);
        } else {
            distanceThreshold.put(playerId, distance);
        }
        markAction(playerId);
        ensureDistanceTask();
        saveDistances();
    }

    // -----------------------------------------------------------------------
    // Apply visibility
    // -----------------------------------------------------------------------

    public void applyVisibility(Player viewer) {
        UUID viewerId = viewer.getUniqueId();
        boolean hiding = isHiding(viewerId);
        int dist = getDistance(viewerId);

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getUniqueId().equals(viewerId)) continue;

            if (!hiding) {
                compat.showPlayer(viewer, target);
            } else if (dist > 0) {
                if (viewer.getWorld().equals(target.getWorld())
                        && viewer.getLocation().distanceSquared(target.getLocation())
                            <= (double) dist * dist) {
                    compat.hidePlayer(viewer, target);
                } else {
                    compat.showPlayer(viewer, target);
                }
            } else {
                compat.hidePlayer(viewer, target);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Distance-based repeating task
    // -----------------------------------------------------------------------

    private void ensureDistanceTask() {
        if (distanceTaskId != -1) return;
        distanceTaskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                boolean anyActive = false;
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    UUID viewerId = viewer.getUniqueId();
                    if (distanceThreshold.containsKey(viewerId)) {
                        anyActive = true;
                        if (isHiding(viewerId)) {
                            applyVisibility(viewer);
                        }
                    }
                }
                if (!anyActive) {
                    Bukkit.getScheduler().cancelTask(distanceTaskId);
                    distanceTaskId = -1;
                }
            }
        }, 10L, 10L).getTaskId();
    }

    // -----------------------------------------------------------------------
    // Cleanup
    // -----------------------------------------------------------------------

    public void handlePlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();
        hidingState.remove(playerId);
        // distanceThreshold is NOT removed — persisted across sessions
        lastActionTime.remove(playerId);
        compat.removeFromNoCollision(player);
    }

    public void handlePlayerJoin(Player player) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.getUniqueId().equals(player.getUniqueId())) {
                compat.showPlayer(player, target);
            }
        }
        compat.ensureNoCollision(player);
        // Restart distance task if this player has a saved distance threshold
        if (distanceThreshold.containsKey(player.getUniqueId())) {
            ensureDistanceTask();
        }
    }

    public void clearAll() {
        if (distanceTaskId != -1) {
            Bukkit.getScheduler().cancelTask(distanceTaskId);
            distanceTaskId = -1;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.getUniqueId().equals(viewer.getUniqueId())) {
                    compat.showPlayer(viewer, target);
                }
            }
        }
        hidingState.clear();
        // distanceThreshold is NOT cleared — caller must save before calling clearAll
        lastActionTime.clear();
        compat.cleanupCollisionTeam();
    }

    // -----------------------------------------------------------------------
    // Distance persistence
    // -----------------------------------------------------------------------

    /**
     * Load persisted distance thresholds from distances.yml.
     */
    public void loadDistances() {
        File file = new File(plugin.getDataFolder(), "distances.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int dist = config.getInt(key, 0);
                if (dist > 0) {
                    distanceThreshold.put(uuid, dist);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    /**
     * Save current distance thresholds to distances.yml.
     */
    public void saveDistances() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File file = new File(dataFolder, "distances.yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Integer> entry : distanceThreshold.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save distances: " + e.getMessage());
        }
    }
}
