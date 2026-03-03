package visibility.compat;

import visibility.i18n.Messages;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

/**
 * Version compat for Minecraft 1.8.x.
 *
 * <ul>
 *   <li>INK_SACK with data values (pre-flattening)</li>
 *   <li>Legacy hidePlayer/showPlayer (no Plugin param)</li>
 *   <li>No collision team support (1.9+ feature)</li>
 *   <li>No locale detection (defaults to JA)</li>
 *   <li>Legacy sound enum names (ORB_PICKUP, ITEM_PICKUP, CLICK)</li>
 *   <li>No off-hand / EquipmentSlot</li>
 * </ul>
 */
public class Compat18 implements VersionCompat {

    private static final short LIME_DYE_DATA = 10;
    private static final short GRAY_DYE_DATA = 8;
    private static final String ITEM_MARKER = "\u00A70\u00A7r\u00A7k\u00A7r";

    private final Plugin plugin;

    public Compat18(Plugin plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Items
    // -----------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    @Override
    public ItemStack createHideItem(UUID playerId) {
        return createDyeItem(LIME_DYE_DATA, ChatColor.RED,
            Messages.itemTotemName(playerId),
            ChatColor.GRAY + Messages.itemTotemLore(playerId));
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemStack createShowItem(UUID playerId) {
        return createDyeItem(GRAY_DYE_DATA, ChatColor.GREEN,
            Messages.itemBarrierName(playerId),
            ChatColor.GRAY + Messages.itemBarrierLore(playerId));
    }

    @Override
    public boolean isPluginItem(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        List<String> lore = meta.getLore();
        return lore != null && lore.contains(ITEM_MARKER);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isHideItem(ItemStack item) {
        return item != null && item.getType() == Material.INK_SACK
            && item.getDurability() == LIME_DYE_DATA && isPluginItem(item);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isShowItem(ItemStack item) {
        return item != null && item.getType() == Material.INK_SACK
            && item.getDurability() == GRAY_DYE_DATA && isPluginItem(item);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void swapItems(Player player, boolean toHiding) {
        short fromData = toHiding ? LIME_DYE_DATA : GRAY_DYE_DATA;
        short toData   = toHiding ? GRAY_DYE_DATA : LIME_DYE_DATA;
        UUID id = player.getUniqueId();

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != Material.INK_SACK) continue;
            if (stack.getDurability() != fromData) continue;
            if (!isPluginItem(stack)) continue;

            ItemStack replacement = new ItemStack(Material.INK_SACK, stack.getAmount(), toData);
            ItemMeta newMeta = replacement.getItemMeta();
            if (newMeta != null) {
                if (toHiding) {
                    newMeta.setDisplayName("\u00A7a" + Messages.itemBarrierName(id));
                    newMeta.setLore(Arrays.asList(
                        "\u00A77" + Messages.itemBarrierLore(id), ITEM_MARKER));
                } else {
                    newMeta.setDisplayName("\u00A7c" + Messages.itemTotemName(id));
                    newMeta.setLore(Arrays.asList(
                        "\u00A77" + Messages.itemTotemLore(id), ITEM_MARKER));
                }
                newMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                newMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                replacement.setItemMeta(newMeta);
            }
            player.getInventory().setItem(i, replacement);
        }
    }

    // -----------------------------------------------------------------------
    // Visibility API (legacy — no Plugin param)
    // -----------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    @Override
    public void hidePlayer(Player viewer, Player target) {
        viewer.hidePlayer(target);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void showPlayer(Player viewer, Player target) {
        viewer.showPlayer(target);
    }

    // -----------------------------------------------------------------------
    // Collision (not available in 1.8)
    // -----------------------------------------------------------------------

    @Override public void ensureNoCollision(Player player) { /* no-op */ }
    @Override public void removeFromNoCollision(Player player) { /* no-op */ }
    @Override public void cleanupCollisionTeam() { /* no-op */ }

    // -----------------------------------------------------------------------
    // Locale (not available in 1.8)
    // -----------------------------------------------------------------------

    @Override
    public String getPlayerLocale(Player player) {
        return null;
    }

    // -----------------------------------------------------------------------
    // Sounds (1.8 legacy enum names)
    // -----------------------------------------------------------------------

    @Override
    public void playShowSound(Player player) {
        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.7f, 1.2f);
    }

    @Override
    public void playHideSound(Player player) {
        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.7f, 0.6f);
    }

    @Override
    public void playItemReceiveSound(Player player) {
        player.playSound(player.getLocation(), Sound.ITEM_PICKUP, 0.7f, 1.1f);
    }

    @Override
    public void playClickSound(Player player) {
        player.playSound(player.getLocation(), Sound.CLICK, 0.6f, 1.0f);
    }

    // -----------------------------------------------------------------------
    // Event helpers (no off-hand in 1.8)
    // -----------------------------------------------------------------------

    @Override
    public boolean isMainHand(PlayerInteractEvent event) {
        return true;
    }

    @Override
    public ItemStack getInteractItem(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return null;
        return item;
    }

    // -----------------------------------------------------------------------
    // Feature support
    // -----------------------------------------------------------------------

    @Override
    public boolean supportsDistance() {
        return false;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    private ItemStack createDyeItem(short dyeData, ChatColor color,
                                     String displayName, String lore) {
        ItemStack item = new ItemStack(Material.INK_SACK, 1, dyeData);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + displayName);
            meta.setLore(Arrays.asList(lore, ITEM_MARKER));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }
}
