# Livonia Web Server

## 目录结构

```
server/
├── start.sh          # 启动脚本
├── stop.sh           # 停止脚本
├── server.xml        # 服务器配置文件
├── core/            # 服务器核心
│   ├── classes/     # 核心类文件
│   └── lib/         # 核心依赖库
├── common/          # 共享库
│   └── lib/         # 共享依赖（如 servlet-api）
└── webapps/         # Web 应用部署目录
    └── testServlet/ # 示例应用
```

## 使用方法

### 编译项目

```bash
# 编译服务器核心
cd ../
mvn clean compile

# 编译测试应用
cd ../../testServlet
mvn clean compile
```

### 启动服务器

```bash
cd web/server
./start.sh
```

### 停止服务器

```bash
./stop.sh
```

### 访问应用

http://localhost:8080/testServlet/hello