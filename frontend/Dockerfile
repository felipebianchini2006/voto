# Multi-stage Dockerfile for React/Vite Frontend
# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY package.json package-lock.json* ./

# Remove rolldown-vite override and use standard Vite 5
RUN npm pkg delete overrides && \
    npm pkg set devDependencies.vite="^5.4.11" && \
    rm -f package-lock.json

# Install all dependencies (including devDependencies for build)
RUN npm install

# Copy source code
COPY . .

# Build application
RUN npm run build

# Stage 2: Production with Nginx
FROM nginx:1.25-alpine AS runtime

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built assets from builder
COPY --from=builder /app/dist /usr/share/nginx/html

# Add healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:80/ || exit 1

# Expose port
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
