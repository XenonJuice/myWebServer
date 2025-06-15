<div align="center">

[![English](https://img.shields.io/badge/Language-English-blue?style=for-the-badge)](README.md)
[![简体中文](https://img.shields.io/badge/语言-简体中文-red?style=for-the-badge)](README.zh-CN.md)
[![日本語](https://img.shields.io/badge/言語-日本語-green?style=for-the-badge)](README.ja.md)

</div>

# Livonia Web Server

<div align="center">
  <img src="https://github.com/XenonJuice/myWebServer/workflows/Build%20and%20Test/badge.svg" alt="Build Status">
  <img src="https://img.shields.io/badge/Java-23-orange.svg" alt="Java 23">
  <img src="https://img.shields.io/badge/Servlet-2.5-green.svg" alt="Servlet 2.5">
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="MIT License">
</div>

## 🚀 Project Overview

Livonia is a lightweight web server implementation in Java, featuring an architecture similar to Apache Tomcat, implementing core Servlet specification functionality. The project encompasses complete implementations of web container architecture, HTTP protocol parsing, custom classloaders, and dynamic deployment.

## ✨ Core Features

- **Complete Servlet Container Implementation** - Supports core components including Servlet, Filter, and Listener
- **Hierarchical Container Architecture** - Server → Service → Engine → Host → Context → Endpoint
- **Multi-Application Deployment** - Single server instance can deploy and run multiple independent web applications
- **Virtual Host Support** - Domain-based virtual hosting, different domains access different application sets
- **Dynamic Application Deployment** - Runtime application deployment/undeployment without server restart
- **Custom ClassLoader** - Implements web application isolation with independent class space for each app
- **HTTP/1.1 Protocol** - Supports persistent connections, chunked transfer encoding
- **Request Mapping & Dispatching** - Complete request routing mechanism implementation
- **XML Configuration Parsing** - Custom XML parser for server.xml and web.xml
- **Lifecycle Management** - Unified component lifecycle management mechanism

## 🏗️ System Architecture

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

## 🛠️ Technical Implementation

### 1. Network Communication Layer
- Java Socket-based network communication
- HTTP/1.1 protocol parsing and response generation
- Persistent connection support (Keep-Alive)
- Chunked Transfer Encoding
- Processor thread object pool reuse

### 2. Container Management
- **Server**: Top-level container managing the entire server instance
- **Service**: Groups Connector and Engine together
- **Engine**: Request processing engine managing virtual hosts
- **Host**: Virtual host managing web applications
- **Context**: Web application context
- **Endpoint**: Servlet manager responsible for servlet lifecycle
- **Channel**: Request processing channel managing checkpoint chain
- **Checkpoint**: Request processing checkpoint implementing request interception and processing

### 3. Request Processing Flow
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
HTTP Response ← Return to Client
```

### 4. ClassLoader Mechanism
- Custom WebAppClassLoader implements application isolation
- Follows parent delegation model, prioritizes web application classes
- Supports hot deployment and hot reloading

### 5. Dynamic Deployment
- InnerHostListener periodically scans webapps directory
- Automatically detects and deploys new applications
- Supports application update detection via web.xml modification time and WebAppClassLoader built-in class update detection
- Automatically saves dynamically deployed applications to configuration file on server shutdown

## 📦 Project Structure

```
myWebServer/
├── src/main/java/livonia/
│   ├── base/          # Core interface definitions
│   ├── core/          # Default implementation classes
│   ├── connector/     # HTTP connector implementation
│   ├── checkpoints/   # Checkpoint implementations
│   ├── lifecycle/     # Lifecycle management
│   ├── loader/        # ClassLoader implementation
│   ├── filter/        # Filter chain implementation
│   ├── listener/      # Listener implementation
│   ├── mapper/        # Request mapper
│   ├── resource/      # Resource management
│   ├── utils/         # Utility classes
│   └── startup/       # Startup classes
├── server/
│   ├── webapps/       # Web application deployment directory
│   ├── lib/           # Server dependency libraries
│   └── server.xml     # Server configuration file
└── testServlet/       # Sample web application
```

## 🚀 Quick Start

### 1. Build Project
```bash
mvn clean package
```

### 2. Start Server
```bash
cd myWebServer/server
./start.sh
```

### 3. Access Sample Applications
- http://localhost:8080/testServlet
- http://localhost:8080/app1
- http://localhost:8080/app2

### 4. Deploy New Applications Dynamically
Copy Servlet-compliant web applications to `server/webapps/` directory, server will automatically detect and deploy within 10 seconds.

## 📝 Configuration

### server.xml Example (Multi-Virtual Host Configuration)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Server shutdownPort="8005" shutdownCommand="SHUTDOWN">
    <Service name="testService">
        <Connector port="8080" protocol="HTTP/1.1"/>
        <Engine name="testEngine" defaultHostName="localhost">
            <!-- Default Virtual Host -->
            <Host name="localhost" appBase="webapps">
                <Context path="/app1" basePath="simpleApp1"/>
                <Context path="/app2" basePath="simpleApp2"/>
                <Context path="/testServlet" basePath="testServlet"/>
            </Host>
            <!-- Second Virtual Host -->
            <Host name="xenonJuice" appBase="webapps">
                <Context path="/app3" basePath="simpleApp3"/>
                <Context path="/dynamicApp" basePath="dynamicApp"/>
            </Host>
        </Engine>
    </Service>
</Server>
```

### Virtual Host Access Demo

Test different virtual hosts using curl:

```bash
# Access default host localhost applications
curl http://localhost:8080/app1
curl http://localhost:8080/testServlet

# Access second virtual host using Host header
curl -H "Host: xenonJuice" http://localhost:8080/app3
curl -H "Host: xenonJuice" http://localhost:8080/dynamicApp

# Or access directly after configuring hosts file
# echo "127.0.0.1 demo.local" >> /etc/hosts
# curl http://demo.local:8080/app3
```

## 🔧 Core Functionality Demo

### 1. Servlet Support
```java
public class HelloServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, 
                        HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.getWriter().println("Hello from Livonia!");
    }
}
```

### 2. Filter Chain
```java
public class LoggingFilter implements Filter {
    public void doFilter(ServletRequest request, 
                        ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        // Pre-processing
        System.out.println("Request received: " + ((HttpServletRequest)request).getRequestURI());
        chain.doFilter(request, response);
        // Post-processing
        System.out.println("Response sent");
    }
}
```

### 3. Listeners
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

## 💡 Design Highlights

1. **Modular Design** - Clear component responsibilities, easy to extend
2. **Design Pattern Application** - Appropriate use of Chain of Responsibility, Observer, Factory patterns
3. **Performance Optimization** - Object pooling, caching for improved performance
4. **Robustness** - Comprehensive exception handling and resource management
5. **Configurability** - Flexible XML configuration support

## 🎯 Technical Challenges & Solutions

1. **HTTP Protocol Parsing** - Implemented complete HTTP/1.1 request parser supporting various request methods and header handling
2. **Concurrent Processing** - Uses HttpProcessor object pool for concurrent request handling with thread-safe container management
3. **Class Isolation** - Custom classloader implementation for class isolation between different web applications
4. **Dynamic Deployment** - Hot deployment through filesystem monitoring and classloader reloading
5. **Request Mapping** - Implements Servlet specification URL pattern matching algorithm

## 🐳 Docker Support

Livonia provides complete Docker containerization support for easy deployment and testing.

### Quick Start with Docker

```bash
# Build and run with Docker Compose
docker-compose up --build

# Run in background
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop container
docker-compose down
```

### Docker Configuration

The project includes:
- **Dockerfile** - Defines container image with OpenJDK 23 and all necessary dependencies
- **docker-compose.yml** - Orchestrates container with volume mounts for webapp hot deployment
- **.dockerignore** - Optimizes build context by excluding unnecessary files

### Development Workflow

1. **Local Development**: Code changes require rebuild
   ```bash
   mvn clean package
   docker-compose down
   docker-compose up --build
   ```

2. **Webapp Changes**: Automatically reflected via volume mounts
   - Changes to files in `webapps/` directory are immediately available
   - No container restart needed for static content

## 🎓 Learning Outcomes

By studying Livonia's implementation code, you can deeply understand:
- Internal working mechanisms and request processing flow of web servers
- Complete lifecycle management of Servlet containers
- Low-level implementation details of HTTP protocol
- Java classloader isolation mechanisms and hot deployment principles
- Multi-threaded concurrent programming and thread-safe design
- Modular architecture design for large projects

## 🔮 Future Roadmap
- **NIO Support** - Introduce Java NIO for improved concurrent processing
- **SSL/TLS** - Add HTTPS secure connection support
- **Servlet 3.0+** - Support asynchronous processing and annotation configuration
- **Server Clustering** - Support multi-instance clustering

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

- **XenonJuice** - [GitHub](https://github.com/XenonJuice)

## 🙏 Acknowledgments

- Thanks to Apache Tomcat project for architectural reference
- Thanks to Servlet specification creators

---

<div align="center">
  <i>If this project helps you, please give it a ⭐ Star!</i>
</div>