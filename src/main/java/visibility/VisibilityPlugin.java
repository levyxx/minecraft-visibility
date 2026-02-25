package visibility;

import visibility.command.VisibilityCommand;
import visibility.i18n.Messages;
import visibility.listener.PlayerListener;
import visibility.manager.VisibilityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin entry point. Wires together the manager, command and listener layers.
 *
 * <p>Provides player visibility toggling (hide/show other players) using
 * Totem of Undying / Barrier items and commands, plus collision toggling.</p>
 */
public class VisibilityPlugin extends JavaPlugin {

    private VisibilityManager visibilityManager;
    private NamespacedKey visItemKey;

    @Override
    public void onEnable() {
        this.visItemKey = new NamespacedKey(this, "vis_utility_item");
        this.visibilityManager = new VisibilityManager(this, visItemKey);

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new PlayerListener(visibilityManager, visItemKey), this);

        // Register commands
        VisibilityCommand executor = new VisibilityCommand(this, visibilityManager);

        registerCommand("show", executor);
        registerCommand("hide", executor);
        registerCommand("visibility", executor);

        getLogger().info("Visibility plugin enabled.");
    }

    @Override
    public void onDisable() {
        visibilityManager.clearAll();
        Messages.clearAll();
        this.visibilityManager = null;
        getLogger().info("Visibility plugin disabled.");
    }

    private void registerCommand(String name, VisibilityCommand executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        } else {
            getLogger().severe("Command /" + name + " is not defined in plugin.yml");
        }
    }

    // -----------------------------------------------------------------------
    // Give utility items
    // -----------------------------------------------------------------------

    /**
     * Give the player the visibility toggle item (Blaze Rod / Breeze Rod).
     * The item's type depends on the player's current hiding state.
     */
    public void giveVisibilityItems(Player player) {
        UUID playerId = player.getUniqueId();

        // Detect and set language from client locale if not already set
        initPlayerLang(player);

        boolean hiding = visibilityManager.isHiding(playerId);

        ItemStack toggleItem;
        if (hiding) {
            // Player is currently hiding → give Breeze Rod (right-click to show)
            toggleItem = createUtilityItem(
                Material.BREEZE_ROD,
                ChatColor.GREEN,
                Messages.itemBarrierName(playerId),
                List.of(ChatColor.GRAY + Messages.itemBarrierLore(playerId))
            );
        } else {
            // Player is currently showing → give Blaze Rod (right-click to hide)
            toggleItem = createUtilityItem(
                Material.BLAZE_ROD,
                ChatColor.RED,
                Messages.itemTotemName(playerId),
                List.of(ChatColor.GRAY + Messages.itemTotemLore(playerId))
            );
        }

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(toggleItem);
        if (!leftovers.isEmpty()) {
            leftovers.values().forEach(stack ->
                player.getWorld().dropItemNaturally(player.getLocation(), stack));
        }

        player.sendMessage(ChatColor.AQUA + Messages.itemsReceived(playerId));
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.1f);
    }

    /**
     * Initialize player language from Minecraft client locale if not already set.
     */
    public void initPlayerLang(Player player) {
        UUID playerId = player.getUniqueId();
        Messages.setLang(playerId, Messages.detectLang(player.getLocale()));
    }

    private ItemStack createUtilityItem(Material material, ChatColor color,
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
            meta.getPersistentDataContainer().set(visItemKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }
}
