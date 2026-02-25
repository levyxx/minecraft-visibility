package visibility.i18n;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized localization system. Holds all user-facing strings in
 * Japanese (jp) and English (en). Each player's language preference is
 * stored in memory and defaults based on their Minecraft client locale.
 */
public final class Messages {

    public enum Lang { JP, EN }

    private static final Map<UUID, Lang> playerLangs = new ConcurrentHashMap<>();

    /** Players who have explicitly chosen their language via command. */
    private static final java.util.Set<UUID> manualLangSet = ConcurrentHashMap.newKeySet();

    private Messages() {}

    // -----------------------------------------------------------------------
    // Player language management
    // -----------------------------------------------------------------------

    /**
     * Set language from auto-detection (locale on join / items command).
     * Ignored if the player has already set their language manually.
     */
    public static void setLang(UUID playerId, Lang lang) {
        if (!manualLangSet.contains(playerId)) {
            playerLangs.put(playerId, lang);
        }
    }

    /**
     * Set language explicitly by player command. Persists until logout.
     */
    public static void setLangManual(UUID playerId, Lang lang) {
        manualLangSet.add(playerId);
        playerLangs.put(playerId, lang);
    }

    public static Lang getLang(UUID playerId) { return playerLangs.getOrDefault(playerId, Lang.JP); }

    public static void removeLang(UUID playerId) {
        playerLangs.remove(playerId);
        manualLangSet.remove(playerId);
    }

    public static void clearAll() {
        playerLangs.clear();
        manualLangSet.clear();
    }

    /** Determine default language from Minecraft client locale string. */
    public static Lang detectLang(String locale) {
        if (locale != null && locale.toLowerCase().startsWith("ja")) return Lang.JP;
        return Lang.EN;
    }

    // -----------------------------------------------------------------------
    // Helper: pick JP/EN string
    // -----------------------------------------------------------------------

    public static String get(UUID playerId, String jp, String en) {
        return getLang(playerId) == Lang.JP ? jp : en;
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
            "使い方: /" + l + " language <jp|en>",
            "Usage: /" + l + " language <jp|en>");
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

    // -----------------------------------------------------------------------
    // Collision messages
    // -----------------------------------------------------------------------

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

    private static final String HC = "\u00A7e";   // command color (YELLOW)
    private static final String HD = "\u00A77";   // description color (GRAY)

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
            HC + "/" + l + " language <jp|en>" + HD + "  言語を変更します",
            HC + "/" + l + " language <jp|en>" + HD + "  Change language");
    }

    public static String helpHelp(UUID id, String l) {
        return get(id,
            HC + "/" + l + " help" + HD + "  このヘルプを表示します",
            HC + "/" + l + " help" + HD + "  Show this help");
    }
}
