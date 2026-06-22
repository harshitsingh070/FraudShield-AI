FROM node:20-alpine
WORKDIR /app

# Copy package.json and package-lock.json if they exist
COPY package*.json ./

# Install dependencies (will skip if no package.json exists yet)
RUN if [ -f "package.json" ]; then npm install; fi

# Copy all source files
COPY . .

EXPOSE 5173

# Run Vite dev server by default, binding to all interfaces
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
