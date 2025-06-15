# 使用 OpenJDK 23 精简版作为基础镜像
FROM openjdk:23-slim

# 设置工作目录
WORKDIR /app/myWebServer/server

# 复制编译后的 JAR 文件
COPY myWebServer/target/myWebServer-1.0-SNAPSHOT.jar app.jar

# 复制服务器配置目录（保持相对路径结构）
COPY myWebServer/server /app/myWebServer/server

# 创建必要的目录
RUN mkdir -p /app/logs

# 暴露默认的 HTTP 端口
EXPOSE 8080

# 运行应用程序，使用服务器配置
CMD ["java", "--enable-preview", "-cp", "app.jar:core/lib/*:common/lib/*", "livonia.startup.Bootstrap", "-start", "-d", "-c", "server.xml"]