package visibility.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Manages player visibility state, distance-based filtering, and collision toggling.
 *
 * <p>Visibility: each player can choose to hide other players. When hidden,
 * the held totem item is swapped to a barrier; when shown, it reverts.</p>
 *
 * <p>Distance filter: if a player sets a distance N, only players within N blocks
 * are hidden; farther players remain visible. A repeating task handles updates.</p>
 *
 * <p>Collision: uses Minecraft scoreboard teams with the {@code NEVER} push rule
 * so that players pass through each other.</p>
 */
public class VisibilityManager {

    private static final String NO_COLLISION_TEAM_NAME = "vis_no_collide";

    /** Players who have enabled hiding mode (value = true means "hiding"). */
    private final Map<UUID, Boolean> hidingState = new ConcurrentHashMap<>();

    /** Per-player distance threshold in blocks (0 or absent = no filter). */
    private final Map<UUID, Integer> distanceThreshold = new ConcurrentHashMap<>();

    /** Cooldown: last action timestamp per player (millis). */
    private final Map<UUID, Long> lastActionTime = new ConcurrentHashMap<>();

    /** Cooldown duration in milliseconds. */
    private static final long COOLDOWN_MS = 1000L;

    private final Plugin plugin;
    private final NamespacedKey itemKey;
    private int distanceTaskId = -1;

    public VisibilityManager(Plugin plugin, NamespacedKey itemKey) {
        this.plugin = plugin;
        this.itemKey = itemKey;
    }

    // -----------------------------------------------------------------------
    // Cooldown
    // -----------------------------------------------------------------------

    /**
     * Check whether the player is still within the 1-second cooldown.
     *
     * @return true if the action is on cooldown (should be blocked).
     */
    public boolean isOnCooldown(UUID playerId) {
        Long last = lastActionTime.get(playerId);
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < COOLDOWN_MS;
    }

    /**
     * Record that the player just performed a visibility action.
     */
    public void markAction(UUID playerId) {
        lastActionTime.put(playerId, System.currentTimeMillis());
    }

    // -----------------------------------------------------------------------
    // Visibility toggle
    // -----------------------------------------------------------------------

    /**
     * @return true if the player is currently in "hiding" mode.
     */
    public boolean isHiding(UUID playerId) {
        return hidingState.getOrDefault(playerId, false);
    }

    /**
     * Hide all other players from the given player.
     *
     * @return true if the state actually changed.
     */
    public boolean hidePlayers(Player player) {
        UUID playerId = player.getUniqueId();
        if (isHiding(playerId)) {
            return false;
        }
        hidingState.put(playerId, true);
        applyVisibility(player);
        swapItem(player, Material.BLAZE_ROD, Material.BREEZE_ROD);
        markAction(playerId);
        return true;
    }

    /**
     * Show all other players to the given player.
     *
     * @return true if the state actually changed.
     */
    public boolean showPlayers(Player player) {
        UUID playerId = player.getUniqueId();
        if (!isHiding(playerId)) {
            return false;
        }
        hidingState.put(playerId, false);
        applyVisibility(player);
        swapItem(player, Material.BREEZE_ROD, Material.BLAZE_ROD);
        markAction(playerId);
        return true;
    }

    // -----------------------------------------------------------------------
    // Distance threshold
    // -----------------------------------------------------------------------

    public int getDistance(UUID playerId) {
        return distanceThreshold.getOrDefault(playerId, 0);
    }

    public void setDistance(UUID playerId, int distance) {
        if (distance <= 0) {
            distanceThreshold.remove(playerId);
        } else {
            distanceThreshold.put(playerId, distance);
        }
        markAction(playerId);
        ensureDistanceTask();
    }

    // -----------------------------------------------------------------------
    // Collision (always disabled for all players)
    // -----------------------------------------------------------------------

    /**
     * Ensure the given player is added to the no-collision team.
     * Called automatically on join.
     */
    public void ensureNoCollision(Player player) {
        addToNoCollisionTeam(player);
    }

    // -----------------------------------------------------------------------
    // Apply visibility to a specific player
    // -----------------------------------------------------------------------

    /**
     * Re-evaluate visibility of all online players with respect to the given viewer.
     * Takes the distance threshold into account.
     */
    public void applyVisibility(Player viewer) {
        UUID viewerId = viewer.getUniqueId();
        boolean hiding = isHiding(viewerId);
        int dist = getDistance(viewerId);

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getUniqueId().equals(viewerId)) continue;

            if (!hiding) {
                viewer.showPlayer(plugin, target);
            } else if (dist > 0) {
                // Distance-based: hide only if within range
                if (viewer.getWorld().equals(target.getWorld())
                        && viewer.getLocation().distanceSquared(target.getLocation()) <= (double) dist * dist) {
                    viewer.hidePlayer(plugin, target);
                } else {
                    viewer.showPlayer(plugin, target);
                }
            } else {
                viewer.hidePlayer(plugin, target);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Item swap helper
    // -----------------------------------------------------------------------

    /**
     * Look through the player's inventory for a utility item of {@code from} material
     * and replace it with {@code to} material, preserving meta and custom key.
     * Only swaps plugin-tagged items.
     */
    public void swapItem(Player player, Material from, Material to) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != from) continue;
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) continue;
            if (!meta.getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE)) continue;

            ItemStack replacement = new ItemStack(to, stack.getAmount());
            ItemMeta newMeta = replacement.getItemMeta();
            if (newMeta != null) {
                // Re-apply display name, lore, and persistent key
                newMeta.setDisplayName(getSwappedDisplayName(player, to));
                newMeta.setLore(getSwappedLore(player, to));
                newMeta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
                for (org.bukkit.inventory.ItemFlag flag : org.bukkit.inventory.ItemFlag.values()) {
                    newMeta.addItemFlags(flag);
                }
                replacement.setItemMeta(newMeta);
            }
            player.getInventory().setItem(i, replacement);
        }
    }

    private String getSwappedDisplayName(Player player, Material to) {
        UUID id = player.getUniqueId();
        if (to == Material.BREEZE_ROD) {
            return "\u00A7a" + visibility.i18n.Messages.itemBarrierName(id);
        } else {
            return "\u00A7c" + visibility.i18n.Messages.itemTotemName(id);
        }
    }

    private java.util.List<String> getSwappedLore(Player player, Material to) {
        UUID id = player.getUniqueId();
        if (to == Material.BREEZE_ROD) {
            return java.util.List.of("\u00A77" + visibility.i18n.Messages.itemBarrierLore(id));
        } else {
            return java.util.List.of("\u00A77" + visibility.i18n.Messages.itemTotemLore(id));
        }
    }

    // -----------------------------------------------------------------------
    // Collision team management
    // -----------------------------------------------------------------------

    private Team getOrCreateNoCollisionTeam() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(NO_COLLISION_TEAM_NAME);
        if (team == null) {
            team = board.registerNewTeam(NO_COLLISION_TEAM_NAME);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }
        return team;
    }

    private void addToNoCollisionTeam(Player player) {
        Team team = getOrCreateNoCollisionTeam();
        team.addEntry(player.getName());
    }

    private void removeFromNoCollisionTeam(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(NO_COLLISION_TEAM_NAME);
        if (team != null) {
            team.removeEntry(player.getName());
        }
    }

    // -----------------------------------------------------------------------
    // Distance-based repeating task
    // -----------------------------------------------------------------------

    /**
     * Ensure a repeating task is running if any player has a distance threshold.
     * The task re-evaluates visibility every 10 ticks (0.5 seconds).
     */
    private void ensureDistanceTask() {
        if (distanceTaskId != -1) return;
        distanceTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // If no players have distance thresholds, cancel the task
            if (distanceThreshold.isEmpty()) {
                Bukkit.getScheduler().cancelTask(distanceTaskId);
                distanceTaskId = -1;
                return;
            }
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                UUID viewerId = viewer.getUniqueId();
                if (distanceThreshold.containsKey(viewerId) && isHiding(viewerId)) {
                    applyVisibility(viewer);
                }
            }
        }, 10L, 10L).getTaskId();
    }

    // -----------------------------------------------------------------------
    // Cleanup
    // -----------------------------------------------------------------------

    /**
     * Called on player quit. Cleans up state.
     */
    public void handlePlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();
        hidingState.remove(playerId);
        distanceThreshold.remove(playerId);
        lastActionTime.remove(playerId);
        removeFromNoCollisionTeam(player);
    }

    /**
     * Called on player join. Re-applies collision if it was somehow persisted
     * (in this implementation state is in-memory only, so this is a no-op).
     */
    public void handlePlayerJoin(Player player) {
        // Re-show all players by default on join
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.getUniqueId().equals(player.getUniqueId())) {
                player.showPlayer(plugin, target);
            }
        }
        // Always disable collision for all players
        ensureNoCollision(player);
    }

    /**
     * Clear all state. Called on plugin disable.
     */
    public void clearAll() {
        if (distanceTaskId != -1) {
            Bukkit.getScheduler().cancelTask(distanceTaskId);
            distanceTaskId = -1;
        }
        // Show all hidden players
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.getUniqueId().equals(viewer.getUniqueId())) {
                    viewer.showPlayer(plugin, target);
                }
            }
        }
        hidingState.clear();
        distanceThreshold.clear();
        lastActionTime.clear();

        // Clean up the team
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(NO_COLLISION_TEAM_NAME);
        if (team != null) {
            team.unregister();
        }
    }
}
