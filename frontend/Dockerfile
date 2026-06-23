FROM node:20-alpine
WORKDIR /app

# Copy manifests first — Docker layer cache avoids re-installing deps on code-only changes
COPY package.json package-lock.json ./

RUN npm ci --prefer-offline

# Copy remaining source after deps are cached
COPY . .

EXPOSE 5173

# Vite reads VITE_API_TARGET from process.env at server startup (not build time),
# so the value injected by Docker Compose is picked up correctly at runtime.
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
