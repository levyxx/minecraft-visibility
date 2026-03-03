package visibility.i18n;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Centralized localization system. Holds all user-facing strings in
 * Japanese (ja) and English (en). Each player's language preference is
 * stored in memory and defaults based on their Minecraft client locale.
 * Manual language choices (set via command) are persisted to languages.yml.
 */
public final class Messages {

    public enum Lang { JA, EN }

    private static final Map<UUID, Lang> playerLangs = new ConcurrentHashMap<UUID, Lang>();

    /** Players who have explicitly chosen their language via command. */
    private static final Set<UUID> manualLangSet = ConcurrentHashMap.newKeySet();

    /** Plugin data folder for persistence (set via {@link #init}). */
    private static volatile File dataFolder;

    private Messages() {}

    // -----------------------------------------------------------------------
    // Initialization
    // -----------------------------------------------------------------------

    /**
     * Initialize with the plugin data folder and load persisted manual languages.
     */
    public static void init(File folder) {
        dataFolder = folder;
        loadManualLangs();
    }

    // -----------------------------------------------------------------------
    // Player language management
    // -----------------------------------------------------------------------

    public static void setLang(UUID playerId, Lang lang) {
        if (!manualLangSet.contains(playerId)) {
            playerLangs.put(playerId, lang);
        }
    }

    public static void setLangManual(UUID playerId, Lang lang) {
        manualLangSet.add(playerId);
        playerLangs.put(playerId, lang);
        saveManualLangs();
    }

    public static Lang getLang(UUID playerId) {
        Lang lang = playerLangs.get(playerId);
        return lang != null ? lang : Lang.JA;
    }

    public static void removeLang(UUID playerId) {
        if (!manualLangSet.contains(playerId)) {
            playerLangs.remove(playerId);
        }
        // Keep manually-set language preferences across sessions
    }

    public static void clearAll() {
        playerLangs.clear();
        manualLangSet.clear();
        dataFolder = null;
    }

    public static Lang detectLang(String locale) {
        if (locale != null && locale.toLowerCase().startsWith("ja")) return Lang.JA;
        return Lang.EN;
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    public static String get(UUID playerId, String ja, String en) {
        return getLang(playerId) == Lang.JA ? ja : en;
    }

    // -----------------------------------------------------------------------
    // Command messages
    // -----------------------------------------------------------------------

    public static String cmdPlayerOnly(UUID id) {
        return get(id, "このコマンドはプレイヤーのみ使用できます。", "This command can only be used by players.");
    }

    public static String cmdUsage(UUID id, String l) {
        return get(id,
            "/" + l + " help でコマンド一覧を確認できます。",
            "Run /" + l + " help to see available commands.");
    }

    public static String cmdUsageLanguage(UUID id, String l) {
        return get(id,
            "使い方: /" + l + " language <ja|en>",
            "Usage: /" + l + " language <ja|en>");
    }

    public static String cmdLangChanged(UUID id) {
        return get(id, "言語を日本語に変更しました。", "Language changed to English.");
    }

    // -----------------------------------------------------------------------
    // Show / Hide messages
    // -----------------------------------------------------------------------

    public static String playersHidden(UUID id) {
        return get(id, "周りのプレイヤーを非表示にしました。", "Nearby players are now hidden.");
    }

    public static String playersShown(UUID id) {
        return get(id, "周りのプレイヤーを表示しました。", "Nearby players are now visible.");
    }

    public static String alreadyHidden(UUID id) {
        return get(id, "既にプレイヤーは非表示です。", "Players are already hidden.");
    }

    public static String alreadyShown(UUID id) {
        return get(id, "既にプレイヤーは表示中です。", "Players are already visible.");
    }

    // -----------------------------------------------------------------------
    // Distance messages
    // -----------------------------------------------------------------------

    public static String distanceSet(UUID id, int n) {
        return get(id,
            "距離を " + n + " ブロックに設定しました。" + n + " ブロック以内のプレイヤーのみ非表示になります。",
            "Distance set to " + n + " blocks. Only players within " + n + " blocks will be hidden.");
    }

    public static String distanceCleared(UUID id) {
        return get(id,
            "距離設定を解除しました。全プレイヤーが非表示/表示の対象になります。",
            "Distance filter cleared. All players will be hidden/shown.");
    }

    public static String distanceInvalid(UUID id) {
        return get(id,
            "正の整数を指定してください。0で距離設定を解除します。",
            "Please specify a positive integer. Use 0 to clear the distance filter.");
    }

    public static String distanceUsage(UUID id, String l) {
        return get(id,
            "使い方: /" + l + " distance <ブロック数>  (0で解除)",
            "Usage: /" + l + " distance <blocks>  (0 to clear)");
    }

    public static String distanceNotSupported(UUID id) {
        return get(id,
            "距離フィルタはこのバージョンでは使用できません。",
            "Distance filter is not available on this server version.");
    }

    // -----------------------------------------------------------------------
    // Cooldown messages
    // -----------------------------------------------------------------------

    public static String cooldownActive(UUID id) {
        return get(id,
            "操作が速すぎます。1秒待ってからもう一度お試しください。",
            "Please wait 1 second before doing that again.");
    }

    // -----------------------------------------------------------------------
    // Items
    // -----------------------------------------------------------------------

    public static String itemsReceived(UUID id) {
        return get(id,
            "プレイヤー表示切替アイテムを受け取りました。所持品を確認してください。",
            "Visibility toggle items received. Check your inventory.");
    }

    public static String itemTotemName(UUID id) {
        return get(id, "プレイヤー非表示", "Hide Players");
    }

    public static String itemTotemLore(UUID id) {
        return get(id, "右クリック: 周りのプレイヤーを非表示", "Right-click: Hide nearby players");
    }

    public static String itemBarrierName(UUID id) {
        return get(id, "プレイヤー表示", "Show Players");
    }

    public static String itemBarrierLore(UUID id) {
        return get(id, "右クリック: 周りのプレイヤーを表示", "Right-click: Show nearby players");
    }

    // -----------------------------------------------------------------------
    // Help messages
    // -----------------------------------------------------------------------

    private static final String HC = "\u00A7e";
    private static final String HD = "\u00A77";

    public static String helpTitle(UUID id) {
        return get(id, "コマンド一覧", "Command List");
    }

    public static String helpShow(UUID id) {
        return get(id,
            HC + "/show" + HD + "  周りのプレイヤーを表示します",
            HC + "/show" + HD + "  Show nearby players");
    }

    public static String helpHide(UUID id) {
        return get(id,
            HC + "/hide" + HD + "  周りのプレイヤーを非表示にします",
            HC + "/hide" + HD + "  Hide nearby players");
    }

    public static String helpDistance(UUID id, String l) {
        return get(id,
            HC + "/" + l + " distance <n>" + HD + "  n ブロック以内のプレイヤーのみ非表示にします (0で解除)",
            HC + "/" + l + " distance <n>" + HD + "  Hide only players within n blocks (0 to clear)");
    }

    public static String helpItems(UUID id, String l) {
        return get(id,
            HC + "/" + l + " items" + HD + "  表示切替用アイテムを受け取ります",
            HC + "/" + l + " items" + HD + "  Receive visibility toggle items");
    }

    public static String helpLanguage(UUID id, String l) {
        return get(id,
            HC + "/" + l + " language <ja|en>" + HD + "  言語を変更します",
            HC + "/" + l + " language <ja|en>" + HD + "  Change language");
    }

    public static String helpHelp(UUID id, String l) {
        return get(id,
            HC + "/" + l + " help" + HD + "  このヘルプを表示します",
            HC + "/" + l + " help" + HD + "  Show this help");
    }

    // -----------------------------------------------------------------------
    // Language persistence
    // -----------------------------------------------------------------------

    /**
     * Save manually-set language preferences to languages.yml.
     */
    public static void saveManualLangs() {
        if (dataFolder == null) return;
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File file = new File(dataFolder, "languages.yml");
        YamlConfiguration config = new YamlConfiguration();
        for (UUID uuid : manualLangSet) {
            Lang lang = playerLangs.get(uuid);
            if (lang != null) {
                config.set(uuid.toString(), lang == Lang.JA ? "ja" : "en");
            }
        }

        try {
            config.save(file);
        } catch (IOException ignored) {
        }
    }

    private static void loadManualLangs() {
        if (dataFolder == null) return;
        File file = new File(dataFolder, "languages.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String langStr = config.getString(key, "ja");
                Lang lang = "en".equalsIgnoreCase(langStr) ? Lang.EN : Lang.JA;
                manualLangSet.add(uuid);
                playerLangs.put(uuid, lang);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}
