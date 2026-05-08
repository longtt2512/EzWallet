# ============================================================================
# EzWallet Frontend - Multi-stage Dockerfile
# ============================================================================

# ----- Stage 1: Build -----
FROM node:20-alpine AS builder
WORKDIR /build

COPY package.json package-lock.json* ./
RUN npm ci || npm install

COPY . .
RUN npm run build:prod

# ----- Stage 2: Runtime (Nginx) -----
FROM nginx:1.25-alpine
COPY --from=builder /build/dist/ezwallet/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
