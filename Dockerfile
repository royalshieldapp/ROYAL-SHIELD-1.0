# Multi-stage build for Node.js backend

# Stage 1: Build
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production

# Stage 2: Runtime
FROM node:18-alpine

WORKDIR /app

# Set environment to production
ENV NODE_ENV=production

# Copy node_modules and app from builder
COPY --from=builder /app/node_modules ./node_modules

# Copy application code
COPY src ./src
COPY package*.json ./

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD node -e "require('http').get('http://localhost:' + (process.env.PORT || 3000) + '/health', (r) => {if (r.statusCode !== 200) throw new Error(r.statusCode)})"

# Expose port
EXPOSE 3000

# Start application
CMD ["node", "src/index.js"]
