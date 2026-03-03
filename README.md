# minecraft-visibility

[日本語](#日本語) | [English](#english)

---

## 日本語

Spigot / Paper 対応の Minecraft サーバープラグイン。  
プレイヤーの表示 / 非表示、距離ベースフィルタリング、当たり判定(コリジョン)の無効化を提供します。

### 対応バージョン

| Minecraft | Java | 備考 |
|-----------|------|------|
| 1.8.x | 8+ | コリジョン無効なし・距離フィルタなし |
| 1.12.x | 8+ | コリジョン無効あり・距離フィルタなし |
| 1.21.x | 17+ | フル機能 |

サーバー: Spigot / Paper

### 機能

- **プレイヤー非表示** — `/hide` コマンドまたはアイテム右クリックで周囲のプレイヤーを非表示
- **プレイヤー表示** — `/show` コマンドまたはアイテム右クリックで再表示
- **距離フィルタ** — `/visibility distance <n>` で指定ブロック数以内のプレイヤーのみ非表示（1.21 のみ）
- **トグルアイテム** — `/visibility items` で黄緑の染料 / 灰色の染料の切替アイテムを取得
- **当たり判定 OFF** — 全プレイヤーのコリジョンが常時無効（1.12 / 1.21 のみ）
- **1秒クールダウン** — 表示 / 非表示 / 距離変更は各プレイヤーにつき 1 秒間隔が必要
- **多言語対応** — クライアントロケールに応じて日本語 / 英語を自動判定（手動切替可）

### コマンド一覧

| コマンド | 説明 | 備考 |
|----------|------|------|
| `/show` | 周囲のプレイヤーを表示 | |
| `/hide` | 周囲のプレイヤーを非表示 | |
| `/visibility items` | 表示切替アイテムを取得 | |
| `/visibility distance <n>` | 距離フィルタを設定 (0 で解除) | 1.21 のみ |
| `/visibility language <ja\|en>` | 表示言語を変更 | |
| `/visibility help` | ヘルプを表示 | |

### インストール

1. `mvn clean package` でビルド
2. 対象バージョンの JAR をサーバーの `plugins/` にコピー:
   - 1.8.x → `plugin-1.8/target/minecraft-visibility-1.8.jar`
   - 1.12.x → `plugin-1.12/target/minecraft-visibility-1.12.jar`
   - 1.21.x → `plugin-1.21/target/minecraft-visibility-1.21.jar`
3. サーバーを再起動

### ライセンス

MIT

---

## English

A Minecraft server plugin for Spigot / Paper.  
Toggle player visibility, apply distance-based filtering, and disable player collision.

### Supported Versions

| Minecraft | Java | Notes |
|-----------|------|-------|
| 1.8.x | 8+ | No collision disable, no distance filter |
| 1.12.x | 8+ | Collision disable, no distance filter |
| 1.21.x | 17+ | Full features |

Server: Spigot / Paper

### Features

- **Hide players** — use `/hide` or right-click the toggle item to hide nearby players
- **Show players** — use `/show` or right-click the toggle item to show them again
- **Distance filter** — `/visibility distance <n>` hides only players within n blocks (1.21 only)
- **Toggle items** — `/visibility items` gives lime dye / gray dye toggle items
- **Collision OFF** — all player collisions are permanently disabled (1.12 / 1.21 only)
- **1-second cooldown** — visibility / distance changes have a per-player 1-second cooldown
- **Bilingual** — auto-detects Japanese / English from client locale (manual override available)

### Commands

| Command | Description | Notes |
|---------|-------------|-------|
| `/show` | Show nearby players | |
| `/hide` | Hide nearby players | |
| `/visibility items` | Receive toggle items | |
| `/visibility distance <n>` | Set distance filter (0 to clear) | 1.21 only |
| `/visibility language <ja\|en>` | Change display language | |
| `/visibility help` | Show help | |

### Installation

1. Build with `mvn clean package`
2. Copy the JAR matching your server version to the `plugins/` folder:
   - 1.8.x → `plugin-1.8/target/minecraft-visibility-1.8.jar`
   - 1.12.x → `plugin-1.12/target/minecraft-visibility-1.12.jar`
   - 1.21.x → `plugin-1.21/target/minecraft-visibility-1.21.jar`
3. Restart the server

### License

MIT
