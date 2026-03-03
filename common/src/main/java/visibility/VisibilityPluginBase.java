package visibility;

import visibility.compat.VersionCompat;
import visibility.command.VisibilityCommand;
import visibility.i18n.Messages;
import visibility.listener.PlayerListener;
import visibility.manager.VisibilityManager;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Base plugin class. Each version module provides a thin subclass that
 * supplies the appropriate {@link VersionCompat} implementation.
 */
public abstract class VisibilityPluginBase extends JavaPlugin {

    private VisibilityManager visibilityManager;
    private VersionCompat compat;

    /**
     * Subclasses must return the version-specific compat implementation.
     */
    protected abstract VersionCompat createCompat();

    @Override
    public void onEnable() {
        this.compat = createCompat();
        this.visibilityManager = new VisibilityManager(this, compat);

        Messages.init(getDataFolder());

        Bukkit.getPluginManager().registerEvents(
            new PlayerListener(this, visibilityManager, compat), this);

        VisibilityCommand executor = new VisibilityCommand(this, visibilityManager, compat);
        registerCommand("show", executor);
        registerCommand("hide", executor);
        registerCommand("visibility", executor);

        if (compat.supportsDistance()) {
            visibilityManager.loadDistances();
        }

        getLogger().info("Visibility plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (compat != null && compat.supportsDistance()) {
            visibilityManager.saveDistances();
        }
        Messages.saveManualLangs();
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
     * Give the player the visibility toggle item (lime/gray dye).
     * The item type depends on the player's current hiding state.
     */
    public void giveVisibilityItems(Player player) {
        UUID playerId = player.getUniqueId();
        initPlayerLang(player);

        boolean hiding = visibilityManager.isHiding(playerId);
        ItemStack toggleItem;
        if (hiding) {
            toggleItem = compat.createShowItem(playerId);
        } else {
            toggleItem = compat.createHideItem(playerId);
        }

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(toggleItem);
        if (!leftovers.isEmpty()) {
            for (ItemStack stack : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), stack);
            }
        }

        player.sendMessage(ChatColor.AQUA + Messages.itemsReceived(playerId));
        compat.playItemReceiveSound(player);
    }

    /**
     * Initialize player language from client locale (if available).
     */
    public void initPlayerLang(Player player) {
        UUID playerId = player.getUniqueId();
        String locale = compat.getPlayerLocale(player);
        if (locale != null) {
            Messages.setLang(playerId, Messages.detectLang(locale));
        } else {
            Messages.setLang(playerId, Messages.Lang.JA);
        }
    }
}
