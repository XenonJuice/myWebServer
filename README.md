# Livonia Web Server

<div align="center">
  <img src="https://github.com/XenonJuice/myWebServer/workflows/Build%20and%20Test/badge.svg" alt="Build Status">
  <img src="https://img.shields.io/badge/Java-23-orange.svg" alt="Java 23">
  <img src="https://img.shields.io/badge/Servlet-2.5-green.svg" alt="Servlet 2.5">
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="MIT License">
</div>

## 🚀 项目简介

Livonia 是一个基于 Java 实现的轻量级 Web 服务器，采用了与 Apache Tomcat 相似的架构设计，实现了 Servlet 规范的核心功能。项目涵盖了 Web 容器架构、HTTP 协议解析、自定义类加载器、动态部署等关键技术的完整实现。

## ✨ 核心特性

- **完整的 Servlet 容器实现** - 支持 Servlet、Filter、Listener 等核心组件
- **层次化容器架构** - Server → Service → Engine → Host → Context → Endpoint
- **多应用部署** - 单个服务器实例可同时部署运行多个独立的 Web 应用
- **虚拟主机支持** - 支持基于域名的虚拟主机，不同域名访问不同的应用集合
- **动态应用部署** - 支持运行时动态部署/卸载 Web 应用，无需重启服务器
- **自定义类加载器** - 实现 Web 应用隔离，每个应用独立的类空间
- **HTTP/1.1 协议** - 支持持久连接、分块传输编码等特性
- **请求映射与分发** - 实现了完整的请求路由机制
- **XML 配置解析** - 自定义 XML 解析器处理 server.xml 和 web.xml
- **生命周期管理** - 统一的组件生命周期管理机制

## 🏗️ 系统架构

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

## 🛠️ 技术实现

### 1. 网络通信层
- 基于 Java Socket 的网络通信实现
- HTTP/1.1 协议解析与响应生成
- 支持持久连接（Keep-Alive）
- 分块传输编码（Chunked Transfer Encoding）
- 对象池复用处理器线程

### 2. 容器管理
- **Server**: 顶层容器，管理整个服务器实例
- **Service**: 将 Connector 和 Engine 组合在一起
- **Engine**: 请求处理引擎，管理虚拟主机
- **Host**: 虚拟主机，管理 Web 应用
- **Context**: Web 应用上下文
- **Endpoint**: Servlet 管理器，负责 Servlet 的生命周期
- **Channel**: 请求处理通道，管理检查点链
- **Checkpoint**: 请求处理检查点，实现请求拦截和处理

### 3. 请求处理流程
```
HTTP Request → Connector → Processor 
                  ↓
              Engine (Channel → Checkpoints → BasicCheckpoint)
                  ↓
               Host (Channel → Checkpoints → BasicCheckpoint)
                  ↓
             Context (Channel → Checkpoints → BasicCheckpoint)
                  ↓
             Endpoint → FilterChain → Servlet
                  ↓
HTTP Response ← 返回给客户端
```

### 4. 类加载机制
- 自定义 WebAppClassLoader 实现应用隔离
- 遵循双亲委派模型，优先加载 Web 应用类
- 支持热部署和热加载

### 5. 动态部署
- InnerHostListener 监听器定期扫描 webapps 目录
- 自动检测新应用并动态部署
- 支持应用更新检测 ： 通过 web.xml 修改时间以及webAppClassLoader内置检测类更新
- 服务器关闭时，自动检测web应用的变化并自动保存动态部署的应用到配置文件

## 📦 项目结构

```
myWebServer/
├── src/main/java/livonia/
│   ├── base/          # 核心接口定义
│   ├── core/          # 默认实现类
│   ├── connector/     # HTTP 连接器实现
│   ├── checkpoints/   # 检查点实现
│   ├── lifecycle/     # 生命周期管理
│   ├── loader/        # 类加载器实现
│   ├── filter/        # 过滤器链实现
│   ├── listener/      # 监听器实现
│   ├── mapper/        # 请求映射器
│   ├── resource/      # 资源管理
│   ├── utils/         # 工具类
│   └── startup/       # 启动类
├── server/
│   ├── webapps/       # Web 应用部署目录
│   ├── lib/           # 服务器依赖库
│   └── server.xml     # 服务器配置文件
└── testServlet/       # 示例 Web 应用
```

## 🚀 快速开始

### 1. 构建项目
```bash
mvn clean package
```

### 2. 启动服务器
```bash
cd myWebServer/server
./start.sh
```

### 3. 访问示例应用
- http://localhost:8080/testServlet
- http://localhost:8080/app1
- http://localhost:8080/app2

### 4. 动态部署新应用
将符合 Servlet 规范的 Web 应用复制到 `server/webapps/` 目录，服务器将在 10 秒内自动检测并部署。

## 📝 配置文件

### server.xml 示例（多虚拟主机配置）
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Server shutdownPort="8005" shutdownCommand="SHUTDOWN">
    <Service name="testService">
        <Connector port="8080" protocol="HTTP/1.1"/>
        <Engine name="testEngine" defaultHostName="localhost">
            <!-- 默认虚拟主机 -->
            <Host name="localhost" appBase="webapps">
                <Context path="/app1" basePath="simpleApp1"/>
                <Context path="/app2" basePath="simpleApp2"/>
                <Context path="/testServlet" basePath="testServlet"/>
            </Host>
            <!-- 第二个虚拟主机 -->
            <Host name="xenonJuice" appBase="webapps">
                <Context path="/app3" basePath="simpleApp3"/>
                <Context path="/dynamicApp" basePath="dynamicApp"/>
            </Host>
        </Engine>
    </Service>
</Server>
```

### 虚拟主机访问演示

使用 curl 测试不同虚拟主机：

```bash
# 访问默认主机 localhost 的应用
curl http://localhost:8080/app1
curl http://localhost:8080/testServlet

# 使用 Host 头访问第二个虚拟主机
curl -H "Host: xenonJuice" http://localhost:8080/app3
curl -H "Host: xenonJuice" http://localhost:8080/dynamicApp

# 或配置 hosts 文件后直接访问
# echo "127.0.0.1 demo.local" >> /etc/hosts
# curl http://demo.local:8080/app3
```

## 🔧 核心功能展示

### 1. Servlet 支持
```java
public class HelloServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, 
                        HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.getWriter().println("Hello from Livonia!");
    }
}
```

### 2. Filter 链
```java
public class LoggingFilter implements Filter {
    public void doFilter(ServletRequest request, 
                        ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        // 请求前处理
        System.out.println("Request received: " + ((HttpServletRequest)request).getRequestURI());
        chain.doFilter(request, response);
        // 响应后处理
        System.out.println("Response sent");
    }
}
```

### 3. 监听器
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

## 💡 设计亮点

1. **模块化设计** - 各组件职责清晰，易于扩展
2. **设计模式应用** - 责任链、观察者、工厂等模式的合理运用
3. **性能优化** - 对象池、缓存等技术提升性能
4. **健壮性** - 完善的异常处理和资源管理
5. **可配置性** - 灵活的 XML 配置支持

## 🎯 技术难点与解决方案

1. **HTTP 协议解析** - 实现了完整的 HTTP/1.1 请求解析器，支持各种请求方法和头部处理
2. **并发处理** - 使用 HttpProcessor 对象池处理并发请求，实现了线程安全的容器管理
3. **类加载隔离** - 自定义类加载器实现不同 Web 应用间的类隔离
4. **动态部署** - 通过文件系统监听和类加载器重载实现热部署
5. **请求映射** - 实现了 Servlet 规范的 URL 模式匹配算法

## 🎓 技术收获

通过阅读Livonia实现代码，可深入理解：
- Web 服务器的内部工作机制与请求处理流程
- Servlet 容器的完整生命周期管理
- HTTP 协议的底层实现细节
- Java 类加载器的隔离机制与热部署原理
- 多线程并发编程与线程安全设计
- 大型项目的模块化架构设计

## 🔮 未来展望
- **NIO 支持** - 引入 Java NIO 提升并发处理能力
- **SSL/TLS** - 添加 HTTPS 安全连接支持
- **Servlet 3.0+** - 支持异步处理和注解配置


## 📄 License

本项目采用 MIT 许可证，详见 [LICENSE](LICENSE) 文件。

## 👨‍💻 作者

- **XenonJuice** - [GitHub](https://github.com/XenonJuice)

## 🙏 致谢

- 感谢 Apache Tomcat 项目提供的架构参考
- 感谢 Servlet 规范制定者

---

<div align="center">
  <i>如果这个项目对你有帮助，请给个 ⭐ Star！</i>
</div>