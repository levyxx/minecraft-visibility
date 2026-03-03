package visibility.compat;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;

/**
 * Abstraction layer for version-specific Minecraft/Bukkit API differences.
 * Each supported server version provides its own implementation.
 */
public interface VersionCompat {

    // -----------------------------------------------------------------------
    // Items
    // -----------------------------------------------------------------------

    /** Create the "hide players" toggle item (lime dye). Given when player is showing. */
    ItemStack createHideItem(UUID playerId);

    /** Create the "show players" toggle item (gray dye). Given when player is hiding. */
    ItemStack createShowItem(UUID playerId);

    /** Check if an ItemStack is a plugin utility item. */
    boolean isPluginItem(ItemStack item);

    /** Check if item is the "hide" toggle (lime dye state — clicking will hide players). */
    boolean isHideItem(ItemStack item);

    /** Check if item is the "show" toggle (gray dye state — clicking will show players). */
    boolean isShowItem(ItemStack item);

    /**
     * Swap plugin items in player inventory.
     * @param toHiding true if swapping to hiding state (lime→gray), false for gray→lime.
     */
    void swapItems(Player player, boolean toHiding);

    // -----------------------------------------------------------------------
    // Visibility API
    // -----------------------------------------------------------------------

    void hidePlayer(Player viewer, Player target);

    void showPlayer(Player viewer, Player target);

    // -----------------------------------------------------------------------
    // Collision
    // -----------------------------------------------------------------------

    void ensureNoCollision(Player player);

    void removeFromNoCollision(Player player);

    void cleanupCollisionTeam();

    // -----------------------------------------------------------------------
    // Locale
    // -----------------------------------------------------------------------

    /**
     * Get the player's client locale string, or {@code null} if not available
     * (e.g. 1.8 does not expose locale).
     */
    String getPlayerLocale(Player player);

    // -----------------------------------------------------------------------
    // Sounds
    // -----------------------------------------------------------------------

    void playShowSound(Player player);

    void playHideSound(Player player);

    void playItemReceiveSound(Player player);

    void playClickSound(Player player);

    // -----------------------------------------------------------------------
    // Event helpers
    // -----------------------------------------------------------------------

    /**
     * Check if the interact event is for the main hand.
     * Returns {@code true} unconditionally on 1.8 (no off-hand concept).
     */
    boolean isMainHand(PlayerInteractEvent event);

    /**
     * Get the item from an interact event, handling version-specific fallbacks.
     * @return the item, or {@code null} if none/air.
     */
    ItemStack getInteractItem(PlayerInteractEvent event);

    // -----------------------------------------------------------------------
    // Feature support
    // -----------------------------------------------------------------------

    /** Whether the distance filter feature is supported on this version. */
    boolean supportsDistance();
}
