# アーキテクチャ

## レイヤ構成

```
┌─────────────────────────────────────────┐
│           VisibilityPlugin              │  エントリポイント / DI
│  (JavaPlugin 継承・配線・アイテム生成)   │
└─────────┬───────────┬───────────────────┘
          │           │
    ┌─────▼─────┐  ┌──▼──────────────┐
    │ Command   │  │   Listener      │  ユーザー入力受付
    │ Layer     │  │   Layer         │
    └─────┬─────┘  └──┬──────────────┘
          │           │
    ┌─────▼───────────▼──────────────┐
    │       VisibilityManager        │  ビジネスロジック
    │  (状態管理・表示制御・距離・    │
    │   クールダウン・コリジョン)     │
    └────────────────────────────────┘
          │
    ┌─────▼──────────────────────────┐
    │         Messages (i18n)        │  多言語メッセージ
    └────────────────────────────────┘
```

## クラス詳細

### VisibilityPlugin

- `JavaPlugin` を継承したプラグインエントリポイント
- `onEnable()` でマネージャ生成、リスナー・コマンド登録
- `onDisable()` で全状態クリア、全プレイヤー再表示
- ユーティリティアイテム（Totem / Barrier）の生成ロジックを持つ
- `NamespacedKey` でプラグイン固有アイテムをタグ付け

### VisibilityCommand

- `TabExecutor` を実装
- `/show`, `/hide`, `/visibility` の3コマンドを処理
- 各操作の前にクールダウンチェック（`VisibilityManager.isOnCooldown()`）を行う
- サブコマンド: `items`, `distance`, `language`, `help`
- タブ補完対応

### PlayerListener

- `PlayerJoinEvent` — 言語自動設定、表示状態リセット、コリジョン無効化
- `PlayerQuitEvent` — 状態クリーンアップ
- `PlayerInteractEvent` — ユーティリティアイテムの左クリック検出（クールダウンチェック付き）
- `PlayerDropItemEvent` — プラグインアイテムのドロップ防止

### VisibilityManager

プラグインのコアロジック。以下の状態を `ConcurrentHashMap` で管理:

| データ | 型 | 説明 |
|--------|-----|------|
| `hidingState` | `Map<UUID, Boolean>` | プレイヤーの非表示モード |
| `distanceThreshold` | `Map<UUID, Integer>` | 距離フィルタ閾値 (ブロック数) |
| `lastActionTime` | `Map<UUID, Long>` | 最終操作タイムスタンプ (ms) |

主要ロジック:
- **表示切替**: `showPlayers()` / `hidePlayers()` で Bukkit の `showPlayer()` / `hidePlayer()` を呼び出し、アイテムを Totem ↔ Barrier に変換
- **距離フィルタ**: 閾値設定時にスケジューラタスク(10 tick 間隔)を起動し、距離に応じて動的に表示/非表示を更新
- **クールダウン**: 各操作時に `System.currentTimeMillis()` を記録し、1秒以内の再操作をブロック
- **コリジョン**: スコアボードチーム `vis_no_collide`（`COLLISION_RULE = NEVER`）に全プレイヤーを参加時に自動追加

### Messages

- `enum Lang { JP, EN }` で言語を管理
- プレイヤーごとに `ConcurrentHashMap<UUID, Lang>` で保持
- クライアントロケール（`player.getLocale()`）から自動判定
- 全メッセージは `get(id, jp, en)` ヘルパーで言語選択

## データフロー

### プレイヤー非表示フロー

```
1. ユーザー: /hide or アイテム左クリック
2. Command/Listener: クールダウンチェック → NG ならメッセージ表示して終了
3. VisibilityManager.hidePlayers():
   a. 状態を hiding=true に更新
   b. applyVisibility(): 全オンラインプレイヤーに対し hidePlayer() 呼出
   c. アイテムを Totem → Barrier に変換
   d. lastActionTime を記録
4. プレイヤーにメッセージ＋効果音を送信
```

### プレイヤー参加フロー

```
1. PlayerJoinEvent 発火
2. PlayerListener:
   a. クライアントロケールから言語を設定
   b. VisibilityManager.handlePlayerJoin():
      - 全プレイヤーを表示状態にリセット
      - no-collision チームに追加（当たり判定無効）
```

## 設計方針

- **シンプルさ**: 外部依存なし（Spigot API のみ）
- **インメモリ**: 永続化なし。サーバー再起動で全状態リセット
- **プレイヤー体験**: 効果音・色付きメッセージ・自動言語検出で直感的な UX
- **安全性**: ドロップ防止、クールダウンで連続操作を抑制
