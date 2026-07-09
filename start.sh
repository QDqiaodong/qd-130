#!/bin/bash

if [ -f .env ]; then
    source .env
fi

FRONTEND_PORT=${FRONTEND_PORT:-8230}
BACKEND_PORT=${BACKEND_PORT:-8330}
MYSQL_PORT=${MYSQL_PORT:-3530}
REDIS_PORT=${REDIS_PORT:-6630}

check_port() {
    if lsof -Pi :$1 -sTCP:LISTEN -t >/dev/null ; then
        return 0
    else
        return 1
    fi
}

get_port_owner() {
    lsof -Pi :$1 -sTCP:LISTEN -t | head -1 | xargs -I{} ps -p {} -o comm= 2>/dev/null
}

find_available_port() {
    local port=$1
    while check_port $port; do
        port=$((port + 1))
    done
    echo $port
}

echo "检查端口占用情况..."

OCCUPIED_PORTS=()

if check_port $FRONTEND_PORT; then
    OWNER=$(get_port_owner $FRONTEND_PORT)
    echo "❌ 端口 $FRONTEND_PORT 已被占用 (进程: $OWNER)"
    OCCUPIED_PORTS+=("FRONTEND_PORT=$FRONTEND_PORT")
fi

if check_port $BACKEND_PORT; then
    OWNER=$(get_port_owner $BACKEND_PORT)
    echo "❌ 端口 $BACKEND_PORT 已被占用 (进程: $OWNER)"
    OCCUPIED_PORTS+=("BACKEND_PORT=$BACKEND_PORT")
fi

if check_port $MYSQL_PORT; then
    OWNER=$(get_port_owner $MYSQL_PORT)
    echo "❌ 端口 $MYSQL_PORT 已被占用 (进程: $OWNER)"
    OCCUPIED_PORTS+=("MYSQL_PORT=$MYSQL_PORT")
fi

if check_port $REDIS_PORT; then
    OWNER=$(get_port_owner $REDIS_PORT)
    echo "❌ 端口 $REDIS_PORT 已被占用 (进程: $OWNER)"
    OCCUPIED_PORTS+=("REDIS_PORT=$REDIS_PORT")
fi

if [ ${#OCCUPIED_PORTS[@]} -gt 0 ]; then
    echo ""
    echo "可用候选端口:"
    NEW_FRONTEND=$(find_available_port 8230)
    NEW_BACKEND=$(find_available_port 8330)
    NEW_MYSQL=$(find_available_port 3530)
    NEW_REDIS=$(find_available_port 6630)
    echo "  前端: $NEW_FRONTEND"
    echo "  后端: $NEW_BACKEND"
    echo "  MySQL: $NEW_MYSQL"
    echo "  Redis: $NEW_REDIS"
    
    read -p "是否使用候选端口更新配置并继续启动? (y/n): " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "更新 .env 配置..."
        sed -i.bak "s/FRONTEND_PORT=.*/FRONTEND_PORT=$NEW_FRONTEND/" .env
        sed -i.bak "s/BACKEND_PORT=.*/BACKEND_PORT=$NEW_BACKEND/" .env
        sed -i.bak "s/MYSQL_PORT=.*/MYSQL_PORT=$NEW_MYSQL/" .env
        sed -i.bak "s/REDIS_PORT=.*/REDIS_PORT=$NEW_REDIS/" .env
        rm -f .env.bak
        
        FRONTEND_PORT=$NEW_FRONTEND
        BACKEND_PORT=$NEW_BACKEND
        MYSQL_PORT=$NEW_MYSQL
        REDIS_PORT=$NEW_REDIS
    else
        echo "退出启动，请手动释放端口或修改 .env 配置"
        exit 1
    fi
fi

echo ""
echo "最终使用端口:"
echo "  前端: $FRONTEND_PORT"
echo "  后端: $BACKEND_PORT"
echo "  MySQL: $MYSQL_PORT"
echo "  Redis: $REDIS_PORT"

FRONTEND_PORT=$FRONTEND_PORT BACKEND_PORT=$BACKEND_PORT MYSQL_PORT=$MYSQL_PORT REDIS_PORT=$REDIS_PORT docker-compose up -d --build

echo ""
echo "服务启动完成!"
echo "前端访问地址: http://localhost:$FRONTEND_PORT"
