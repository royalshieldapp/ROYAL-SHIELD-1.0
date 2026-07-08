# Use Node.js LTS Alpine image for a lightweight runtime
FROM node:18-alpine

# Set working directory
WORKDIR /usr/src/app

# Copy dependency files
COPY package*.json ./

# Install production dependencies only
RUN npm ci --only=production

# Copy application source code
COPY . .

# Expose the API port
EXPOSE 3000

# Define entrypoint
CMD ["node", "server.js"]
