package visibility.listener;

import visibility.compat.VersionCompat;
import visibility.i18n.Messages;
import visibility.manager.VisibilityManager;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Handles player interact events (dye toggle items), drop prevention,
 * and join/quit lifecycle. All version-specific logic is delegated to
 * {@link VersionCompat}.
 */
public class PlayerListener implements Listener {

    private final Plugin plugin;
    private final VisibilityManager visibilityManager;
    private final VersionCompat compat;

    public PlayerListener(Plugin plugin, VisibilityManager visibilityManager, VersionCompat compat) {
        this.plugin = plugin;
        this.visibilityManager = visibilityManager;
        this.compat = compat;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        String locale = compat.getPlayerLocale(player);
        if (locale != null) {
            Messages.setLang(playerId, Messages.detectLang(locale));
        } else {
            Messages.setLang(playerId, Messages.Lang.JA);
        }
        visibilityManager.handlePlayerJoin(player);

        // When a player reconnects, in-memory state is reset to "showing"
        // but the inventory may still contain the gray dye (show item).
        // Swap it back to lime dye (hide item) 1 tick later (after inventory load).
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    compat.swapItems(player, false);
                }
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Messages.removeLang(player.getUniqueId());
        visibilityManager.handlePlayerQuit(player);
    }

    /**
     * Prevent dropping plugin utility items.
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (compat.isPluginItem(dropped)) {
            event.setCancelled(true);
        }
    }

    /**
     * Right-click lime dye → hide players (item becomes gray dye).
     * Right-click gray dye → show players (item becomes lime dye).
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!compat.isMainHand(event)) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = compat.getInteractItem(event);
        if (item == null) {
            return;
        }

        if (!compat.isPluginItem(item)) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (compat.isHideItem(item)) {
            event.setCancelled(true);
            if (visibilityManager.isOnCooldown(playerId)) {
                player.sendMessage(ChatColor.RED + Messages.cooldownActive(playerId));
                return;
            }
            boolean changed = visibilityManager.hidePlayers(player);
            if (changed) {
                player.sendMessage(ChatColor.GREEN + Messages.playersHidden(playerId));
                compat.playHideSound(player);
            } else {
                player.sendMessage(ChatColor.GRAY + Messages.alreadyHidden(playerId));
            }
        } else if (compat.isShowItem(item)) {
            event.setCancelled(true);
            if (visibilityManager.isOnCooldown(playerId)) {
                player.sendMessage(ChatColor.RED + Messages.cooldownActive(playerId));
                return;
            }
            boolean changed = visibilityManager.showPlayers(player);
            if (changed) {
                player.sendMessage(ChatColor.GREEN + Messages.playersShown(playerId));
                compat.playShowSound(player);
            } else {
                player.sendMessage(ChatColor.GRAY + Messages.alreadyShown(playerId));
            }
        }
    }
}
