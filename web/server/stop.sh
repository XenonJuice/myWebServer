#!/bin/bash

# Livonia Web Server 停止脚本

# 设置脚本路径
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="${SCRIPT_DIR}/.."
PID_FILE="${SCRIPT_DIR}/livonia.pid"

# 创建日志目录
LOG_DIR="${PROJECT_DIR}/logs"
if [ ! -d "$LOG_DIR" ]; then
    mkdir -p "$LOG_DIR"
fi

# 设置日志文件
STOP_LOG="${LOG_DIR}/livonia-stop-$(date +%Y%m%d-%H%M%S).log"
LATEST_STOP_LOG="${LOG_DIR}/livonia-stop.log"

# 创建日志函数
log() {
    echo "$1" | tee -a "$STOP_LOG"
}

# 创建最新日志的软链接
ln -sf "$STOP_LOG" "$LATEST_STOP_LOG"

log "停止 Livonia Web Server"

# 检查 PID 文件是否存在
if [ ! -f "$PID_FILE" ]; then
    log "找不到 PID 文件，Livonia 服务器可能没有运行"
    
    # 尝试通过进程名查找
    log "尝试查找 Livonia 进程..."
    PIDS=$(ps aux | grep "livonia.startup.Bootstrap" | grep -v grep | awk '{print $2}')
    
    if [ -n "$PIDS" ]; then
        log "找到以下 Livonia 进程："
        for PID in $PIDS; do
            log "  PID: $PID"
        done
        log ""
        read -p "是否要停止这些进程？(y/n) " -n 1 -r
        echo ""
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            for PID in $PIDS; do
                log "停止进程 $PID ..."
                kill $PID 2>&1 | tee -a "$STOP_LOG"
            done
            log "已发送停止信号"
        fi
    else
        log "没有找到正在运行的 Livonia 进程"
    fi
    exit 0
fi

# 读取 PID
PID=$(cat "$PID_FILE")

# 检查进程是否存在
if ! ps -p $PID > /dev/null 2>&1; then
    log "进程 $PID 不存在，清理 PID 文件"
    rm -f "$PID_FILE"
    exit 0
fi

log "找到 Livonia 服务器进程 (PID: $PID)"

# 尝试使用 shutdown 命令停止（如果配置了）
DEFAULT_CONFIG="${SCRIPT_DIR}/server.xml"
if [ -f "$DEFAULT_CONFIG" ]; then
    log "尝试通过 shutdown 命令优雅停止服务器..."
    
    # 进入项目目录
    cd "$PROJECT_DIR"
    
    # 设置 CLASSPATH
    CLASSPATH="server/core/classes"
    
    # 添加依赖 JAR 包
    for jar in $(mvn dependency:build-classpath -q -DincludeScope=compile -Dmdep.outputFile=/dev/stdout 2>/dev/null); do
        CLASSPATH="$CLASSPATH:$jar"
    done
    
    # 添加资源目录
    CLASSPATH="$CLASSPATH:src/main/resources"
    
    # 设置 JVM 参数
    JVM_OPTS=""
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d '"' -f 2 | cut -d '.' -f 1)
    if [ "$JAVA_VERSION" = "23" ]; then
        JVM_OPTS="$JVM_OPTS --enable-preview"
    fi
    
    # 设置核心目录
    JVM_OPTS="$JVM_OPTS -Dcore.dir=${PROJECT_DIR}"
    JVM_OPTS="$JVM_OPTS -Ddeploy.dir=${PROJECT_DIR}"
    
    # 执行停止命令
    # 执行停止命令（不显示详细命令）
    java $JVM_OPTS -cp $CLASSPATH livonia.startup.Bootstrap -stop -c $DEFAULT_CONFIG 2>&1 | tee -a "$STOP_LOG"
    
    # 等待服务器停止
    log "等待服务器停止..."
    for i in {1..10}; do
        if ! ps -p $PID > /dev/null 2>&1; then
            log "Livonia 服务器已成功停止"
            rm -f "$PID_FILE"
            log "停止日志保存在: $STOP_LOG"
            exit 0
        fi
        sleep 1
    done
fi

# 如果 shutdown 命令失败，使用 kill 命令
log "使用 kill 命令停止服务器..."
kill $PID 2>&1 | tee -a "$STOP_LOG"

# 等待进程结束
log "等待进程结束..."
for i in {1..10}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        log "Livonia 服务器已停止"
        rm -f "$PID_FILE"
        log "停止日志保存在: $STOP_LOG"
        exit 0
    fi
    sleep 1
done

# 如果进程仍在运行，强制终止
log "进程未响应，强制终止..."
kill -9 $PID 2>&1 | tee -a "$STOP_LOG"
sleep 1

if ps -p $PID > /dev/null 2>&1; then
    log "错误：无法停止进程 $PID"
    exit 1
else
    log "Livonia 服务器已强制停止"
    rm -f "$PID_FILE"
    log "停止日志保存在: $STOP_LOG"
fi