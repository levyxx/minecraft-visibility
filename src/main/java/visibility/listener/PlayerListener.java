package visibility.listener;

import visibility.i18n.Messages;
import visibility.manager.VisibilityManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles player interact events for the visibility toggle item (Totem ↔ Barrier),
 * and player join/quit lifecycle.
 */
public class PlayerListener implements Listener {

    private final VisibilityManager visibilityManager;
    private final NamespacedKey itemKey;

    public PlayerListener(VisibilityManager visibilityManager, NamespacedKey itemKey) {
        this.visibilityManager = visibilityManager;
        this.itemKey = itemKey;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Messages.setLang(player.getUniqueId(), Messages.detectLang(player.getLocale()));
        visibilityManager.handlePlayerJoin(player);
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
        ItemMeta meta = dropped.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    /**
     * Left-click Totem of Undying → hide players (item becomes Barrier).
     * Left-click Barrier → show players (item becomes Totem).
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();

        // Only respond to right-clicks
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            item = event.getPlayer().getInventory().getItem(event.getHand());
        }
        if (item == null || item.getType().isAir()) {
            return;
        }

        // Check it's our plugin item
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE)) {
            return;
        }

        Material type = item.getType();
        Player player = event.getPlayer();
        java.util.UUID playerId = player.getUniqueId();

        if (type == Material.BLAZE_ROD) {
            event.setCancelled(true);
            if (visibilityManager.isOnCooldown(playerId)) {
                player.sendMessage(ChatColor.RED + Messages.cooldownActive(playerId));
                return;
            }
            boolean changed = visibilityManager.hidePlayers(player);
            if (changed) {
                player.sendMessage(ChatColor.GREEN + Messages.playersHidden(playerId));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 0.6f);
            } else {
                player.sendMessage(ChatColor.GRAY + Messages.alreadyHidden(playerId));
            }
        } else if (type == Material.BREEZE_ROD) {
            event.setCancelled(true);
            if (visibilityManager.isOnCooldown(playerId)) {
                player.sendMessage(ChatColor.RED + Messages.cooldownActive(playerId));
                return;
            }
            boolean changed = visibilityManager.showPlayers(player);
            if (changed) {
                player.sendMessage(ChatColor.GREEN + Messages.playersShown(playerId));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
            } else {
                player.sendMessage(ChatColor.GRAY + Messages.alreadyShown(playerId));
            }
        }
    }
}
