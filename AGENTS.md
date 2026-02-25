# AGENTS.md — AI エージェント向けガイド

このドキュメントは、AI コーディングエージェントがこのリポジトリを理解し、安全に変更を加えるためのガイドです。

## プロジェクト概要

Spigot API 1.20 向け Minecraft プラグイン（Java 17）。  
プレイヤーの表示 / 非表示切替、距離ベースフィルタ、常時コリジョン無効を提供します。

## ビルド

```bash
mvn clean package
```

成果物: `target/minecraft-visibility-1.0.0.jar`

## ディレクトリ構成

```
src/main/java/visibility/
├── VisibilityPlugin.java    # プラグインエントリポイント
├── command/
│   └── VisibilityCommand.java  # コマンドハンドラ (/show, /hide, /visibility)
├── i18n/
│   └── Messages.java           # 多言語メッセージ (JP/EN)
├── listener/
│   └── PlayerListener.java     # イベントリスナー (join/quit/interact/drop)
└── manager/
    └── VisibilityManager.java  # ビジネスロジック (表示制御・距離・コリジョン・クールダウン)
```

## アーキテクチャ概要

- **VisibilityPlugin** — `JavaPlugin` を継承。マネージャ、コマンド、リスナーの配線と、ユーティリティアイテム生成を担当。
- **VisibilityManager** — 表示 / 非表示状態、距離閾値、クールダウン、コリジョンチーム管理を保持するステートフルシングルトン。全状態はインメモリ（`ConcurrentHashMap`）。
- **VisibilityCommand** — `/show`, `/hide`, `/visibility` のコマンドハンドラ。クールダウンチェックを含む。
- **PlayerListener** — アイテムクリック、ドロップ防止、Join/Quit イベントを処理。
- **Messages** — 日英のメッセージ文字列を一元管理。プレイヤーごとの言語設定を保持。

## コーディングルール

- Java 17 の機能（パターンマッチング `instanceof`、`switch` 式）を使用可
- `ConcurrentHashMap` でスレッドセーフを確保（Bukkit のメインスレッドとスケジューラの両方からアクセスされうるため）
- Bukkit API の `showPlayer` / `hidePlayer` は必ず `Plugin` インスタンスを引数に使用
- メッセージ追加時は `Messages.java` に JP/EN 両方を追加
- `plugin.yml` にコマンドを追加する場合は `VisibilityPlugin.registerCommand` も対応させること

## 重要な制約

- **クールダウン**: 表示 / 非表示 / 距離変更は各プレイヤーにつき1秒間隔でしか実行できない
- **コリジョン**: プレイヤー参加時にスコアボードチームに自動追加し、全プレイヤーの当たり判定を常時無効にする。トグル機能は存在しない
- **アイテムドロップ**: プラグインのユーティリティアイテムはドロップ不可
- **距離タスク**: 距離閾値を持つプレイヤーが存在するときのみ繰り返しタスクが稼働する

## テスト

```bash
mvn test
```

現在テストは最小限。Bukkit API をモックするには Mockito + MockBukkit が必要。
