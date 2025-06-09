@echo off
setlocal enabledelayedexpansion

REM Livonia Web Server 启动脚本 (Windows版本)
REM 支持参数：
REM   -d, --debug : 启用 DEBUG 日志级别
REM   -c, --config : 指定 server.xml 配置文件路径
REM   -h, --help : 显示帮助信息
REM   -v, --version : 显示版本信息

REM 设置脚本路径
set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."

REM 检查项目目录是否存在
if not exist "%PROJECT_DIR%" (
    echo 错误：找不到项目目录 %PROJECT_DIR%
    exit /b 1
)

REM 设置默认值
set "DEBUG_MODE="
set "CONFIG_FILE="
set "SHOW_HELP=false"
set "SHOW_VERSION=false"
set "EXTRA_ARGS="

REM 解析命令行参数
:parse_args
if "%~1"=="" goto end_parse
if /i "%~1"=="-d" goto set_debug
if /i "%~1"=="--debug" goto set_debug
if /i "%~1"=="-c" goto set_config
if /i "%~1"=="--config" goto set_config
if /i "%~1"=="-h" goto show_help
if /i "%~1"=="--help" goto show_help
if /i "%~1"=="-v" goto set_version
if /i "%~1"=="--version" goto set_version
set "EXTRA_ARGS=%EXTRA_ARGS% %~1"
shift
goto parse_args

:set_debug
set "DEBUG_MODE=-d"
echo 启用 DEBUG 模式
shift
goto parse_args

:set_config
shift
if "%~1"=="" (
    echo 错误：-c 参数需要指定配置文件路径
    exit /b 1
)
set "CONFIG_FILE=%~1"
shift
goto parse_args

:show_help
echo Livonia Web Server 启动脚本
echo.
echo 用法: %~nx0 [选项]
echo.
echo 选项:
echo   -d, --debug     启用 DEBUG 日志级别
echo   -c, --config    指定 server.xml 配置文件路径
echo   -h, --help      显示此帮助信息
echo   -v, --version   显示版本信息
echo.
echo 示例:
echo   %~nx0                           # 使用默认配置启动
echo   %~nx0 -d                        # 以 DEBUG 模式启动
echo   %~nx0 -c C:\path\to\server.xml  # 使用指定配置文件启动
echo   %~nx0 -d -c C:\path\to\server.xml # DEBUG 模式 + 指定配置文件
exit /b 0

:set_version
set "SHOW_VERSION=true"
shift
goto parse_args

:end_parse

REM 检查是否已经在运行
set "PID_FILE=%SCRIPT_DIR%livonia.pid"
if exist "%PID_FILE%" (
    set /p PID=<"%PID_FILE%"
    REM Windows 下检查进程是否存在
    tasklist /FI "PID eq !PID!" 2>nul | find "!PID!" >nul
    if !errorlevel!==0 (
        echo Livonia 服务器已经在运行 ^(PID: !PID!^)
        exit /b 1
    ) else (
        del "%PID_FILE%"
    )
)

REM 进入项目目录
cd /d "%PROJECT_DIR%"

REM 检查是否已经编译
if not exist "server\core\classes" (
    echo 项目尚未编译，正在执行 Maven 编译...
    call mvn clean compile
    if !errorlevel! neq 0 (
        echo 编译失败，请检查错误信息
        exit /b 1
    )
)

REM 设置 CLASSPATH
set "CLASSPATH=server\core\classes"

REM 添加依赖 JAR 包到 CLASSPATH
for /f "delims=" %%i in ('mvn dependency:build-classpath -q -DincludeScope^=compile -Dmdep.outputFile^=/dev/stdout') do (
    set "CLASSPATH=!CLASSPATH!;%%i"
)

REM 添加资源目录
set "CLASSPATH=!CLASSPATH!;src\main\resources"

REM 设置 JVM 参数
set "JVM_OPTS=-Xms256m -Xmx1024m"
set "JVM_OPTS=%JVM_OPTS% -Dfile.encoding=UTF-8"
set "JVM_OPTS=%JVM_OPTS% -Dlivonia.banner.animation=true"

REM 检查 Java 版本
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION_STRING=%%g
)
set JAVA_VERSION_STRING=%JAVA_VERSION_STRING:"=%
for /f "delims=. tokens=1" %%g in ("%JAVA_VERSION_STRING%") do (
    set JAVA_VERSION=%%g
)
if "%JAVA_VERSION%"=="23" (
    set "JVM_OPTS=%JVM_OPTS% --enable-preview"
)

REM 设置核心目录和部署目录
set "JVM_OPTS=%JVM_OPTS% -Dcore.dir=%PROJECT_DIR%"
set "JVM_OPTS=%JVM_OPTS% -Ddeploy.dir=%PROJECT_DIR%"

REM 构建启动命令
set "CMD=java %JVM_OPTS% -cp %CLASSPATH% livonia.startup.Bootstrap -start"

REM 添加调试模式
if defined DEBUG_MODE (
    set "CMD=%CMD% %DEBUG_MODE%"
)

REM 添加配置文件
if defined CONFIG_FILE (
    set "CMD=%CMD% -c %CONFIG_FILE%"
) else (
    REM 使用默认配置文件
    set "DEFAULT_CONFIG=%SCRIPT_DIR%server.xml"
    if exist "!DEFAULT_CONFIG!" (
        set "CMD=%CMD% -c !DEFAULT_CONFIG!"
    ) else (
        echo 警告：找不到默认配置文件 !DEFAULT_CONFIG!
        echo 请创建 server.xml 或使用 -c 参数指定配置文件
        exit /b 1
    )
)

REM 添加额外参数
if defined EXTRA_ARGS (
    set "CMD=%CMD% %EXTRA_ARGS%"
)

echo 启动 Livonia Web Server

REM 创建日志目录
set "LOG_DIR=%PROJECT_DIR%\logs"
if not exist "%LOG_DIR%" (
    mkdir "%LOG_DIR%"
)

REM 设置日志文件
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c%%a%%b)
for /f "tokens=1-2 delims=/: " %%a in ('time /t') do (set mytime=%%a%%b)
set "LOG_FILE=%LOG_DIR%\livonia-%mydate%-%mytime%.log"
set "LATEST_LOG=%LOG_DIR%\livonia.log"

echo 日志文件: %LOG_FILE%
echo.

REM 启动服务器并保存 PID
start /b cmd /c "%CMD% > "%LOG_FILE%" 2>&1"

REM 获取启动的进程 PID (这在 Windows 下比较复杂)
REM 等待一下让进程启动
timeout /t 2 /nobreak >nul

REM 查找 Java 进程
for /f "tokens=2" %%i in ('tasklist /v ^| findstr /i "livonia.startup.Bootstrap"') do (
    set PID=%%i
    echo !PID! > "%PID_FILE%"
    goto found_pid
)

:found_pid
if defined PID (
    echo.
    echo Livonia 服务器启动成功 ^(PID: !PID!^)
    if defined DEBUG_MODE (
        echo 日志级别: DEBUG
    ) else (
        echo 日志级别: INFO
    )
    echo.
    echo 访问地址: http://localhost:8080/testServlet/hello
    echo 查看日志: type "%LATEST_LOG%"
    echo.
    echo 使用 stop.bat 停止服务器
) else (
    echo.
    echo Livonia 服务器启动失败
    if exist "%PID_FILE%" del "%PID_FILE%"
    exit /b 1
)

endlocal