# Build React/Vite thành các tệp tĩnh.
FROM node:22-alpine AS build

WORKDIR /app
COPY package*.json ./
RUN npm ci

COPY . .
ARG VITE_API_BASE_URL=/graduration
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

# Nginx phục vụ SPA và reverse proxy API về container backend.
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
