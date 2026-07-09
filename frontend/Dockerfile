ARG DOCKER_REGISTRY=
FROM ${DOCKER_REGISTRY}node:18-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json ./

RUN npm config set registry https://mirrors.cloud.tencent.com/npm/

RUN npm ci

COPY . .

RUN npm run build

FROM ${DOCKER_REGISTRY}nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]