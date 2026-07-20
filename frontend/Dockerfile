# Étape 1 — build Vite (bundle statique)
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Étape 2 — image d'exécution nginx, même famille que le site vitrine
# (autoecoleconnect-infra/platform/site-vitrine) : plus de serveur Node à
# faire tourner en continu, juste des fichiers statiques.
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
