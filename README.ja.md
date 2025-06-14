<div align="center">

[![English](https://img.shields.io/badge/Language-English-blue?style=for-the-badge)](README.md)
[![简体中文](https://img.shields.io/badge/语言-简体中文-red?style=for-the-badge)](README.zh-CN.md)
[![日本語](https://img.shields.io/badge/言語-日本語-green?style=for-the-badge)](README.ja.md)

</div>

# Livonia Webサーバー

<div align="center">
  <img src="https://github.com/XenonJuice/myWebServer/workflows/Build%20and%20Test/badge.svg" alt="Build Status">
  <img src="https://img.shields.io/badge/Java-23-orange.svg" alt="Java 23">
  <img src="https://img.shields.io/badge/Servlet-2.5-green.svg" alt="Servlet 2.5">
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="MIT License">
</div>

## 🚀 プロジェクト概要

LivoniaはJavaで実装された軽量なWebサーバーで、Apache Tomcatと類似したアーキテクチャ設計を採用し、Servlet仕様のコア機能を実装しています。このプロジェクトは、Webコンテナアーキテクチャ、HTTPプロトコル解析、カスタムクラスローダー、動的デプロイメントなどの重要な技術の完全な実装を含んでいます。

## ✨ コア機能

- **完全なServletコンテナ実装** - Servlet、Filter、Listenerなどのコアコンポーネントをサポート
- **階層型コンテナアーキテクチャ** - Server → Service → Engine → Host → Context → Endpoint
- **マルチアプリケーションデプロイメント** - 単一のサーバーインスタンスで複数の独立したWebアプリケーションを同時にデプロイ・実行可能
- **バーチャルホストサポート** - ドメインベースのバーチャルホスティング、異なるドメインで異なるアプリケーションセットにアクセス
- **動的アプリケーションデプロイメント** - サーバー再起動なしでランタイムアプリケーションのデプロイ/アンデプロイをサポート
- **カスタムクラスローダー** - 各アプリケーション独立のクラス空間でWebアプリケーション分離を実現
- **HTTP/1.1プロトコル** - 持続的接続、チャンク転送エンコーディングをサポート
- **リクエストマッピング＆ディスパッチング** - 完全なリクエストルーティングメカニズムの実装
- **XML設定解析** - server.xmlとweb.xml用のカスタムXMLパーサー
- **ライフサイクル管理** - 統一されたコンポーネントライフサイクル管理メカニズム

## 🏗️ システムアーキテクチャ

```
┌─────────────────────────────────────────────────────┐
│                     Server                          │
│  ┌───────────────────────────────────────────────┐  │
│  │                  Service                      │  │
│  │  ┌─────────────┐    ┌─────────────────────┐   │  │
│  │  │  Connector  │───▶│      Engine         │   │  │
│  │  │  (HTTP/1.1) │    │  ┌───────────────┐  │   │  │
│  │  └─────────────┘    │  │     Host      │  │   │  │
│  │                     │  │  ┌─────────┐  │  │   │  │
│  │                     │  │  │ Context │  │  │   │  │
│  │                     │  │  │┌─────────┐ │  │   │  │ 
│  │                     │  │  ││Endpoint │ │  │   │  │ 
│  │                     │  │  │└─────────┘ │  │   │  │  
│  │                     │  │  └─────────┘  │  │   │  │
│  │                     │  └───────────────┘  │   │  │
│  │                     └─────────────────────┘   │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

## 🛠️ 技術実装

### 1. ネットワーク通信層
- Java Socketベースのネットワーク通信実装
- HTTP/1.1プロトコルの解析とレスポンス生成
- 持続的接続サポート（Keep-Alive）
- チャンク転送エンコーディング（Chunked Transfer Encoding）
- プロセッサースレッドオブジェクトプールの再利用

### 2. コンテナ管理
- **Server**: サーバーインスタンス全体を管理するトップレベルコンテナ
- **Service**: ConnectorとEngineをグループ化
- **Engine**: バーチャルホストを管理するリクエスト処理エンジン
- **Host**: Webアプリケーションを管理するバーチャルホスト
- **Context**: Webアプリケーションコンテキスト
- **Endpoint**: サーブレットのライフサイクルを管理するサーブレットマネージャー
- **Channel**: チェックポイントチェーンを管理するリクエスト処理チャネル
- **Checkpoint**: リクエストの傍受と処理を実装するリクエスト処理チェックポイント

### 3. リクエスト処理フロー
```
HTTPリクエスト → Connector → Processor 
                  ↓
              Engine (Channel → Checkpoints → BasicCheckpoint)
                  ↓
               Host (Channel → Checkpoints → BasicCheckpoint)
                  ↓
             Context (Channel → Checkpoints → BasicCheckpoint)
                  ↓
             Endpoint → FilterChain → Servlet
                  ↓
HTTPレスポンス ← クライアントに返す
```

### 4. クラスローダーメカニズム
- カスタムWebAppClassLoaderがアプリケーション分離を実装
- 親委譲モデルに従い、Webアプリケーションクラスを優先的にロード
- ホットデプロイメントとホットリロードをサポート

### 5. 動的デプロイメント
- InnerHostListenerが定期的にwebappsディレクトリをスキャン
- 新しいアプリケーションを自動的に検出してデプロイ
- web.xmlの変更時刻とWebAppClassLoader内蔵のクラス更新検出によるアプリケーション更新検出をサポート
- サーバーシャットダウン時に動的デプロイされたアプリケーションを設定ファイルに自動保存

## 📦 プロジェクト構造

```
myWebServer/
├── src/main/java/livonia/
│   ├── base/          # コアインターフェース定義
│   ├── core/          # デフォルト実装クラス
│   ├── connector/     # HTTPコネクタ実装
│   ├── checkpoints/   # チェックポイント実装
│   ├── lifecycle/     # ライフサイクル管理
│   ├── loader/        # クラスローダー実装
│   ├── filter/        # フィルターチェーン実装
│   ├── listener/      # リスナー実装
│   ├── mapper/        # リクエストマッパー
│   ├── resource/      # リソース管理
│   ├── utils/         # ユーティリティクラス
│   └── startup/       # 起動クラス
├── server/
│   ├── webapps/       # Webアプリケーションデプロイメントディレクトリ
│   ├── lib/           # サーバー依存ライブラリ
│   └── server.xml     # サーバー設定ファイル
└── testServlet/       # サンプルWebアプリケーション
```

## 🚀 クイックスタート

### 1. プロジェクトビルド
```bash
mvn clean package
```

### 2. サーバー起動
```bash
cd myWebServer/server
./start.sh
```

### 3. サンプルアプリケーションへのアクセス
- http://localhost:8080/testServlet
- http://localhost:8080/app1
- http://localhost:8080/app2

### 4. 新規アプリケーションの動的デプロイ
Servlet仕様準拠のWebアプリケーションを`server/webapps/`ディレクトリにコピーすると、サーバーは10秒以内に自動的に検出してデプロイします。

## 📝 設定

### server.xml例（マルチバーチャルホスト設定）
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Server shutdownPort="8005" shutdownCommand="SHUTDOWN">
    <Service name="testService">
        <Connector port="8080" protocol="HTTP/1.1"/>
        <Engine name="testEngine" defaultHostName="localhost">
            <!-- デフォルトバーチャルホスト -->
            <Host name="localhost" appBase="webapps">
                <Context path="/app1" basePath="simpleApp1"/>
                <Context path="/app2" basePath="simpleApp2"/>
                <Context path="/testServlet" basePath="testServlet"/>
            </Host>
            <!-- 第二バーチャルホスト -->
            <Host name="xenonJuice" appBase="webapps">
                <Context path="/app3" basePath="simpleApp3"/>
                <Context path="/dynamicApp" basePath="dynamicApp"/>
            </Host>
        </Engine>
    </Service>
</Server>
```

### バーチャルホストアクセスデモ

curlを使用して異なるバーチャルホストをテスト：

```bash
# デフォルトホストlocalhostのアプリケーションにアクセス
curl http://localhost:8080/app1
curl http://localhost:8080/testServlet

# Hostヘッダーを使用して第二バーチャルホストにアクセス
curl -H "Host: xenonJuice" http://localhost:8080/app3
curl -H "Host: xenonJuice" http://localhost:8080/dynamicApp

# またはhostsファイル設定後に直接アクセス
# echo "127.0.0.1 demo.local" >> /etc/hosts
# curl http://demo.local:8080/app3
```

## 🔧 コア機能デモ

### 1. サーブレットサポート
```java
public class HelloServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, 
                        HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.getWriter().println("Hello from Livonia!");
    }
}
```

### 2. フィルターチェーン
```java
public class LoggingFilter implements Filter {
    public void doFilter(ServletRequest request, 
                        ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        // 前処理
        System.out.println("Request received: " + ((HttpServletRequest)request).getRequestURI());
        chain.doFilter(request, response);
        // 後処理
        System.out.println("Response sent");
    }
}
```

### 3. リスナー
```java
public class AppContextListener implements ServletContextListener {
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Application started: " + sce.getServletContext().getContextPath());
    }
    
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Application stopped: " + sce.getServletContext().getContextPath());
    }
}
```

## 💡 設計のハイライト

1. **モジュラー設計** - 明確なコンポーネント責任、拡張が容易
2. **デザインパターンの適用** - Chain of Responsibility、Observer、Factoryパターンの適切な使用
3. **パフォーマンス最適化** - オブジェクトプーリング、キャッシングによるパフォーマンス向上
4. **堅牢性** - 包括的な例外処理とリソース管理
5. **構成可能性** - 柔軟なXML設定サポート

## 🎯 技術的課題と解決策

1. **HTTPプロトコル解析** - 様々なリクエストメソッドとヘッダー処理をサポートする完全なHTTP/1.1リクエストパーサーを実装
2. **並行処理** - スレッドセーフなコンテナ管理でHttpProcessorオブジェクトプールを使用した並行リクエスト処理
3. **クラス分離** - 異なるWebアプリケーション間のクラス分離のためのカスタムクラスローダー実装
4. **動的デプロイメント** - ファイルシステム監視とクラスローダーリロードによるホットデプロイメント
5. **リクエストマッピング** - Servlet仕様のURLパターンマッチングアルゴリズムを実装

## 🎓 学習成果

Livoniaの実装コードを学習することで、以下を深く理解できます：
- Webサーバーの内部動作メカニズムとリクエスト処理フロー
- Servletコンテナの完全なライフサイクル管理
- HTTPプロトコルの低レベル実装詳細
- Javaクラスローダーの分離メカニズムとホットデプロイメントの原理
- マルチスレッド並行プログラミングとスレッドセーフ設計
- 大規模プロジェクトのモジュラーアーキテクチャ設計

## 🔮 今後のロードマップ
- **NIOサポート** - 並行処理能力向上のためJava NIOを導入
- **SSL/TLS** - HTTPSセキュア接続サポートを追加
- **Servlet 3.0+** - 非同期処理とアノテーション設定をサポート
- **サーバークラスタリング** - マルチインスタンスクラスタリングをサポート

## 📄 ライセンス

このプロジェクトはMITライセンスの下でライセンスされています - 詳細は[LICENSE](LICENSE)ファイルを参照してください。

## 👨‍💻 作者

- **XenonJuice** - [GitHub](https://github.com/XenonJuice)

## 🙏 謝辞

- アーキテクチャ参考のためのApache Tomcatプロジェクトに感謝
- Servlet仕様作成者に感謝

---

<div align="center">
  <i>このプロジェクトが役立った場合は、⭐ Starをお願いします！</i>
</div>