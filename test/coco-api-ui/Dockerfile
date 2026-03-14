# 多阶段构建 Dockerfile
# Node.js后端构建 + 生产镜像（Vue前端直接使用预构建的dist）

# ==================== 阶段1: 构建Node后端 ====================
FROM node:20-alpine AS backend-builder

WORKDIR /app/backend

# 安装pnpm
RUN npm install -g pnpm@9

# 复制后端依赖文件
COPY server/package.json ./
COPY server/tsconfig.json ./

# 安装后端依赖
RUN pnpm install

# 复制后端源码
COPY server/src ./src

# 构建TypeScript
RUN pnpm build

# ==================== 阶段2: 生产镜像 ====================
FROM nginx:alpine AS production

# 安装Node.js和npm（用于运行后端服务）
RUN apk add --no-cache nodejs npm

# 安装pnpm
RUN npm install -g pnpm@9

# 创建工作目录
WORKDIR /app

# 复制nginx配置
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf

# 复制Vue前端构建产物（已在build-docker.bat中构建好）
COPY dist /usr/share/nginx/html

# 复制Node后端构建产物
COPY server/dist ./server/dist
COPY server/package.json ./server/

# 安装后端生产依赖
WORKDIR /app/server
RUN pnpm install --prod

# 回到应用根目录
WORKDIR /app

# 复制启动脚本
COPY docker/start.sh ./start.sh
RUN chmod +x ./start.sh

# 暴露端口
# 80 - nginx (Vue前端)
# 15088 - Node后端（固定端口）
EXPOSE 80 15088

# 启动服务
CMD ["./start.sh"]
