#!/bin/sh

# 启动脚本 - 同时启动Node后端和nginx

echo "=========================================="
echo "   Coco API UI Server 启动中..."
echo "=========================================="

# 启动Node后端服务（在后台运行）
echo "[1/2] 启动Node.js后端服务（端口15088）..."
cd /app/server
node dist/index.js &
NODE_PID=$!

# 等待Node服务启动
echo "等待Node服务就绪..."
sleep 3

# 检查Node进程是否还在运行
if ! kill -0 $NODE_PID 2>/dev/null; then
    echo "错误: Node后端启动失败"
    exit 1
fi

echo "Node后端已启动 (PID: $NODE_PID)"

# 启动nginx
echo "[2/2] 启动nginx..."
nginx -g 'daemon off;' &
NGINX_PID=$!

echo ""
echo "=========================================="
echo "   所有服务已启动"
echo "=========================================="
echo "  Vue前端: http://localhost:80"
echo "  Node后端: http://localhost:15088"
echo "=========================================="
echo ""

# 等待任一进程退出
wait -n

# 如果任一进程退出，终止另一个
kill $NODE_PID 2>/dev/null
kill $NGINX_PID 2>/dev/null

echo "服务已停止"
