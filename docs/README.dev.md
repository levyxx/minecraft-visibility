# 開発者ガイド

## 開発環境セットアップ

### 必要なもの

- **Java 17+** (JDK)
- **Maven 3.8+**
- IDE (IntelliJ IDEA / VS Code 推奨)

### ビルド

```bash
mvn clean package
```

生成物: `target/minecraft-visibility-1.0.0.jar`

### テスト

```bash
mvn test
```

## プロジェクト構成

```
minecraft-visibility/
├── pom.xml                        # Maven 設定
├── AGENTS.md                      # AI エージェント向けガイド
├── README.md                      # ユーザー向け README
├── docs/
│   ├── ARCHITECTURE.md            # アーキテクチャ設計書
│   └── README.dev.md              # 本ファイル（開発者ガイド）
└── src/
    └── main/
        ├── java/visibility/
        │   ├── VisibilityPlugin.java      # エントリポイント
        │   ├── command/
        │   │   └── VisibilityCommand.java # コマンドハンドラ
        │   ├── i18n/
        │   │   └── Messages.java          # 多言語メッセージ
        │   ├── listener/
        │   │   └── PlayerListener.java    # イベントリスナー
        │   └── manager/
        │       └── VisibilityManager.java # ビジネスロジック
        └── resources/
            └── plugin.yml                 # Bukkit プラグイン定義
```

## コーディング規約

### Java

- **Java 17** の機能を活用する
  - パターンマッチング `instanceof` (`if (sender instanceof Player player)`)
  - `switch` 式
- `ConcurrentHashMap` でスレッドセーフを確保
- Bukkit API の `showPlayer` / `hidePlayer` 呼出時は必ず `Plugin` インスタンスを渡す

### メッセージ追加手順

1. `Messages.java` に新メソッドを追加
2. `get(id, "日本語", "English")` で JP/EN 両方を定義
3. 呼出元で `Messages.newMethod(playerId)` のように使用

### コマンド追加手順

1. `plugin.yml` にコマンド定義を追加
2. `VisibilityPlugin.onEnable()` に `registerCommand("name", executor)` を追加
3. `VisibilityCommand.onCommand()` の `switch` に分岐追加
4. 必要に応じてタブ補完も対応

## 重要な設計制約

### クールダウン (1秒)

表示 / 非表示 / 距離変更は各プレイヤーにつき **1秒間隔** でしか実行できません。  
`VisibilityManager.isOnCooldown()` と `markAction()` で制御しています。

- コマンド経由 (`VisibilityCommand`) とアイテムクリック経由 (`PlayerListener`) の両方でチェック
- `System.currentTimeMillis()` ベースの簡易実装

### コリジョン（常時無効）

全プレイヤーの当たり判定は **常時無効** です。トグル機能はありません。

- プレイヤー参加時にスコアボードチーム `vis_no_collide` に自動追加
- `Team.Option.COLLISION_RULE = NEVER` で実現
- プレイヤー退出時にチームから削除

### 距離タスク

距離閾値を持つプレイヤーが**1人以上**存在する場合のみ、10 tick (0.5秒) 間隔の繰り返しタスクが稼働します。該当プレイヤーがいなくなるとタスクは自動停止します。

### アイテムドロップ防止

プラグインのユーティリティアイテム（Totem / Barrier）は `PersistentDataContainer` の `NamespacedKey` でタグ付けされており、ドロップイベントがキャンセルされます。

## デプロイ

1. `mvn clean package` を実行
2. `target/minecraft-visibility-1.0.0.jar` を Spigot/Paper サーバーの `plugins/` ディレクトリにコピー
3. サーバーを再起動（または `/reload confirm`）

## トラブルシューティング

| 症状 | 対処 |
|------|------|
| コマンドが動かない | `plugin.yml` にコマンドが定義されているか確認 |
| アイテムが機能しない | `NamespacedKey` の一致を確認 |
| コリジョンが無効にならない | スコアボードチーム `vis_no_collide` の存在を確認 |
| 距離フィルタが反映されない | プレイヤーが `hiding` モードか確認 |
