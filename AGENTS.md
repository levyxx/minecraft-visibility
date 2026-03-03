# AGENTS.md — AI エージェント向けガイド

このドキュメントは、AI コーディングエージェントがこのリポジトリを理解し、安全に変更を加えるためのガイドです。

## プロジェクト概要

Minecraft プレイヤーの表示 / 非表示切替、距離ベースフィルタを提供する Spigot プラグイン。  
**common + compat パターン**のマルチモジュール構成で、3 つの Minecraft バージョン向けにビルド可能:

| モジュール | 対象 MC バージョン | Spigot API | Java | 備考 |
|---|---|---|---|---|
| `common` | — | 1.8.8-R0.1-SNAPSHOT (コンパイル用) | 8 | 共通ロジック。単体では動作しない |
| `plugin-1.8` | 1.8.x | 1.8.8-R0.1-SNAPSHOT | 8 | コリジョン無効なし、レガシーAPI |
| `plugin-1.12` | 1.12.x | 1.12.2-R0.1-SNAPSHOT | 8 | コリジョン無効あり |
| `plugin-1.21` | 1.21.x | 1.21.4-R0.1-SNAPSHOT | 17 | フル機能 |

`maven-shade-plugin` により、各 `plugin-*` JAR に `common` が同梱される。

## ビルド

```bash
mvn clean package
```

成果物:
- `plugin-1.8/target/minecraft-visibility-1.8.jar`
- `plugin-1.12/target/minecraft-visibility-1.12.jar`
- `plugin-1.21/target/minecraft-visibility-1.21.jar`

## ディレクトリ構成

```
pom.xml                          # 親 POM (マルチモジュール)
common/                          # 共通モジュール
├── pom.xml
└── src/main/java/visibility/
    ├── VisibilityPluginBase.java    # 抽象基底クラス (JavaPlugin)
    ├── compat/VersionCompat.java    # バージョン差分を吸収するインタフェース
    ├── command/VisibilityCommand.java
    ├── i18n/Messages.java
    ├── listener/PlayerListener.java
    └── manager/VisibilityManager.java
plugin-1.8/                      # Minecraft 1.8.x 用
├── pom.xml
└── src/main/java/visibility/
    ├── VisibilityPlugin.java        # VisibilityPluginBase の薄いサブクラス
    └── compat/Compat18.java         # VersionCompat 実装
plugin-1.12/                     # Minecraft 1.12.x 用
├── pom.xml
└── src/main/java/visibility/
    ├── VisibilityPlugin.java
    └── compat/Compat112.java
plugin-1.21/                     # Minecraft 1.21.x 用
├── pom.xml
└── src/main/java/visibility/
    ├── VisibilityPlugin.java
    └── compat/Compat121.java
```

## アーキテクチャ概要

### common + compat パターン

共通ロジックは `common` モジュールに集約し、バージョン固有の API 呼び出しは `VersionCompat` インタフェースで抽象化する。各 `plugin-*` モジュールは `VersionCompat` の具象クラスと、`VisibilityPluginBase` を継承する薄いエントリポイントのみを持つ。

### common モジュールのクラス

- **VisibilityPluginBase** — `JavaPlugin` を継承する抽象クラス。`abstract VersionCompat createCompat()` を定義。マネージャ、コマンド、リスナーの配線と、ユーティリティアイテム（黄緑 / 灰色の染料）生成を担当。
- **VersionCompat** — バージョン差分を吸収するインタフェース。アイテム生成/判定、表示/非表示API、コリジョン、ロケール検出、効果音、イベントヘルパーを定義。
- **VisibilityManager** — 表示 / 非表示状態、距離閾値、クールダウン管理を保持するステートフルシングルトン。全状態はインメモリ（`ConcurrentHashMap`）。バージョン固有の操作は `VersionCompat` に委譲。
- **VisibilityCommand** — `/show`, `/hide`, `/visibility` のコマンドハンドラ。クールダウンチェックを含む。
- **PlayerListener** — アイテムクリック（染料の右クリック）、ドロップ防止、Join/Quit イベントを処理。バージョン固有の操作は `VersionCompat` に委譲。
- **Messages** — 日英のメッセージ文字列を一元管理。プレイヤーごとの言語設定を保持。

### VersionCompat が抽象化する差分

| メソッド群 | 目的 |
|---|---|
| `createHideItem`, `createShowItem` | アイテム生成（素材・メタデータ方式の差分） |
| `isPluginItem`, `isHideItem`, `isShowItem` | アイテム判定 |
| `swapItems` | 表示/非表示切替時のインベントリアイテム入替 |
| `hidePlayer`, `showPlayer` | 表示/非表示 API（引数の差分） |
| `ensureNoCollision`, `removeFromNoCollision`, `cleanupCollisionTeam` | コリジョン管理 |
| `getPlayerLocale` | 言語自動検出 |
| `playShowSound`, `playHideSound`, `playItemReceiveSound`, `playClickSound` | 効果音 |
| `isMainHand`, `getInteractItem` | イベント処理ヘルパー |

### バージョン間の主な差分

| 機能 | 1.8.x (Compat18) | 1.12.x (Compat112) | 1.21.x (Compat121) |
|---|---|---|---|
| アイテム素材 | `INK_SACK` (data 10/8) | `INK_SACK` (data 10/8) | `LIME_DYE` / `GRAY_DYE` |
| アイテム判定 | ロアマーカー | ロアマーカー | `PersistentDataContainer` |
| hidePlayer API | `hidePlayer(Player)` | `hidePlayer(Plugin, Player)` | `hidePlayer(Plugin, Player)` |
| 効果音 | `ORB_PICKUP` 等 | `ENTITY_EXPERIENCE_ORB_PICKUP` 等 | `ENTITY_EXPERIENCE_ORB_PICKUP` 等 |
| コリジョン無効 | no-op (1.9+ 機能) | `Team.Option.COLLISION_RULE` | `Team.Option.COLLISION_RULE` |
| 言語自動検出 | `null` (デフォルト JA) | `Player.getLocale()` | `Player.getLocale()` |
| isMainHand | 常に `true` | `EquipmentSlot.HAND` | `EquipmentSlot.HAND` |

## コーディングルール

- **common モジュール**: Java 8 互換必須。`List.of()` (Java 9+)、`var` (Java 10+)、switch 式 (Java 14+) 等は使用不可。`Arrays.asList()` / `Collections.singletonList()` を使用。
- **plugin-1.21 モジュール**: Java 17 の機能（パターンマッチング `instanceof`、`switch` 式等）を使用可。
- `ConcurrentHashMap` でスレッドセーフを確保（Bukkit のメインスレッドとスケジューラの両方からアクセスされうるため）
- バージョン固有の API 呼び出しはすべて `VersionCompat` を経由すること。共通コード内で直接呼ばない。
- メッセージ追加時は `common` の `Messages.java` に JP/EN 両方を追加
- `plugin.yml` にコマンドを追加する場合は `VisibilityPluginBase.registerCommand` も対応させること
- 新しいバージョン差分が必要な場合は `VersionCompat` にメソッドを追加し、各 `Compat*` クラスで実装すること

## 重要な制約

- **クールダウン**: 表示 / 非表示 / 距離変更は各プレイヤーにつき1秒間隔でしか実行できない
- **コリジョン**: プレイヤー参加時にスコアボードチームに自動追加し、全プレイヤーの当たり判定を常時無効にする（1.12/1.21 のみ）。トグル機能は存在しない
- **アイテムドロップ**: プラグインのユーティリティアイテムはドロップ不可
- **距離タスク**: 距離閾値を持つプレイヤーが存在するときのみ繰り返しタスクが稼働する
- **Shade**: 各 `plugin-*` の JAR は `maven-shade-plugin` で `common` を同梱。`createDependencyReducedPom` は `false`

## テスト

```bash
mvn test
```

現在テストは最小限。Bukkit API をモックするには Mockito + MockBukkit が必要。
