package visibility.command;

import visibility.VisibilityPluginBase;
import visibility.compat.VersionCompat;
import visibility.i18n.Messages;
import visibility.i18n.Messages.Lang;
import visibility.manager.VisibilityManager;
import java.util.Arrays;
import java.util.Collections;
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
 * Handles commands: /show, /hide, /visibility.
 * Version-specific sound playback is delegated to {@link VersionCompat}.
 */
public class VisibilityCommand implements TabExecutor {

    private final VisibilityPluginBase plugin;
    private final VisibilityManager visibilityManager;
    private final VersionCompat compat;

    public VisibilityCommand(VisibilityPluginBase plugin,
                             VisibilityManager visibilityManager,
                             VersionCompat compat) {
        this.plugin = plugin;
        this.visibilityManager = visibilityManager;
        this.compat = compat;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        String cmdName = command.getName().toLowerCase(Locale.ROOT);

        if ("show".equals(cmdName)) {
            handleShow(player, playerId);
        } else if ("hide".equals(cmdName)) {
            handleHide(player, playerId);
        } else if ("visibility".equals(cmdName)) {
            handleVisibilityCommand(player, playerId, label, args);
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
            compat.playShowSound(player);
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
            compat.playHideSound(player);
        } else {
            player.sendMessage(ChatColor.GRAY + Messages.alreadyHidden(playerId));
        }
    }

    // -----------------------------------------------------------------------
    // /visibility <subcommand>
    // -----------------------------------------------------------------------

    private void handleVisibilityCommand(Player player, UUID playerId,
                                         String label, String[] args) {
        if (args.length == 0) {
            sendUsage(player, playerId, label);
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if ("items".equals(sub)) {
            plugin.giveVisibilityItems(player);
        } else if ("distance".equals(sub) || "dist".equals(sub)) {
            if (!compat.supportsDistance()) {
                player.sendMessage(ChatColor.RED + Messages.distanceNotSupported(playerId));
                return;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + Messages.distanceUsage(playerId, label));
                return;
            }
            handleDistance(player, playerId, args[1]);
        } else if ("language".equals(sub) || "lang".equals(sub)) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + Messages.cmdUsageLanguage(playerId, label));
                return;
            }
            handleLanguage(player, playerId, args[1]);
        } else if ("help".equals(sub)) {
            sendHelp(player, playerId, label);
        } else {
            sendUsage(player, playerId, label);
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

        if (visibilityManager.isHiding(playerId)) {
            visibilityManager.applyVisibility(player);
        }

        compat.playClickSound(player);
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
        if (compat.supportsDistance()) {
            player.sendMessage(Messages.helpDistance(playerId, label));
        }
        player.sendMessage(Messages.helpItems(playerId, label));
        player.sendMessage(Messages.helpLanguage(playerId, label));
        player.sendMessage(Messages.helpHelp(playerId, label));
        player.sendMessage(ChatColor.DARK_AQUA + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // -----------------------------------------------------------------------
    // Tab completion
    // -----------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        String cmdName = command.getName().toLowerCase(Locale.ROOT);

        if (!"visibility".equals(cmdName)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> options;
            if (compat.supportsDistance()) {
                options = Arrays.asList("items", "distance", "language", "help");
            } else {
                options = Arrays.asList("items", "language", "help");
            }
            return options.stream()
                .filter(opt -> opt.startsWith(args[0].toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ("language".equals(sub) || "lang".equals(sub)) {
                return Arrays.asList("ja", "en").stream()
                    .filter(opt -> opt.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
            }
            if ("distance".equals(sub) || "dist".equals(sub)) {
                return Arrays.asList("0", "5", "10", "20", "50").stream()
                    .filter(opt -> opt.startsWith(args[1]))
                    .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
