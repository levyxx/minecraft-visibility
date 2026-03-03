package visibility.compat;

import visibility.i18n.Messages;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Version compat for Minecraft 1.21.x.
 *
 * <ul>
 *   <li>LIME_DYE / GRAY_DYE (post-flattening)</li>
 *   <li>PersistentDataContainer for item identification</li>
 *   <li>hidePlayer(Plugin, Player) / showPlayer(Plugin, Player)</li>
 *   <li>Collision via Team.Option.COLLISION_RULE</li>
 *   <li>Locale via Player.getLocale()</li>
 *   <li>Sound: ENTITY_EXPERIENCE_ORB_PICKUP, ENTITY_ITEM_PICKUP, UI_BUTTON_CLICK</li>
 * </ul>
 */
public class Compat121 implements VersionCompat {

    private static final String NO_COLLISION_TEAM_NAME = "vis_no_collide";

    private final Plugin plugin;
    private final NamespacedKey itemKey;

    public Compat121(Plugin plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "vis_utility_item");
    }

    // -----------------------------------------------------------------------
    // Items
    // -----------------------------------------------------------------------

    @Override
    public ItemStack createHideItem(UUID playerId) {
        return createItem(Material.LIME_DYE, ChatColor.RED,
            Messages.itemTotemName(playerId),
            Collections.singletonList(ChatColor.GRAY + Messages.itemTotemLore(playerId)));
    }

    @Override
    public ItemStack createShowItem(UUID playerId) {
        return createItem(Material.GRAY_DYE, ChatColor.GREEN,
            Messages.itemBarrierName(playerId),
            Collections.singletonList(ChatColor.GRAY + Messages.itemBarrierLore(playerId)));
    }

    @Override
    public boolean isPluginItem(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null
            && meta.getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }

    @Override
    public boolean isHideItem(ItemStack item) {
        return item != null && item.getType() == Material.LIME_DYE && isPluginItem(item);
    }

    @Override
    public boolean isShowItem(ItemStack item) {
        return item != null && item.getType() == Material.GRAY_DYE && isPluginItem(item);
    }

    @Override
    public void swapItems(Player player, boolean toHiding) {
        Material from = toHiding ? Material.LIME_DYE : Material.GRAY_DYE;
        Material to   = toHiding ? Material.GRAY_DYE : Material.LIME_DYE;
        UUID id = player.getUniqueId();

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
                if (toHiding) {
                    newMeta.setDisplayName("\u00A7a" + Messages.itemBarrierName(id));
                    newMeta.setLore(Collections.singletonList(
                        "\u00A77" + Messages.itemBarrierLore(id)));
                } else {
                    newMeta.setDisplayName("\u00A7c" + Messages.itemTotemName(id));
                    newMeta.setLore(Collections.singletonList(
                        "\u00A77" + Messages.itemTotemLore(id)));
                }
                newMeta.getPersistentDataContainer().set(
                    itemKey, PersistentDataType.BYTE, (byte) 1);
                for (ItemFlag flag : ItemFlag.values()) {
                    newMeta.addItemFlags(flag);
                }
                replacement.setItemMeta(newMeta);
            }
            player.getInventory().setItem(i, replacement);
        }
    }

    // -----------------------------------------------------------------------
    // Visibility API
    // -----------------------------------------------------------------------

    @Override
    public void hidePlayer(Player viewer, Player target) {
        viewer.hidePlayer(plugin, target);
    }

    @Override
    public void showPlayer(Player viewer, Player target) {
        viewer.showPlayer(plugin, target);
    }

    // -----------------------------------------------------------------------
    // Collision
    // -----------------------------------------------------------------------

    @Override
    public void ensureNoCollision(Player player) {
        Team team = getOrCreateNoCollisionTeam();
        team.addEntry(player.getName());
    }

    @Override
    public void removeFromNoCollision(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(NO_COLLISION_TEAM_NAME);
        if (team != null) {
            team.removeEntry(player.getName());
        }
    }

    @Override
    public void cleanupCollisionTeam() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(NO_COLLISION_TEAM_NAME);
        if (team != null) {
            team.unregister();
        }
    }

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

    // -----------------------------------------------------------------------
    // Locale
    // -----------------------------------------------------------------------

    @Override
    public String getPlayerLocale(Player player) {
        return player.getLocale();
    }

    // -----------------------------------------------------------------------
    // Sounds
    // -----------------------------------------------------------------------

    @Override
    public void playShowSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
    }

    @Override
    public void playHideSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 0.6f);
    }

    @Override
    public void playItemReceiveSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.1f);
    }

    @Override
    public void playClickSound(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
    }

    // -----------------------------------------------------------------------
    // Event helpers
    // -----------------------------------------------------------------------

    @Override
    public boolean isMainHand(PlayerInteractEvent event) {
        return event.getHand() == EquipmentSlot.HAND;
    }

    @Override
    public ItemStack getInteractItem(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            item = event.getPlayer().getInventory().getItem(event.getHand());
        }
        if (item == null || item.getType().isAir()) return null;
        return item;
    }

    // -----------------------------------------------------------------------
    // Feature support
    // -----------------------------------------------------------------------

    @Override
    public boolean supportsDistance() {
        return true;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private ItemStack createItem(Material material, ChatColor color,
                                  String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + displayName);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(
                itemKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }
}
