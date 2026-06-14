> English version: [README.md](README.md)

# Wi-Fi Analyzer with Mist & Aruba Central Integration

ネットワークエンジニア向けのAndroid Wi-Fiアナライザーアプリです。リアルタイムなBSSIDスキャン、Juniper Mist / HPE Aruba Central API経由のAP名解決、スナップショットベースの現地サーベイ機能を備えています。

## 機能

### Wi-Fiスキャン
- RSSI・チャネル・チャネル幅・セキュリティ・バンドを含むリアルタイムBSSIDスキャン
- IEEE OUIデータベース（39,000件以上）によるベンダー判定
- SSID・バンド（2.4/5/6 GHz）・セキュリティ種別でのフィルタ
- RSSI・SSID・チャネルでのソート
- 間隔指定（5秒 / 10秒 / 30秒）の自動スキャン
- OSスロットリング検知とキャッシュ結果の表示

### AP名解決
- **Juniper Mist**: radio MACプレフィックスのマッチングでAP名を解決
  - Org全体 / 特定Site単位の同期に対応
  - クラウドリージョン選択（Global 01-04, EMEA 01-03, APAC 01）
- **HPE Aruba Central**: BSSID完全一致でAP名を解決
  - ODataフィルタ構文によるSiteフィルタに対応
  - 複数クラスタ対応（Internal, US, EU, APAC, Canada, UAE, China）
  - OAuth2トークンの自動リフレッシュ

### Monitor画面
- 接続中APのRSSI・TX/RX速度・チャネルをリアルタイムグラフ表示
- 1秒ごとに自動更新

### RSSI比較
- 長押しで複数BSSIDを選択
- 色分け凡例付きのマルチシリーズRSSIトレンドグラフ

### スナップショットとエクスポート
- スキャン結果を場所・フロア情報付きの名前付きスナップショットとして保存
- AP名を含む全BSSID詳細のCSVエクスポート
- Android共有シート経由での共有

## インストール

[Releases](https://github.com/kshimonoj/wifi-analyzer-mist-central/releases) ページから最新のデバッグAPKをダウンロードします。

1. Androidの設定で **「提供元不明のアプリ」** を許可
2. 最新リリースから `wifi-analyzer-*-debug.apk` をダウンロード
3. APKをタップしてインストール
4. 初回起動時に **位置情報** の権限を許可（Wi-Fiスキャンに必須）

## Androidアプリの使い方

アプリ下部のナビゲーションバーには5つのタブがあります: **Scan**, **Snapshots**, **Monitor**, **Map**, **Settings**。

### タブ概要

#### 📡 Scan
周辺のBSSIDをRSSI・チャネル・バンド・セキュリティ・解決済みAP名とともにリアルタイム一覧表示します。上部のフィルタ・ソートで一覧を絞り込めます。自動スキャンをオンにすると一定間隔で更新されます。

<!-- SCREENSHOT: Scan画面 — スキャン一覧のスクショをここに配置 -->
<p align="center">
  <img src="docs/images/scan.png" alt="Scan画面" width="300">
</p>

#### 💾 Snapshots
保存したスナップショットの一覧です。各スナップショットはその時点の全スキャン結果を、場所・フロア情報（任意）とともに保持します。ここからサーベイZIPのエクスポートや一括削除ができます。

<!-- SCREENSHOT: Snapshots画面 — スナップショット一覧のスクショをここに配置 -->
<p align="center">
  <img src="docs/images/snapshots.png" alt="Snapshots画面" width="300">
</p>

#### 📈 Monitor
現在接続中のAPについて、RSSI・TX/RX速度・チャネルを1秒ごとに更新するリアルタイムグラフを表示します。ライブのリンク品質の観察に便利です。

<!-- SCREENSHOT: Monitor画面 — モニターグラフのスクショをここに配置 -->
<p align="center">
  <img src="docs/images/monitor.png" alt="Monitor画面" width="300">
</p>

#### 🗺️ Map
フロアマップを読み込み（Mist API / Aruba Central API / ローカルファイル）、AP位置をプロットし、タップでスナップショットをマップ上に配置できます。接続線が各スナップショットと接続中APを結びます。現地サーベイワークフローの中核機能です。

<!-- SCREENSHOT: Map画面 — スナップショット配置済みフロアマップのスクショをここに配置 -->
<p align="center">
  <img src="docs/images/map.png" alt="Map画面" width="300">
</p>

#### ⚙️ Settings
Mist・Aruba Central APIの認証情報の設定、クラウドリージョン/クラスタの選択、AP名の同期を行います。下記の [セットアップ](#セットアップ) を参照してください。

<!-- SCREENSHOT: Settings画面 — 設定画面のスクショをここに配置 -->
<p align="center">
  <img src="docs/images/settings.png" alt="Settings画面" width="300">
</p>

### 標準的なサーベイの流れ

Wi-Fi現地サーベイを実施し、結果を分析するまでの一連の流れです:

1. **AP名解決の設定**（Settingsタブ）
   MistまたはAruba Centralの認証情報を入力し、**Sync APs** をタップして、スキャンしたBSSIDがわかりやすいAP名に解決されるようにします。

2. **フロアマップの読み込み**（Mapタブ）
   Mist / Aruba Central API またはローカル画像ファイルからフロアプランを読み込みます。AP位置情報が取得できる場合は自動的にプロットされます。

3. **スキャンとスナップショット配置**（Scan → Mapタブ）
   各測定ポイントに移動してスキャンし、スナップショットとして保存後、立っている場所をタップしてフロアマップ上に配置します。

<!-- SCREENSHOT: マップへのスナップショット配置 — 配置操作のスクショをここに配置 -->
<p align="center">
  <img src="docs/images/placing-snapshot.png" alt="スナップショット配置" width="300">
</p>

4. **サーベイZIPのエクスポート**（Mapタブ → Export）
   フロアマップ・AP位置・全スナップショットを含むZIPをエクスポートします。

5. **Webツールで分析**
   ZIPを [Survey Analyzer](#survey-analyzer-webツール) にアップロードして、RSSIヒートマップ・ローミング分析・同一チャネル干渉・最適AP接続チェックを確認します。

> **スナップショット配置に関する注意:** スナップショットの位置はフロアマップの座標系で保存されます。フロアマップを再読み込み・更新した場合は、位置を揃えるために既存のスナップショットを再配置してください。

## 動作要件

- Android 10（API 29）以降
- 位置情報の権限（Wi-Fiスキャンに必須）

## テスト済みデバイス

| デバイス | Androidバージョン | 備考 |
|--------|----------------|-------|
| Pixel 9 | Android 16 | 主要テスト端末 |

## 技術スタック

- **言語**: Kotlin
- **UI**: Jetpack Compose + Material3
- **アーキテクチャ**: MVVM
- **DI**: Hilt
- **データベース**: Room
- **HTTP**: Ktor Client
- **グラフ**: Vico
- **非同期**: Coroutines + Flow
- **設定**: DataStore Preferences

## セットアップ

### Mist API
1. アプリのSettings画面を開く
2. クラウドリージョンを選択
3. Mist APIトークンを入力
4. 「Test Connection」をタップ
5. OrgとSiteを選択
6. 「Sync APs」をタップ

### Aruba Central API
1. アプリのSettings画面を開く
2. クラスタを選択
3. Client IDとClient Secret（HPE GreenLakeから取得）を入力
4. 「Test Connection」をタップ
5. Siteを選択（任意）
6. 「Sync APs」をタップ

## リポジトリ構成

このリポジトリには2つのコンポーネントが含まれます:

- ルートディレクトリ + `app/` — Android Wi-Fiアナライザーアプリ
- `analyzer/` — Webベースのサーベイアナライザー（Streamlit + Docker）

Androidプロジェクトファイル（build.gradle.kts, settings.gradle.kts など）はルートにあります。アナライザーは独立したサブディレクトリ内のスタンドアロンツールです。

```
analyzer/
├── Dockerfile
├── docker-compose.yml
├── requirements.txt
└── app/
    ├── app.py          # Streamlitメインアプリ
    ├── analysis.py     # 分析ロジック
    └── map_plot.py     # フロアマップ可視化
```

## アーキテクチャ

```
app/
├── data/
│   ├── wifi/          # WifiScanner, ScanResultMapper, ConnectedApMonitor
│   ├── mist/          # MistApiClient, MistRepository
│   ├── aruba/         # ArubaApiClient, ArubaRepository
│   ├── oui/           # OuiVendorRepository
│   ├── db/            # Room Database, DAOs, Entities
│   └── settings/      # DataStore SettingsRepository
├── domain/
│   ├── model/         # WifiObservation, BssidSummary, Snapshot
│   └── usecase/       # ExportCsvUseCase
└── ui/
    ├── scan/          # ScanScreen, ScanViewModel
    ├── monitor/       # MonitorScreen, MonitorViewModel
    ├── compare/       # CompareGraphScreen
    ├── snapshot/      # SnapshotListScreen, SnapshotViewModel
    ├── map/           # FloorMapScreen, MapViewModel
    └── settings/      # SettingsScreen, SettingsViewModel
```

## Survey Analyzer（Webツール）

本アプリからエクスポートしたサーベイデータを分析するStreamlitベースのツールです。

```bash
cd analyzer
docker-compose up
# http://localhost:8501 を開く
```

サーベイZIPをアップロードすると以下を確認できます:
- 測定ポイントと接続線を含むフロアマップ
- AP単位のRSSI集計（BSSID単位ではない）
- ローミング検知とローミング問題のフラグ
- ポイントごとの同一チャネル干渉（管理AP vs 管理外AP）
- 最適AP接続チェック（接続中 vs 最適な選択肢）
- 2.4/5/6 GHzバンド分析

詳細は [analyzer/README.md](analyzer/README.md) を参照してください。

## 既知の制限

### Wi-Fiスキャンのスロットリング（Android 9以降）

Android 9以降では、Wi-Fiスキャン頻度にOSレベルの制限があります:

- **フォアグラウンド**: 2分あたり最大4回
- **バックグラウンド**: 30分あたり最大1回

制限に達すると、アプリは前回のスキャンのキャッシュ結果を表示し、警告バナー **「Scan throttled by OS – showing cached results」** を表示します。

補足:
- 他のアプリが実行するWi-Fiスキャンも同じ割り当てを消費するため、混雑した環境ではスロットリングが起きやすくなります。
- 短い間隔（例: 5秒）での自動スキャンは、制限に早く到達しやすくなります。
- スロットリングの厳しさは、デバイスメーカーやAndroidバージョンによって異なる場合があります。

## ライセンス

MIT License - 詳細は [LICENSE](LICENSE) を参照してください。

## 謝辞

- ベンダー判定に [IEEE OUI Database](https://standards-oui.ieee.org/)
- グラフ描画に [Vico](https://github.com/patrykandpatrick/vico)
- [Juniper Mist API](https://www.mist.com/documentation/)
- [HPE Aruba Central API](https://developer.arubanetworks.com/new-central/)
