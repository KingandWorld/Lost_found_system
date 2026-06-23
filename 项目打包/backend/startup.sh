#!/bin/bash
# ===================================================
# 失物招领系统 — Spring Boot 启动脚本
# 使用方式：bash startup.sh [start|stop|restart|status]
# ===================================================

APP_NAME="lost-found-system"
JAR_FILE="springboot-0.0.1-SNAPSHOT.jar"
LOG_FILE="./logs/${APP_NAME}.log"
PID_FILE="./${APP_NAME}.pid"

# JVM 参数（根据服务器配置调整）
JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Dfile.encoding=UTF-8"

# ==================== 环境变量（⚠️ 必须修改） ====================
export SPRING_SECURITY_PASSWORD="Admin@123!"
# ⚠️ JWT_SECRET 必须是固定值！不要每次启动都随机生成，否则用户 token 会失效
# 生成方式：SSH 执行 openssl rand -base64 48
export JWT_SECRET="CHANGE_ME_GENERATE_WITH_openssl_rand_base64_48"
export DB_PASSWORD="YOUR_DB_PASSWORD_HERE"
export USER_DEFAULT_PASSWORD="123456"
export KNIFE4J_PASSWORD="Admin@123!"

# 创建日志目录
mkdir -p logs

# 检查是否已运行
is_running() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            return 0
        fi
    fi
    return 1
}

start() {
    if is_running; then
        echo "[INFO] ${APP_NAME} 已在运行中 (PID: $(cat $PID_FILE))"
        return
    fi
    echo "[INFO] 正在启动 ${APP_NAME} ..."
    nohup java $JAVA_OPTS \
        -jar "$JAR_FILE" \
        --spring.profiles.active=prod \
        >> "$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"
    sleep 3
    if is_running; then
        echo "[OK] ${APP_NAME} 启动成功 (PID: $(cat $PID_FILE))"
        echo "[INFO] 日志文件: ${LOG_FILE}"
    else
        echo "[ERROR] ${APP_NAME} 启动失败，请查看日志: ${LOG_FILE}"
    fi
}

stop() {
    if ! is_running; then
        echo "[INFO] ${APP_NAME} 未在运行"
        return
    fi
    PID=$(cat "$PID_FILE")
    echo "[INFO] 正在停止 ${APP_NAME} (PID: $PID) ..."
    kill "$PID"
    # 等待最多30秒
    for i in $(seq 1 30); do
        if ! ps -p "$PID" > /dev/null 2>&1; then
            echo "[OK] ${APP_NAME} 已停止"
            rm -f "$PID_FILE"
            return
        fi
        sleep 1
    done
    echo "[WARN] 正常停止超时，强制终止..."
    kill -9 "$PID" 2>/dev/null
    rm -f "$PID_FILE"
    echo "[OK] ${APP_NAME} 已强制停止"
}

restart() {
    stop
    sleep 2
    start
}

status() {
    if is_running; then
        echo "[INFO] ${APP_NAME} 运行中 (PID: $(cat $PID_FILE))"
    else
        echo "[INFO] ${APP_NAME} 未运行"
    fi
}

case "$1" in
    start)   start   ;;
    stop)    stop    ;;
    restart) restart ;;
    status)  status  ;;
    *)
        echo "使用方式: bash startup.sh {start|stop|restart|status}"
        exit 1
        ;;
esac
