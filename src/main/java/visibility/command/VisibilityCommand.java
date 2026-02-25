package visibility.command;

import visibility.VisibilityPlugin;
import visibility.i18n.Messages;
import visibility.i18n.Messages.Lang;
import visibility.manager.VisibilityManager;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/**
 * Handles commands: /show, /hide, /visibility, /collision
 */
public class VisibilityCommand implements TabExecutor {

    private final VisibilityPlugin plugin;
    private final VisibilityManager visibilityManager;

    public VisibilityCommand(VisibilityPlugin plugin, VisibilityManager visibilityManager) {
        this.plugin = plugin;
        this.visibilityManager = visibilityManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        UUID playerId = player.getUniqueId();
        String cmdName = command.getName().toLowerCase(Locale.ROOT);

        switch (cmdName) {
            case "show" -> handleShow(player, playerId);
            case "hide" -> handleHide(player, playerId);
            case "visibility" -> handleVisibilityCommand(player, playerId, label, args);
            default -> {}
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // /show
    // -----------------------------------------------------------------------

    private void handleShow(Player player, UUID playerId) {
        if (visibilityManager.isOnCooldown(playerId)) {
            player.sendMessage(ChatColor.RED + Messages.cooldownActive(playerId));
            return;
        }
        boolean changed = visibilityManager.showPlayers(player);
        if (changed) {
            player.sendMessage(ChatColor.GREEN + Messages.playersShown(playerId));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
        } else {
            player.sendMessage(ChatColor.GRAY + Messages.alreadyShown(playerId));
        }
    }

    // -----------------------------------------------------------------------
    // /hide
    // -----------------------------------------------------------------------

    private void handleHide(Player player, UUID playerId) {
        if (visibilityManager.isOnCooldown(playerId)) {
            player.sendMessage(ChatColor.RED + Messages.cooldownActive(playerId));
            return;
        }
        boolean changed = visibilityManager.hidePlayers(player);
        if (changed) {
            player.sendMessage(ChatColor.GREEN + Messages.playersHidden(playerId));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 0.6f);
        } else {
            player.sendMessage(ChatColor.GRAY + Messages.alreadyHidden(playerId));
        }
    }

    // -----------------------------------------------------------------------
    // /visibility <subcommand>
    // -----------------------------------------------------------------------

    private void handleVisibilityCommand(Player player, UUID playerId, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(player, playerId, label);
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "items" -> plugin.giveVisibilityItems(player);
            case "distance", "dist" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + Messages.distanceUsage(playerId, label));
                    return;
                }
                handleDistance(player, playerId, args[1]);
            }
            case "language", "lang" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + Messages.cmdUsageLanguage(playerId, label));
                    return;
                }
                handleLanguage(player, playerId, args[1]);
            }
            case "help" -> sendHelp(player, playerId, label);
            default -> sendUsage(player, playerId, label);
        }
    }

    private void handleDistance(Player player, UUID playerId, String value) {
        if (visibilityManager.isOnCooldown(playerId)) {
            player.sendMessage(ChatColor.RED + Messages.cooldownActive(playerId));
            return;
        }
        int n;
        try {
            n = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + Messages.distanceInvalid(playerId));
            return;
        }

        if (n < 0) {
            player.sendMessage(ChatColor.RED + Messages.distanceInvalid(playerId));
            return;
        }

        visibilityManager.setDistance(playerId, n);

        if (n == 0) {
            player.sendMessage(ChatColor.GREEN + Messages.distanceCleared(playerId));
        } else {
            player.sendMessage(ChatColor.GREEN + Messages.distanceSet(playerId, n));
        }

        // Re-apply visibility if currently hiding
        if (visibilityManager.isHiding(playerId)) {
            visibilityManager.applyVisibility(player);
        }

        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
    }

    private void handleLanguage(Player player, UUID playerId, String langArg) {
        String lower = langArg.toLowerCase(Locale.ROOT);
        if ("ja".equals(lower)) {
            Messages.setLangManual(playerId, Lang.JA);
        } else if ("en".equals(lower)) {
            Messages.setLangManual(playerId, Lang.EN);
        } else {
            player.sendMessage(ChatColor.RED + Messages.cmdUsageLanguage(playerId, "visibility"));
            return;
        }
        player.sendMessage(ChatColor.GREEN + Messages.cmdLangChanged(playerId));
    }

    private void sendUsage(Player player, UUID playerId, String label) {
        player.sendMessage(ChatColor.GRAY + Messages.cmdUsage(playerId, label));
    }

    private void sendHelp(Player player, UUID playerId, String label) {
        player.sendMessage(ChatColor.DARK_AQUA + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage(ChatColor.AQUA + "  /" + label + " " + Messages.helpTitle(playerId));
        player.sendMessage(ChatColor.DARK_AQUA + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage(Messages.helpShow(playerId));
        player.sendMessage(Messages.helpHide(playerId));
        player.sendMessage(Messages.helpDistance(playerId, label));
        player.sendMessage(Messages.helpItems(playerId, label));
        player.sendMessage(Messages.helpLanguage(playerId, label));
        player.sendMessage(Messages.helpHelp(playerId, label));
        player.sendMessage(ChatColor.DARK_AQUA + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // -----------------------------------------------------------------------
    // Tab completion
    // -----------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return List.of();
        }

        String cmdName = command.getName().toLowerCase(Locale.ROOT);

        // /show, /hide, /collision have no sub-args
        if (!"visibility".equals(cmdName)) {
            return List.of();
        }

        if (args.length == 1) {
            return List.of("items", "distance", "language", "help").stream()
                .filter(opt -> opt.startsWith(args[0].toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ("language".equals(sub) || "lang".equals(sub)) {
                return List.of("ja", "en").stream()
                    .filter(opt -> opt.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
            }
            if ("distance".equals(sub) || "dist".equals(sub)) {
                return List.of("0", "5", "10", "20", "50").stream()
                    .filter(opt -> opt.startsWith(args[1]))
                    .collect(Collectors.toList());
            }
        }

        return List.of();
    }
}
