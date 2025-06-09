#!/bin/bash

# Livonia Web Server 启动脚本
# 支持参数：
#   -d, --debug : 启用 DEBUG 日志级别
#   -c, --config : 指定 server.xml 配置文件路径
#   -h, --help : 显示帮助信息
#   -v, --version : 显示版本信息

# 设置脚本路径
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="${SCRIPT_DIR}/.."

# 检查项目目录是否存在
if [ ! -d "$PROJECT_DIR" ]; then
    echo "错误：找不到项目目录 $PROJECT_DIR"
    exit 1
fi

# 设置默认值
DEBUG_MODE=""
CONFIG_FILE=""
SHOW_HELP=false
SHOW_VERSION=false
FOREGROUND=false
EXTRA_ARGS=""

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -d|--debug)
            DEBUG_MODE="-d"
            echo "启用 DEBUG 模式"
            shift
            ;;
        -c|--config)
            CONFIG_FILE="$2"
            shift 2
            ;;
        -h|--help)
            SHOW_HELP=true
            shift
            ;;
        -f|--foreground)
            FOREGROUND=true
            shift
            ;;
        -v|--version)
            SHOW_VERSION=true
            shift
            ;;
        *)
            EXTRA_ARGS="$EXTRA_ARGS $1"
            shift
            ;;
    esac
done

# 显示帮助信息
if [ "$SHOW_HELP" = true ]; then
    echo "Livonia Web Server 启动脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -d, --debug       启用 DEBUG 日志级别"
    echo "  -c, --config      指定 server.xml 配置文件路径"
    echo "  -f, --foreground  在前台运行并显示日志"
    echo "  -h, --help        显示此帮助信息"
    echo "  -v, --version     显示版本信息"
    echo ""
    echo "示例:"
    echo "  $0                           # 使用默认配置启动（后台运行）"
    echo "  $0 -f                        # 在前台运行并显示日志"
    echo "  $0 -d                        # 以 DEBUG 模式启动"
    echo "  $0 -c /path/to/server.xml    # 使用指定配置文件启动"
    echo "  $0 -d -c /path/to/server.xml # DEBUG 模式 + 指定配置文件"
    exit 0
fi

# 检查是否已经在运行
PID_FILE="${SCRIPT_DIR}/livonia.pid"
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null 2>&1; then
        echo "Livonia 服务器已经在运行 (PID: $PID)"
        exit 1
    else
        rm -f "$PID_FILE"
    fi
fi

# 进入项目目录
cd "$PROJECT_DIR"

# 检查是否已经编译
if [ ! -d "server/core/classes" ]; then
    echo "项目尚未编译，正在执行 Maven 编译..."
    mvn clean compile
    if [ $? -ne 0 ]; then
        echo "编译失败，请检查错误信息"
        exit 1
    fi
fi

# 设置 CLASSPATH
CLASSPATH="server/core/classes"

# 添加依赖 JAR 包到 CLASSPATH
# 这里使用 Maven 的依赖路径
for jar in $(mvn dependency:build-classpath -q -DincludeScope=compile -Dmdep.outputFile=/dev/stdout); do
    CLASSPATH="$CLASSPATH:$jar"
done

# 添加资源目录
CLASSPATH="$CLASSPATH:src/main/resources"

# 设置 JVM 参数
JVM_OPTS="-Xms256m -Xmx1024m"
JVM_OPTS="$JVM_OPTS -Dfile.encoding=UTF-8"
JVM_OPTS="$JVM_OPTS -Dlivonia.banner.animation=true"

# 如果使用 Java 23，添加预览特性
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d '"' -f 2 | cut -d '.' -f 1)
if [ "$JAVA_VERSION" = "23" ]; then
    JVM_OPTS="$JVM_OPTS --enable-preview"
fi

# 设置核心目录和部署目录
JVM_OPTS="$JVM_OPTS -Dcore.dir=${PROJECT_DIR}"
JVM_OPTS="$JVM_OPTS -Ddeploy.dir=${PROJECT_DIR}"

# 构建启动命令
CMD="java $JVM_OPTS -cp $CLASSPATH livonia.startup.Bootstrap -start"

# 添加调试模式
if [ -n "$DEBUG_MODE" ]; then
    CMD="$CMD $DEBUG_MODE"
fi

# 添加配置文件
if [ -n "$CONFIG_FILE" ]; then
    CMD="$CMD -c $CONFIG_FILE"
else
    # 使用默认配置文件（当前目录下的 server.xml）
    DEFAULT_CONFIG="${SCRIPT_DIR}/server.xml"
    if [ -f "$DEFAULT_CONFIG" ]; then
        CMD="$CMD -c $DEFAULT_CONFIG"
    else
        echo "警告：找不到默认配置文件 $DEFAULT_CONFIG"
        echo "请创建 server.xml 或使用 -c 参数指定配置文件"
        exit 1
    fi
fi

# 添加额外参数
if [ -n "$EXTRA_ARGS" ]; then
    CMD="$CMD $EXTRA_ARGS"
fi

echo "启动 Livonia Web Server"

# 创建日志目录
LOG_DIR="${PROJECT_DIR}/logs"
if [ ! -d "$LOG_DIR" ]; then
    mkdir -p "$LOG_DIR"
fi

# 设置日志文件
LOG_FILE="${LOG_DIR}/livonia-$(date +%Y%m%d-%H%M%S).log"
LATEST_LOG="${LOG_DIR}/livonia.log"

echo "日志文件: $LOG_FILE"
echo ""

# 根据前台/后台模式启动服务器
if [ "$FOREGROUND" = true ]; then
    # 前台运行，实时显示日志
    echo "在前台运行 Livonia Web Server..."
    echo "按 Ctrl+C 停止服务器"
    echo "========================================="
    # 创建最新日志的软链接
    ln -sf "$LOG_FILE" "$LATEST_LOG"
    # 使用 tee 命令同时输出到控制台和日志文件
    exec $CMD 2>&1 | tee "$LOG_FILE"
else
    # 后台运行
    $CMD > "$LOG_FILE" 2>&1 &
    PID=$!
    echo $PID > "$PID_FILE"
    
    # 创建最新日志的软链接
    ln -sf "$LOG_FILE" "$LATEST_LOG"
    
    # 等待服务器启动
    sleep 2
    
    # 检查是否成功启动
    if ps -p $PID > /dev/null 2>&1; then
        echo ""
        echo "Livonia 服务器启动成功 (PID: $PID)"
        echo "日志级别: $([ -n "$DEBUG_MODE" ] && echo "DEBUG" || echo "INFO")"
        echo ""
        echo "访问地址: http://localhost:8080/testServlet/hello"
        echo "查看日志: tail -f ${LATEST_LOG}"
        echo ""
        echo "使用 ./stop.sh 停止服务器"
    else
        echo ""
        echo "Livonia 服务器启动失败"
        rm -f "$PID_FILE"
        exit 1
    fi
fi