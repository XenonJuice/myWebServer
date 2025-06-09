@echo off
setlocal enabledelayedexpansion

REM Livonia Web Server 停止脚本 (Windows版本)

REM 设置脚本路径
set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."
set "PID_FILE=%SCRIPT_DIR%livonia.pid"

REM 创建日志目录
set "LOG_DIR=%PROJECT_DIR%\logs"
if not exist "%LOG_DIR%" (
    mkdir "%LOG_DIR%"
)

REM 设置日志文件
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c%%a%%b)
for /f "tokens=1-2 delims=/: " %%a in ('time /t') do (set mytime=%%a%%b)
set "STOP_LOG=%LOG_DIR%\livonia-stop-%mydate%-%mytime%.log"
set "LATEST_STOP_LOG=%LOG_DIR%\livonia-stop.log"

REM 日志函数 (使用 call :log "message")
goto :skip_log_function
:log
echo %~1
echo %~1 >> "%STOP_LOG%"
goto :eof
:skip_log_function

REM 创建最新日志的链接
if exist "%LATEST_STOP_LOG%" del "%LATEST_STOP_LOG%"
echo. > "%LATEST_STOP_LOG%"

call :log "停止 Livonia Web Server"

REM 检查 PID 文件是否存在
if not exist "%PID_FILE%" (
    call :log "找不到 PID 文件，Livonia 服务器可能没有运行"
    
    REM 尝试通过进程名查找
    call :log "尝试查找 Livonia 进程..."
    
    set "FOUND_PIDS="
    for /f "tokens=2" %%i in ('tasklist /v ^| findstr /i "livonia.startup.Bootstrap"') do (
        set "FOUND_PIDS=!FOUND_PIDS! %%i"
        call :log "  PID: %%i"
    )
    
    if defined FOUND_PIDS (
        call :log ""
        set /p "REPLY=是否要停止这些进程？(y/n) "
        if /i "!REPLY!"=="y" (
            for %%p in (!FOUND_PIDS!) do (
                call :log "停止进程 %%p ..."
                taskkill /PID %%p /F >> "%STOP_LOG%" 2>&1
            )
            call :log "已发送停止信号"
        )
    ) else (
        call :log "没有找到正在运行的 Livonia 进程"
    )
    exit /b 0
)

REM 读取 PID
set /p PID=<"%PID_FILE%"

REM 检查进程是否存在
tasklist /FI "PID eq %PID%" 2>nul | find "%PID%" >nul
if !errorlevel! neq 0 (
    call :log "进程 %PID% 不存在，清理 PID 文件"
    del "%PID_FILE%"
    exit /b 0
)

call :log "找到 Livonia 服务器进程 (PID: %PID%)"

REM 尝试使用 shutdown 命令停止（如果配置了）
set "DEFAULT_CONFIG=%SCRIPT_DIR%server.xml"
if exist "%DEFAULT_CONFIG%" (
    call :log "尝试通过 shutdown 命令优雅停止服务器..."
    
    REM 进入项目目录
    cd /d "%PROJECT_DIR%"
    
    REM 设置 CLASSPATH
    set "CLASSPATH=server\core\classes"
    
    REM 添加依赖 JAR 包
    for /f "delims=" %%i in ('mvn dependency:build-classpath -q -DincludeScope^=compile -Dmdep.outputFile^=/dev/stdout 2^>nul') do (
        set "CLASSPATH=!CLASSPATH!;%%i"
    )
    
    REM 添加 server 目录（包含 logback.xml）
    set "CLASSPATH=!CLASSPATH!;%SCRIPT_DIR%"
    
    REM 设置 JVM 参数
    set "JVM_OPTS="
    for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        set JAVA_VERSION_STRING=%%g
    )
    set JAVA_VERSION_STRING=!JAVA_VERSION_STRING:"=!
    for /f "delims=. tokens=1" %%g in ("!JAVA_VERSION_STRING!") do (
        set JAVA_VERSION=%%g
    )
    if "!JAVA_VERSION!"=="23" (
        set "JVM_OPTS=!JVM_OPTS! --enable-preview"
    )
    
    REM 设置核心目录
    set "JVM_OPTS=!JVM_OPTS! -Dcore.dir=%PROJECT_DIR%"
    set "JVM_OPTS=!JVM_OPTS! -Ddeploy.dir=%PROJECT_DIR%"
    
    REM 执行停止命令
    REM 执行停止命令（不显示详细命令）
    java !JVM_OPTS! -cp !CLASSPATH! livonia.startup.Bootstrap -stop -c "%DEFAULT_CONFIG%" >> "%STOP_LOG%" 2>&1
    
    REM 等待服务器停止
    call :log "等待服务器停止..."
    for /l %%i in (1,1,10) do (
        tasklist /FI "PID eq %PID%" 2>nul | find "%PID%" >nul
        if !errorlevel! neq 0 (
            call :log "Livonia 服务器已成功停止"
            del "%PID_FILE%"
            call :log ""
            call :log "停止日志保存在: %STOP_LOG%"
            exit /b 0
        )
        timeout /t 1 /nobreak >nul
    )
)

REM 如果 shutdown 命令失败，使用 taskkill 命令
call :log "使用 taskkill 命令停止服务器..."
taskkill /PID %PID% /F >> "%STOP_LOG%" 2>&1

REM 等待进程结束
call :log "等待进程结束..."
for /l %%i in (1,1,10) do (
    tasklist /FI "PID eq %PID%" 2>nul | find "%PID%" >nul
    if !errorlevel! neq 0 (
        call :log "Livonia 服务器已停止"
        del "%PID_FILE%"
        call :log ""
        call :log "停止日志保存在: %STOP_LOG%"
        exit /b 0
    )
    timeout /t 1 /nobreak >nul
)

REM 如果进程仍在运行
tasklist /FI "PID eq %PID%" 2>nul | find "%PID%" >nul
if !errorlevel!==0 (
    call :log "错误：无法停止进程 %PID%"
    exit /b 1
) else (
    call :log "Livonia 服务器已停止"
    del "%PID_FILE%"
    call :log ""
    call :log "停止日志保存在: %STOP_LOG%"
)

endlocal